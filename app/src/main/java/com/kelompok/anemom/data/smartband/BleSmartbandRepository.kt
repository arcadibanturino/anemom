package com.kelompok.anemom.data.smartband

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Smartband asli lewat Bluetooth Low Energy (ESP32-C3 + MAX30102, firmware/anemom_smartband).
 *
 * Protokol (harus sama dengan firmware):
 *  - Control (write) 1 byte: 0x01 mulai rekam, 0x00 berhenti.
 *  - PPG (notify): seq u16 LE, count u8, flags u8 (bit0 kulit menempel, bit1 jenuh),
 *    lalu per sampel red u24 LE + ir u24 LE. Butuh MTU >= 67, jadi MTU diminta 247.
 *  - Battery Service standar 0x180F / 0x2A19.
 *
 * Nilai yang dikirim ke layar dan disimpan adalah nilai MENTAH sensor (intensitas pantulan);
 * gelombangnya terbalik (turun saat denyut), lihat [waveInverted].
 */
@SuppressLint("MissingPermission") // izin dicek di hasPermissions() sebelum setiap akses
class BleSmartbandRepository(private val context: Context) : SmartbandRepository {

    override val isSimulated: Boolean = false
    override val sampleRateHz: Int = SAMPLE_RATE_HZ
    override val waveInverted: Boolean = true
    override val requiredPermissions: Array<String> = permissions()

    private val adapter: BluetoothAdapter?
        get() = context.getSystemService(BluetoothManager::class.java)?.adapter

    private val _bluetoothEnabled = MutableStateFlow(adapter?.isEnabled == true)
    override val bluetoothEnabled: StateFlow<Boolean> = _bluetoothEnabled.asStateFlow()

    private val _connected = MutableStateFlow<SmartbandInfo?>(null)
    override val connectedDevice: StateFlow<SmartbandInfo?> = _connected.asStateFlow()

    /** Nama + RSSI dari pindai terakhir, dipakai saat connect. */
    private val lastScan = ConcurrentHashMap<String, SmartbandInfo>()

    @Volatile private var gatt: BluetoothGatt? = null
    @Volatile private var connectWaiter: CompletableDeferred<Unit>? = null
    @Volatile private var pending: CompletableDeferred<Any?>? = null
    private val opMutex = Mutex()

    private sealed interface BandEvent {
        class Packet(val bytes: ByteArray) : BandEvent
        data object Disconnected : BandEvent
    }

    private val events = MutableSharedFlow<BandEvent>(extraBufferCapacity = 256)

    init {
        // Pantau Bluetooth dinyalakan/dimatikan dari luar aplikasi.
        ContextCompat.registerReceiver(
            context.applicationContext,
            object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _bluetoothEnabled.value = state == BluetoothAdapter.STATE_ON
                    if (state == BluetoothAdapter.STATE_OFF) onLinkLost()
                }
            },
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    // ---------------- Izin & Bluetooth ----------------

    override fun hasPermissions(): Boolean = requiredPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun setBluetoothEnabled(enabled: Boolean) {
        if (!hasPermissions()) return
        // Android 13+ melarang aplikasi menyalakan/mematikan Bluetooth langsung: minta lewat dialog sistem.
        val intent = if (enabled) Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        else Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // ---------------- Pindai ----------------

    override suspend fun scan(): List<SmartbandInfo> {
        if (!hasPermissions()) throw SecurityException("Izin perangkat sekitar belum diberikan.")
        val scanner = adapter?.bluetoothLeScanner ?: return emptyList()
        val found = LinkedHashMap<String, SmartbandInfo>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val address = result.device.address
                val name = result.scanRecord?.deviceName ?: result.device.name ?: "Anemom Band"
                synchronized(found) {
                    found[address] = SmartbandInfo(id = address, name = name, signal = signalOf(result.rssi))
                }
            }
        }
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        // Android 8-11: pindai BLE juga butuh Lokasi (GPS) menyala, kalau tidak hasilnya kosong.
        scanner.startScan(listOf(filter), settings, callback)
        try {
            delay(SCAN_MILLIS)
        } finally {
            runCatching { scanner.stopScan(callback) }
        }
        val list = synchronized(found) { found.values.toList() }
        list.forEach { lastScan[it.id] = it }
        return list
    }

    // ---------------- Sambung / putus ----------------

    override suspend fun connect(deviceId: String): Result<SmartbandInfo> = runCatching {
        if (!hasPermissions()) throw SecurityException("Izin perangkat sekitar belum diberikan.")
        val bt = adapter?.takeIf { it.isEnabled } ?: throw IllegalStateException("Bluetooth mati.")
        disconnect()

        val device: BluetoothDevice = bt.getRemoteDevice(deviceId)
        val waiter = CompletableDeferred<Unit>()
        connectWaiter = waiter
        gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
            ?: throw IllegalStateException("Gagal membuka sambungan.")

        try {
            withTimeout(CONNECT_TIMEOUT_MILLIS) { waiter.await() }
            gattOp<Int> { it.requestMtu(MTU) }
            gattOp<Unit> { it.discoverServices() }
            val service = gatt?.getService(SERVICE_UUID)
                ?: throw IllegalStateException("Perangkat ini bukan smartband Anemom.")
            if (service.getCharacteristic(PPG_UUID) == null || service.getCharacteristic(CONTROL_UUID) == null) {
                throw IllegalStateException("Firmware smartband tidak sesuai.")
            }
            val battery = readBattery()
            val info = (lastScan[deviceId] ?: SmartbandInfo(id = deviceId, name = "Anemom Band"))
                .copy(batteryPercent = battery, lastSyncAtMillis = System.currentTimeMillis())
            _connected.value = info
            info
        } catch (e: Throwable) {
            closeGatt()
            throw if (e is kotlinx.coroutines.TimeoutCancellationException) {
                IllegalStateException("Smartband tidak merespons. Dekatkan perangkat lalu coba lagi.")
            } else e
        } finally {
            connectWaiter = null
        }
    }

    override suspend fun disconnect() {
        val g = gatt ?: return
        runCatching { g.disconnect() }
        delay(300)
        closeGatt()
    }

    // ---------------- Rekam PPG ----------------

    override fun streamPpg(): Flow<PpgChunk> = channelFlow {
        val g = gatt ?: throw IllegalStateException("Smartband belum tersambung.")
        val service = g.getService(SERVICE_UUID) ?: throw IllegalStateException("Layanan smartband tidak ada.")
        val ppg = service.getCharacteristic(PPG_UUID)
        val control = service.getCharacteristic(CONTROL_UUID)

        // Mulai mendengar sebelum sensor dinyalakan agar paket pertama tidak hilang.
        launch(start = CoroutineStart.UNDISPATCHED) {
            var lastSeq = -1
            events.collect { ev ->
                when (ev) {
                    is BandEvent.Packet -> parse(ev.bytes, lastSeq)?.let { (seq, chunk) ->
                        lastSeq = seq
                        send(chunk)
                    }
                    BandEvent.Disconnected -> close(IllegalStateException("Smartband terputus."))
                }
            }
        }

        try {
            setNotify(ppg, true)
            writeControl(control, START)
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                runCatching { writeControl(control, STOP) }
                runCatching { setNotify(ppg, false) }
            }
        }
    }

    /** Ubah satu paket notify menjadi potongan sampel. Mengembalikan (seq, chunk). */
    private fun parse(b: ByteArray, lastSeq: Int): Pair<Int, PpgChunk>? {
        if (b.size < 4) return null
        val seq = u8(b, 0) or (u8(b, 1) shl 8)
        val count = u8(b, 2)
        val flags = u8(b, 3)
        if (b.size < 4 + count * 6) return null

        val red = ArrayList<Float>(count)
        val ir = ArrayList<Float>(count)
        for (i in 0 until count) {
            val o = 4 + i * 6
            red += u24(b, o).toFloat()
            ir += u24(b, o + 3).toFloat()
        }
        val lost = lastSeq >= 0 && seq != (lastSeq + 1) and 0xFFFF
        val quality = when {
            flags and 0x01 == 0 -> PpgQuality.LEMAH   // sensor tidak menempel kulit
            flags and 0x02 != 0 || lost -> PpgQuality.SEDANG
            else -> PpgQuality.BAIK
        }
        return seq to PpgChunk(red = red, ir = ir, quality = quality)
    }

    // ---------------- Simpan / kirim rekaman ----------------

    /**
     * TODO(backend): endpoint belum ada. Usulan POST screenings/ppg
     *  {device_id, sample_rate_hz, started_at, red[], ir[]}.
     * Sementara rekaman disimpan sebagai CSV di HP agar bisa dikumpulkan untuk data latih:
     *  Android/data/com.kelompok.anemom/files/ppg/ (buka lewat kabel USB).
     */
    override suspend fun uploadRecording(recording: PpgRecording): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "ppg").apply { mkdirs() }
            val file = File(dir, "ppg_${recording.startedAtMillis}.csv")
            file.bufferedWriter().use { w ->
                w.appendLine("# device_id=${recording.deviceId}")
                w.appendLine("# sample_rate_hz=${recording.sampleRateHz}")
                w.appendLine("# started_at_millis=${recording.startedAtMillis}")
                w.appendLine("index,red,ir")
                for (i in recording.red.indices) {
                    w.append(i.toString()).append(',')
                        .append(recording.red[i].toLong().toString()).append(',')
                        .append(recording.ir.getOrNull(i)?.toLong()?.toString() ?: "")
                        .append('\n')
                }
            }
            file.name
        }
    }

    // ---------------- GATT ----------------

    /** Satu perintah GATT pada satu waktu; hasilnya datang dari callback. */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> gattOp(start: (BluetoothGatt) -> Boolean): T = opMutex.withLock {
        val g = gatt ?: throw IllegalStateException("Smartband belum tersambung.")
        val d = CompletableDeferred<Any?>()
        pending = d
        try {
            if (!start(g)) throw IllegalStateException("Perintah Bluetooth ditolak.")
            withTimeout(OP_TIMEOUT_MILLIS) { d.await() } as T
        } finally {
            pending = null
        }
    }

    private suspend fun readBattery(): Int? = runCatching {
        val ch = gatt?.getService(BATTERY_SERVICE)?.getCharacteristic(BATTERY_LEVEL) ?: return null
        val v = gattOp<ByteArray> { it.readCharacteristic(ch) }
        v.firstOrNull()?.toInt()?.and(0xFF)
    }.getOrNull()

    @Suppress("DEPRECATION")
    private suspend fun writeControl(ch: BluetoothGattCharacteristic, value: Byte) {
        val bytes = byteArrayOf(value)
        val type = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        gattOp<Unit> { g ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeCharacteristic(ch, bytes, type) == BluetoothGatt.GATT_SUCCESS
            } else {
                ch.writeType = type
                ch.value = bytes
                g.writeCharacteristic(ch)
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun setNotify(ch: BluetoothGattCharacteristic, enable: Boolean) {
        val g = gatt ?: throw IllegalStateException("Smartband belum tersambung.")
        g.setCharacteristicNotification(ch, enable)
        val cccd = ch.getDescriptor(CCCD_UUID) ?: return
        val value = if (enable) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        gattOp<Unit> { gg ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gg.writeDescriptor(cccd, value) == BluetoothGatt.GATT_SUCCESS
            } else {
                cccd.value = value
                gg.writeDescriptor(cccd)
            }
        }
    }

    private fun finish(status: Int, value: Any?) {
        val d = pending ?: return
        if (status == BluetoothGatt.GATT_SUCCESS) d.complete(value)
        else d.completeExceptionally(IllegalStateException("Galat Bluetooth (status $status)."))
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                connectWaiter?.complete(Unit)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                connectWaiter?.completeExceptionally(IllegalStateException("Sambungan gagal (status $status)."))
                if (gatt === g) onLinkLost()
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) = finish(status, mtu)

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) = finish(status, Unit)

        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) =
            finish(status, Unit)

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) =
            finish(status, Unit)

        // Android 13+
        override fun onCharacteristicRead(
            g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray, status: Int
        ) = finish(status, value)

        @Deprecated("Android 12 ke bawah")
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) =
            finish(status, c.value ?: ByteArray(0))

        // Android 13+
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) {
            if (c.uuid == PPG_UUID) events.tryEmit(BandEvent.Packet(value))
        }

        @Deprecated("Android 12 ke bawah")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            if (c.uuid == PPG_UUID) events.tryEmit(BandEvent.Packet(c.value?.copyOf() ?: return))
        }
    }

    private fun onLinkLost() {
        pending?.completeExceptionally(IllegalStateException("Smartband terputus."))
        events.tryEmit(BandEvent.Disconnected)
        closeGatt()
    }

    private fun closeGatt() {
        val g = gatt
        gatt = null
        _connected.value = null
        runCatching { g?.close() }
    }

    private fun signalOf(rssi: Int): SignalStrength = when {
        rssi >= -65 -> SignalStrength.KUAT
        rssi >= -80 -> SignalStrength.SEDANG
        else -> SignalStrength.LEMAH
    }

    private fun u8(b: ByteArray, i: Int) = b[i].toInt() and 0xFF
    private fun u24(b: ByteArray, i: Int) = u8(b, i) or (u8(b, i + 1) shl 8) or (u8(b, i + 2) shl 16)

    companion object {
        const val SAMPLE_RATE_HZ = 50
        private const val MTU = 247
        private const val SCAN_MILLIS = 4_000L
        private const val CONNECT_TIMEOUT_MILLIS = 10_000L
        private const val OP_TIMEOUT_MILLIS = 5_000L
        private const val START: Byte = 0x01
        private const val STOP: Byte = 0x00

        // Sama dengan firmware/anemom_smartband/anemom_smartband.ino
        val SERVICE_UUID: UUID = UUID.fromString("6e0a0001-7a3b-4c1e-9f00-616e656d6f6d")
        val CONTROL_UUID: UUID = UUID.fromString("6e0a0002-7a3b-4c1e-9f00-616e656d6f6d")
        val PPG_UUID: UUID = UUID.fromString("6e0a0003-7a3b-4c1e-9f00-616e656d6f6d")
        val BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private fun permissions(): Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
    }
}

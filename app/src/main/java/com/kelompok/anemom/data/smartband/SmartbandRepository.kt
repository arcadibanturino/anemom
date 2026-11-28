package com.kelompok.anemom.data.smartband

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Smartband Anemom yang terlihat oleh aplikasi (hasil pindai atau yang sedang tersambung). */
data class SmartbandInfo(
    /** ID perangkat. Untuk BLE asli: alamat MAC, mis. "AA:BB:CC:DD:EE:FF". */
    val id: String,
    val name: String,
    val batteryPercent: Int? = null,
    val lastSyncAtMillis: Long? = null,
    val signal: SignalStrength = SignalStrength.KUAT
)

/** Kekuatan sinyal Bluetooth (RSSI) antara ponsel dan smartband. */
enum class SignalStrength(val label: String) {
    KUAT("Sinyal kuat"),
    SEDANG("Sinyal sedang"),
    LEMAH("Sinyal lemah")
}

/** Kualitas sinyal PPG (kontak sensor dengan kulit, gerakan, cahaya luar). */
enum class PpgQuality(val label: String) {
    BAIK("Baik"),
    SEDANG("Sedang"),
    LEMAH("Lemah")
}

/**
 * Potongan sinyal PPG yang dikirim smartband berkala selama merekam.
 *
 * Sensor MAX30102 punya dua kanal: cahaya merah (red) dan inframerah (ir). Perbandingan
 * penyerapan keduanya yang nanti dipakai server untuk estimasi Hb, jadi keduanya disimpan.
 */
data class PpgChunk(
    val red: List<Float>,
    val ir: List<Float>,
    val quality: PpgQuality
)

/** Satu rekaman utuh yang siap dikirim ke server. */
class PpgRecording(
    val deviceId: String,
    val sampleRateHz: Int,
    val startedAtMillis: Long,
    val red: FloatArray,
    val ir: FloatArray
) {
    val durationSeconds: Int get() = red.size / sampleRateHz
}

/**
 * Kontrak smartband. Layar Koneksi Bluetooth dan Rekam PPG hanya bergantung pada interface ini.
 *
 * Sekarang dipakai [SimulatedSmartbandRepository]. Saat perangkat IoT (ESP32-C3 + MAX30102) siap:
 *  1. Buat `BleSmartbandRepository : SmartbandRepository` (BluetoothLeScanner + BluetoothGatt).
 *  2. Ganti satu baris `smartbandRepository` di AppContainer.
 * Layar dan ViewModel tidak perlu diubah.
 */
interface SmartbandRepository {

    /** true selama data berasal dari simulasi; layar menampilkan label "Data simulasi". */
    val isSimulated: Boolean

    /** Frekuensi sampel PPG (Hz). */
    val sampleRateHz: Int

    val waveInverted: Boolean get() = false

    val requiredPermissions: Array<String> get() = emptyArray()

    fun hasPermissions(): Boolean = true

    val bluetoothEnabled: StateFlow<Boolean>

    /** Smartband yang sedang tersambung, atau null. */
    val connectedDevice: StateFlow<SmartbandInfo?>

    /**
     * Menyalakan/mematikan Bluetooth.
     * TODO(ble): Android 13+ tidak mengizinkan aplikasi menyalakan Bluetooth langsung. Dari layar,
     *  jalankan Intent BluetoothAdapter.ACTION_REQUEST_ENABLE, lalu perbarui [bluetoothEnabled]
     *  lewat BroadcastReceiver BluetoothAdapter.ACTION_STATE_CHANGED.
     */
    fun setBluetoothEnabled(enabled: Boolean)

    /**
     * Pindai smartband di sekitar selama beberapa detik. Hasil boleh berisi perangkat yang
     * sedang tersambung; layar yang menyaringnya.
     * TODO(ble): butuh izin BLUETOOTH_SCAN + BLUETOOTH_CONNECT (Android 12+) atau
     *  ACCESS_FINE_LOCATION (Android 8-11). Saring hanya perangkat dengan UUID layanan Anemom.
     */
    suspend fun scan(): List<SmartbandInfo>

    suspend fun connect(deviceId: String): Result<SmartbandInfo>

    suspend fun disconnect()

    /**
     * Aliran sinyal PPG. Mulai dikumpulkan = sensor mulai merekam; dibatalkan = sensor berhenti.
     * Melempar exception bila smartband terputus di tengah rekaman.
     * TODO(ble): aktifkan notifikasi karakteristik PPG di GATT, ubah byte menjadi sampel red/ir.
     *  Nilai mentah MAX30102 adalah intensitas cahaya yang TURUN saat denyut, jadi gelombangnya
     *  terbalik; balik tandanya sebelum dikirim ke layar.
     */
    fun streamPpg(): Flow<PpgChunk>

    /**
     * Kirim rekaman ke server untuk dianalisis. Mengembalikan ID rekaman/skrining.
     * TODO(backend): endpoint belum ada. Usulan: POST screenings/ppg
     *  {device_id, sample_rate_hz, started_at, red[], ir[]}, lalu diteruskan ke ml-service
     *  seperti alur /predict/eye.
     */
    suspend fun uploadRecording(recording: PpgRecording): Result<String>
}

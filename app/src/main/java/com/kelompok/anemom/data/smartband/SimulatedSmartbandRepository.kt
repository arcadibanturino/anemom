package com.kelompok.anemom.data.smartband

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * SIMULASI smartband: tanpa Bluetooth dan tanpa sensor. Dipakai sampai perangkat IoT siap.
 * Gelombang PPG dibuat dari rumus (puncak sistolik + takik dikrotik), bukan data asli.
 */
class SimulatedSmartbandRepository : SmartbandRepository {

    override val isSimulated: Boolean = true
    override val sampleRateHz: Int = SAMPLE_RATE_HZ

    private val nearby = listOf(
        SmartbandInfo(id = "SIM-RANI", name = "Anemom Band · Rani", batteryPercent = 82),
        SmartbandInfo(
            id = "SIM-MELATI",
            name = "Anemom Band · Melati",
            batteryPercent = 64,
            signal = SignalStrength.SEDANG
        )
    )

    private val _bluetoothEnabled = MutableStateFlow(true)
    override val bluetoothEnabled: StateFlow<Boolean> = _bluetoothEnabled.asStateFlow()

    // Simulasi: band "Rani" dianggap sudah pernah dipasangkan dan langsung tersambung.
    private val _connected = MutableStateFlow<SmartbandInfo?>(
        nearby.first().copy(lastSyncAtMillis = System.currentTimeMillis())
    )
    override val connectedDevice: StateFlow<SmartbandInfo?> = _connected.asStateFlow()

    override fun setBluetoothEnabled(enabled: Boolean) {
        _bluetoothEnabled.value = enabled
        if (!enabled) _connected.value = null
    }

    override suspend fun scan(): List<SmartbandInfo> {
        delay(1_500)
        if (!_bluetoothEnabled.value) return emptyList()
        return nearby
    }

    override suspend fun connect(deviceId: String): Result<SmartbandInfo> {
        if (!_bluetoothEnabled.value) return Result.failure(IllegalStateException("Bluetooth mati."))
        val device = nearby.firstOrNull { it.id == deviceId }
            ?: return Result.failure(IllegalArgumentException("Perangkat tidak ditemukan."))
        delay(1_200)
        val connected = device.copy(lastSyncAtMillis = System.currentTimeMillis())
        _connected.value = connected
        return Result.success(connected)
    }

    override suspend fun disconnect() {
        delay(400)
        _connected.value = null
    }

    override fun streamPpg(): Flow<PpgChunk> = flow {
        var n = 0L
        while (true) {
            check(_connected.value != null) { "Smartband terputus." }
            val red = ArrayList<Float>(CHUNK)
            val ir = ArrayList<Float>(CHUNK)
            repeat(CHUNK) {
                val t = n++ / SAMPLE_RATE_HZ.toFloat()
                val pulse = pulse(t)
                val noise = (Random.nextFloat() - 0.5f) * 0.03f
                red += pulse + noise
                ir += pulse * 1.4f + noise
            }
            emit(PpgChunk(red = red, ir = ir, quality = PpgQuality.BAIK))
            delay(1_000L * CHUNK / SAMPLE_RATE_HZ)
        }
    }

    override suspend fun uploadRecording(recording: PpgRecording): Result<String> {
        delay(1_500)
        return Result.success("SIM-${recording.startedAtMillis}")
    }

    /** Bentuk gelombang PPG sederhana (±75 denyut/menit) dengan sedikit pergeseran garis dasar. */
    private fun pulse(t: Float): Float {
        val phase = (t * BEATS_PER_SECOND) % 1f
        val systolic = exp(-sq((phase - 0.18f) / 0.06f))
        val dicrotic = 0.38f * exp(-sq((phase - 0.46f) / 0.09f))
        val baseline = 0.05f * sin(2f * PI.toFloat() * 0.2f * t)
        return systolic + dicrotic + baseline
    }

    private fun sq(x: Float): Float = x * x

    private companion object {
        const val SAMPLE_RATE_HZ = 50
        const val CHUNK = 5 // 5 sampel tiap 100 ms
        const val BEATS_PER_SECOND = 1.25f
    }
}

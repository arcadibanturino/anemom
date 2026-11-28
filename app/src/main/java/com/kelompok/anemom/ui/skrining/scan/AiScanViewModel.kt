package com.kelompok.anemom.ui.skrining.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.repository.Screening
import com.kelompok.anemom.data.repository.ScreeningRepository
import com.kelompok.anemom.ml.segmentation.ConjunctivaSegmenter
import com.kelompok.anemom.ml.segmentation.MaskQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Tahap layar AI Scan Mata. */
sealed interface ScanStep {
    /** Kamera aktif, menunggu foto. */
    data object Camera : ScanStep

    /** Foto sedang diperiksa U-Net di HP. */
    data object Checking : ScanStep

    /** Hasil pemeriksaan: foto (sudah dipotong) + mask, ibu memilih kirim atau ulangi. */
    data class Review(
        val photo: Bitmap,
        val maskOverlay: Bitmap,
        val verdict: MaskQuality.Verdict,
        val areaRatio: Float,
        val isSending: Boolean = false,
        val sendError: String? = null
    ) : ScanStep

    /** Foto sudah diterima server. */
    data class Sent(val screening: Screening) : ScanStep
}

data class AiScanUiState(
    val step: ScanStep = ScanStep.Camera,
    /** Pesan singkat di layar kamera (mis. gagal memotret, model tidak bisa dimuat). */
    val message: String? = null
)

/**
 * Alur: kamera -> foto dipotong sesuai kotak panduan -> U-Net di HP (cek kualitas) -> pratinjau mask
 * -> kirim foto potongan ke server (server menjalankan segmentasi + klasifikasi yang sama).
 *
 * Klasifikasi anemia BELUM dijalankan di HP: fitur warna harus dihitung persis seperti saat pelatihan,
 * jadi untuk sekarang hanya server yang menghitungnya.
 */
class AiScanViewModel(
    private val segmenterProvider: () -> ConjunctivaSegmenter,
    private val screeningRepository: ScreeningRepository,
    private val cacheDir: File
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiScanUiState())
    val uiState: StateFlow<AiScanUiState> = _uiState.asStateFlow()

    /**
     * Model segmentasi yang sama, untuk petunjuk live di kamera ([LiveEyeAnalyzer]).
     * Dipanggil dari thread analisis, bukan thread utama (memuat model butuh waktu).
     */
    val liveSegmenterProvider: () -> ConjunctivaSegmenter get() = segmenterProvider

    /** Dipanggil layar setelah foto diambil dan dipotong sesuai kotak panduan. */
    fun onPhotoCaptured(croppedPhoto: Bitmap) {
        _uiState.update { it.copy(step = ScanStep.Checking, message = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching { segmenterProvider().segment(croppedPhoto) }
            }
            result
                .onSuccess { seg ->
                    _uiState.update {
                        it.copy(
                            step = ScanStep.Review(
                                photo = croppedPhoto,
                                maskOverlay = seg.toOverlay(MASK_COLOR),
                                verdict = MaskQuality.check(seg),
                                areaRatio = seg.areaRatio
                            )
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            step = ScanStep.Camera,
                            message = "Model AI tidak dapat dijalankan di HP ini (${e.javaClass.simpleName})."
                        )
                    }
                }
        }
    }

    fun onCaptureFailed(reason: String?) {
        _uiState.update {
            it.copy(step = ScanStep.Camera, message = reason ?: "Gagal mengambil foto. Coba lagi.")
        }
    }

    fun retake() = _uiState.update { AiScanUiState() }

    fun send() {
        val review = _uiState.value.step as? ScanStep.Review ?: return
        if (review.isSending || review.verdict != MaskQuality.Verdict.OK) return
        _uiState.update { it.copy(step = review.copy(isSending = true, sendError = null)) }
        viewModelScope.launch {
            val file = File(cacheDir, "scan_mata_${System.currentTimeMillis()}.jpg")
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { review.photo.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
                }
                val segmenter = withContext(Dispatchers.Default) { segmenterProvider().info.segmenter }
                screeningRepository.submitEye(file, onDevice = null, segmenter = segmenter).getOrThrow()
            }
            withContext(Dispatchers.IO) { file.delete() }
            result
                .onSuccess { screening -> _uiState.update { it.copy(step = ScanStep.Sent(screening)) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            step = review.copy(
                                isSending = false,
                                sendError = e.message ?: "Gagal mengirim foto. Periksa koneksi internet."
                            )
                        )
                    }
                }
        }
    }

    private companion object {
        /** Hijau semi-transparan (ARGB) untuk menandai konjungtiva di pratinjau. */
        val MASK_COLOR: Int = 0x9900C853.toInt()
        const val JPEG_QUALITY = 92
    }
}

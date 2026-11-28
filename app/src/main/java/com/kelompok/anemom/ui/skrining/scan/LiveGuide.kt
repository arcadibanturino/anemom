package com.kelompok.anemom.ui.skrining.scan

import com.kelompok.anemom.ml.segmentation.MaskQuality
import kotlin.math.abs
import kotlin.math.max

/**
 * Logika petunjuk langsung (live) AI Scan Mata, seperti verifikasi wajah otomatis.
 *
 * Berkas ini Kotlin murni (tanpa Android) supaya bisa diuji tanpa HP. Data per frame dibuat oleh
 * [LiveEyeAnalyzer]; berkas ini hanya memutuskan petunjuk apa yang tampil dan kapan foto diambil.
 *
 * Dua tahap, karena detektor wajah tidak bisa bekerja saat HP sudah dekat (wajah tidak utuh di layar):
 *  1. Jauh  : detektor wajah ML Kit -> wajah lurus, satu mata di bingkai, cukup dekat.
 *  2. Dekat : U-Net yang sama dengan pemeriksaan foto -> kelopak bawah sudah ditarik (konjungtiva terlihat).
 * Lalu cek cahaya, ketajaman, dan kestabilan. Semua lolos selama [LiveGuideConfig.holdMs] dan minimal
 * [LiveGuideConfig.minReadyFrames] frame -> foto otomatis.
 *
 * Semua posisi memakai koordinat RELATIF terhadap kotak panduan ([GuideBox]): 0..1 berarti di dalam kotak.
 */

/** Data mata dari detektor wajah (tahap jauh). */
data class EyeObservation(
    /** Pusat mata, relatif terhadap kotak panduan (0..1 = di dalam kotak). */
    val centerX: Float,
    val centerY: Float,
    /** Lebar mata dibagi lebar kotak panduan. */
    val widthRatio: Float,
    /** Putaran kepala kiri-kanan dan miring (derajat, dari ML Kit). */
    val yaw: Float,
    val roll: Float
)

/** Hasil pengamatan satu frame kamera (resolusi kecil, hanya untuk petunjuk). */
data class LiveObservation(
    val timeMs: Long,
    /** Rata-rata kecerahan (luma 0..255) di dalam kotak panduan. */
    val brightness: Float,
    /** Varians Laplacian di dalam kotak (makin besar makin tajam). */
    val sharpness: Float,
    /** Rata-rata selisih gambar kecil dengan frame sebelumnya (0..255). Besar = HP bergerak. */
    val motion: Float,
    /** Mata terpilih dari detektor wajah; null bila wajah tidak terdeteksi. */
    val eye: EyeObservation?,
    /**
     * Luas mask U-Net (0..1) pada potongan ketat di sekitar konjungtiva ([ConjunctivaFocus]), atau pada
     * seluruh kotak bila konjungtiva belum ditemukan. null bila segmentasi tidak tersedia di HP ini.
     */
    val maskArea: Float?,
    /** Titik tengah mask, relatif terhadap kotak (0..1). */
    val maskCenterX: Float = 0.5f,
    val maskCenterY: Float = 0.5f,
    /** Luas mask di seluruh kotak (tahap 1); [maskArea] dari potongan ketat bila ada. Untuk debug. */
    val seedMaskArea: Float? = null
)

/** Petunjuk yang tampil di layar, urut dari tahap awal ke akhir. */
enum class GuideHint(val text: String) {
    STARTING("Menyiapkan kamera..."),
    AUTO_UNAVAILABLE("Petunjuk otomatis tidak tersedia di HP ini. Gunakan tombol Ambil foto manual."),
    TOO_DARK("Terlalu gelap. Cari tempat yang lebih terang."),
    TOO_BRIGHT("Terlalu silau. Hindari cahaya langsung ke mata."),
    FIND_EYE("Arahkan kamera ke mata Bunda."),
    FACE_STRAIGHT("Hadapkan wajah lurus ke kamera."),
    MOVE_EYE("Taruh mata di garis atas bingkai, kelopak bawah di lengkung merah."),
    MOVE_CLOSER("Dekatkan HP ke mata."),
    PULL_LID("Lihat ke atas, tarik kelopak bawah, lalu arahkan kamera sedikit dari atas ke kelopak bawah."),
    PULL_LID_MORE("Tarik kelopak bawah sedikit lagi. Bidik bagian merahnya, kamera sedikit dari atas."),
    TOO_CLOSE("Terlalu dekat. Jauhkan HP sedikit."),
    BLURRY("Gambar buram. Jauhkan HP sedikit, lalu tahan."),
    HOLD_STILL("Tahan HP, jangan bergerak."),
    READY("Bagus! Tahan posisi..."),
    CAPTURING("Memotret, tetap tahan...")
}

/** Daftar periksa di bawah kamera (centang hijau bila lolos). */
data class GuideChecks(
    val light: Boolean = false,
    val eyeInFrame: Boolean = false,
    val lidPulled: Boolean = false,
    val sharpAndSteady: Boolean = false
)

data class LiveGuide(
    val hint: GuideHint = GuideHint.STARTING,
    val checks: GuideChecks = GuideChecks(),
    /** 0..1, terisi selama semua syarat lolos. Penuh -> [shouldCapture]. */
    val holdProgress: Float = 0f,
    /** true satu kali: saatnya layar mengambil foto resolusi penuh. */
    val shouldCapture: Boolean = false
) {
    val ready: Boolean get() = hint == GuideHint.READY || hint == GuideHint.CAPTURING
}

/**
 * Ambang batas. Nilai awal dari perkiraan; KALIBRASI di beberapa HP dengan angka debug di layar
 * (build debug menampilkan kecerahan, ketajaman, gerak, dan luas mask).
 */
data class LiveGuideConfig(
    val minBrightness: Float = 45f,
    val maxBrightness: Float = 220f,
    /** Batas mutlak ketajaman minimum. */
    val minSharpness: Float = 12f,
    /** Ketajaman harus >= pecahan ini dari ketajaman terbaik yang baru saja terlihat. */
    val relativeSharpness: Float = 0.55f,
    /** Ketajaman terbaik "dilupakan" perlahan per frame, supaya menyesuaikan bila cahaya berubah. */
    val peakDecay: Float = 0.97f,
    val maxMotion: Float = 7f,
    /**
     * Luas mask minimum saat live. Sedikit di atas [MaskQuality.MIN_AREA] supaya foto resolusi penuh
     * (yang diperiksa ulang di layar pratinjau) juga lolos.
     */
    val minMaskArea: Float = MaskQuality.MIN_AREA * 1.3f,
    val maxMaskArea: Float = MaskQuality.MAX_AREA,
    /** Titik tengah mask harus berada di bagian tengah kotak (bukan di tepi). */
    val maskCenterMargin: Float = 0.12f,
    val maxYaw: Float = 25f,
    val maxRoll: Float = 25f,
    /** Pusat mata harus berada sejauh ini dari tepi kiri/kanan kotak (relatif). */
    val eyeMargin: Float = 0.1f,
    /**
     * Rentang tinggi pusat mata (relatif kotak). Yang dibidik di tengah kotak adalah KELOPAK BAWAH,
     * jadi mata berada di bagian atas kotak, boleh sedikit di atasnya (nilai negatif).
     */
    val eyeMinY: Float = -0.35f,
    val eyeMaxY: Float = 0.5f,
    /** Tahap jauh: mata harus selebar ini terhadap kotak. Lebih dekat lagi wajah biasanya sudah tidak terdeteksi. */
    val minEyeWidth: Float = 0.3f,
    /** Lama mata dianggap "baru saja di bingkai" setelah wajah hilang karena HP didekatkan. */
    val eyeMemoryMs: Long = 3000,
    /** Luas mask kecil yang menandakan ada sedikit konjungtiva (kelopak baru sedikit ditarik). */
    val partialMaskArea: Float = 0.003f,
    /**
     * Lama semua syarat harus lolos sebelum foto diambil. 1,5 dtk (v1) terlalu lama: mata perih dan
     * berkedip (uji 28 Nov). Kestabilan dijaga oleh [minReadyFrames], bukan oleh lamanya menunggu.
     */
    val holdMs: Long = 700,
    /** Minimal sekian frame berturut-turut lolos (bukan satu frame kebetulan). */
    val minReadyFrames: Int = 3,
    /** Syarat boleh gagal sesaat (mis. berkedip, satu-dua frame) tanpa mengulang hitungan. */
    val graceMs: Long = 500
)

/**
 * Mesin petunjuk. Panggil [update] untuk setiap frame (di thread utama), dan [reset] setelah foto
 * diambil atau gagal. Tidak thread-safe.
 */
class LiveGuideEngine(private val config: LiveGuideConfig = LiveGuideConfig()) {

    private var peakSharpness = 0f
    private var lastEyeInFrameMs: Long? = null
    private var readySinceMs: Long? = null
    private var lastReadyMs: Long? = null
    private var readyFrames = 0
    private var fired = false

    fun reset() {
        peakSharpness = 0f
        lastEyeInFrameMs = null
        readySinceMs = null
        lastReadyMs = null
        readyFrames = 0
        fired = false
    }

    fun update(o: LiveObservation): LiveGuide {
        val c = config
        if (fired) return LiveGuide(GuideHint.CAPTURING, allChecks, holdProgress = 1f)

        val light = o.brightness in c.minBrightness..c.maxBrightness

        val eye = o.eye
        val eyeStraight = eye != null && abs(eye.yaw) <= c.maxYaw && abs(eye.roll) <= c.maxRoll
        val eyeInBox = eye != null &&
            eye.centerX in c.eyeMargin..(1f - c.eyeMargin) &&
            eye.centerY in c.eyeMinY..c.eyeMaxY
        if (eyeInBox && eyeStraight) lastEyeInFrameMs = o.timeMs
        val eyeRecently = lastEyeInFrameMs?.let { o.timeMs - it <= c.eyeMemoryMs } ?: false

        val area = o.maskArea
        val maskCentered = o.maskCenterX in c.maskCenterMargin..(1f - c.maskCenterMargin) &&
            o.maskCenterY in c.maskCenterMargin..(1f - c.maskCenterMargin)
        // Titik tengah mask dihitung di seluruh kotak (tahap 1), jadi tetap bermakna walau ada potongan ketat.
        // Bila wajah masih terlihat, mask hanya dipercaya kalau mata memang di bingkai
        // (U-Net tetap menghasilkan mask pada gambar yang salah, mis. bibir).
        val lidPulled = area != null && area in c.minMaskArea..c.maxMaskArea && maskCentered &&
            (eye == null || eyeInBox)

        if (lidPulled) peakSharpness = max(o.sharpness, peakSharpness * c.peakDecay)
        val sharp = o.sharpness >= c.minSharpness && o.sharpness >= peakSharpness * c.relativeSharpness
        val steady = o.motion <= c.maxMotion

        val checks = GuideChecks(
            light = light,
            eyeInFrame = lidPulled || (eyeInBox && eyeStraight) || (eye == null && eyeRecently),
            lidPulled = lidPulled,
            sharpAndSteady = lidPulled && sharp && steady
        )

        val hint = when {
            area == null -> GuideHint.AUTO_UNAVAILABLE
            o.brightness < c.minBrightness -> GuideHint.TOO_DARK
            o.brightness > c.maxBrightness -> GuideHint.TOO_BRIGHT
            lidPulled && !sharp -> GuideHint.BLURRY
            lidPulled && !steady -> GuideHint.HOLD_STILL
            lidPulled -> GuideHint.READY
            area > c.maxMaskArea -> GuideHint.TOO_CLOSE
            eye != null && !eyeStraight -> GuideHint.FACE_STRAIGHT
            eye != null && !eyeInBox -> GuideHint.MOVE_EYE
            eye != null && eye.widthRatio < c.minEyeWidth -> GuideHint.MOVE_CLOSER
            eye != null -> GuideHint.PULL_LID
            eyeRecently || area >= c.partialMaskArea -> GuideHint.PULL_LID_MORE
            else -> GuideHint.FIND_EYE
        }

        // Hitungan tahan: mulai saat READY, tetap jalan bila gagal sesaat (< graceMs).
        val now = o.timeMs
        if (hint == GuideHint.READY) {
            if (readySinceMs == null) readySinceMs = now
            lastReadyMs = now
            readyFrames++
        } else if (lastReadyMs.let { it == null || now - it > c.graceMs }) {
            readySinceMs = null
            readyFrames = 0
        }
        val since = readySinceMs
        // Penuh bila waktu DAN jumlah frame lolos sama-sama cukup.
        val progress = if (since == null) 0f else minOf(
            (now - since).toFloat() / c.holdMs,
            readyFrames.toFloat() / c.minReadyFrames
        ).coerceIn(0f, 1f)

        if (progress >= 1f && hint == GuideHint.READY) {
            fired = true
            return LiveGuide(GuideHint.CAPTURING, allChecks, holdProgress = 1f, shouldCapture = true)
        }
        // Saat masa tenggang, tetap tampilkan petunjuk sebenarnya supaya Bunda tahu apa yang perlu diperbaiki.
        return LiveGuide(hint, checks, progress)
    }

    private companion object {
        val allChecks = GuideChecks(light = true, eyeInFrame = true, lidPulled = true, sharpAndSteady = true)
    }
}

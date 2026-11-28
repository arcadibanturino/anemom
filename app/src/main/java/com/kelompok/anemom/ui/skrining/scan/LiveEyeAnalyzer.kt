package com.kelompok.anemom.ui.skrining.scan

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.kelompok.anemom.ml.segmentation.ConjunctivaSegmenter
import com.kelompok.anemom.ml.segmentation.SegmentationResult
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Hasil analisis satu frame untuk layar: data untuk [LiveGuideEngine] + gambar untuk ditumpuk di preview.
 * Semua koordinat relatif terhadap kotak panduan dan BELUM dibalik cermin (layar yang membalik untuk kamera depan).
 */
class LiveFrame(
    val observation: LiveObservation,
    /** Mask U-Net berwarna (ukuran input model) untuk digambar di atas kotak panduan, atau di atas [focus]. */
    val maskOverlay: Bitmap?,
    /** Garis mata dari detektor wajah: x0, y0, x1, y1, ... relatif terhadap kotak. */
    val eyeOutline: FloatArray?,
    /**
     * Potongan ketat di sekitar konjungtiva ([ConjunctivaFocus]), [l, t, r, b] relatif terhadap kotak.
     * Bila ada, [maskOverlay] milik potongan ini (bukan seluruh kotak) dan foto otomatis dipotong sama.
     */
    val focus: FloatArray? = null,
    /** Lama analisis frame ini (ms), untuk angka debug. */
    val processMs: Long = 0
)

/**
 * Menganalisis frame kamera (resolusi kecil, ±640x480) untuk petunjuk live AI Scan Mata:
 *  - kecerahan, ketajaman (varians Laplacian), dan gerak di dalam kotak panduan;
 *  - detektor wajah ML Kit (model bawaan, offline) untuk posisi mata saat HP masih agak jauh;
 *  - U-Net yang SAMA dengan pemeriksaan foto, pada potongan kotak panduan, untuk melihat apakah
 *    kelopak bawah sudah ditarik (konjungtiva terlihat) dan untuk overlay segmentasi live.
 *
 * Berjalan di executor analisis (bukan thread utama). Frame dibatasi tiap [MIN_INTERVAL_MS] supaya
 * HP tidak panas. Foto yang dikirim ke server tetap foto resolusi penuh dari ImageCapture.
 */
class LiveEyeAnalyzer(
    segmenterProvider: () -> ConjunctivaSegmenter,
    private val onFrame: (LiveFrame) -> Unit
) : ImageAnalysis.Analyzer, AutoCloseable {

    /** Ukuran tampilan kamera di layar (piksel), untuk memetakan kotak panduan ke frame. */
    @Volatile var viewWidth = 0
    @Volatile var viewHeight = 0

    /** true saat foto sedang diambil/diperiksa: frame dibuang. */
    @Volatile var paused = false

    /** Dibuat di thread analisis (bukan thread utama), karena memuat model butuh waktu. */
    private val segmenter: ConjunctivaSegmenter? by lazy { runCatching(segmenterProvider).getOrNull() }
    private var segmenterFailures = 0

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.2f)
            .build()
    )

    private var lastRunMs = 0L
    private var previousFocus: FloatArray? = null
    private var frameIndex = 0
    private var previousThumb: FloatArray? = null

    override fun analyze(image: ImageProxy) {
        val vw = viewWidth
        val vh = viewHeight
        val now = SystemClock.uptimeMillis()
        if (paused || vw <= 0 || vh <= 0 || now - lastRunMs < MIN_INTERVAL_MS) {
            image.close()
            return
        }
        lastRunMs = now
        val frame = runCatching {
            val upright = image.use { it.toBitmap().rotatedBy(it.imageInfo.rotationDegrees) }
            try {
                process(upright, vw, vh, now)
            } finally {
                upright.recycle()
            }
        }.getOrNull()
        if (frame != null && !paused) onFrame(frame)
    }

    private fun process(frame: Bitmap, vw: Int, vh: Int, now: Long): LiveFrame {
        // Kotak panduan dipetakan ke frame dengan cara yang sama seperti foto (GuideBox, FILL_CENTER, rasio 4:3).
        val box = GuideBox.cropRectInPhoto(frame.width, frame.height, vw, vh)
        val crop = Bitmap.createBitmap(frame, box[0], box[1], box[2] - box[0], box[3] - box[1])

        val stats = grayStats(crop)
        frameIndex++
        // Detektor wajah hanya berguna saat HP masih agak jauh. Saat konjungtiva sudah terlacak (HP dekat,
        // wajah tidak utuh), lewati supaya frame lebih cepat; tetap dicoba sesekali.
        val tracking = previousFocus != null
        val eye = if (!tracking || frameIndex % FACE_EVERY_WHEN_TRACKING == 0) detectEye(frame, box) else null

        var area: Float? = null
        var seedArea: Float? = null
        var centerX = 0.5f
        var centerY = 0.5f
        var overlay: Bitmap? = null
        var focusRel: FloatArray? = null
        val seg = segmenter
        if (seg != null && segmenterFailures < MAX_SEGMENTER_FAILURES) {
            try {
                val prev = previousFocus
                if (prev == null || frameIndex % WIDE_EVERY == 0) {
                    // Tahap 1: seluruh kotak -> letak kasar konjungtiva.
                    val wide = seg.segment(crop)
                    segmenterFailures = 0
                    seedArea = wide.areaRatio
                    area = wide.areaRatio
                    overlay = wide.toOverlay(LIVE_MASK_COLOR)
                    maskCentroid(wide)?.let { (x, y) -> centerX = x; centerY = y }
                    // Tahap 2: potong ketat di sekitar mask (tetap memuat mata), U-Net lagi.
                    val rel = ConjunctivaFocus.fromMask(wide.mask, wide.width, wide.height, wide.threshold)
                        ?.let { ConjunctivaFocus.smooth(prev, it) }
                    previousFocus = rel
                    if (rel != null) {
                        segmentFocus(seg, frame, box, rel)?.let { close ->
                            area = close.areaRatio
                            overlay = close.toOverlay(LIVE_MASK_COLOR)
                            focusRel = rel
                        }
                    }
                } else {
                    // Cepat: konjungtiva sudah terlacak -> cukup satu U-Net di potongan ketat sebelumnya.
                    val close = segmentFocus(seg, frame, box, prev)
                    segmenterFailures = 0
                    if (close == null) {
                        previousFocus = null
                    } else {
                        area = close.areaRatio
                        overlay = close.toOverlay(LIVE_MASK_COLOR)
                        focusRel = prev
                        maskCentroid(close)?.let { (x, y) ->
                            val (bx, by) = ConjunctivaFocus.toBox(prev, x, y)
                            centerX = bx
                            centerY = by
                        }
                        // Ikuti pergeseran kecil; bila konjungtiva hilang, frame berikutnya mencari di seluruh kotak.
                        previousFocus = ConjunctivaFocus.refine(prev, close.mask, close.width, close.height, close.threshold)
                            ?.let { ConjunctivaFocus.smooth(prev, it) }
                    }
                }
            } catch (e: Throwable) {
                // Mis. flavor yolo yang belum selesai (NotImplementedError): layar beralih ke mode manual.
                segmenterFailures++
            }
        }
        // Setelah beberapa kali gagal, area tetap null -> petunjuk "gunakan tombol manual".
        if (crop !== frame) crop.recycle()

        val observation = LiveObservation(
            timeMs = now,
            brightness = stats.brightness,
            sharpness = stats.sharpness,
            motion = stats.motion,
            eye = eye?.first,
            maskArea = if (seg == null || segmenterFailures >= MAX_SEGMENTER_FAILURES) null else (area ?: 0f),
            maskCenterX = centerX,
            maskCenterY = centerY,
            seedMaskArea = seedArea
        )
        return LiveFrame(observation, overlay, eye?.second, focusRel, SystemClock.uptimeMillis() - now)
    }

    /** Mata (kiri/kanan) yang paling dekat ke tengah kotak, beserta garis konturnya. */
    private fun detectEye(frame: Bitmap, box: IntArray): Pair<EyeObservation, FloatArray>? {
        val faces = runCatching {
            Tasks.await(faceDetector.process(InputImage.fromBitmap(frame, 0)), FACE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        }.getOrNull() ?: return null
        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: return null

        val left = box[0].toFloat()
        val top = box[1].toFloat()
        val w = (box[2] - box[0]).toFloat()
        val h = (box[3] - box[1]).toFloat()
        fun relX(p: PointF) = (p.x - left) / w
        fun relY(p: PointF) = (p.y - top) / h

        val eyes = listOf(FaceContour.LEFT_EYE, FaceContour.RIGHT_EYE)
            .mapNotNull { type -> face.getContour(type)?.points?.takeIf { it.size >= 3 } }
        val best = eyes.minByOrNull { pts ->
            hypot(pts.map(::relX).average() - 0.5, pts.map(::relY).average() - 0.5)
        } ?: return null

        val minX = best.minOf { it.x }
        val maxX = best.maxOf { it.x }
        val minY = best.minOf { it.y }
        val maxY = best.maxOf { it.y }
        val observation = EyeObservation(
            centerX = ((minX + maxX) / 2f - left) / w,
            centerY = ((minY + maxY) / 2f - top) / h,
            widthRatio = (maxX - minX) / w,
            yaw = face.headEulerAngleY,
            roll = face.headEulerAngleZ
        )
        val outline = FloatArray(best.size * 2)
        best.forEachIndexed { i, p ->
            outline[2 * i] = relX(p)
            outline[2 * i + 1] = relY(p)
        }
        return observation to outline
    }

    /** U-Net pada potongan ketat [rel] di dalam kotak [box]; null bila potongan terlalu kecil. */
    private fun segmentFocus(seg: ConjunctivaSegmenter, frame: Bitmap, box: IntArray, rel: FloatArray): SegmentationResult? {
        val r = ConjunctivaFocus.applyTo(box, rel)
        if (r[2] - r[0] < MIN_FOCUS_PIXELS || r[3] - r[1] < MIN_FOCUS_PIXELS) return null
        val tight = Bitmap.createBitmap(frame, r[0], r[1], r[2] - r[0], r[3] - r[1])
        try {
            return seg.segment(tight)
        } finally {
            if (tight !== frame) tight.recycle()
        }
    }

    /** Titik tengah mask (pecahan 0..1 dari lebar/tinggi mask), null bila kosong. */
    private fun maskCentroid(result: SegmentationResult): Pair<Float, Float>? {
        var sumX = 0L
        var sumY = 0L
        var count = 0
        for (i in result.mask.indices) {
            if (result.mask[i] >= result.threshold) {
                sumX += i % result.width
                sumY += i / result.width
                count++
            }
        }
        if (count == 0) return null
        return sumX.toFloat() / count / result.width to sumY.toFloat() / count / result.height
    }

    private class Stats(val brightness: Float, val sharpness: Float, val motion: Float)

    /**
     * Kecerahan, ketajaman, dan gerak dari potongan kotak yang dikecilkan ke lebar [STATS_WIDTH]
     * (ukuran tetap, supaya angka ketajaman sebanding antar HP dan antar jarak).
     */
    private fun grayStats(crop: Bitmap): Stats {
        val sw = STATS_WIDTH
        val sh = max(THUMB_BLOCK * 2, (sw * crop.height / crop.width.toFloat()).roundToInt())
        val small = Bitmap.createScaledBitmap(crop, sw, sh, true)
        val px = IntArray(sw * sh)
        small.getPixels(px, 0, sw, 0, 0, sw, sh)
        if (small !== crop) small.recycle()

        val gray = FloatArray(px.size)
        var sum = 0.0
        for (i in px.indices) {
            val p = px[i]
            val g = 0.299f * ((p shr 16) and 0xFF) + 0.587f * ((p shr 8) and 0xFF) + 0.114f * (p and 0xFF)
            gray[i] = g
            sum += g
        }
        val brightness = (sum / gray.size).toFloat()

        // Varians Laplacian (4 tetangga): ukuran ketajaman yang umum dipakai.
        var lapSum = 0.0
        var lapSq = 0.0
        var n = 0
        for (y in 1 until sh - 1) {
            val row = y * sw
            for (x in 1 until sw - 1) {
                val i = row + x
                val v = 4 * gray[i] - gray[i - 1] - gray[i + 1] - gray[i - sw] - gray[i + sw]
                lapSum += v
                lapSq += v * v
                n++
            }
        }
        val mean = lapSum / n
        val sharpness = (lapSq / n - mean * mean).toFloat()

        // Gerak: gambar sangat kecil (rata-rata blok) dibandingkan dengan frame sebelumnya.
        val tw = sw / THUMB_BLOCK
        val th = sh / THUMB_BLOCK
        val thumb = FloatArray(tw * th)
        for (ty in 0 until th) for (tx in 0 until tw) {
            var s = 0f
            for (dy in 0 until THUMB_BLOCK) for (dx in 0 until THUMB_BLOCK) {
                s += gray[(ty * THUMB_BLOCK + dy) * sw + tx * THUMB_BLOCK + dx]
            }
            thumb[ty * tw + tx] = s / (THUMB_BLOCK * THUMB_BLOCK)
        }
        val prev = previousThumb
        val motion = if (prev != null && prev.size == thumb.size) {
            var d = 0f
            for (i in thumb.indices) d += abs(thumb[i] - prev[i])
            d / thumb.size
        } else {
            0f
        }
        previousThumb = thumb
        return Stats(brightness, sharpness, motion)
    }

    /** Model segmentasi milik AppContainer (dipakai bersama), jadi hanya detektor wajah yang ditutup. */
    override fun close() = faceDetector.close()

    private companion object {
        const val MIN_INTERVAL_MS = 80L
        const val FACE_TIMEOUT_MS = 1500L
        const val MAX_SEGMENTER_FAILURES = 3
        /** Saat konjungtiva terlacak, U-Net di seluruh kotak hanya tiap sekian frame (koreksi bila melenceng). */
        const val WIDE_EVERY = 4
        /** Saat konjungtiva terlacak, detektor wajah hanya tiap sekian frame. */
        const val FACE_EVERY_WHEN_TRACKING = 5
        /** Potongan ketat terlalu kecil (HP terlalu jauh) tidak dipakai: hasil U-Net dari gambar sekecil itu tidak berarti. */
        const val MIN_FOCUS_PIXELS = 64
        const val STATS_WIDTH = 160
        const val THUMB_BLOCK = 5

        /** Hijau semi-transparan untuk overlay live (sedikit lebih tipis dari pratinjau foto). */
        val LIVE_MASK_COLOR: Int = 0x8800E676.toInt()
    }
}

private fun Bitmap.rotatedBy(degrees: Int): Bitmap {
    if (degrees % 360 == 0) return this
    val out = Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
    if (out !== this) recycle()
    return out
}

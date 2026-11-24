package com.kelompok.anemom.ui.skrining.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import android.util.Size
import androidx.camera.core.CameraControl
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Preview dan foto memakai rasio 4:3 yang sama, supaya bagian yang terlihat di layar sama dengan
 * isi foto (syarat pemetaan kotak panduan di [GuideBox]).
 */
private val ratio43 = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY

/** Preview cukup sedang; ketajaman foto ditentukan oleh [captureResolutionSelector]. */
val previewResolutionSelector: ResolutionSelector = ResolutionSelector.Builder()
    .setAspectRatioStrategy(ratio43)
    .setResolutionStrategy(
        ResolutionStrategy(Size(1920, 1440), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
    )
    .build()

/**
 * Foto sekitar 12 MP (4032 x 3024). Tidak memakai "resolusi tertinggi" karena sebagian HP punya
 * mode 50-200 MP yang lambat dan boros memori. Hanya bagian kotak panduan yang di-decode.
 * Kamera depan yang lebih kecil (5-8 MP) otomatis memakai resolusi tertingginya, bukan diperbesar.
 */
val captureResolutionSelector: ResolutionSelector = ResolutionSelector.Builder()
    .setAspectRatioStrategy(ratio43)
    .setResolutionStrategy(
        ResolutionStrategy(Size(4032, 3024), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
    )
    .build()

/** Lebar maksimum potongan hasil. Lebih lebar dari ini dikecilkan (kelipatan 2) agar hemat memori. */
private const val MAX_CROP_WIDTH = 2600

/**
 * Fokus dan atur cahaya pada [point] (pusat kotak panduan), lalu tunggu kamera selesai fokus.
 * Tidak melempar galat: bila HP tidak mendukung atau terlalu lama, foto tetap diambil.
 */
suspend fun CameraControl.focusOn(point: MeteringPoint, executor: Executor, timeoutMs: Long = 2500) {
    val action = FocusMeteringAction.Builder(
        point,
        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
    ).setAutoCancelDuration(5, TimeUnit.SECONDS).build()
    withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { cont ->
            val future = startFocusAndMetering(action)
            future.addListener({ if (cont.isActive) cont.resume(Unit) }, executor)
            cont.invokeOnCancellation { future.cancel(false) }
        }
    }
}

/**
 * Ambil satu foto ke memori. Callback CameraX berjalan di [executor]; ImageProxy WAJIB ditutup
 * oleh pemanggil (lihat [decodeGuideCropAndClose]).
 */
suspend fun ImageCapture.takePictureAsync(executor: Executor): ImageProxy =
    suspendCancellableCoroutine { cont ->
        takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                if (cont.isActive) cont.resume(image) else image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                if (cont.isActive) cont.resumeWithException(exception)
            }
        })
    }

/**
 * Ambil bagian foto di dalam kotak panduan (tampilan kamera [viewWidth] x [viewHeight]),
 * sudah tegak, lalu tutup ImageProxy. Berat: panggil di Dispatchers.Default.
 *
 * Untuk JPEG, hanya potongan yang di-decode (BitmapRegionDecoder), jadi foto 12 MP tidak perlu
 * dimuat utuh ke memori.
 */
fun ImageProxy.decodeGuideCropAndClose(viewWidth: Int, viewHeight: Int, mirror: Boolean = false): Bitmap =
    decodeGuideCrop(viewWidth, viewHeight).let { if (mirror) it.mirrored() else it }

/**
 * Kamera depan: preview tampil seperti cermin, tetapi foto CameraX tidak. Foto dibalik supaya hasil yang
 * diperiksa Bunda sama dengan yang terlihat di layar. Warna (yang dianalisis model) tidak berubah.
 */
private fun Bitmap.mirrored(): Bitmap {
    val out = Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { preScale(-1f, 1f) }, true)
    if (out !== this) recycle()
    return out
}

private fun ImageProxy.decodeGuideCrop(viewWidth: Int, viewHeight: Int): Bitmap = use { proxy ->
    val rotation = proxy.imageInfo.rotationDegrees
    if (proxy.format != ImageFormat.JPEG) {
        // Cadangan (jarang): decode utuh, putar, lalu potong.
        val upright = proxy.toBitmap().rotated(rotation)
        val cropped = GuideBox.crop(upright, viewWidth, viewHeight)
        if (cropped !== upright) upright.recycle()
        return@use cropped
    }
    val buffer = proxy.planes[0].buffer
    val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
    val decoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        BitmapRegionDecoder.newInstance(bytes, 0, bytes.size)
    } else {
        @Suppress("DEPRECATION")
        BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false)
    } ?: error("JPEG tidak dapat dibaca")
    try {
        val rawW = decoder.width
        val rawH = decoder.height
        val sideways = rotation % 180 != 0
        val uprightW = if (sideways) rawH else rawW
        val uprightH = if (sideways) rawW else rawH
        val c = GuideBox.cropRectInPhoto(uprightW, uprightH, viewWidth, viewHeight)
        val raw = uprightRectToRaw(c[0], c[1], c[2], c[3], rawW, rawH, rotation)
        var sample = 1
        while ((c[2] - c[0]) / sample > MAX_CROP_WIDTH) sample *= 2
        val region = decoder.decodeRegion(raw, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("Potongan foto gagal dibaca")
        region.rotated(rotation)
    } finally {
        decoder.recycle()
    }
}

/**
 * Kotak [left, top, right, bottom] pada foto TEGAK -> kotak pada JPEG mentah (orientasi sensor).
 * [rotation] = derajat putar searah jarum jam dari mentah ke tegak (ImageInfo.rotationDegrees).
 * Diuji untuk 0/90/180/270 derajat.
 */
internal fun uprightRectToRaw(left: Int, top: Int, right: Int, bottom: Int, rawW: Int, rawH: Int, rotation: Int): Rect =
    when (((rotation % 360) + 360) % 360) {
        90 -> Rect(top, rawH - right, bottom, rawH - left)
        180 -> Rect(rawW - right, rawH - bottom, rawW - left, rawH - top)
        270 -> Rect(rawW - bottom, left, rawW - top, right)
        else -> Rect(left, top, right, bottom)
    }

private fun Bitmap.rotated(degrees: Int): Bitmap {
    if (degrees % 360 == 0) return this
    val out = Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
    if (out !== this) recycle()
    return out
}

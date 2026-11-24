package com.kelompok.anemom.ml.segmentation

import android.graphics.Bitmap

/**
 * Kontrak BERSAMA untuk model segmentasi konjungtiva (tahap 1 AI Scan Mata).
 *
 * Implementasinya ada di source set flavor masing-masing, jadi tim U-Net dan tim YOLO
 * tidak pernah mengubah berkas yang sama:
 *  - app/src/unet/...  -> UnetTfliteSegmenter (dipakai sekarang)
 *  - app/src/yolo/...  -> YoloSegmenter       (eksperimen perbandingan)
 *
 * Layar, unggah foto, API, dan database TIDAK tahu model mana yang dipakai; mereka hanya
 * memakai antarmuka ini lewat [SegmenterProvider.create].
 *
 * Ubah berkas ini hanya bila kedua tim sepakat (lihat README-PEMISAHAN.md).
 */
interface ConjunctivaSegmenter : AutoCloseable {

    /** Identitas model. [SegmenterInfo.modelName] harus sama dengan `ai_models.name` di database. */
    val info: SegmenterInfo

    /**
     * Potong area konjungtiva dari [image].
     * Pemanggilan bersifat blocking (inferensi): panggil dari Dispatchers.Default, bukan thread utama.
     */
    fun segment(image: Bitmap): SegmentationResult
}

/**
 * @param segmenter kode singkat yang dikirim ke server (`unet` / `yolo`), agar server memakai model yang sama.
 * @param architecture sama dengan `ai_models.architecture`.
 * @param modelName sama dengan `ai_models.name` (runtime `tflite`).
 * @param modelVersion sama dengan `ai_models.version`.
 */
data class SegmenterInfo(
    val segmenter: String,
    val architecture: String,
    val modelName: String,
    val modelVersion: String
)

/**
 * Mask probabilitas hasil segmentasi, ukuran [width] x [height], disimpan per baris (row-major),
 * nilai 0..1. Piksel dengan nilai >= [threshold] dianggap konjungtiva.
 * Ukuran mask mengikuti input model (U-Net: 256 x 256), bukan ukuran foto asli.
 */
class SegmentationResult(
    val mask: FloatArray,
    val width: Int,
    val height: Int,
    val threshold: Float = 0.5f
) {
    init {
        require(mask.size == width * height) { "Ukuran mask ${mask.size} != $width x $height" }
    }

    /** Fraksi piksel konjungtiva (0..1). Dipakai untuk pemeriksaan kualitas foto. */
    val areaRatio: Float by lazy { mask.count { it >= threshold }.toFloat() / mask.size }

    /** Mask sebagai gambar transparan berwarna [argbColor], untuk ditumpuk di atas foto. */
    fun toOverlay(argbColor: Int): Bitmap {
        val pixels = IntArray(mask.size) { i -> if (mask[i] >= threshold) argbColor else 0 }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}

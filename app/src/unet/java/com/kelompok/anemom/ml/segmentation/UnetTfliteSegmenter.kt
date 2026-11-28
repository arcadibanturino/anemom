package com.kelompok.anemom.ml.segmentation

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Segmentasi konjungtiva palpebra dengan U-Net (`anemom_palpebral_unet`) di HP.
 * Milik tim U-Net: hanya berada di flavor `unet` (app/src/unet).
 *
 * Model: input float32 [1, 256, 256, 3] (RGB), output float32 [1, 256, 256, 1] (sigmoid 0..1).
 *
 * TODO(tim AI) KONFIRMASI sebelum dipakai untuk hasil:
 *  1. Normalisasi input saat pelatihan: dibagi 255 (0..1)? Model tidak punya layer Rescaling,
 *     jadi normalisasi dilakukan di luar model. Diasumsikan [PIXEL_SCALE] = 255.
 *  2. Foto diubah ukurannya langsung ke 256x256 (rasio tidak dipertahankan)? Diasumsikan ya.
 *  3. Ambang mask 0,5 (sesuai `segmentation_threshold` di berkas joblib).
 */
class UnetTfliteSegmenter(context: Context) : ConjunctivaSegmenter {

    override val info = SegmenterInfo(
        segmenter = "unet",
        architecture = "unet",
        modelName = "conjunctiva-unet-tflite",
        modelVersion = "2026.11.18"
    )

    private val interpreter = Interpreter(
        loadModel(context, MODEL_ASSET),
        // 2-4 thread sesuai jumlah inti CPU: petunjuk live menjalankan U-Net berkali-kali per detik.
        Interpreter.Options().setNumThreads(Runtime.getRuntime().availableProcessors().coerceIn(2, 4))
    )

    private val input: ByteBuffer =
        ByteBuffer.allocateDirect(4 * SIZE * SIZE * 3).order(ByteOrder.nativeOrder())
    private val output: ByteBuffer =
        ByteBuffer.allocateDirect(4 * SIZE * SIZE).order(ByteOrder.nativeOrder())
    private val pixels = IntArray(SIZE * SIZE)

    /** Interpreter dan buffer dipakai ulang, jadi satu pemanggilan pada satu waktu. */
    @Synchronized
    override fun segment(image: Bitmap): SegmentationResult {
        val scaled = if (image.width == SIZE && image.height == SIZE) {
            image
        } else {
            Bitmap.createScaledBitmap(image, SIZE, SIZE, true)
        }
        scaled.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
        if (scaled !== image) scaled.recycle()

        input.rewind()
        for (p in pixels) {
            input.putFloat(((p shr 16) and 0xFF) / PIXEL_SCALE) // R
            input.putFloat(((p shr 8) and 0xFF) / PIXEL_SCALE)  // G
            input.putFloat((p and 0xFF) / PIXEL_SCALE)          // B
        }
        input.rewind()
        output.rewind()

        interpreter.run(input, output)

        output.rewind()
        val mask = FloatArray(SIZE * SIZE)
        output.asFloatBuffer().get(mask)
        return SegmentationResult(mask, SIZE, SIZE, THRESHOLD)
    }

    override fun close() = interpreter.close()

    private companion object {
        const val MODEL_ASSET = "models/segmentation/unet/anemom_unet_fp32.tflite"
        const val SIZE = 256
        const val PIXEL_SCALE = 255f
        const val THRESHOLD = 0.5f

        /** Butuh `androidResources { noCompress += "tflite" }` di build.gradle agar bisa di-map. */
        fun loadModel(context: Context, asset: String): MappedByteBuffer =
            context.assets.openFd(asset).use { fd ->
                FileInputStream(fd.fileDescriptor).use { stream ->
                    stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
    }
}

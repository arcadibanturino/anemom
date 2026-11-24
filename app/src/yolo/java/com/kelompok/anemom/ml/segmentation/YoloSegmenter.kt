package com.kelompok.anemom.ml.segmentation

import android.content.Context
import android.graphics.Bitmap

/**
 * Segmentasi konjungtiva dengan YOLOv8-seg. Milik tim YOLO: hanya berada di flavor `yolo` (app/src/yolo).
 *
 * BELUM DIBUAT. Yang perlu dikerjakan tim YOLO (tanpa mengubah berkas di app/src/main):
 *  1. Ekspor model:  yolo export model=best.pt format=tflite imgsz=<ukuran latih>
 *     lalu simpan di app/src/yolo/assets/models/segmentation/yolo/.
 *  2. Jalankan model dengan org.tensorflow.lite.Interpreter (lihat UnetTfliteSegmenter sebagai contoh).
 *     Output YOLOv8-seg ada dua: deteksi (kotak + skor + 32 koefisien mask) dan prototipe mask.
 *  3. Pascaproses: pilih deteksi skor tertinggi (atau NMS), gabungkan koefisien x prototipe -> sigmoid,
 *     potong ke dalam kotak, lalu ubah ukuran ke 256 x 256 supaya sebanding dengan U-Net.
 *  4. Kembalikan SegmentationResult (mask 0..1, 256 x 256). Bila tidak ada deteksi, kembalikan mask nol;
 *     MaskQuality akan menolaknya sebagai TOO_SMALL.
 *  5. Isi [info] sesuai baris `ai_models` (name `conjunctiva-yolov8-tflite`, version model yang diekspor).
 */
class YoloSegmenter(@Suppress("UNUSED_PARAMETER") context: Context) : ConjunctivaSegmenter {

    override val info = SegmenterInfo(
        segmenter = "yolo",
        architecture = "yolov8n-seg",
        modelName = "conjunctiva-yolov8-tflite",
        modelVersion = "0.0.1"
    )

    override fun segment(image: Bitmap): SegmentationResult =
        throw NotImplementedError("YoloSegmenter belum dibuat (tugas tim YOLO, lihat komentar di atas).")

    override fun close() = Unit
}

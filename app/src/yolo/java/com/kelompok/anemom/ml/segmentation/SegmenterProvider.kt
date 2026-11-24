package com.kelompok.anemom.ml.segmentation

import android.content.Context

/** Pintu masuk model segmentasi untuk flavor `yolo` (pasangan berkas yang sama di app/src/unet). */
object SegmenterProvider {
    fun create(context: Context): ConjunctivaSegmenter = YoloSegmenter(context.applicationContext)
}

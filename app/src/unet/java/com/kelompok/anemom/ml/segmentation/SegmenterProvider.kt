package com.kelompok.anemom.ml.segmentation

import android.content.Context

/**
 * Pintu masuk model segmentasi untuk flavor `unet`.
 * Flavor `yolo` punya berkas dengan nama dan isi fungsi yang sama di app/src/yolo, sehingga kode
 * di app/src/main cukup memanggil [SegmenterProvider.create] tanpa tahu model mana yang aktif.
 */
object SegmenterProvider {
    fun create(context: Context): ConjunctivaSegmenter = UnetTfliteSegmenter(context.applicationContext)
}

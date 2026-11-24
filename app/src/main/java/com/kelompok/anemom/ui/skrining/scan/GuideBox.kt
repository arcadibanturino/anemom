package com.kelompok.anemom.ui.skrining.scan

import android.graphics.Bitmap
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Kotak panduan di layar kamera. Ibu menaruh kelopak mata bawah di dalam kotak ini, lalu foto
 * DIPOTONG sesuai kotak sebelum masuk U-Net dan sebelum dikirim ke server.
 *
 * Alasannya: U-Net dilatih dengan foto close-up (dataset Eyes-defy-anemia) di mana konjungtiva
 * memenuhi sebagian besar bingkai. Foto HP utuh (wajah, alis, jari) membuat konjungtiva tinggal
 * garis tipis setelah dikecilkan ke 256x256, dan U-Net gagal. Diuji 25 Nov 2026: foto utuh
 * luas mask 0,0003; potongan ketat 0,029 dengan mask tepat di konjungtiva.
 *
 * Ukuran dalam pecahan lebar/tinggi tampilan kamera, sehingga sama di semua layar.
 */
object GuideBox {
    /** Lebar kotak terhadap lebar tampilan kamera. */
    const val WIDTH_FRACTION = 0.86f

    /** Lebar : tinggi kotak. Potongan uji yang berhasil sekitar 600 x 390 (≈ 1,5). */
    const val ASPECT = 1.5f

    /** Posisi pusat kotak terhadap tinggi tampilan kamera. */
    const val CENTER_Y_FRACTION = 0.5f

    /** Kotak dalam piksel tampilan kamera berukuran [viewWidth] x [viewHeight]. */
    fun rectInView(viewWidth: Float, viewHeight: Float): RectF {
        val w = viewWidth * WIDTH_FRACTION
        val h = w / ASPECT
        val cx = viewWidth / 2f
        val cy = viewHeight * CENTER_Y_FRACTION
        return RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
    }

    /**
     * Potong [photo] (sudah tegak) sesuai kotak panduan yang terlihat di tampilan kamera
     * berukuran [viewWidth] x [viewHeight] piksel.
     *
     * PreviewView memakai FILL_CENTER: foto diperbesar sampai menutupi tampilan, lalu bagian
     * tengahnya yang terlihat. Jadi koordinat kotak di layar dipetakan balik ke koordinat foto.
     * Kotak berada di tengah secara horizontal, sehingga hasilnya sama untuk kamera depan (cermin).
     */
    fun crop(photo: Bitmap, viewWidth: Int, viewHeight: Int): Bitmap {
        val r = cropRectInPhoto(photo.width, photo.height, viewWidth, viewHeight)
        return Bitmap.createBitmap(photo, r[0], r[1], r[2] - r[0], r[3] - r[1])
    }

    /** [left, top, right, bottom] potongan dalam piksel foto. Dipisah agar mudah diuji. */
    fun cropRectInPhoto(photoWidth: Int, photoHeight: Int, viewWidth: Int, viewHeight: Int): IntArray {
        require(viewWidth > 0 && viewHeight > 0) { "Ukuran tampilan kamera belum diketahui" }
        val scale = max(viewWidth / photoWidth.toFloat(), viewHeight / photoHeight.toFloat())
        val offsetX = (photoWidth - viewWidth / scale) / 2f
        val offsetY = (photoHeight - viewHeight / scale) / 2f
        val box = rectInView(viewWidth.toFloat(), viewHeight.toFloat())
        val left = (offsetX + box.left / scale).roundToInt().coerceIn(0, photoWidth - 1)
        val top = (offsetY + box.top / scale).roundToInt().coerceIn(0, photoHeight - 1)
        val right = (offsetX + box.right / scale).roundToInt().coerceIn(left + 1, photoWidth)
        val bottom = (offsetY + box.bottom / scale).roundToInt().coerceIn(top + 1, photoHeight)
        return intArrayOf(left, top, right, bottom)
    }
}

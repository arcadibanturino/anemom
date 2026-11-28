package com.kelompok.anemom.ui.skrining.scan

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Potongan KETAT di sekitar konjungtiva, di dalam kotak panduan.
 *
 * Masalah yang diatasi (uji 28 Nov 2026): kotak panduan memuat mata, pipi, dan jari, sehingga
 * konjungtiva hanya pita tipis dan luas mask U-Net ±1% walau gambar tajam. U-Net dilatih dengan
 * close-up (dataset Eyes-defy-anemia) dan potongan ketat terbukti berhasil (uji 25 Nov: 0,029).
 *
 * Caranya dua kali U-Net:
 *  1. U-Net di seluruh kotak -> letak kasar konjungtiva (kotak pembatas mask).
 *  2. Potong di sekitar kotak pembatas itu (masih memuat seluruh mata sebagai konteks), U-Net lagi.
 * Potongan yang sama dipakai untuk foto resolusi penuh yang dikirim ke server.
 *
 * Koordinat relatif terhadap kotak panduan: [left, top, right, bottom] dalam 0..1. Rasio potongan sama
 * dengan kotak ([GuideBox.ASPECT]), jadi pecahan lebar = pecahan tinggi.
 * Kotlin murni, bisa diuji tanpa HP.
 */
object ConjunctivaFocus {

    /** Luas mask minimum di kotak besar untuk mulai memotong ketat (di bawah ini dianggap belum ada). */
    const val MIN_SEED_AREA = 0.002f

    /** Lebar potongan = kelipatan lebar mask (pita konjungtiva kira-kira selebar mata; 2,2x memuat seluruh mata). */
    const val PAD_WIDTH = 2.2f

    /** Tinggi potongan minimal kelipatan tinggi mask (pita tipis tetap dapat konteks atas-bawah). */
    const val PAD_HEIGHT = 4f

    /** Potongan paling kecil 35% kotak: membatasi zoom supaya tidak hanya warna merah tanpa konteks. */
    const val MIN_FRACTION = 0.35f

    /** Jumlah piksel mask minimum (dari 256x256) supaya kotak pembatas tidak berasal dari bintik acak. */
    const val MIN_PIXELS = 40

    /**
     * Potongan ketat dari mask kotak besar ([mask] ukuran [w] x [h], meregang memenuhi kotak).
     * null bila mask terlalu kecil/kosong.
     */
    fun fromMask(mask: FloatArray, w: Int, h: Int, threshold: Float): FloatArray? {
        val b = bounds(mask, w, h, threshold) ?: return null
        return fromBounds(b[0], b[1], b[2], b[3])
    }

    /**
     * Perbarui potongan dari mask POTONGAN KETAT sebelumnya ([previous], relatif kotak), tanpa U-Net di
     * seluruh kotak. Dipakai supaya per frame cukup satu kali U-Net (lebih cepat). null bila konjungtiva
     * hilang dari potongan (frame berikutnya mencari lagi di seluruh kotak).
     */
    fun refine(previous: FloatArray, mask: FloatArray, w: Int, h: Int, threshold: Float): FloatArray? {
        val b = bounds(mask, w, h, threshold) ?: return null
        val size = previous[2] - previous[0]
        return fromBounds(
            previous[0] + b[0] * size,
            previous[1] + b[1] * size,
            previous[0] + b[2] * size,
            previous[1] + b[3] * size
        )
    }

    /** Titik tengah mask potongan ketat, dipetakan ke koordinat kotak. */
    fun toBox(focus: FloatArray, x: Float, y: Float): Pair<Float, Float> {
        val size = focus[2] - focus[0]
        return (focus[0] + x * size) to (focus[1] + y * size)
    }

    /** Kotak pembatas mask [l, t, r, b] (pecahan 0..1), null bila terlalu sedikit piksel. */
    private fun bounds(mask: FloatArray, w: Int, h: Int, threshold: Float): FloatArray? {
        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1
        var count = 0
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                if (mask[row + x] >= threshold) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                    count++
                }
            }
        }
        if (count < MIN_PIXELS || count.toFloat() / (w * h) < MIN_SEED_AREA) return null
        return floatArrayOf(minX.toFloat() / w, minY.toFloat() / h, (maxX + 1).toFloat() / w, (maxY + 1).toFloat() / h)
    }

    /**
     * Potongan dari kotak pembatas mask (relatif kotak). Mask meregang memenuhi kotak dan potongan
     * berasio sama dengan kotak, jadi pecahan lebar = pecahan tinggi = [size].
     */
    private fun fromBounds(l: Float, t: Float, r: Float, b: Float): FloatArray {
        val size = max(max(PAD_WIDTH * (r - l), PAD_HEIGHT * (b - t)), MIN_FRACTION).coerceAtMost(1f)
        return centeredInside((l + r) / 2f, (t + b) / 2f, size)
    }

    /** Kotak [size] x [size] (pecahan) berpusat di (cx, cy), digeser supaya tetap di dalam 0..1. */
    private fun centeredInside(cx: Float, cy: Float, size: Float): FloatArray {
        val half = size / 2f
        val l = (cx - half).coerceIn(0f, 1f - size)
        val t = (cy - half).coerceIn(0f, 1f - size)
        return floatArrayOf(l, t, l + size, t + size)
    }

    /**
     * Haluskan potongan antar frame supaya tidak bergetar. Bila potongan baru jauh dari sebelumnya
     * (mata pindah), langsung pakai yang baru.
     */
    fun smooth(previous: FloatArray?, next: FloatArray, weight: Float = 0.5f): FloatArray {
        if (previous == null) return next
        val pcx = (previous[0] + previous[2]) / 2f
        val pcy = (previous[1] + previous[3]) / 2f
        val ncx = (next[0] + next[2]) / 2f
        val ncy = (next[1] + next[3]) / 2f
        if (abs(pcx - ncx) > 0.15f || abs(pcy - ncy) > 0.15f) return next
        val size = previous[2] - previous[0] + weight * ((next[2] - next[0]) - (previous[2] - previous[0]))
        return centeredInside(pcx + weight * (ncx - pcx), pcy + weight * (ncy - pcy), size)
    }

    /**
     * Terapkan potongan relatif [rel] pada kotak piksel [box] = [left, top, right, bottom].
     * Dipakai untuk frame analisis maupun foto resolusi penuh.
     */
    fun applyTo(box: IntArray, rel: FloatArray): IntArray {
        val bw = box[2] - box[0]
        val bh = box[3] - box[1]
        val l = box[0] + (rel[0] * bw).toInt()
        val t = box[1] + (rel[1] * bh).toInt()
        val r = min(box[2], box[0] + (rel[2] * bw).toInt()).coerceAtLeast(l + 1)
        val b = min(box[3], box[1] + (rel[3] * bh).toInt()).coerceAtLeast(t + 1)
        return intArrayOf(l, t, r, b)
    }
}

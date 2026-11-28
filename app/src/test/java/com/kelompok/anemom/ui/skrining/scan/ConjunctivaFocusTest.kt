package com.kelompok.anemom.ui.skrining.scan

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConjunctivaFocusTest {

    private val n = 256

    /** Mask 256x256 berisi persegi panjang [x0, x1) x [y0, y1). */
    private fun mask(x0: Int, x1: Int, y0: Int, y1: Int) = FloatArray(n * n) { i ->
        val x = i % n
        val y = i / n
        if (x in x0 until x1 && y in y0 until y1) 1f else 0f
    }

    @Test
    fun pita_tipis_dipotong_ketat_dan_luas_naik() {
        // Mirip uji 28 Nov: pita konjungtiva tipis, luas ±1% dari kotak.
        val m = mask(100, 180, 150, 158)
        val wideArea = 80 * 8f / (n * n)
        val rel = ConjunctivaFocus.fromMask(m, n, n, 0.5f)
        assertNotNull(rel)
        rel!!
        val size = rel[2] - rel[0]
        assertEquals(size, rel[3] - rel[1], 1e-5f) // rasio sama dengan kotak
        // Mask 80 px -> potongan 2,2 x 80 = 176 px dari 256 (0,6875).
        assertEquals(176f / 256, size, 1e-3f)
        // Luas di potongan ketat = luas di kotak / size^2 -> ±2x lipat.
        assertTrue(wideArea / (size * size) > wideArea * 2)
        // Pita tetap di dalam potongan.
        assertTrue(rel[0] <= 100f / n && rel[2] >= 180f / n)
        assertTrue(rel[1] <= 150f / n && rel[3] >= 158f / n)
    }

    @Test
    fun mask_kecil_atau_kosong_tidak_dipotong() {
        assertNull(ConjunctivaFocus.fromMask(FloatArray(n * n), n, n, 0.5f))
        assertNull(ConjunctivaFocus.fromMask(mask(10, 14, 10, 14), n, n, 0.5f))
    }

    @Test
    fun zoom_dibatasi_dan_tetap_di_dalam_kotak() {
        // Mask kecil di pojok: potongan minimal 35% dan digeser masuk kotak.
        val rel = ConjunctivaFocus.fromMask(mask(0, 20, 240, 256), n, n, 0.5f)!!
        assertEquals(ConjunctivaFocus.MIN_FRACTION, rel[2] - rel[0], 1e-5f)
        assertTrue(rel[0] >= 0f && rel[1] >= 0f && rel[2] <= 1f && rel[3] <= 1f)
    }

    @Test
    fun mask_besar_memakai_seluruh_kotak() {
        val rel = ConjunctivaFocus.fromMask(mask(20, 236, 60, 200), n, n, 0.5f)!!
        assertArrayEquals(floatArrayOf(0f, 0f, 1f, 1f), rel, 1e-5f)
    }

    @Test
    fun terapkan_ke_piksel_foto() {
        val box = intArrayOf(100, 200, 1100, 866)
        val r = ConjunctivaFocus.applyTo(box, floatArrayOf(0.25f, 0.5f, 0.75f, 1f))
        assertArrayEquals(intArrayOf(350, 533, 850, 866), r)
    }

    @Test
    fun penghalusan_mengikuti_tetapi_lompat_bila_jauh() {
        val a = floatArrayOf(0.2f, 0.2f, 0.6f, 0.6f)
        val near = floatArrayOf(0.3f, 0.2f, 0.7f, 0.6f)
        val s = ConjunctivaFocus.smooth(a, near)
        assertEquals(0.25f, s[0], 1e-5f)
        val far = floatArrayOf(0.6f, 0.6f, 1f, 1f)
        assertArrayEquals(far, ConjunctivaFocus.smooth(a, far), 1e-5f)
    }

    @Test
    fun pelacakan_dari_potongan_ketat_tetap_memuat_konjungtiva() {
        val prev = floatArrayOf(0.25f, 0.25f, 0.75f, 0.75f)
        // Pita di dalam potongan ketat: x 100..180, y 150..158 dari 256.
        val rel = ConjunctivaFocus.refine(prev, mask(100, 180, 150, 158), n, n, 0.5f)!!
        val bandLeft = 0.25f + 100f / n * 0.5f
        val bandRight = 0.25f + 180f / n * 0.5f
        assertTrue(rel[0] <= bandLeft && rel[2] >= bandRight)
        assertTrue(rel[0] >= 0f && rel[3] <= 1f)
        assertNull(ConjunctivaFocus.refine(prev, FloatArray(n * n), n, n, 0.5f))
    }
}

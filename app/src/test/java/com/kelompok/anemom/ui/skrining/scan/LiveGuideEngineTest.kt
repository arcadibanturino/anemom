package com.kelompok.anemom.ui.skrining.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Urutan petunjuk AI Scan Mata otomatis (logika murni, tanpa kamera). */
class LiveGuideEngineTest {

    private lateinit var engine: LiveGuideEngine
    private var t = 0L

    @Before
    fun setUp() {
        engine = LiveGuideEngine()
        t = 0L
    }

    private fun frame(
        eye: EyeObservation? = null,
        area: Float? = 0f,
        brightness: Float = 120f,
        sharpness: Float = 40f,
        motion: Float = 2f
    ): LiveGuide {
        val g = engine.update(LiveObservation(t, brightness, sharpness, motion, eye, area))
        t += 150
        return g
    }

    private fun eye(x: Float = 0.5f, y: Float = 0.15f, width: Float = 0.35f, yaw: Float = 0f) =
        EyeObservation(x, y, width, yaw, roll = 0f)

    @Test
    fun cahaya_diperiksa_lebih_dulu() {
        assertEquals(GuideHint.TOO_DARK, frame(brightness = 20f).hint)
        assertEquals(GuideHint.TOO_BRIGHT, frame(brightness = 240f).hint)
    }

    @Test
    fun tahap_jauh_mengarahkan_mata() {
        assertEquals(GuideHint.FIND_EYE, frame().hint)
        assertEquals(GuideHint.FACE_STRAIGHT, frame(eye = eye(yaw = 40f)).hint)
        assertEquals(GuideHint.MOVE_EYE, frame(eye = eye(x = 0.02f)).hint)
        assertEquals(GuideHint.MOVE_CLOSER, frame(eye = eye(width = 0.15f)).hint)
        assertEquals(GuideHint.PULL_LID, frame(eye = eye()).hint)
        // Wajah hilang karena HP didekatkan: masih diingat, petunjuk lanjut ke tarik kelopak.
        assertEquals(GuideHint.PULL_LID_MORE, frame(eye = null).hint)
    }

    @Test
    fun mata_di_bagian_atas_bingkai_karena_yang_dibidik_kelopak_bawah() {
        // Mata sedikit di atas kotak: masih benar (kelopak bawah di tengah kotak).
        assertEquals(GuideHint.PULL_LID, frame(eye = eye(y = -0.2f)).hint)
        // Mata di tengah/bawah kotak: kamera terlalu tinggi, kelopak bawah di luar bingkai.
        assertEquals(GuideHint.MOVE_EYE, frame(eye = eye(y = 0.7f)).hint)
    }

    @Test
    fun mask_tidak_dipercaya_bila_mata_di_luar_bingkai() {
        val g = frame(eye = eye(x = 0.02f), area = 0.05f)
        assertFalse(g.checks.lidPulled)
        assertEquals(GuideHint.MOVE_EYE, g.hint)
    }

    @Test
    fun buram_dan_goyang_menahan_jepret() {
        assertEquals(GuideHint.BLURRY, frame(area = 0.03f, sharpness = 3f).hint)
        assertEquals(GuideHint.HOLD_STILL, frame(area = 0.03f, motion = 20f).hint)
    }

    @Test
    fun jepret_otomatis_setelah_tahan_dan_hanya_sekali() {
        var captures = 0
        repeat(20) { if (frame(area = 0.03f).shouldCapture) captures++ }
        assertEquals(1, captures)
        assertEquals(GuideHint.CAPTURING, frame(area = 0.03f).hint)
        engine.reset()
        assertEquals(GuideHint.FIND_EYE, frame().hint)
    }

    @Test
    fun gagal_sesaat_tidak_mengulang_hitungan() {
        repeat(2) { frame(area = 0.03f) }
        val before = frame(area = 0.03f, motion = 20f).holdProgress // mis. berkedip / goyang sesaat
        val after = frame(area = 0.03f).holdProgress
        assertTrue(before > 0f)
        assertTrue(after > before)
    }

    @Test
    fun jepret_cepat_tapi_butuh_beberapa_frame_lolos() {
        // Frame lambat (400 ms): waktu tahan sudah lewat, tetapi tetap butuh 3 frame lolos.
        fun slow(): LiveGuide { val g = frame(area = 0.03f); t += 250; return g }
        assertFalse(slow().shouldCapture)
        assertFalse(slow().shouldCapture)
        assertTrue(slow().shouldCapture)
        // Frame cepat (150 ms): butuh ±700 ms, bukan 1,5 dtk seperti versi awal.
        engine.reset()
        val start = t
        while (!frame(area = 0.03f).shouldCapture) Unit
        assertTrue(t - start <= 900)
    }

    @Test
    fun tanpa_segmentasi_minta_mode_manual() {
        assertEquals(GuideHint.AUTO_UNAVAILABLE, frame(area = null).hint)
    }
}

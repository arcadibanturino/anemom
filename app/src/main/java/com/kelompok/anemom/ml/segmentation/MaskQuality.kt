package com.kelompok.anemom.ml.segmentation

/**
 * Pemeriksaan kualitas foto dari hasil segmentasi, SAMA untuk semua model (U-Net maupun YOLO),
 * supaya perbandingan kedua model adil.
 *
 * U-Net tidak memberi skor "ada konjungtiva atau tidak"; foto yang salah pun tetap menghasilkan mask.
 * Karena itu luas mask dipakai sebagai penyaring sederhana sebelum foto dikirim ke server.
 * Luas dihitung pada foto yang SUDAH dipotong sesuai kotak panduan kamera.
 *
 * TODO(tim AI): kalibrasi [MIN_AREA] dan [MAX_AREA] dari sebaran luas mask pada data latih.
 * MIN_AREA 0,015: foto HP asli (potongan ketat, kelopak sedikit ditarik) menghasilkan luas 0,029,
 * jadi batas awal 0,03 terlalu ketat. Harus sama dengan MIN_MASK_AREA di ml-service.
 */
object MaskQuality {

    const val MIN_AREA = 0.015f
    const val MAX_AREA = 0.60f

    enum class Verdict(val message: String?) {
        OK(null),
        TOO_SMALL("Bagian dalam kelopak mata belum terlihat jelas. Tarik kelopak bawah sedikit lagi dan dekatkan kamera."),
        TOO_LARGE("Foto terlalu dekat atau bukan foto mata. Ulangi dengan posisi mata di dalam bingkai.")
    }

    fun check(result: SegmentationResult): Verdict = when {
        result.areaRatio < MIN_AREA -> Verdict.TOO_SMALL
        result.areaRatio > MAX_AREA -> Verdict.TOO_LARGE
        else -> Verdict.OK
    }
}

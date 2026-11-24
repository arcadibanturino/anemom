package com.kelompok.anemom.ui.nutrisi

import com.kelompok.anemom.data.model.AllergyStatus
import com.kelompok.anemom.data.model.FoodContext
import com.kelompok.anemom.data.model.HbRisk

/**
 * Teks penyesuaian saran makanan dari konteks ibu (risiko Hb terakhir + alergi).
 * Tanpa takaran: saran berupa ragam bahan, bukan jumlah. Porsi ditentukan bidan/dokter.
 */
internal object FoodGuidance {

    /** Baris pertama kartu "Sesuai kebutuhan Bunda", mis. "Risiko sedang · Alergi sesuai profil". */
    fun summaryLine(ctx: FoodContext): String {
        val risk = ctx.riskLabel ?: "Hb belum tercatat"
        val allergy = when (ctx.allergyStatus) {
            AllergyStatus.ADA -> "Alergi sesuai profil"
            AllergyStatus.TIDAK -> "Tanpa alergi makanan"
            AllergyStatus.TIDAK_TAHU -> "Alergi belum diketahui"
            null -> "Alergi belum diisi"
        }
        return "$risk · $allergy"
    }

    /** Baris kedua kartu. */
    fun mainLine(ctx: FoodContext): String =
        if (ctx.allergens.isNotEmpty()) {
            "Bahan pemicu dikecualikan. Variasikan sumber gizi, bukan memperbesar porsi."
        } else {
            "Variasikan sumber gizi, bukan memperbesar porsi."
        }

    /** Teks kecil di bawah kartu: saran sesuai risiko + keterangan alergi. */
    fun detailLine(ctx: FoodContext): String =
        listOfNotNull(riskAdvice(ctx), allergyAdvice(ctx)).joinToString(" ")

    /** Kalimat risiko di bagian "Sesuai risiko & alergi" pada detail bahan. */
    fun detailRiskSentence(ctx: FoodContext): String = when (ctx.risk) {
        HbRisk.NORMAL -> "Hb terakhir normal: pertahankan variasi menu; porsi sesuai kebutuhan."
        HbRisk.MODERATE -> "${label(ctx, HbRisk.MODERATE)}: variasikan sumber gizi; porsi tetap sesuai kebutuhan."
        HbRisk.HIGH -> "${label(ctx, HbRisk.HIGH)}: variasikan sumber gizi; porsi tetap sesuai kebutuhan."
        null -> "Porsi sesuai kebutuhan dan anjuran bidan atau dokter."
    }

    private fun riskAdvice(ctx: FoodContext): String = when (ctx.risk) {
        HbRisk.NORMAL -> "Hb terakhir normal: pertahankan menu seimbang dan TTD sesuai anjuran."
        HbRisk.MODERATE ->
            "${label(ctx, HbRisk.MODERATE)}: lebih banyak ragam sumber zat besi, bukan porsi berlebihan."
        HbRisk.HIGH ->
            "${label(ctx, HbRisk.HIGH)}: lebih banyak ragam sumber zat besi, bukan porsi berlebihan. " +
                "Konsultasikan dengan bidan atau dokter."
        null -> if (ctx.riskLabel != null) {
            "${ctx.riskLabel}: ikuti anjuran bidan atau dokter."
        } else {
            "Belum ada hasil Hb: catat hasil tes darah agar saran lebih sesuai."
        }
    }

    /** Label dari server bila ada (mengikuti hb_risk_levels), selain itu label bawaan aplikasi. */
    private fun label(ctx: FoodContext, risk: HbRisk): String = ctx.riskLabel ?: risk.label

    private fun allergyAdvice(ctx: FoodContext): String? = when {
        ctx.allergens.size == 1 -> {
            val name = ctx.allergens.first()
            "Alergi $name: ${name.lowercase()} dikecualikan."
        }
        ctx.allergens.size > 1 -> "Alergi ${ctx.allergens.joinToString(", ")}: bahan tersebut dikecualikan."
        ctx.allergyStatus == AllergyStatus.TIDAK -> null
        else -> "Jika belum tahu ada alergi, cek komposisi dan konsultasikan."
    }
}

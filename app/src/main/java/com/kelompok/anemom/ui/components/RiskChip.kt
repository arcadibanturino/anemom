package com.kelompok.anemom.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kelompok.anemom.data.model.HbRisk
import com.kelompok.anemom.ui.theme.AmberBg
import com.kelompok.anemom.ui.theme.AmberText
import com.kelompok.anemom.ui.theme.ErrorRed
import com.kelompok.anemom.ui.theme.GreenBg
import com.kelompok.anemom.ui.theme.GreenText
import com.kelompok.anemom.ui.theme.RedBg

/** Chip kategori risiko Hb: hijau (normal), kuning (sedang), merah (tinggi). */
@Composable
fun RiskChip(risk: HbRisk, modifier: Modifier = Modifier) {
    val (container, content) = when (risk) {
        HbRisk.NORMAL -> GreenBg to GreenText
        HbRisk.MODERATE -> AmberBg to AmberText
        HbRisk.HIGH -> RedBg to ErrorRed
    }
    AnemomChip(
        text = risk.label,
        containerColor = container,
        contentColor = content,
        modifier = modifier
    )
}

package com.kelompok.anemom.ui.nutrisi

import androidx.annotation.DrawableRes
import com.kelompok.anemom.R

data class MenuRecommendation(
    val title: String,
    val ironText: String,
    @DrawableRes val imageRes: Int,
    val portionText: String = "Estimasi • 1 porsi",
    val warning: String? = null
)

// Data statis sementara. Nanti diganti data dari server (satu tempat saja).
// Angka zat besi masih perkiraan dari desain, sebaiknya diverifikasi ahli gizi/TKPI.
val menuRecommendations = listOf(
    MenuRecommendation(
        title = "Nasi & Daging Sapi",
        ironText = "±2 mg zat besi / porsi",
        imageRes = R.drawable.img_menu_nasi_daging
    ),
    MenuRecommendation(
        title = "Sup Bayam",
        ironText = "±3 mg zat besi / porsi",
        imageRes = R.drawable.img_menu_sup_bayam
    ),
    MenuRecommendation(
        title = "Hati Ayam Matang",
        ironText = "±6 mg zat besi / porsi",
        imageRes = R.drawable.img_menu_hati_ayam,
        warning = "Tidak dianjurkan saat hamil • tinggi vitamin A"
    )
)
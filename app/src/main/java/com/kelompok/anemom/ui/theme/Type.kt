package com.kelompok.anemom.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Desain Figma memakai font Poppins. Untuk memakainya:
// 1) unduh Poppins dari Google Fonts, letakkan file .ttf di app/src/main/res/font/
//    (poppins_regular.ttf, poppins_medium.ttf, poppins_semibold.ttf, poppins_bold.ttf)
// 2) ganti nilai di bawah dengan:
//    FontFamily(Font(R.font.poppins_regular, FontWeight.Normal), Font(R.font.poppins_medium, FontWeight.Medium),
//               Font(R.font.poppins_semibold, FontWeight.SemiBold), Font(R.font.poppins_bold, FontWeight.Bold))
val AnemomFontFamily: FontFamily = FontFamily.Default

val AnemomTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = AnemomFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AnemomFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AnemomFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AnemomFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AnemomFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AnemomFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AnemomFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AnemomFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

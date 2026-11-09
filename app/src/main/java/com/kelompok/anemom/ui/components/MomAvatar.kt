package com.kelompok.anemom.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kelompok.anemom.R
import com.kelompok.anemom.ui.theme.Pink100

/** Avatar bulat dengan ilustrasi ibu hamil (dipakai di Profil dan Tentang Anemom). */
@Composable
fun MomAvatar(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Pink100),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.img_banner_mom),
            contentDescription = null,
            modifier = Modifier
                .fillMaxHeight(0.84f)
                .padding(start = 2.dp),
            contentScale = ContentScale.Fit
        )
    }
}

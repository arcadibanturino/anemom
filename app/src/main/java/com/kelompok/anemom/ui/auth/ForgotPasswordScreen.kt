package com.kelompok.anemom.ui.auth

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.R
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.AnemomOutlinedButton
import com.kelompok.anemom.ui.components.AnemomTextField
import com.kelompok.anemom.ui.components.AnemomTopBar
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.components.NoteCard
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500

/**
 * Alur "Lupa kata sandi?": form email -> konfirmasi "Link reset telah dikirim".
 * Saat ini masih simulasi (belum ada email yang benar-benar dikirim).
 */
@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Back sistem di layar konfirmasi = kembali ke form, bukan keluar halaman.
    BackHandler(enabled = state.isSent) { viewModel.editEmail() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.isSent) {
            AnemomTopBar(
                title = "Periksa email Bunda",
                subtitle = "Pemulihan kata sandi",
                onBack = viewModel::editEmail
            )
            AnemomChip(
                text = "Data simulasi",
                containerColor = Pink100,
                contentColor = Pink500,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(16.dp))
            ResetLogo()
            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Pink100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.MailOutline,
                    contentDescription = null,
                    tint = Pink500,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Link reset telah dikirim",
                style = MaterialTheme.typography.headlineMedium,
                color = Maroon,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Jika email terdaftar, link reset dikirim ke:",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = state.email,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Maroon,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))

            AnemomCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Buka email dan pilih link untuk membuat kata sandi baru. " +
                            "Periksa folder spam jika email belum terlihat.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Maroon
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Simulasi tampilan · Tidak ada email yang benar-benar dikirim.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
            Spacer(Modifier.height(20.dp))

            GradientButton(text = "Kembali ke Masuk", onClick = onBackToLogin)
            Spacer(Modifier.height(12.dp))
            AnemomOutlinedButton(
                text = if (state.resendCooldown > 0) {
                    "Kirim Ulang Link (${state.resendCooldown} dtk)"
                } else {
                    "Kirim Ulang Link"
                },
                onClick = {
                    if (state.resendCooldown == 0) {
                        viewModel.resend()
                        Toast.makeText(context, "Link dikirim ulang (simulasi)", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.alpha(if (state.resendCooldown > 0) 0.5f else 1f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Email kurang tepat? Ubah alamat email",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
                modifier = Modifier
                    .clickable(onClick = viewModel::editEmail)
                    .padding(8.dp)
            )
        } else {
            AnemomTopBar(
                title = "Lupa kata sandi",
                subtitle = "Bantu Bunda kembali masuk",
                onBack = onBackToLogin
            )
            Spacer(Modifier.height(48.dp))
            ResetLogo()
            Spacer(Modifier.height(28.dp))
            Text(
                text = "Atur ulang kata sandi",
                style = MaterialTheme.typography.headlineMedium,
                color = Maroon,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Masukkan email yang terdaftar. Kami akan mengirim link untuk membuat kata sandi baru.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            AnemomTextField(
                label = "Email terdaftar",
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = "nama@email.com",
                leadingIcon = Icons.Outlined.MailOutline,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                onImeAction = viewModel::sendResetLink,
                errorText = state.emailError
            )
            Spacer(Modifier.height(16.dp))
            GradientButton(
                text = "Kirim Link Reset",
                onClick = viewModel::sendResetLink,
                isLoading = state.isLoading
            )
            Spacer(Modifier.height(12.dp))
            AnemomOutlinedButton(text = "Kembali ke Masuk", onClick = onBackToLogin)
            Spacer(Modifier.height(20.dp))
            NoteCard(
                text = "Jangan bagikan link reset kepada siapa pun. " +
                        "Anemom tidak pernah meminta kata sandi Bunda."
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ResetLogo() {
    Image(
        painter = painterResource(R.drawable.img_logo_login),
        contentDescription = "Logo Anemom",
        modifier = Modifier.width(210.dp),
        contentScale = ContentScale.Fit
    )
}
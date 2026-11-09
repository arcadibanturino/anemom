package com.kelompok.anemom.ui.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.kelompok.anemom.ui.components.AnemomTextField
import com.kelompok.anemom.ui.components.AnemomTopBar
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.theme.ErrorRed
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink500

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onBack: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
        AnemomTopBar(title = "Selamat datang", onBack = onBack)

        Spacer(Modifier.height(8.dp))
        Image(
            painter = painterResource(R.drawable.img_logo_login),
            contentDescription = "Logo Anemom",
            modifier = Modifier.width(210.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Masuk ke Anemom",
            style = MaterialTheme.typography.headlineMedium,
            color = Maroon,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Teman perjalanan sehat Bunda\ndan si kecil, setiap hari.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        AnemomTextField(
            label = "Email",
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "nama@email.com",
            leadingIcon = Icons.Outlined.MailOutline,
            keyboardType = KeyboardType.Email,
            errorText = state.emailError
        )
        Spacer(Modifier.height(12.dp))
        AnemomTextField(
            label = "Kata sandi",
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Masukkan kata sandi",
            leadingIcon = Icons.Outlined.Lock,
            isPassword = true,
            imeAction = ImeAction.Done,
            onImeAction = { viewModel.login(onLoginSuccess) },
            errorText = state.passwordError
        )

        Text(
            text = "Lupa kata sandi?",
            color = Pink500,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.End)
                .clickable {
                    // TODO(fitur): alur reset kata sandi
                    Toast.makeText(context, "Fitur lupa kata sandi segera hadir", Toast.LENGTH_SHORT).show()
                }
                .padding(vertical = 10.dp)
        )

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage.orEmpty(),
                color = ErrorRed,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        GradientButton(
            text = "Masuk",
            onClick = { viewModel.login(onLoginSuccess) },
            isLoading = state.isLoading
        )

        Spacer(Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Belum punya akun?  ",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )
            Text(
                text = "Daftar",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Pink500,
                modifier = Modifier.clickable(onClick = onNavigateToRegister)
            )
        }

        Spacer(Modifier.height(28.dp))
        Icon(
            imageVector = Icons.Outlined.GppGood,
            contentDescription = null,
            tint = Pink500,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Data kesehatan Bunda tersimpan aman.\nDengan masuk, Bunda menyetujui kebijakan privasi.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
    }
}

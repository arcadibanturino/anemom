package com.kelompok.anemom.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PregnantWoman
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomTextField
import com.kelompok.anemom.ui.components.AnemomTopBar
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.ErrorRed
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import com.kelompok.anemom.util.DateFormats
import com.kelompok.anemom.util.PregnancyCalculator
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var trimesterMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        AnemomTopBar(
            title = "Daftar Anemom",
            subtitle = "Mulai perjalanan sehat Bunda",
            onBack = onBack
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Halo, calon Bunda hebat!",
            style = MaterialTheme.typography.headlineMedium,
            color = Maroon
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Lengkapi data berikut agar Anemom dapat menemani Bunda dengan lebih personal.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText
        )
        Spacer(Modifier.height(20.dp))

        AnemomTextField(
            label = "Nama lengkap",
            value = state.name,
            onValueChange = viewModel::onNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Nama lengkap Bunda",
            leadingIcon = Icons.Outlined.Person,
            errorText = state.nameError
        )
        Spacer(Modifier.height(12.dp))
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
            label = "Nomor telepon",
            value = state.phone,
            onValueChange = viewModel::onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "0812 3456 7890",
            leadingIcon = Icons.Outlined.Phone,
            keyboardType = KeyboardType.Phone,
            errorText = state.phoneError
        )
        Spacer(Modifier.height(12.dp))
        AnemomTextField(
            label = "Kata sandi",
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Minimal 8 karakter",
            leadingIcon = Icons.Outlined.Lock,
            isPassword = true,
            errorText = state.passwordError
        )
        Spacer(Modifier.height(12.dp))
        AnemomTextField(
            label = "Konfirmasi kata sandi",
            value = state.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Ulangi kata sandi",
            leadingIcon = Icons.Outlined.Lock,
            isPassword = true,
            errorText = state.confirmError
        )
        Spacer(Modifier.height(12.dp))
        AnemomTextField(
            label = "Hari Pertama Haid Terakhir (HPHT)",
            value = state.hpht?.let { DateFormats.long(it) }.orEmpty(),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Pilih Tanggal",
            leadingIcon = Icons.Outlined.CalendarMonth,
            readOnly = true,
            onClick = { showDatePicker = true },
            errorText = state.hphtError
        )
        Spacer(Modifier.height(12.dp))
        AnemomTextField(
            label = "Usia Kehamilan",
            value = state.pregnancyWeeks?.takeIf { it >= 0 }?.let { "$it Minggu" }.orEmpty(),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = "- Minggu",
            leadingIcon = Icons.Outlined.Schedule,
            readOnly = true
        )
        Spacer(Modifier.height(12.dp))
        Box {
            AnemomTextField(
                label = "Trimester Kehamilan",
                value = state.trimester?.let { PregnancyCalculator.trimesterLabel(it) }.orEmpty(),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Pilih Trimester",
                leadingIcon = Icons.Outlined.PregnantWoman,
                trailingIcon = Icons.Filled.KeyboardArrowDown,
                readOnly = true,
                onClick = { trimesterMenuOpen = true },
                errorText = state.trimesterError
            )
            DropdownMenu(
                expanded = trimesterMenuOpen,
                onDismissRequest = { trimesterMenuOpen = false }
            ) {
                (1..3).forEach { trimester ->
                    DropdownMenuItem(
                        text = { Text(PregnancyCalculator.trimesterLabel(trimester)) },
                        onClick = {
                            viewModel.onTrimesterSelected(trimester)
                            trimesterMenuOpen = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(16.dp))
                .background(Pink100.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MutedText
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Pastikan data kehamilan diisi dengan benar agar informasi dan rekomendasi lebih akurat.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.agreed,
                onCheckedChange = viewModel::onAgreedChange,
                colors = CheckboxDefaults.colors(checkedColor = Pink500)
            )
            Text(
                text = "Saya menyetujui Syarat Penggunaan dan Kebijakan Privasi Anemom.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
                modifier = Modifier.clickable { viewModel.onAgreedChange(!state.agreed) }
            )
        }

        if (state.errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.errorMessage.orEmpty(),
                color = ErrorRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))
        GradientButton(
            text = "Buat akun",
            onClick = { viewModel.register(onRegisterSuccess) },
            isLoading = state.isLoading
        )

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sudah punya akun? Masuk",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Pink500,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable(onClick = onNavigateToLogin)
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showDatePicker) {
        // HPHT tidak boleh di masa depan (toleransi 1 hari karena picker memakai UTC).
        val pickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis() + 24L * 60 * 60 * 1000
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            viewModel.onHphtSelected(date)
                        }
                        showDatePicker = false
                    }
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

package com.kelompok.anemom.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PregnantWoman
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomDatePickerDialog
import com.kelompok.anemom.ui.components.AnemomOutlinedButton
import com.kelompok.anemom.ui.components.AnemomTextField
import com.kelompok.anemom.ui.components.AnemomTopBar
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.components.MomAvatar
import com.kelompok.anemom.ui.components.NoteCard
import com.kelompok.anemom.ui.theme.ErrorRed
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.util.AgeCalculator
import com.kelompok.anemom.util.DateFormats
import com.kelompok.anemom.util.PregnancyCalculator
import java.time.LocalDate

/**
 * Halaman "Edit profil". Yang bisa diubah: nama/nama panggilan, tanggal lahir, email, HPHT,
 * dan fasilitas kesehatan. Usia, usia kehamilan, trimester, dan HPL hanya ditampilkan
 * (dihitung dari tanggal lahir dan HPHT). Nomor telepon sudah tidak dipakai.
 */
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showBirthDatePicker by remember { mutableStateOf(false) }
    var showHphtPicker by remember { mutableStateOf(false) }

    val firstName = state.name.trim().split(" ").firstOrNull().orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        AnemomTopBar(
            title = "Edit profil",
            subtitle = if (firstName.isBlank()) "Perbarui data Bunda" else "Perbarui data Bunda $firstName",
            onBack = onBack
        )

        if (!state.isLoaded) return@Column

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MomAvatar(size = 76.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = if (firstName.isBlank()) "Bunda" else "Bunda $firstName",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Maroon
                )
                state.ageYears?.let {
                    Text(
                        text = "$it tahun",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText
                    )
                }
            }
        }

        SectionTitle("Data pribadi")
        AnemomTextField(
            label = "Nama atau Nama Panggilan",
            value = state.name,
            onValueChange = viewModel::onNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Nama atau nama panggilan Bunda",
            leadingIcon = Icons.Outlined.Person,
            errorText = state.nameError
        )
        Spacer(Modifier.height(12.dp))
        AnemomTextField(
            label = "Tanggal lahir",
            value = state.birthDate?.let { DateFormats.long(it) }.orEmpty(),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Pilih Tanggal",
            leadingIcon = Icons.Outlined.CalendarMonth,
            readOnly = true,
            onClick = { showBirthDatePicker = true },
            errorText = state.birthDateError
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

        SectionTitle("Data kehamilan")
        AnemomTextField(
            label = "Hari Pertama Haid Terakhir (HPHT)",
            value = state.hpht?.let { DateFormats.long(it) }.orEmpty(),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Pilih Tanggal",
            leadingIcon = Icons.Outlined.CalendarMonth,
            readOnly = true,
            onClick = { showHphtPicker = true },
            errorText = state.hphtError
        )
        Spacer(Modifier.height(12.dp))
        AnemomTextField(
            label = "Usia kehamilan",
            value = state.pregnancyWeeks?.let { "$it Minggu" }.orEmpty(),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = "- Minggu",
            leadingIcon = Icons.Outlined.Schedule,
            readOnly = true
        )
        Spacer(Modifier.height(12.dp))
        AnemomTextField(
            label = "Trimester",
            value = state.trimester?.let { PregnancyCalculator.trimesterLabel(it) }.orEmpty(),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Otomatis dari HPHT",
            leadingIcon = Icons.Outlined.PregnantWoman,
            readOnly = true
        )
        Spacer(Modifier.height(12.dp))
        AnemomTextField(
            label = "Perkiraan tanggal lahir (HPL)",
            value = state.dueDate?.let { DateFormats.long(it) }.orEmpty(),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Otomatis dari HPHT",
            leadingIcon = Icons.Outlined.CalendarMonth,
            readOnly = true
        )
        Spacer(Modifier.height(12.dp))
        AnemomTextField(
            label = "Fasilitas kesehatan",
            value = state.healthFacility,
            onValueChange = viewModel::onFacilityChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Mis. Puskesmas atau klinik Bunda",
            leadingIcon = Icons.Outlined.LocalHospital
        )

        Spacer(Modifier.height(12.dp))
        NoteCard("Usia kehamilan, trimester, dan HPL dihitung otomatis dari HPHT. Sesuaikan HPHT dengan catatan tenaga kesehatan.")

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
            text = "Simpan Perubahan",
            onClick = { viewModel.save(onSaved) },
            isLoading = state.isSaving
        )
        Spacer(Modifier.height(10.dp))
        AnemomOutlinedButton(text = "Batal", onClick = onBack)
        Spacer(Modifier.height(24.dp))
    }

    if (showBirthDatePicker) {
        val today = LocalDate.now()
        AnemomDatePickerDialog(
            initialDate = state.birthDate,
            minDate = today.minusYears(AgeCalculator.MAX_AGE.toLong() + 1),
            maxDate = today.minusYears(AgeCalculator.MIN_AGE.toLong()),
            onDismiss = { showBirthDatePicker = false },
            onDateSelected = viewModel::onBirthDateSelected,
            displayDate = today.minusYears(25)
        )
    }

    if (showHphtPicker) {
        val today = LocalDate.now()
        AnemomDatePickerDialog(
            initialDate = state.hpht,
            minDate = today.minusWeeks(PregnancyCalculator.MAX_WEEKS.toLong()),
            maxDate = today,
            onDismiss = { showHphtPicker = false },
            onDateSelected = viewModel::onHphtSelected
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = Maroon,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
    )
}

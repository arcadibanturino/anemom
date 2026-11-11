package com.kelompok.anemom.ui.skrining

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.data.model.HbRisk
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.components.AnemomTextField
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.components.NoteCard
import com.kelompok.anemom.ui.components.RiskChip
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.util.DateFormats
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Input hasil Hb darah dari laboratorium/fasilitas kesehatan. Disimpan lokal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputHbScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: InputHbViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.consumeSaved()
            onSaved()
        }
    }

    AnemomScreen(
        title = "Input Hb darah",
        subtitle = "Catat hasil tes darah Bunda",
        onBack = onBack
    ) {
        AnemomCard(Modifier.fillMaxWidth()) {
            Text(
                text = if (state.motherName.isBlank()) "Bunda" else "Bunda ${state.motherName}",
                style = MaterialTheme.typography.titleMedium,
                color = Maroon
            )
            state.pregnancyWeek?.let {
                Text("Usia kehamilan $it minggu", style = MaterialTheme.typography.bodySmall, color = MutedText)
            }
        }
        Spacer(Modifier.height(14.dp))

        AnemomTextField(
            label = "Kadar Hb (g/dL)",
            value = state.hbText,
            onValueChange = viewModel::onHbChange,
            placeholder = "Contoh: 10,8",
            leadingIcon = Icons.Outlined.WaterDrop,
            keyboardType = KeyboardType.Decimal,
            errorText = state.hbError
        )
        val preview = state.hbText.replace(',', '.').toFloatOrNull()
        if (preview != null && preview in InputHbViewModel.MIN_HB..InputHbViewModel.MAX_HB) {
            Spacer(Modifier.height(8.dp))
            RiskChip(HbRisk.classify(preview))
        }
        Spacer(Modifier.height(12.dp))
        AnemomTextField(
            label = "Tanggal tes",
            value = DateFormats.long(state.date),
            onValueChange = {},
            leadingIcon = Icons.Outlined.CalendarMonth,
            readOnly = true,
            onClick = { showPicker = true }
        )
        Spacer(Modifier.height(12.dp))
        AnemomTextField(
            label = "Fasilitas kesehatan (opsional)",
            value = state.facility,
            onValueChange = viewModel::onFacilityChange,
            placeholder = "Mis. Puskesmas / Lab",
            leadingIcon = Icons.Outlined.LocalHospital,
            imeAction = ImeAction.Done
        )
        Spacer(Modifier.height(18.dp))
        GradientButton(
            text = "Simpan hasil",
            onClick = viewModel::save,
            isLoading = state.isSaving,
            icon = Icons.Filled.Check
        )
        Spacer(Modifier.height(12.dp))
        NoteCard("Kategori Normal/Risiko sedang/Risiko tinggi hanyalah rujukan aplikasi. Keputusan medis tetap dari tenaga kesehatan.")
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        viewModel.onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Batal") } }
        ) { DatePicker(state = pickerState) }
    }
}

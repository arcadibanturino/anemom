package com.kelompok.anemom.ui.healthprofile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.data.model.AllergyStatus
import com.kelompok.anemom.data.model.AnemiaHistory
import com.kelompok.anemom.data.model.FoodGroup
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.AnemomOutlinedButton
import com.kelompok.anemom.ui.components.AnemomTextField
import com.kelompok.anemom.ui.components.AnemomTopBar
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.ErrorRed
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500

/**
 * Profil Kesehatan Awal (langkah 2 dari 2 pendaftaran): tinggi badan, berat sebelum hamil,
 * riwayat anemia, alergi makanan (centang dari daftar bahan Anemom), dan LiLA (opsional).
 *
 * [onSaved]: data tersimpan -> buka dashboard. [onLoggedOut]: Bunda memilih keluar dari pendaftaran.
 */
@Composable
fun HealthProfileScreen(
    onSaved: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: HealthProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    // Akun sudah dibuat di langkah 1, jadi "kembali" tidak membuka Daftar lagi: tawarkan keluar.
    BackHandler { showExitDialog = true }

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
            title = "Profil Kesehatan Awal",
            subtitle = "Langkah 2 dari 2 · Lengkapi profil Bunda",
            onBack = { showExitDialog = true }
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Kenali kebutuhan Bunda",
            style = MaterialTheme.typography.headlineMedium,
            color = Maroon
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Data ini membantu menyesuaikan saran makanan dan skrining, bukan untuk diagnosis.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText
        )
        Spacer(Modifier.height(20.dp))

        AnemomTextField(
            label = "Tinggi badan (cm) *",
            value = state.height,
            onValueChange = viewModel::onHeightChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Contoh: 158",
            keyboardType = KeyboardType.Decimal,
            errorText = state.heightError
        )
        Spacer(Modifier.height(12.dp))
        AnemomTextField(
            label = "Berat sebelum hamil (kg) *",
            value = state.weight,
            onValueChange = viewModel::onWeightChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Contoh: 52",
            keyboardType = KeyboardType.Decimal,
            errorText = state.weightError
        )
        Spacer(Modifier.height(12.dp))

        ChoiceField(
            label = "Riwayat anemia *",
            options = AnemiaHistory.entries,
            selected = state.anemia,
            optionLabel = { it.label },
            onSelect = viewModel::onAnemiaChange,
            hint = if (state.anemia == null) "Belum dipilih" else "Pilihan Bunda: ${state.anemia?.label}",
            errorText = state.anemiaError
        )
        Spacer(Modifier.height(12.dp))

        ChoiceField(
            label = "Alergi makanan *",
            options = AllergyStatus.entries,
            selected = state.allergy,
            optionLabel = { it.label },
            onSelect = viewModel::onAllergyChange,
            hint = when (state.allergy) {
                null -> "Belum dipilih"
                AllergyStatus.ADA -> "Pilihan Bunda: Ada"
                else -> "Pilihan Bunda: ${state.allergy?.label} · Pilih Ada untuk menandai bahan makanan"
            },
            errorText = state.allergyError
        )

        // Muncul hanya bila Bunda memilih "Ada": bahan dicentang dari daftar Anemom, tidak diketik.
        AnimatedVisibility(
            visible = state.allergy == AllergyStatus.ADA,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                AllergenChecklist(
                    options = state.options,
                    selected = state.selectedAllergens,
                    search = state.allergenSearch,
                    errorText = state.allergenError,
                    onSearchChange = viewModel::onAllergenSearchChange,
                    onToggle = viewModel::onToggleAllergen,
                    onRetry = viewModel::loadOptions
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        AnemomTextField(
            label = "LiLA (cm) · Opsional",
            value = state.lila,
            onValueChange = viewModel::onLilaChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Contoh: 25",
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
            onImeAction = { focusManager.clearFocus() },
            errorText = state.lilaError
        )
        if (state.lilaError == null) {
            Text(
                text = "Lingkar lengan atas; boleh dikosongkan.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Pink100.copy(alpha = 0.75f))
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "* Semua data wajib harus lengkap.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Maroon
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tinggi dan berat berupa angka >0. Pilih satu jawaban pada riwayat anemia dan alergi. " +
                    "Jika Ada, detail alergi wajib diisi.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
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
            text = "Simpan",
            onClick = {
                focusManager.clearFocus()
                viewModel.save(onSaved)
            },
            isLoading = state.isSaving
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Sesudah disimpan, masuk untuk membuka dashboard.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = Color.White,
            title = { Text("Keluar dari pendaftaran?", color = Maroon) },
            text = {
                Text(
                    "Akun Bunda sudah dibuat. Bunda bisa melanjutkan mengisi profil kesehatan saat masuk kembali.",
                    color = MutedText
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    viewModel.logout(onLoggedOut)
                }) { Text("Keluar", color = Pink500, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Tetap di sini", color = MutedText) }
            }
        )
    }
}

/** Pilihan satu dari beberapa (radio) dalam satu kartu bergaya Anemom, dengan teks bantu dan pesan galat. */
@Composable
private fun <T> ChoiceField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    hint: String,
    errorText: String?
) {
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Maroon
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color.White)
                .border(1.dp, if (errorText != null) ErrorRed else CardBorder, shape)
                .padding(horizontal = 12.dp)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .selectable(
                            selected = option == selected,
                            onClick = { onSelect(option) },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = option == selected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Pink500,
                            unselectedColor = MutedText
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = optionLabel(option),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Maroon
                    )
                }
            }
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** Daftar bahan makanan Anemom per kelompok (Sayur, Buah, Protein, Lainnya); Bunda tinggal mencentang. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllergenChecklist(
    options: AllergenOptionsState,
    selected: Set<Long>,
    search: String,
    errorText: String?,
    onSearchChange: (String) -> Unit,
    onToggle: (Long) -> Unit,
    onRetry: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, if (errorText != null) ErrorRed else CardBorder, shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Bahan makanan yang membuat Bunda alergi",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Maroon
                )
                Text(
                    text = "Centang semua yang sesuai.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
            if (selected.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                AnemomChip(text = "${selected.size} dipilih", containerColor = Pink100, contentColor = Maroon)
            }
        }
        Spacer(Modifier.height(12.dp))

        when (options) {
            AllergenOptionsState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Pink500, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Memuat daftar bahan...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }

            is AllergenOptionsState.Failed -> Column {
                Text(
                    text = options.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed
                )
                Spacer(Modifier.height(8.dp))
                AnemomOutlinedButton(text = "Coba lagi", onClick = onRetry)
            }

            is AllergenOptionsState.Ready -> {
                SearchField(value = search, onValueChange = onSearchChange)
                val query = search.trim()
                val visible = options.items.filter { query.isEmpty() || it.name.contains(query, ignoreCase = true) }
                if (visible.isEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Tidak ada bahan yang cocok.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
                FoodGroup.entries.forEach { group ->
                    val items = visible.filter { it.group == group }
                    if (items.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = group.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Maroon
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items.forEach { item ->
                                AllergenChip(
                                    text = item.name,
                                    checked = item.id in selected,
                                    onToggle = { onToggle(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (errorText != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed
            )
        }
    }
}

/** Satu bahan yang bisa dicentang: berisi tanda centang saat dipilih. */
@Composable
private fun AllergenChip(text: String, checked: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(if (checked) Pink100 else Color.White)
            .border(1.dp, if (checked) Pink500 else CardBorder, shape)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Pink500,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
            color = if (checked) Maroon else MutedText
        )
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Cari bahan makanan", color = MutedText.copy(alpha = 0.7f)) },
        leadingIcon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = null) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Pink500,
            unfocusedBorderColor = CardBorder,
            cursorColor = Pink500,
            focusedTextColor = Maroon,
            unfocusedTextColor = Maroon,
            focusedLeadingIconColor = Pink500,
            unfocusedLeadingIconColor = Pink500
        )
    )
}

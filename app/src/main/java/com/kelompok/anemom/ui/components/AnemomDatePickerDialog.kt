package com.kelompok.anemom.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Dialog pilih tanggal yang dipakai Register dan Edit Profil.
 * Hanya tanggal pada rentang [minDate]..[maxDate] yang bisa dipilih.
 * Date picker Material bekerja dalam UTC, jadi konversi dari/ke LocalDate memakai UTC juga.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnemomDatePickerDialog(
    initialDate: LocalDate?,
    minDate: LocalDate,
    maxDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    /** Bulan yang tampil pertama kali bila [initialDate] kosong (mis. 25 tahun lalu untuk tanggal lahir). */
    displayDate: LocalDate = maxDate
) {
    // Tanggal tersimpan di luar rentang akan membuat date picker crash, jadi diabaikan.
    val safeInitial = initialDate?.takeIf { !it.isBefore(minDate) && !it.isAfter(maxDate) }
    val safeDisplay = displayDate.coerceIn(minDate, maxDate)
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = safeInitial?.toUtcMillis(),
        initialDisplayedMonthMillis = (safeInitial ?: safeDisplay).toUtcMillis(),
        yearRange = minDate.year..maxDate.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return !date.isBefore(minDate) && !date.isAfter(maxDate)
            }

            override fun isSelectableYear(year: Int): Boolean = year in minDate.year..maxDate.year
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    onDismiss()
                }
            ) { Text("Pilih") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    ) {
        DatePicker(state = pickerState)
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

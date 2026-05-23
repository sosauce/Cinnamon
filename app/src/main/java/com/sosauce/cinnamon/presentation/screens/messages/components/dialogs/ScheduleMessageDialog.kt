@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.messages.components.dialogs

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.sosauce.cinnamon.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun ScheduleMessageDialog(
    onDismissRequest: () -> Unit,
    onSetScheduledTime: (Long) -> Unit
) {
    val localTime = remember { LocalTime.now() }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
        selectableDates = FutureOrPresentSelectableDates
    )
    val timePickerState = rememberTimePickerState(
        initialHour = localTime.hour,
        initialMinute = localTime.minute,
        is24Hour = true
    )
    var step by remember { mutableStateOf(ScheduleStep.DATE) }


    when (step) {
        ScheduleStep.DATE -> {

            DatePickerDialog(
                onDismissRequest = onDismissRequest,
                confirmButton = {
                    TextButton(
                        onClick = {
                            step = ScheduleStep.TIME
                        }
                    ) { Text(stringResource(R.string.next)) }
                }
            ) { DatePicker(datePickerState) }
        }

        ScheduleStep.TIME -> {
            TimePickerDialog(
                onDismissRequest = { step = ScheduleStep.DATE },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val timeMillis =
                                (timePickerState.hour * 60 * 60 * 1000) + (timePickerState.minute * 60 * 1000)
                            val dateMillis = datePickerState.selectedDateMillis ?: return@TextButton
                            val offsetMillis = ZoneId.systemDefault()
                                .rules
                                .getOffset(Instant.ofEpochMilli(dateMillis))
                                .totalSeconds * 1000L
                            val scheduledTime = dateMillis - offsetMillis + timeMillis
                            onSetScheduledTime(scheduledTime)
                            onDismissRequest()
                        },
                        shapes = ButtonDefaults.shapes(),
                        enabled = timePickerState.hour >= localTime.hour
                    ) { Text("Set") }
                },
                title = { Text("Select time") }
            ) { TimePicker(timePickerState) }
        }
    }
}

private enum class ScheduleStep {
    DATE,
    TIME
}

object FutureOrPresentSelectableDates : SelectableDates {
    private val today = LocalDate.now()

    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val date = Instant.ofEpochMilli(utcTimeMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return !date.isBefore(today)
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year >= today.year
    }
}
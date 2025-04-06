package nl.avans.todo.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import nl.avans.todo.ui.screens.NotificationTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Event


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, LocalDateTime?, Boolean, Int, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var notificationEnabled by remember { mutableStateOf(true) }
    var notificationMinutesBefore by remember { mutableStateOf(30) }
    var addedToCalendar by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var showNotificationMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm") }

    val notificationTimes = remember {
        listOf(
            NotificationTime(5, "5 minutes before"),
            NotificationTime(10, "10 minutes before"),
            NotificationTime(15, "15 minutes before"),
            NotificationTime(30, "30 minutes before"),
            NotificationTime(60, "1 hour before"),
            NotificationTime(120, "2 hours before"),
            NotificationTime(180, "3 hours before"),
            NotificationTime(360, "6 hours before"),
            NotificationTime(720, "12 hours before"),
            NotificationTime(1440, "1 day before")
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val currentDate = selectedDate ?: LocalDate.now()
        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                selectedDate = LocalDate.of(year, month + 1, day)
                showDatePicker = false
                showTimePicker = true
            },
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        ).show()
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val currentTime = selectedTime ?: LocalTime.now()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                selectedTime = LocalTime.of(hour, minute)
                showTimePicker = false
                selectedDate?.let { date ->
                    dueDate = LocalDateTime.of(date, selectedTime!!)
                }
            },
            currentTime.hour,
            currentTime.minute,
            true
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Task Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Due Date Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Due Date",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Date Picker
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select date",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = if (selectedDate != null)
                                    selectedDate!!.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                                else
                                    "Select Date"
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Time Picker
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedDate != null
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Select time",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = if (selectedTime != null)
                                    selectedTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))
                                else
                                    "Select Time"
                            )
                        }

                        if (dueDate != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Selected: ${dueDate!!.format(dateFormatter)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notification Settings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Notification Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Notifications",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (notificationEnabled) {
                                    Text(
                                        text = notificationTimes.find { it.minutes == notificationMinutesBefore }?.label
                                            ?: "$notificationMinutesBefore minutes before",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = notificationEnabled && !addedToCalendar,
                                onCheckedChange = { enabled ->
                                    notificationEnabled = enabled
                                },
                                enabled = !addedToCalendar && dueDate != null
                            )

                            if (notificationEnabled && !addedToCalendar) {
                                IconButton(onClick = { showNotificationMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select notification time"
                                    )
                                }

                                DropdownMenu(
                                    expanded = showNotificationMenu,
                                    onDismissRequest = { showNotificationMenu = false }
                                ) {
                                    notificationTimes.forEach { time ->
                                        DropdownMenuItem(
                                            onClick = {
                                                notificationMinutesBefore = time.minutes
                                                showNotificationMenu = false
                                            },
                                            text = { Text(time.label) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Calendar Integration
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Calendar",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Add to Calendar",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (addedToCalendar)
                                        "Will be added to your calendar"
                                    else
                                        "Not added to calendar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = addedToCalendar,
                                onCheckedChange = {
                                    addedToCalendar = it
                                    if (it) notificationEnabled = false
                                },
                                enabled = dueDate != null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name,
                        description,
                        dueDate,
                        notificationEnabled,
                        notificationMinutesBefore,
                        addedToCalendar
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
package nl.avans.todo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import nl.avans.todo.viewmodels.TodoViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.time.LocalDate
import java.time.LocalTime
import androidx.compose.material.icons.automirrored.filled.ArrowBack

data class NotificationTime(
    val minutes: Int,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoEditScreen(
    navController: NavController,
    todoId: Int,
    todoViewModel: TodoViewModel = viewModel()
) {
    val todos by todoViewModel.todos.collectAsState()
    val todo = remember(todos) { todos.find { it.id == todoId } }
    val isLoading by todoViewModel.isLoading.collectAsState()
    
    var name by remember(todo?.id, todo?.name) { mutableStateOf(todo?.name ?: "") }
    var description by remember(todo?.id, todo?.description) { mutableStateOf(todo?.description ?: "") }
    var completed by remember(todo?.id, todo?.completed) { mutableStateOf(todo?.completed ?: false) }
    var dueDate by remember(todo?.id, todo?.dueDate) { mutableStateOf(todo?.dueDate) }
    var notificationEnabled by remember(todo?.id, todo?.notificationEnabled) { 
        mutableStateOf(todo?.notificationEnabled ?: true) 
    }
    var notificationMinutesBefore by remember(todo?.id, todo?.notificationMinutesBefore) {
        mutableStateOf(todo?.notificationMinutesBefore ?: 30)
    }
    var addedToCalendar by remember(todo?.id, todo?.addedToCalendar) { 
        mutableStateOf(todo?.addedToCalendar ?: false) 
    }
    var showNotificationMenu by remember { mutableStateOf(false) }
    
    // Date and Time picker states
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    
    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm") }

    // Initialize selected date and time from dueDate if it exists
    LaunchedEffect(dueDate) {
        dueDate?.let {
            selectedDate = it.toLocalDate()
            selectedTime = it.toLocalTime()
        }
    }

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

    LaunchedEffect(Unit) {
        if (todos.isEmpty()) {
            todoViewModel.loadTodos()
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val currentDate = selectedDate ?: LocalDate.now()
        DatePickerDialog(
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (todoId > 0) "Edit Task" else "New Task") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            if (todoId > 0) {
                                todo?.let {
                                    todoViewModel.updateTodo(
                                        it.copy(
                                            name = name,
                                            description = description,
                                            completed = completed,
                                            dueDate = dueDate,
                                            notificationEnabled = notificationEnabled,
                                            notificationMinutesBefore = notificationMinutesBefore,
                                            addedToCalendar = addedToCalendar
                                        )
                                    )
                                }
                            } else {
                                todoViewModel.addTodo(
                                    name = name,
                                    description = description,
                                    dueDate = dueDate,
                                    notificationEnabled = notificationEnabled,
                                    notificationMinutesBefore = notificationMinutesBefore,
                                    addedToCalendar = addedToCalendar
                                )
                            }
                            navController.popBackStack()
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Task details
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Button
                    Button(
                        onClick = {
                            if (todoId > 0) {
                                todo?.let {
                                    todoViewModel.updateTodo(
                                        it.copy(
                                            name = name,
                                            description = description,
                                            completed = completed,
                                            dueDate = dueDate,
                                            notificationEnabled = notificationEnabled,
                                            notificationMinutesBefore = notificationMinutesBefore,
                                            addedToCalendar = addedToCalendar
                                        )
                                    )
                                }
                            } else {
                                todoViewModel.addTodo(
                                    name = name,
                                    description = description,
                                    dueDate = dueDate,
                                    notificationEnabled = notificationEnabled,
                                    notificationMinutesBefore = notificationMinutesBefore,
                                    addedToCalendar = addedToCalendar
                                )
                            }
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save Task")
                    }
                }
            }
        }
    }
} 
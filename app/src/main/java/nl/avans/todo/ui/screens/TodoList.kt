package nl.avans.todo.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import nl.avans.todo.data.model.Todo
import nl.avans.todo.data.preferences.SortOrder
import nl.avans.todo.viewmodels.AuthViewModel
import nl.avans.todo.viewmodels.TodoViewModel
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import nl.avans.todo.ui.theme.Warning
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ExitToApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoList(
    navController: NavController,
    todoViewModel: TodoViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val todos by todoViewModel.todos.collectAsState()
    val isLoading by todoViewModel.isLoading.collectAsState()
    val error by todoViewModel.error.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showAddTodoDialog by remember { mutableStateOf(false) }
    var selectedTodo by remember { mutableStateOf<Todo?>(null) }
    
    // Get the current back stack entry for navigation tracking
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    
    // Load todos when screen is shown
    LaunchedEffect(navBackStackEntry?.id) {
        val currentRoute = navBackStackEntry?.destination?.route
        if (currentRoute == "todo_list") {
            todoViewModel.loadTodos()
        }
    }
    
    LaunchedEffect(error) {
        error?.let { 
            coroutineScope.launch {
                snackbarHostState.showSnackbar(it)
                todoViewModel.clearError()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todo List") },
                actions = {
                    var showSortMenu by remember { mutableStateOf(false) }
                    
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOrder.values().forEach { sortOrder ->
                            DropdownMenuItem(
                                onClick = {
                                    todoViewModel.setSortOrder(sortOrder)
                                    showSortMenu = false
                                },
                                text = { 
                                    Text(when (sortOrder) {
                                        SortOrder.CREATED_ASC -> "Oldest First"
                                        SortOrder.CREATED_DESC -> "Newest First"
                                        SortOrder.NAME_ASC -> "Name (A to Z)"
                                        SortOrder.NAME_DESC -> "Name (Z to A)"
                                        SortOrder.DUE_DATE_ASC -> "Due Date (Earliest First)"
                                        SortOrder.DUE_DATE_DESC -> "Due Date (Latest First)"
                                        SortOrder.DUE_SOON_FIRST -> "Overdue & Due Soon First"
                                    })
                                }
                            )
                        }
                    }
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                    IconButton(onClick = { todoViewModel.loadTodos() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { 
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("todo_list") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTodoDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Simple refresh button at the top instead of pull-to-refresh
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
            
            if (isLoading && todos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            } else if (todos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "No tasks found. Add a new task!",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = todos,
                        key = { it.id }
                    ) { todo ->
                        TodoItem(
                            todo = todo,
                            onToggleCompleted = { todoViewModel.toggleTodoCompleted(todo) },
                            onEdit = { navController.navigate("todo_edit/${todo.id}") },
                            onDelete = { todoViewModel.deleteTodo(todo) },
                            onClick = { navController.navigate("todo_detail/${todo.id}") },
                            onLongClick = { navController.navigate("todo_edit/${todo.id}") }
                        )
                    }
                }
            }
            
            if (showAddTodoDialog) {
                AddTodoDialog(
                    onDismiss = { showAddTodoDialog = false },
                    onConfirm = { name, description, dueDate, notificationEnabled, notificationMinutesBefore, addedToCalendar ->
                        todoViewModel.addTodo(name, description, dueDate, notificationEnabled, notificationMinutesBefore, addedToCalendar)
                        showAddTodoDialog = false
                    }
                )
            }
            
            selectedTodo?.let { todo ->
                AddTodoDialog(
                    onDismiss = { selectedTodo = null },
                    onConfirm = { name, description, dueDate, notificationEnabled, notificationMinutesBefore, addedToCalendar ->
                        todoViewModel.updateTodo(todo.copy(name = name, description = description, dueDate = dueDate, notificationEnabled = notificationEnabled, notificationMinutesBefore = notificationMinutesBefore, addedToCalendar = addedToCalendar))
                        selectedTodo = null
                    }
                )
            }
        }
    }
}

@Composable
fun TodoItem(
    todo: Todo,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, HH:mm") }
    val now = remember { LocalDateTime.now() }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(todo.id) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onTap = { onClick() }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.completed,
                onCheckedChange = { onToggleCompleted() }
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = todo.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (todo.completed) TextDecoration.LineThrough else TextDecoration.None
                )
                
                if (todo.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = todo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (todo.completed) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
                
                todo.dueDate?.let { dueDate ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Due date",
                            modifier = Modifier.size(16.dp),
                            tint = when {
                                todo.completed -> MaterialTheme.colorScheme.onSurfaceVariant
                                dueDate < now -> MaterialTheme.colorScheme.error
                                dueDate.minusHours(24) < now -> Warning
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dueDate.format(dateFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                todo.completed -> MaterialTheme.colorScheme.onSurfaceVariant
                                dueDate < now -> MaterialTheme.colorScheme.error
                                dueDate.minusHours(24) < now -> Warning
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        
                        if (todo.notificationEnabled) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications enabled",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (todo.addedToCalendar) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "Added to calendar",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            IconButton(
                onClick = { onDelete() },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete todo"
                )
            }
        }
    }
}

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
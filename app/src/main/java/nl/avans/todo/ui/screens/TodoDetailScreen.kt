package nl.avans.todo.ui.screens

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import nl.avans.todo.viewmodels.TodoViewModel
import nl.avans.todo.utils.PermissionHandler
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetailScreen(
    navController: NavController,
    todoId: Int,
    todoViewModel: TodoViewModel = viewModel()
) {
    val context = LocalContext.current
    val todos by todoViewModel.todos.collectAsState()
    val todo = remember(todos) { todos.find { it.id == todoId } }
    val isLoading by todoViewModel.isLoading.collectAsState()
    val error by todoViewModel.error.collectAsState()
    val showNotificationPermissionRequest by todoViewModel.showNotificationPermissionRequest.collectAsState()
    val showExactAlarmPermissionRequest by todoViewModel.showExactAlarmPermissionRequest.collectAsState()
    
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm") }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            todo?.let {
                todoViewModel.toggleCalendarEvent(it)
                Toast.makeText(
                    context,
                    if (!it.addedToCalendar) "Added to calendar" else "Removed from calendar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                "Calendar permissions are required to add events",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        todoViewModel.onNotificationPermissionResult(isGranted)
        if (isGranted && todo != null) {
            todoViewModel.toggleNotifications(todo)
        }
    }

    // Refresh todos when entering the screen
    LaunchedEffect(todoId) {
        todoViewModel.loadTodos()
    }

    // Show loading state
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Show error if todo not found
    if (todo == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Task not found")
        }
        return
    }

    // Reload todos when returning from edit screen
    LaunchedEffect(currentRoute) {
        if (currentRoute == "todo_detail/$todoId") {
            todoViewModel.loadTodos()
        }
    }

    // Show error message if any
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            todoViewModel.clearError()
        }
    }

    // Show notification permission dialog
    if (showNotificationPermissionRequest) {
        AlertDialog(
            onDismissRequest = { todoViewModel.onNotificationPermissionResult(false) },
            title = { Text("Notification Permission Required") },
            text = { Text("This app needs notification permission to send you reminders about your tasks.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { todoViewModel.onNotificationPermissionResult(false) }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Show exact alarm permission dialog
    if (showExactAlarmPermissionRequest) {
        AlertDialog(
            onDismissRequest = { todoViewModel.onExactAlarmPermissionResult(false) },
            title = { Text("Exact Alarm Permission Required") },
            text = { Text("This app needs exact alarm permission to send you accurate reminders about your tasks.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        context.startActivity(intent)
                        todoViewModel.onExactAlarmPermissionResult(true)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { todoViewModel.onExactAlarmPermissionResult(false) }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(todo?.name ?: "Task Details") },
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
                            todo?.let { 
                                navController.navigate("todo_edit/${it.id}") 
                            }
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(
                        onClick = {
                            todo?.let { todo ->
                                todoViewModel.deleteTodo(todo)
                                navController.popBackStack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete todo"
                        )
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status: ",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (todo.completed) "Completed" else "Active",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (todo.completed) 
                            MaterialTheme.colorScheme.primary
                        else 
                            MaterialTheme.colorScheme.tertiary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Task",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = todo.name,
                    style = MaterialTheme.typography.headlineMedium,
                    textDecoration = if (todo.completed) TextDecoration.LineThrough else TextDecoration.None
                )
                
                if (todo.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = todo.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                if (todo.dueDate != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Due Date",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = todo.dueDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (todo.notificationEnabled) 
                                Icons.Default.Notifications 
                            else 
                                Icons.Default.NotificationsOff,
                            contentDescription = "Notifications",
                            tint = if (todo.notificationEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = if (todo.notificationEnabled) 
                                "Notifications enabled"
                            else 
                                "Notifications disabled",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        if (todo.addedToCalendar) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "Added to Calendar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { todoViewModel.toggleTodoCompleted(todo) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (todo.completed) "Mark as Active" else "Mark as Completed")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { navController.navigate("todo_edit/$todoId") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Edit Task")
                    }
                }
                
                if (todo.dueDate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { todoViewModel.toggleNotifications(todo) },
                            modifier = Modifier.weight(1f),
                            enabled = !todo.addedToCalendar
                        ) {
                            Text(
                                if (todo.notificationEnabled) 
                                    "Disable Notifications" 
                                else 
                                    "Enable Notifications"
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        OutlinedButton(
                            onClick = { 
                                if (!todo.addedToCalendar) {
                                    if (PermissionHandler.hasCalendarPermission(context)) {
                                        todoViewModel.toggleCalendarEvent(todo)
                                        Toast.makeText(
                                            context,
                                            "Added to calendar",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        calendarPermissionLauncher.launch(arrayOf(
                                            Manifest.permission.READ_CALENDAR,
                                            Manifest.permission.WRITE_CALENDAR
                                        ))
                                    }
                                } else {
                                    todoViewModel.toggleCalendarEvent(todo)
                                    Toast.makeText(
                                        context,
                                        "Removed from calendar",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (todo.addedToCalendar) 
                                    "Remove from Calendar" 
                                else 
                                    "Add to Calendar"
                            )
                        }
                    }
                }

                // Test Notification Button
                if (todo.notificationEnabled && todo.dueDate != null) {
                    OutlinedButton(
                        onClick = { todoViewModel.sendTestNotification(todo) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Test notification",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Test Notification")
                    }
                }
            }
        }
    }
} 
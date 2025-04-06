package nl.avans.todo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import nl.avans.todo.data.model.Todo
import nl.avans.todo.data.preferences.SortOrder
import nl.avans.todo.viewmodels.AuthViewModel
import nl.avans.todo.viewmodels.TodoViewModel
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import nl.avans.todo.ui.components.AddTodoDialog
import nl.avans.todo.ui.components.TodoItem

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


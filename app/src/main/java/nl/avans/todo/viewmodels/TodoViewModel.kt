package nl.avans.todo.viewmodels

import android.app.Application
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.avans.todo.data.api.TodoService
import nl.avans.todo.data.model.Todo
import nl.avans.todo.data.preferences.SortOrder
import nl.avans.todo.data.preferences.UserPreferences
import nl.avans.todo.utils.SessionManager
import nl.avans.todo.notifications.NotificationService
import nl.avans.todo.calendar.CalendarService
import java.time.LocalDateTime

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    val todos: StateFlow<List<Todo>> = _todos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showNotificationPermissionRequest = MutableStateFlow(false)
    val showNotificationPermissionRequest: StateFlow<Boolean> = _showNotificationPermissionRequest.asStateFlow()

    private val _showExactAlarmPermissionRequest = MutableStateFlow(false)
    val showExactAlarmPermissionRequest: StateFlow<Boolean> = _showExactAlarmPermissionRequest.asStateFlow()

    private val userPreferences = UserPreferences(application)
    private val notificationService = NotificationService(application)
    private val calendarService = CalendarService(application)

    init {
        loadTodos()
    }

    fun clearError() {
        _error.value = null
    }

    fun setSortOrder(sortOrder: SortOrder) {
        userPreferences.sortOrder = sortOrder
        loadTodos()
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun requestNotificationPermission() {
        _showNotificationPermissionRequest.value = true
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _showNotificationPermissionRequest.value = false
        if (!granted) {
            _error.value = "Notification permission is required for reminders"
        }
    }

    fun hasExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun requestExactAlarmPermission() {
        _showExactAlarmPermissionRequest.value = true
    }

    fun onExactAlarmPermissionResult(granted: Boolean) {
        _showExactAlarmPermissionRequest.value = false
        if (!granted) {
            _error.value = "Exact alarm permission is required for accurate reminders"
        }
    }

    fun loadTodos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val token = SessionManager.getToken(getApplication()) ?: return@launch
                val userId = SessionManager.getUserId(getApplication())
                val sortOrder = userPreferences.sortOrder.apiValue
                val todoList = TodoService.getTasks(token, userId, sortOrder)
                
                if (todoList != null) {
                    _todos.value = todoList
                } else {
                    _error.value = "Failed to load tasks"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addTodo(
        name: String,
        description: String,
        dueDate: LocalDateTime?,
        notificationEnabled: Boolean,
        notificationMinutesBefore: Int,
        addedToCalendar: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken(getApplication()) ?: return@launch
                val userId = SessionManager.getUserId(getApplication())
                val todo = Todo(
                    id = 0,  // New todo, ID will be assigned by the server
                    name = name,
                    description = description,
                    dueDate = dueDate,
                    notificationEnabled = notificationEnabled,
                    notificationMinutesBefore = notificationMinutesBefore,
                    addedToCalendar = addedToCalendar
                )
                val result = TodoService.addTask(token, todo, userId)
                if (result != null) {
                    loadTodos()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTodo(todo: Todo) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken(getApplication()) ?: return@launch
                val result = TodoService.updateTask(token, todo)
                if (result != null) {
                    loadTodos()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleNotifications(todo: Todo) {
        if (!todo.notificationEnabled) {
            if (!hasNotificationPermission()) {
                requestNotificationPermission()
                return
            }
        }

        val updatedTodo = todo.copy(notificationEnabled = !todo.notificationEnabled)
        updateTodo(updatedTodo)
    }

    fun toggleCalendarEvent(todo: Todo) {
        if (!todo.addedToCalendar) {
            if (!calendarService.hasCalendarPermission()) {
                _error.value = "Calendar permission is required to add events"
                return
            }
            
            val success = calendarService.addTodoToCalendar(todo)
            if (!success) {
                _error.value = "Failed to add event to calendar"
                return
            }
        } else {
            calendarService.removeTodoFromCalendar(todo)
        }
        
        val updatedTodo = todo.copy(
            addedToCalendar = !todo.addedToCalendar,
            notificationEnabled = if (!todo.addedToCalendar) false else todo.notificationEnabled
        )
        updateTodo(updatedTodo)
    }

    fun toggleTodoCompleted(todo: Todo) {
        val updatedTodo = todo.copy(completed = !todo.completed)
        updateTodo(updatedTodo)
    }

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val token = SessionManager.getToken(getApplication()) ?: return@launch
                val success = TodoService.deleteTask(token, todo.id)
                
                if (success) {
                    _todos.value = _todos.value.filter { it.id != todo.id }
                    notificationService.cancelNotification(todo.id)
                } else {
                    _error.value = "Failed to delete task"
                    loadTodos()
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete todo: ${e.message}"
                loadTodos()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendTestNotification(todo: Todo) {
        notificationService.showNotification(todo, todo.notificationMinutesBefore)
    }

    fun handleSharedContent(content: String) {
        if (content.isBlank()) {
            _error.value = "No content to share"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val token = SessionManager.getToken(getApplication()) ?: return@launch
                val userId = SessionManager.getUserId(getApplication())
                
                // Try to extract title and description from the content
                val (title, description) = parseSharedContent(content)
                
                val newTodo = Todo(
                    id = 0,  // New todo, ID will be assigned by the server
                    name = title,
                    description = description
                )
                
                val result = TodoService.addTask(token, newTodo, userId)
                
                if (result != null) {
                    loadTodos()
                } else {
                    _error.value = "Failed to create task from shared content"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parseSharedContent(content: String): Pair<String, String> {
        // If it's a URL, use the domain as title and full URL as description
        if (content.startsWith("http://") || content.startsWith("https://")) {
            val url = java.net.URL(content)
            val title = url.host
            return Pair(title, content)
        }
        
        // For text content, use first line as title and rest as description
        val lines = content.lines()
        val title = lines.firstOrNull() ?: "Shared Content"
        val description = if (lines.size > 1) {
            lines.drop(1).joinToString("\n")
        } else {
            ""
        }
        
        return Pair(title, description)
    }

    fun markAsCompleted(todo: Todo) {
        val updatedTodo = todo.copy(completed = !todo.completed)
        updateTodo(updatedTodo)
    }
} 
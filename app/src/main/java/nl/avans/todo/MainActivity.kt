package nl.avans.todo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import nl.avans.todo.data.api.SupabaseClient
import nl.avans.todo.ui.screens.LoginScreen
import nl.avans.todo.ui.screens.RegisterScreen
import nl.avans.todo.ui.screens.TodoList
import nl.avans.todo.ui.screens.TodoDetailScreen
import nl.avans.todo.ui.screens.TodoEditScreen
import nl.avans.todo.ui.screens.UserProfileScreen
import nl.avans.todo.ui.theme.TodoTheme
import nl.avans.todo.utils.SessionManager
import nl.avans.todo.viewmodels.AuthViewModel
import nl.avans.todo.viewmodels.TodoViewModel

class MainActivity : ComponentActivity() {
    private val todoViewModel: TodoViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        
        SupabaseClient.initialize(this)

        setContent {
            TodoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authState by authViewModel.authState.collectAsState()
                    
                    // Check for existing session
                    LaunchedEffect(Unit) {
                        val token = SessionManager.getToken(this@MainActivity)
                        val userId = SessionManager.getUserId(this@MainActivity)
                        if (token != null && userId != null) {
                            // Try to get user info to verify the session is still valid
                            try {
                                val user = authViewModel.user.value
                                if (user != null) {
                                    navController.navigate("todo_list") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            } catch (e: Exception) {
                                // If we can't verify the session, clear it
                                SessionManager.clearSession(this@MainActivity)
                            }
                        }
                    }
                    
                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") { LoginScreen(navController, authViewModel) }
                        composable("register") { RegisterScreen(navController) }
                        composable("todo_list") { TodoList(navController, todoViewModel, authViewModel) }
                        composable("profile") { UserProfileScreen(navController) }
                        composable("todo_detail/{todoId}") { backStackEntry ->
                            val todoId = backStackEntry.arguments?.getString("todoId")?.toIntOrNull()
                            if (todoId != null) {
                                TodoDetailScreen(navController, todoId, todoViewModel)
                            }
                        }
                        composable("todo_edit/{todoId}") { backStackEntry ->
                            val todoId = backStackEntry.arguments?.getString("todoId")?.toIntOrNull()
                            if (todoId != null) {
                                TodoEditScreen(navController, todoId, todoViewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                        todoViewModel.handleSharedContent(text)
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                intent.data?.toString()?.let { url ->
                    todoViewModel.handleSharedContent(url)
                }
            }
        }
    }
}

@Composable
fun TodoApp(application: android.app.Application) {
    val navController = rememberNavController()
    val startDestination = "login"

    LaunchedEffect(Unit) {
        val token = SessionManager.getToken(application)
        if (token != null) {
            navController.navigate("todo_list") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    NavHost(navController, startDestination = startDestination) {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("todo_list") { TodoList(navController) }
        composable("profile") { UserProfileScreen(navController) }
        composable("todo_detail/{todoId}") { backStackEntry ->
            val todoId = backStackEntry.arguments?.getString("todoId")?.toIntOrNull()
            if (todoId != null) {
                TodoDetailScreen(navController, todoId)
            }
        }
        composable("todo_edit/{todoId}") { backStackEntry ->
            val todoId = backStackEntry.arguments?.getString("todoId")?.toIntOrNull()
            if (todoId != null) {
                TodoEditScreen(navController, todoId)
            }
        }
    }
}


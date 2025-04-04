package nl.avans.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import nl.avans.todo.data.api.SupabaseClient
import nl.avans.todo.ui.screens.LoginScreen
import nl.avans.todo.ui.screens.RegisterScreen
import nl.avans.todo.ui.screens.TodoList
import nl.avans.todo.ui.theme.TodoTheme
import nl.avans.todo.utils.SessionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SupabaseClient.initialize(this)

        setContent {
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
                composable("todo_list") { TodoList() }
            }
        }
    }
}


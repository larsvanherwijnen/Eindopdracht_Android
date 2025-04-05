package nl.avans.todo.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.avans.todo.data.api.AuthService
import nl.avans.todo.data.model.User
import nl.avans.todo.utils.SessionManager

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val _authState = MutableStateFlow<Boolean?>(null)
    val authState: StateFlow<Boolean?> = _authState.asStateFlow()

    private val _updateState = MutableStateFlow<Boolean?>(null)
    val updateState: StateFlow<Boolean?> = _updateState.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _error = MutableStateFlow<String>("")
    val error: StateFlow<String> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = ""
            try {
                val result = AuthService.login(email, password)
                if (result != null) {
                    val (token, user) = result
                    SessionManager.saveToken(getApplication(), token)
                    _user.value = user
                    _authState.value = true
                } else {
                    _error.value = "Login failed"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = ""
            try {
                val result = AuthService.register(email, password)
                if (result != null) {
                    // After registration, automatically log in
                    login(email, password)
                } else {
                    _error.value = "Registration failed"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val token = SessionManager.getToken(getApplication()) ?: return@launch
            AuthService.logout(token)
            SessionManager.clearSession(getApplication())
            _authState.value = false
            _user.value = null
        }
    }

    fun updateUserInfo(email: String? = null, password: String? = null) {
        viewModelScope.launch {
            val token = SessionManager.getToken(getApplication()) ?: return@launch
            try {
                val updatedUser = AuthService.updateUser(token, email, password)
                _updateState.value = updatedUser != null
                if (updatedUser != null) {
                    _user.value = updatedUser
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error updating user info: ${e.message}")
                _updateState.value = false
            }
        }
    }

    fun clearUpdateState() {
        _updateState.value = null
    }
}
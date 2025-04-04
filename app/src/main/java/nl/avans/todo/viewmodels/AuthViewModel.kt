package nl.avans.todo.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import nl.avans.todo.data.api.AuthService
import nl.avans.todo.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val _authState = MutableStateFlow<Boolean?>(value = null)
    val authState: StateFlow<Boolean?> = _authState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val token = AuthService.login(email, password)
            if (token != null) {
                SessionManager.saveToken(getApplication(), token)
                _authState.value = true
            } else {
                _authState.value = false
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            val success = AuthService.register(email, password)
//            _authState.value = success
        }
    }

    fun logout() {
        viewModelScope.launch {
            val token = SessionManager.getToken(getApplication()) ?: return@launch
            AuthService.logout(token)
            SessionManager.clearSession(getApplication())
            _authState.value = false
        }
    }
}
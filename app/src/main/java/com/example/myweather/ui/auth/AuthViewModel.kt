package com.example.myweather.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myweather.R
import com.example.myweather.data.WeatherRepository
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class Error(val messageResId: Int) : AuthState()
}

class AuthViewModel(private val repository: WeatherRepository) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error(R.string.error_empty_fields)
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                repository.login(email, password)
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapError(e))
            }
        }
    }

    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error(R.string.error_empty_fields)
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                repository.register(email, password)
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapError(e))
            }
        }
    }

    private fun mapError(e: Exception): Int {
        return when (e) {
            is FirebaseAuthWeakPasswordException -> R.string.error_weak_password
            is FirebaseAuthInvalidCredentialsException -> R.string.error_invalid_credentials
            is FirebaseAuthUserCollisionException -> R.string.error_email_already_in_use
            is FirebaseAuthException -> {
                when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" -> R.string.error_invalid_email
                    "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD" -> R.string.error_invalid_credentials
                    else -> R.string.error_unknown
                }
            }
            else -> R.string.error_unknown
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

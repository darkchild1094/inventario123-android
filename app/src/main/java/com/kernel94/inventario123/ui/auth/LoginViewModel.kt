package com.kernel94.inventario123.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.repository.AuthRepository
import com.kernel94.inventario123.data.repository.ResultadoLogin
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val cuentasGuardadas = authRepository.cuentasGuardadas

    fun login(onExito: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            error = "Ingresa tu correo y contraseña."
            return
        }
        cargando = true
        error = null
        viewModelScope.launch {
            when (val r = authRepository.login(email.trim(), password)) {
                is ResultadoLogin.Exito -> { cargando = false; onExito() }
                is ResultadoLogin.Error -> { cargando = false; error = r.mensaje }
            }
        }
    }

    fun eliminarCuenta(email: String) {
        viewModelScope.launch { authRepository.eliminarCuentaGuardada(email) }
    }
}

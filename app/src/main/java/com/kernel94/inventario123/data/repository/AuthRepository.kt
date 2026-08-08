package com.kernel94.inventario123.data.repository

import com.kernel94.inventario123.data.model.LoginRequest
import com.kernel94.inventario123.data.model.Perfil
import com.kernel94.inventario123.data.remote.ApiService
import com.kernel94.inventario123.data.remote.SessionManager

sealed class ResultadoLogin {
    data class Exito(val nombre: String, val tipo: String) : ResultadoLogin()
    data class Error(val mensaje: String) : ResultadoLogin()
}

class AuthRepository(private val api: ApiService, private val sessionManager: SessionManager) {
    suspend fun login(email: String, password: String): ResultadoLogin = try {
        val resp = api.login(LoginRequest(email, password))
        if (resp.success && !resp.session_id.isNullOrBlank() && resp.usuario != null) {
            sessionManager.guardarSesion(
                resp.session_id, resp.usuario.id, resp.usuario.nombre, 
                resp.usuario.tipo, email, resp.usuario.foto
            )
            ResultadoLogin.Exito(resp.usuario.nombre, resp.usuario.tipo)
        } else {
            ResultadoLogin.Error(resp.message ?: "Credenciales incorrectas.")
        }
    } catch (e: Exception) {
        ResultadoLogin.Error("No se pudo conectar al servidor. Verifica tu internet.")
    }

    suspend fun logout() {
        try { api.logout() } catch (_: Exception) {}
        sessionManager.cerrarSesion()
    }

    suspend fun haySesionActiva(): Boolean = sessionManager.haySesionActiva()
    suspend fun obtenerPerfil(): Perfil? = try { api.obtenerPerfil() } catch (e: Exception) { null }

    val cuentasGuardadas = sessionManager.cuentasGuardadasFlow
    suspend fun eliminarCuentaGuardada(email: String) = sessionManager.eliminarCuentaGuardada(email)
}

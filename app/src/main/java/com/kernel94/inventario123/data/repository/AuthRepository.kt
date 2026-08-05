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
        when {
            !resp.success -> ResultadoLogin.Error(resp.message ?: "Credenciales incorrectas.")
            resp.usuario == null -> ResultadoLogin.Error("Respuesta del servidor incompleta (usuario nulo).")
            resp.session_id.isNullOrBlank() -> {
                // Debug: session_id vacío pero login dice success=true. Problema en el backend.
                android.util.Log.e("AuthRepository", "session_id vacío en respuesta login. Resp: ${resp.message}")
                ResultadoLogin.Error("Sesión vacía. Contacta al administrador.")
            }
            else -> {
                sessionManager.guardarSesion(resp.session_id, resp.usuario.id, resp.usuario.nombre, resp.usuario.tipo)
                ResultadoLogin.Exito(resp.usuario.nombre, resp.usuario.tipo)
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("AuthRepository", "Error login: ${e.message}", e)
        ResultadoLogin.Error("No se pudo conectar al servidor. Verifica tu internet.")
    }

    suspend fun logout() {
        try { api.logout() } catch (_: Exception) {}
        sessionManager.cerrarSesion()
    }

    suspend fun haySesionActiva(): Boolean = sessionManager.haySesionActiva()
    suspend fun obtenerPerfil(): Perfil? = try { api.obtenerPerfil() } catch (e: Exception) { null }
}

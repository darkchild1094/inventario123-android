package com.kernel94.inventario123.data.repository

import com.google.gson.Gson
import com.kernel94.inventario123.data.model.ApiResultado
import com.kernel94.inventario123.data.model.Usuario
import com.kernel94.inventario123.data.remote.ApiService
import retrofit2.HttpException

class UsuarioRepository(private val api: ApiService) {
    suspend fun listar(): Resultado<List<Usuario>> = try {
        Resultado.Exito(api.listarUsuarios())
    } catch (e: Exception) {
        Resultado.Error("No se pudo cargar la lista de usuarios.")
    }

    suspend fun obtener(id: Int): Resultado<Usuario> = try {
        Resultado.Exito(api.obtenerUsuario(id))
    } catch (e: Exception) {
        Resultado.Error("No se pudo cargar el usuario.")
    }

    suspend fun crear(datos: Map<String, Any?>): Resultado<ApiResultado> =
        conMotivoDelServidor("No se pudo crear el usuario.") { api.guardarUsuario(datos) }

    suspend fun actualizar(datos: Map<String, Any?>): Resultado<ApiResultado> =
        conMotivoDelServidor("No se pudo actualizar el usuario.") { api.actualizarUsuario(datos) }

    suspend fun eliminar(id: Int): Resultado<ApiResultado> =
        conMotivoDelServidor("No se pudo eliminar.") { api.eliminarUsuario(id) }

    /**
     * Muestra el motivo que da el servidor, no uno genérico.
     *
     * Antes todo fallo caía en un `catch (e: Exception)` que decía "No se pudo
     * conectar al servidor", y los 4xx/5xx de Retrofit llegan precisamente como
     * excepción: así que un "Debes seleccionar al menos una plaza" o un "No
     * tienes permiso" se veían como problema de red. Eso dejó a un admin sin
     * poder cambiar una contraseña y sin saber por qué.
     */
    private suspend fun conMotivoDelServidor(
        fallback: String,
        bloque: suspend () -> ApiResultado,
    ): Resultado<ApiResultado> = try {
        val r = bloque()
        if (r.success) Resultado.Exito(r) else Resultado.Error(r.message ?: fallback)
    } catch (e: HttpException) {
        val cuerpo = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val msg = runCatching { Gson().fromJson(cuerpo, ApiResultado::class.java) }.getOrNull()?.message
        Resultado.Error(msg ?: "$fallback (error ${e.code()})")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }
}

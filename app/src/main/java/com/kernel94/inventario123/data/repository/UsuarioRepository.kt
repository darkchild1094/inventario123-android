package com.kernel94.inventario123.data.repository

import com.kernel94.inventario123.data.model.ApiResultado
import com.kernel94.inventario123.data.model.Usuario
import com.kernel94.inventario123.data.remote.ApiService

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

    suspend fun crear(datos: Map<String, Any?>): Resultado<ApiResultado> = try {
        val r = api.guardarUsuario(datos)
        if (r.success) Resultado.Exito(r) else Resultado.Error(r.message ?: "No se pudo crear el usuario.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun actualizar(datos: Map<String, Any?>): Resultado<ApiResultado> = try {
        val r = api.actualizarUsuario(datos)
        if (r.success) Resultado.Exito(r) else Resultado.Error(r.message ?: "No se pudo actualizar el usuario.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun eliminar(id: Int): Resultado<ApiResultado> = try {
        val r = api.eliminarUsuario(id)
        if (r.success) Resultado.Exito(r) else Resultado.Error(r.message ?: "No se pudo eliminar.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }
}

package com.kernel94.inventario123.data.repository

import com.kernel94.inventario123.data.model.ApiResultado
import com.kernel94.inventario123.data.model.Modelo
import com.kernel94.inventario123.data.remote.ApiService

/** CRUD del catálogo de modelos (solo admin). Espeja UsuarioRepository. */
class ModeloRepository(private val api: ApiService) {

    suspend fun listar(): Resultado<List<Modelo>> = try {
        Resultado.Exito(api.listarModelos())
    } catch (e: Exception) {
        Resultado.Error("No se pudo cargar el catálogo de modelos.")
    }

    suspend fun guardar(datos: Map<String, Any?>): Resultado<ApiResultado> = try {
        val r = api.guardarModelo(datos)
        if (r.success) Resultado.Exito(r) else Resultado.Error(r.message ?: "No se pudo guardar el modelo.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun actualizar(datos: Map<String, Any?>): Resultado<ApiResultado> = try {
        val r = api.actualizarModelo(datos)
        if (r.success) Resultado.Exito(r) else Resultado.Error(r.message ?: "No se pudo actualizar el modelo.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    /** reasignarA = modelo destino cuando el modelo a borrar tiene activos. */
    suspend fun eliminar(id: Int, reasignarA: Int? = null): Resultado<ApiResultado> = try {
        val body = mutableMapOf<String, Any?>("id" to id)
        if (reasignarA != null) body["reasignar_a"] = reasignarA
        val r = api.eliminarModelo(body)
        if (r.success) Resultado.Exito(r) else Resultado.Error(r.message ?: "No se pudo eliminar el modelo.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }
}

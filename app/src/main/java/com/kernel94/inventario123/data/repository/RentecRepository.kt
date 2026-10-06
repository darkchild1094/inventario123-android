package com.kernel94.inventario123.data.repository

import com.google.gson.Gson
import com.kernel94.inventario123.data.model.ApiResultado
import com.kernel94.inventario123.data.model.ProyectoRentec
import com.kernel94.inventario123.data.remote.ApiService
import retrofit2.HttpException

/** Proyectos de Renovación Tecnológica (RENTEC): folio, activos recibidos/instalados. */
class RentecRepository(private val api: ApiService) {

    suspend fun listar(): Resultado<List<ProyectoRentec>> = try {
        Resultado.Exito(api.rentecListar())
    } catch (e: Exception) {
        Resultado.Error("No se pudieron cargar los proyectos RENTEC.")
    }

    suspend fun crear(nombre: String): Resultado<ProyectoRentec> = try {
        val r = api.rentecCrear(mapOf("nombre" to nombre))
        if (r.success && r.id != null) {
            // Pide el registro real (usuario_nombre, creado_en, etc.) en vez de
            // fabricarlo localmente con datos a medias.
            Resultado.Exito(api.rentecDetalle(r.id))
        } else {
            Resultado.Error(r.message ?: "No se pudo crear el proyecto.")
        }
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun detalle(id: Int): Resultado<ProyectoRentec> = try {
        Resultado.Exito(api.rentecDetalle(id))
    } catch (e: Exception) {
        Resultado.Error("No se pudo cargar el proyecto.")
    }

    suspend fun cerrar(id: Int): Resultado<ProyectoRentec> = try {
        val r = api.rentecCerrar(mapOf("id" to id))
        if (r.success && r.proyecto != null) Resultado.Exito(r.proyecto)
        else Resultado.Error(r.message ?: "No se pudo cerrar el proyecto.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    /**
     * Borra el folio. El servidor sólo deja borrar los que no tienen huella; si
     * ya tiene equipo o bitácora responde 409, y ese mensaje se muestra tal cual
     * porque explica cuánto tiene y qué hacer en su lugar (cerrarlo).
     */
    suspend fun eliminar(id: Int): Resultado<String> = try {
        val r = api.rentecEliminar(mapOf("id" to id))
        if (r.success) Resultado.Exito(r.message ?: "Proyecto borrado.")
        else Resultado.Error(r.message ?: "No se pudo borrar el proyecto.")
    } catch (e: HttpException) {
        val cuerpo = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val msg = runCatching { Gson().fromJson(cuerpo, ApiResultado::class.java) }.getOrNull()?.message
        Resultado.Error(msg ?: "No se pudo borrar el proyecto.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }
}

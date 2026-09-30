package com.kernel94.inventario123.data.repository

import com.kernel94.inventario123.data.model.ProyectoRentec
import com.kernel94.inventario123.data.remote.ApiService

/** Proyectos de Renovación Tecnológica (RENTEC): folio, activos recibidos/instalados. */
class RentecRepository(private val api: ApiService) {

    suspend fun listar(): Resultado<List<ProyectoRentec>> = try {
        Resultado.Exito(api.rentecListar())
    } catch (e: Exception) {
        Resultado.Error("No se pudieron cargar los proyectos RENTEC.")
    }

    suspend fun crear(nombre: String): Resultado<ProyectoRentec> = try {
        val r = api.rentecCrear(mapOf("nombre" to nombre))
        if (r.success && r.id != null && r.folio != null) {
            Resultado.Exito(ProyectoRentec(id = r.id, folio = r.folio, nombre = nombre))
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
}

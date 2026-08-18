package com.kernel94.inventario123.data.repository

import com.kernel94.inventario123.data.model.Activo
import com.kernel94.inventario123.data.model.ApiResultado
import com.kernel94.inventario123.data.model.ListadoActivosResponse
import com.kernel94.inventario123.data.remote.ApiService

class ActivoRepository(private val api: ApiService) {

    suspend fun listar(
        vista: String? = null, negocioId: Int? = null, regionId: Int? = null,
        plazaId: Int? = null, usuarioId: Int? = null, status: String? = null,
        busqueda: String? = null, pagina: Int = 1, porPagina: Int = 5000,
    ): Resultado<ListadoActivosResponse> = try {
        Resultado.Exito(api.listarActivos(vista, negocioId, regionId, plazaId, usuarioId, status, busqueda, pagina, porPagina))
    } catch (e: Exception) {
        Resultado.Error("No se pudo cargar el listado.")
    }

    suspend fun obtener(id: Int): Resultado<Activo> = try {
        Resultado.Exito(api.obtenerActivo(id))
    } catch (e: Exception) {
        Resultado.Error("No se pudo cargar el detalle del activo.")
    }

    suspend fun crear(
        serie: String, placa: String?, modeloId: Int?, status: String,
        negocioId: Int?, plazaId: Int?, procedenciaTiendaId: Int?, tiendaUsoId: Int?,
        asignadoUsuarioId: Int?, stockDestino: String?,
    ): Resultado<ApiResultado> = try {
        val r = api.guardarActivo(serie, placa, modeloId, status, negocioId, plazaId, procedenciaTiendaId, tiendaUsoId, asignadoUsuarioId, stockDestino)
        if (r.success) Resultado.Exito(r) else Resultado.Error(r.message ?: "No se pudo guardar el activo.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun actualizar(
        id: Int, serie: String, placa: String?, modeloId: Int?, status: String,
        procedenciaTiendaId: Int?, tiendaUsoId: Int?, asignadoUsuarioId: Int?,
    ): Resultado<ApiResultado> = try {
        val r = api.actualizarActivo(id, serie, placa, modeloId, status, procedenciaTiendaId, tiendaUsoId, asignadoUsuarioId)
        if (r.success) Resultado.Exito(r) else Resultado.Error(r.message ?: "No se pudo actualizar el activo.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun eliminar(id: Int): Resultado<ApiResultado> = try {
        val r = api.eliminarActivo(id)
        if (r.success) Resultado.Exito(r) else Resultado.Error(r.message ?: "No se pudo eliminar.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }
}

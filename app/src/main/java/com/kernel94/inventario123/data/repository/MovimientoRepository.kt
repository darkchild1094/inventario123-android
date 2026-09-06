package com.kernel94.inventario123.data.repository

import com.kernel94.inventario123.data.model.ListadoMovimientosResponse
import com.kernel94.inventario123.data.model.Movimiento
import com.kernel94.inventario123.data.remote.ApiService

/** Historial de movimientos (tabla `movimiento`, API action=listarHistorial). */
class MovimientoRepository(private val api: ApiService) {

    suspend fun listar(
        activoId: Int? = null, serie: String? = null, evento: String? = null,
        tiendaId: Int? = null, usuarioId: Int? = null, desde: String? = null,
        hasta: String? = null, pagina: Int = 1, porPagina: Int = 30,
    ): Resultado<ListadoMovimientosResponse> = try {
        Resultado.Exito(
            api.listarHistorial(
                activoId, serie?.ifBlank { null }, evento?.ifBlank { null },
                tiendaId, usuarioId, desde?.ifBlank { null }, hasta?.ifBlank { null },
                pagina, porPagina,
            )
        )
    } catch (e: Exception) {
        Resultado.Error("No se pudo cargar el historial.")
    }

    /** Línea de tiempo de un activo (más reciente primero). */
    suspend fun timelineDe(activoId: Int): List<Movimiento> =
        when (val r = listar(activoId = activoId, porPagina = 100)) {
            is Resultado.Exito -> r.datos.movimientos
            is Resultado.Error -> emptyList()
        }
}

package com.kernel94.inventario123.data.repository

import com.kernel94.inventario123.data.model.ListaTiendasResponse
import com.kernel94.inventario123.data.model.Tienda
import com.kernel94.inventario123.data.model.Usuario
import com.kernel94.inventario123.data.remote.ApiService

/** Tiendas + asignación del ATI responsable (migración 002/003, solo admin). */
class TiendaRepository(private val api: ApiService) {

    suspend fun tiendasPorPlaza(plazaId: Int): List<Tienda> = try {
        api.obtenerTiendasPorPlaza(plazaId)
    } catch (e: Exception) { emptyList() }

    /** Lista del módulo "Tiendas": acotada al rol, con nº de activos por tienda. */
    suspend fun listar(plazaId: Int? = null, busqueda: String? = null): Resultado<ListaTiendasResponse> = try {
        Resultado.Exito(api.listarTiendas(plazaId, busqueda?.ifBlank { null }))
    } catch (e: Exception) {
        Resultado.Error("No se pudieron cargar las tiendas.")
    }

    suspend fun atisPorPlaza(plazaId: Int): List<Usuario> = try {
        api.obtenerAtisPorPlaza(plazaId)
    } catch (e: Exception) { emptyList() }

    suspend fun asignarAti(tiendaId: Int, atiUsuarioId: Int?): Resultado<String> = try {
        val r = api.asignarAtiTienda(tiendaId, atiUsuarioId)
        if (r.success) Resultado.Exito(r.message ?: "ATI responsable actualizado.")
        else Resultado.Error(r.message ?: "No se pudo actualizar.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }
}

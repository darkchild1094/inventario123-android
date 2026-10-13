package com.kernel94.inventario123.data.repository

import com.kernel94.inventario123.data.model.InventarioTiendaResponse
import com.kernel94.inventario123.data.model.ListaTiendasResponse
import com.kernel94.inventario123.data.model.ResumenInventarioTienda
import com.kernel94.inventario123.data.model.Tienda
import com.kernel94.inventario123.data.model.Usuario
import com.kernel94.inventario123.data.remote.ApiService

/** Tiendas: CRUD + ATI responsable + checklist de inventario (migraciones 002/003/036). */
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

    suspend fun obtener(id: Int): Resultado<Tienda> = try {
        Resultado.Exito(api.obtenerTienda(id))
    } catch (e: Exception) {
        Resultado.Error("No se pudo cargar la tienda.")
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

    suspend fun crear(cr: String, nombre: String, plazaId: Int, atiUsuarioId: Int?): Resultado<Int> = try {
        val r = api.tiendaCrear(cr, nombre, plazaId, atiUsuarioId)
        if (r.success) Resultado.Exito(r.id ?: 0) else Resultado.Error(r.message ?: "No se pudo crear la tienda.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun actualizar(id: Int, cr: String, nombre: String, plazaId: Int, atiUsuarioId: Int?): Resultado<String> = try {
        val r = api.tiendaActualizar(id, cr, nombre, plazaId, atiUsuarioId)
        if (r.success) Resultado.Exito(r.message ?: "Tienda actualizada.") else Resultado.Error(r.message ?: "No se pudo actualizar.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun eliminar(id: Int): Resultado<String> = try {
        val r = api.tiendaEliminar(id)
        if (r.success) Resultado.Exito(r.message ?: "Tienda eliminada.") else Resultado.Error(r.message ?: "No se pudo eliminar.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    // ── Inventario (checklist) de tienda ─────────────────────────────────

    suspend fun inventarioAbierto(tiendaId: Int): Int? = try {
        api.inventarioTiendaAbierto(tiendaId).abierto?.id
    } catch (e: Exception) { null }

    suspend fun inventarioIniciar(tiendaId: Int): Resultado<InventarioTiendaResponse> = try {
        val r = api.inventarioTiendaIniciar(mapOf("tienda_id" to tiendaId))
        if (r.success && r.inventario != null) Resultado.Exito(r.inventario)
        else Resultado.Error("No se pudo abrir el inventario.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun inventarioDetalle(id: Int): Resultado<InventarioTiendaResponse> = try {
        Resultado.Exito(api.inventarioTiendaDetalle(id))
    } catch (e: Exception) {
        Resultado.Error("No se pudo cargar el inventario.")
    }

    suspend fun inventarioEscanear(id: Int, codigo: String): Resultado<Int> = try {
        val r = api.inventarioTiendaEscanear(mapOf("id" to id, "codigo" to codigo))
        if (r.success) Resultado.Exito(r.activo_id ?: 0)
        else Resultado.Error(r.message ?: "Ese código no pertenece a esta tienda.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun inventarioMarcar(detalleId: Int, encontrado: Boolean): Resultado<String> = try {
        val r = api.inventarioTiendaMarcar(mapOf("detalle_id" to detalleId, "encontrado" to encontrado))
        if (r.success) Resultado.Exito("") else Resultado.Error(r.message ?: "No se pudo marcar.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun inventarioNota(detalleId: Int, nota: String): Resultado<String> = try {
        val r = api.inventarioTiendaNota(mapOf("detalle_id" to detalleId, "nota" to nota))
        if (r.success) Resultado.Exito("") else Resultado.Error(r.message ?: "No se pudo guardar la nota.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun inventarioAgregarNuevo(inventarioId: Int, activoId: Int): Resultado<String> = try {
        val r = api.inventarioTiendaAgregarNuevo(mapOf("id" to inventarioId, "activo_id" to activoId))
        if (r.success) Resultado.Exito("Activo agregado.") else Resultado.Error(r.message ?: "No se pudo agregar.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun inventarioCerrar(id: Int): Resultado<ResumenInventarioTienda> = try {
        val r = api.inventarioTiendaCerrar(mapOf("id" to id))
        if (r.success) Resultado.Exito(r.resumen ?: ResumenInventarioTienda())
        else Resultado.Error("No se pudo cerrar el inventario.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun inventarioCancelar(id: Int): Resultado<String> = try {
        val r = api.inventarioTiendaCancelar(mapOf("id" to id))
        if (r.success) Resultado.Exito("") else Resultado.Error(r.message ?: "No se pudo cancelar.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }
}

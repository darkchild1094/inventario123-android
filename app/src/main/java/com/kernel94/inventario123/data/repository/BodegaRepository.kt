package com.kernel94.inventario123.data.repository

import com.kernel94.inventario123.data.model.Bodega
import com.kernel94.inventario123.data.model.InventarioBodega
import com.kernel94.inventario123.data.model.Usuario
import com.kernel94.inventario123.data.remote.ApiService

/** Inventario físico de bodega: auditoría mensual por escaneo (migración 027). */
class BodegaRepository(private val api: ApiService) {

    suspend fun bodegas(): List<Bodega> = try {
        api.inventarioBodegaBodegas()
    } catch (e: Exception) { emptyList() }

    suspend fun historico(bodegaId: Int): Resultado<List<InventarioBodega>> = try {
        Resultado.Exito(api.inventarioBodegaListar(bodegaId))
    } catch (e: Exception) {
        Resultado.Error("No se pudo cargar el histórico de inventarios.")
    }

    /** Abre el inventario del mes en curso, o retoma el que ya estaba abierto. */
    suspend fun iniciar(bodegaId: Int): Resultado<InventarioBodega> = try {
        val r = api.inventarioBodegaIniciar(mapOf("bodega_id" to bodegaId))
        if (r.success && r.inventario != null) Resultado.Exito(r.inventario)
        else Resultado.Error(r.message ?: "No se pudo abrir el inventario.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun detalle(inventarioId: Int): Resultado<InventarioBodega> = try {
        Resultado.Exito(api.inventarioBodegaDetalle(inventarioId))
    } catch (e: Exception) {
        Resultado.Error("No se pudo cargar el inventario.")
    }

    suspend fun escanear(inventarioId: Int, codigo: String): Resultado<InventarioBodega> = try {
        val r = api.inventarioBodegaEscanear(mapOf("inventario_id" to inventarioId, "codigo" to codigo))
        if (r.success && r.inventario != null) Resultado.Exito(r.inventario)
        else Resultado.Error(r.message ?: "Ese activo no pertenece a este inventario.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun guardarNota(detalleId: Int, nota: String): Resultado<String> = try {
        val r = api.inventarioBodegaNota(mapOf("detalle_id" to detalleId, "nota" to nota))
        if (r.success) Resultado.Exito(r.message ?: "Nota guardada.")
        else Resultado.Error(r.message ?: "No se pudo guardar la nota.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun cerrar(inventarioId: Int): Resultado<InventarioBodega> = try {
        val r = api.inventarioBodegaCerrar(mapOf("id" to inventarioId))
        if (r.success && r.inventario != null) Resultado.Exito(r.inventario)
        else Resultado.Error(r.message ?: "No se pudo cerrar el inventario.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    // ── Inventario físico de stock personal (Mi Stock / Stock PFS) ──────
    // detalle()/escanear()/guardarNota()/cerrar() de arriba ya son genéricos
    // (el backend distingue bodega vs. stock personal por el inventario_id).

    suspend fun stockUsuarios(): List<Usuario> = try {
        api.inventarioStockUsuarios()
    } catch (e: Exception) { emptyList() }

    suspend fun historicoUsuario(stockUsuarioId: Int): Resultado<List<InventarioBodega>> = try {
        Resultado.Exito(api.inventarioStockListar(stockUsuarioId))
    } catch (e: Exception) {
        Resultado.Error("No se pudo cargar el histórico de inventarios.")
    }

    suspend fun iniciarUsuario(stockUsuarioId: Int): Resultado<InventarioBodega> = try {
        val r = api.inventarioStockIniciar(mapOf("stock_usuario_id" to stockUsuarioId))
        if (r.success && r.inventario != null) Resultado.Exito(r.inventario)
        else Resultado.Error(r.message ?: "No se pudo abrir el inventario.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }
}

package com.kernel94.inventario123.data.repository

import android.content.Context
import com.google.gson.Gson
import com.kernel94.inventario123.data.model.*
import com.kernel94.inventario123.data.remote.ApiService
import java.io.File

/**
 * Catálogos. Con señal se traen del servidor y se cachean en filesDir; sin señal
 * se sirven del caché para que el alta de activos funcione offline.
 */
class CatalogoRepository(private val api: ApiService, private val context: Context? = null) {

    private val gson = Gson()
    private val cache: File? get() = context?.let { File(it.filesDir, "catalogos_cache.json") }

    suspend fun obtenerCatalogos(): Resultado<Catalogos> = try {
        val c = api.obtenerCatalogos()
        cache?.let { runCatching { it.writeText(gson.toJson(c)) } }
        Resultado.Exito(c)
    } catch (e: Exception) {
        val local = cache?.takeIf { it.exists() }?.let {
            runCatching { gson.fromJson(it.readText(), Catalogos::class.java) }.getOrNull()
        }
        if (local != null) Resultado.Exito(local)
        else Resultado.Error("No se pudieron cargar los catálogos (sin señal y sin caché).")
    }

    suspend fun modelosPorDispositivo(dispositivoId: Int): List<Modelo> = try {
        api.obtenerModelosPorDispositivo(dispositivoId)
    } catch (e: Exception) { emptyList() }

    suspend fun plazasPorNegocio(negocioId: Int): List<Plaza> = try {
        api.obtenerPlazasPorNegocio(negocioId)
    } catch (e: Exception) { emptyList() }

    suspend fun regionesPorNegocio(negocioId: Int): List<Region> = try {
        api.obtenerRegionesPorNegocio(negocioId)
    } catch (e: Exception) { emptyList() }

    suspend fun tiendasPorPlaza(plazaId: Int): List<Tienda> = try {
        api.obtenerTiendasPorPlaza(plazaId)
    } catch (e: Exception) { emptyList() }

    suspend fun usuariosPorPlaza(plazaId: Int): List<Usuario> = try {
        api.obtenerUsuariosPorPlaza(plazaId)
    } catch (e: Exception) { emptyList() }

    suspend fun plazasPorRegion(regionId: Int): List<Plaza> = try {
        api.obtenerPlazasPorRegion(regionId)
    } catch (e: Exception) { emptyList() }

    /** Usuarios tipo ATI de una plaza (responsable de garantía/baja). */
    suspend fun atisPorPlaza(plazaId: Int): List<Usuario> = try {
        api.obtenerAtisPorPlaza(plazaId)
    } catch (e: Exception) { emptyList() }
}

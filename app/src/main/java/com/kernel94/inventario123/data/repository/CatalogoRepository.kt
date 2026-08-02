package com.kernel94.inventario123.data.repository

import com.kernel94.inventario123.data.model.*
import com.kernel94.inventario123.data.remote.ApiService

class CatalogoRepository(private val api: ApiService) {
    suspend fun obtenerCatalogos(): Resultado<Catalogos> = try {
        Resultado.Exito(api.obtenerCatalogos())
    } catch (e: Exception) {
        Resultado.Error("No se pudieron cargar los catálogos.")
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
}

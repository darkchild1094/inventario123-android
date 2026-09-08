package com.kernel94.inventario123.data.remote

import com.kernel94.inventario123.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Exportar ─────────────────────────────────────────────────────────
    @Streaming
    @GET("index.php?controller=export&action=inventario")
    suspend fun exportarInventario(): Response<ResponseBody>

    // Exporta sólo los activos de un módulo (Tiendas, Bodega, Mi Stock, Stock PFS, ATI).
    @Streaming
    @GET("index.php?controller=export&action=modulo")
    suspend fun exportarModulo(
        @Query("modulo") modulo: String,
        @Query("tienda_id") tiendaId: Int? = null,
        @Query("plaza_id") plazaId: Int? = null,
    ): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("index.php?controller=api&action=login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("index.php?controller=api&action=logout")
    suspend fun logout(): ApiResultado

    @GET("index.php?controller=api&action=obtenerPerfil")
    suspend fun obtenerPerfil(): Perfil

    @GET("index.php?controller=api&action=resumenDashboard")
    suspend fun resumenDashboard(): ResumenDashboard

    @GET("index.php?controller=api&action=listarActivos")
    suspend fun listarActivos(
        @Query("modulo") modulo: String? = null,
        @Query("vista") vista: String? = null,
        @Query("negocio_id") negocioId: Int? = null,
        @Query("region_id") regionId: Int? = null,
        @Query("plaza_id") plazaId: Int? = null,
        @Query("tienda_id") tiendaId: Int? = null,
        @Query("usuario_id") usuarioId: Int? = null,
        @Query("status") status: String? = null,
        @Query("busqueda") busqueda: String? = null,
        @Query("pagina") pagina: Int = 1,
        @Query("por_pagina") porPagina: Int = 20,
    ): ListadoActivosResponse

    @GET("index.php?controller=api&action=obtenerActivo")
    suspend fun obtenerActivo(@Query("id") id: Int): Activo

    // Módulo "Consulta": identifica un equipo (global, sólo lectura).
    @GET("index.php?controller=api&action=consultar")
    suspend fun consultar(@Query("q") q: String): ConsultaResponse

    // Form "Movimiento en tienda": ¿la serie está en mi stock (instalación=mover)
    // o instalada en esta tienda (retiro)?
    @GET("index.php?controller=api&action=resolverSerie")
    suspend fun resolverSerie(
        @Query("serie") serie: String,
        @Query("tienda_id") tiendaId: Int? = null,
    ): ResolverSerieResponse

    // Lista del módulo "Tiendas": acotada al rol, con nº de activos por tienda.
    @GET("index.php?controller=api&action=listarTiendas")
    suspend fun listarTiendas(
        @Query("plaza_id") plazaId: Int? = null,
        @Query("busqueda") busqueda: String? = null,
    ): ListaTiendasResponse

    // Multipart porque guardarActivo/actualizarActivo ahora aceptan las 3 fotos
    // opcionales del activo (migración 007 + ImageHelper), igual que crear.php/editar.php
    // (enctype="multipart/form-data"). Los campos de texto van por @PartMap (se omite
    // el que venga null, igual que antes con @Field), las fotos por @Part nullable.
    @Multipart
    @POST("index.php?controller=api&action=guardarActivo")
    suspend fun guardarActivo(
        @PartMap datos: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part fotoEquipo: MultipartBody.Part?,
        @Part fotoSerie: MultipartBody.Part?,
        @Part fotoActivo: MultipartBody.Part?,
        // Reemplazo: foto del equipo que sale (parte "foto_equipo_salida").
        @Part fotoEquipoSalida: MultipartBody.Part? = null,
    ): ApiResultado

    @Multipart
    @POST("index.php?controller=api&action=actualizarActivo")
    suspend fun actualizarActivo(
        @PartMap datos: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part fotoEquipo: MultipartBody.Part?,
        @Part fotoSerie: MultipartBody.Part?,
        @Part fotoActivo: MultipartBody.Part?,
        @Part fotoEquipoSalida: MultipartBody.Part? = null,
    ): ApiResultado

    @FormUrlEncoded
    @POST("index.php?controller=api&action=eliminarActivo")
    suspend fun eliminarActivo(@Field("id") id: Int): ApiResultado

    @GET("index.php?controller=api&action=obtenerCatalogos")
    suspend fun obtenerCatalogos(): Catalogos

    @GET("index.php?controller=api&action=obtenerModelosPorDispositivo")
    suspend fun obtenerModelosPorDispositivo(@Query("dispositivo_id") dispositivoId: Int): List<Modelo>

    // Pistas para el lector de series por tipo de dispositivo (prefijos frecuentes, OCR).
    @GET("index.php?controller=api&action=obtenerHintsEscaner")
    suspend fun obtenerHintsEscaner(): HintsEscaner

    @GET("index.php?controller=api&action=obtenerPlazasPorNegocio")
    suspend fun obtenerPlazasPorNegocio(@Query("negocio_id") negocioId: Int): List<Plaza>

    @GET("index.php?controller=api&action=obtenerRegionesPorNegocio")
    suspend fun obtenerRegionesPorNegocio(@Query("negocio_id") negocioId: Int): List<Region>

    @GET("index.php?controller=api&action=obtenerTiendasPorPlaza")
    suspend fun obtenerTiendasPorPlaza(@Query("plaza_id") plazaId: Int): List<Tienda>

    @GET("index.php?controller=api&action=obtenerUsuariosPorPlaza")
    suspend fun obtenerUsuariosPorPlaza(@Query("plaza_id") plazaId: Int): List<Usuario>

    @GET("index.php?controller=api&action=obtenerPlazasPorRegion")
    suspend fun obtenerPlazasPorRegion(@Query("region_id") regionId: Int): List<Plaza>

    // ── Historial (tabla movimiento, migración 002) ──────────────────────
    @GET("index.php?controller=api&action=listarHistorial")
    suspend fun listarHistorial(
        @Query("activo_id") activoId: Int? = null,
        @Query("serie") serie: String? = null,
        @Query("evento") evento: String? = null,
        @Query("tienda_id") tiendaId: Int? = null,
        @Query("usuario_id") usuarioId: Int? = null,
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null,
        @Query("pagina") pagina: Int = 1,
        @Query("por_pagina") porPagina: Int = 30,
    ): ListadoMovimientosResponse

    // ── ATI responsable por tienda (migración 002/003) ───────────────────
    @GET("index.php?controller=api&action=obtenerAtisPorPlaza")
    suspend fun obtenerAtisPorPlaza(@Query("plaza_id") plazaId: Int): List<Usuario>

    @FormUrlEncoded
    @POST("index.php?controller=api&action=asignarAtiTienda")
    suspend fun asignarAtiTienda(
        @Field("tienda_id") tiendaId: Int,
        @Field("ati_usuario_id") atiUsuarioId: Int?,
    ): ApiResultado

    // Alimenta el selector "¿Reemplaza a?" del alta de un activo "en uso".
    // dispositivoId null → todas las categorías (reemplazo entre categorías).
    @GET("index.php?controller=api&action=obtenerActivosEnTiendaPorDispositivo")
    suspend fun obtenerActivosEnTiendaPorDispositivo(
        @Query("tienda_id") tiendaId: Int,
        @Query("dispositivo_id") dispositivoId: Int? = null,
        @Query("excepto_id") exceptoId: Int? = null,
    ): List<Activo>

    @GET("index.php?controller=api&action=obtenerActivosEnBodega")
    suspend fun obtenerActivosEnBodega(@Query("bodega_id") bodegaId: Int): List<Activo>

    // ── Catálogo de modelos (solo admin) ────────────────────────────────
    @GET("index.php?controller=api&action=listarModelos")
    suspend fun listarModelos(): List<Modelo>

    @GET("index.php?controller=api&action=obtenerModelo")
    suspend fun obtenerModelo(@Query("id") id: Int): Modelo

    @Headers("Content-Type: application/json")
    @POST("index.php?controller=api&action=guardarModelo")
    suspend fun guardarModelo(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiResultado

    @Headers("Content-Type: application/json")
    @POST("index.php?controller=api&action=actualizarModelo")
    suspend fun actualizarModelo(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiResultado

    @Headers("Content-Type: application/json")
    @POST("index.php?controller=api&action=eliminarModelo")
    suspend fun eliminarModelo(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiResultado

    // ── Solicitudes de traslado a bodega (doble firma) ──────────────────
    @GET("index.php?controller=api&action=listarSolicitudes")
    suspend fun listarSolicitudes(@Query("estado") estado: String? = null): ListaSolicitudesResponse

    @GET("index.php?controller=api&action=obtenerSolicitud")
    suspend fun obtenerSolicitud(@Query("id") id: Int): SolicitudTraslado

    @GET("index.php?controller=api&action=contarSolicitudesPendientes")
    suspend fun contarSolicitudesPendientes(): ConteoPendientes

    @Multipart
    @POST("index.php?controller=api&action=crearSolicitud")
    suspend fun crearSolicitud(
        @Part("destino") destino: RequestBody,
        @Part("origen_tipo") origenTipo: RequestBody,
        @Part("nota") nota: RequestBody?,
        @Part("origen_tienda_id") origenTiendaId: RequestBody?,
        @Part("origen_bodega_id") origenBodegaId: RequestBody?,
        @Part("destino_bodega_id") destinoBodegaId: RequestBody?,
        @Part("destino_usuario_id") destinoUsuarioId: RequestBody?,
        @Part("activos[]") activos: List<@JvmSuppressWildcards RequestBody>,
        @Part firma: MultipartBody.Part,
    ): ApiResultado

    @Multipart
    @POST("index.php?controller=api&action=aprobarSolicitud")
    suspend fun aprobarSolicitud(
        @Part("id") id: RequestBody,
        @Part firma: MultipartBody.Part,
    ): ApiResultado

    @Headers("Content-Type: application/json")
    @POST("index.php?controller=api&action=rechazarSolicitud")
    suspend fun rechazarSolicitud(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiResultado

    @Headers("Content-Type: application/json")
    @POST("index.php?controller=api&action=cancelarSolicitud")
    suspend fun cancelarSolicitud(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiResultado

    @GET("index.php?controller=api&action=listarUsuarios")
    suspend fun listarUsuarios(): List<Usuario>

    @GET("index.php?controller=api&action=obtenerUsuario")
    suspend fun obtenerUsuario(@Query("id") id: Int): Usuario

    @Headers("Content-Type: application/json")
    @POST("index.php?controller=api&action=guardarUsuario")
    suspend fun guardarUsuario(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiResultado

    @Headers("Content-Type: application/json")
    @POST("index.php?controller=api&action=actualizarUsuario")
    suspend fun actualizarUsuario(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiResultado

    @FormUrlEncoded
    @POST("index.php?controller=api&action=eliminarUsuario")
    suspend fun eliminarUsuario(@Field("id") id: Int): ApiResultado
}

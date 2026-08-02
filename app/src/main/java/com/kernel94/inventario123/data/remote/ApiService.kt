package com.kernel94.inventario123.data.remote

import com.kernel94.inventario123.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Exportar ─────────────────────────────────────────────────────────
    @Streaming
    @GET("index.php?controller=export&action=inventario")
    suspend fun exportarInventario(): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("index.php?controller=api&action=login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("index.php?controller=api&action=logout")
    suspend fun logout(): ApiResultado

    @GET("index.php?controller=api&action=obtenerPerfil")
    suspend fun obtenerPerfil(): Perfil

    @GET("index.php?controller=api&action=listarActivos")
    suspend fun listarActivos(
        @Query("vista") vista: String? = null,
        @Query("negocio_id") negocioId: Int? = null,
        @Query("region_id") regionId: Int? = null,
        @Query("plaza_id") plazaId: Int? = null,
        @Query("usuario_id") usuarioId: Int? = null,
        @Query("status") status: String? = null,
        @Query("busqueda") busqueda: String? = null,
        @Query("pagina") pagina: Int = 1,
        @Query("por_pagina") porPagina: Int = 20,
    ): ListadoActivosResponse

    @GET("index.php?controller=api&action=obtenerActivo")
    suspend fun obtenerActivo(@Query("id") id: Int): Activo

    @FormUrlEncoded
    @POST("index.php?controller=api&action=guardarActivo")
    suspend fun guardarActivo(
        @Field("serie") serie: String,
        @Field("placa") placa: String?,
        @Field("modelo_id") modeloId: Int?,
        @Field("status") status: String,
        @Field("negocio_id") negocioId: Int?,
        @Field("plaza_id") plazaId: Int?,
        @Field("procedencia_tienda_id") procedenciaTiendaId: Int?,
        @Field("tienda_uso_id") tiendaUsoId: Int?,
        @Field("asignado_usuario_id") asignadoUsuarioId: Int?,
        @Field("stock_destino") stockDestino: String?,
    ): ApiResultado

    @FormUrlEncoded
    @POST("index.php?controller=api&action=actualizarActivo")
    suspend fun actualizarActivo(
        @Field("id") id: Int,
        @Field("serie") serie: String,
        @Field("placa") placa: String?,
        @Field("modelo_id") modeloId: Int?,
        @Field("status") status: String,
        @Field("procedencia_tienda_id") procedenciaTiendaId: Int?,
        @Field("tienda_uso_id") tiendaUsoId: Int?,
        @Field("asignado_usuario_id") asignadoUsuarioId: Int?,
    ): ApiResultado

    @FormUrlEncoded
    @POST("index.php?controller=api&action=eliminarActivo")
    suspend fun eliminarActivo(@Field("id") id: Int): ApiResultado

    @GET("index.php?controller=api&action=obtenerCatalogos")
    suspend fun obtenerCatalogos(): Catalogos

    @GET("index.php?controller=api&action=obtenerModelosPorDispositivo")
    suspend fun obtenerModelosPorDispositivo(@Query("dispositivo_id") dispositivoId: Int): List<Modelo>

    @GET("index.php?controller=api&action=obtenerPlazasPorNegocio")
    suspend fun obtenerPlazasPorNegocio(@Query("negocio_id") negocioId: Int): List<Plaza>

    @GET("index.php?controller=api&action=obtenerRegionesPorNegocio")
    suspend fun obtenerRegionesPorNegocio(@Query("negocio_id") negocioId: Int): List<Region>

    @GET("index.php?controller=api&action=obtenerTiendasPorPlaza")
    suspend fun obtenerTiendasPorPlaza(@Query("plaza_id") plazaId: Int): List<Tienda>

    @GET("index.php?controller=api&action=obtenerUsuariosPorPlaza")
    suspend fun obtenerUsuariosPorPlaza(@Query("plaza_id") plazaId: Int): List<Usuario>

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

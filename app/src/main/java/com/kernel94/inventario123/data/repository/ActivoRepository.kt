package com.kernel94.inventario123.data.repository

import android.content.Context
import android.net.Uri
import com.kernel94.inventario123.data.model.Activo
import com.kernel94.inventario123.data.model.ApiResultado
import com.kernel94.inventario123.data.model.ListadoActivosResponse
import com.kernel94.inventario123.data.remote.ApiService
import com.kernel94.inventario123.data.remote.ImagenUtil

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

    /** Activos "en uso" de una tienda para el selector "¿Reemplaza a?".
     *  dispositivoId null → todas las categorías. */
    suspend fun activosEnTiendaPorDispositivo(
        tiendaId: Int, dispositivoId: Int? = null, exceptoId: Int? = null,
    ): List<Activo> = try {
        api.obtenerActivosEnTiendaPorDispositivo(tiendaId, dispositivoId, exceptoId)
    } catch (e: Exception) { emptyList() }

    suspend fun resumenDashboard(): Resultado<com.kernel94.inventario123.data.model.ResumenDashboard> = try {
        Resultado.Exito(api.resumenDashboard())
    } catch (e: Exception) {
        Resultado.Error("No se pudo cargar el resumen.")
    }

    suspend fun crear(
        context: Context,
        serie: String, codigoBarras: String?, numActivo: String?, modeloId: Int?, status: String,
        negocioId: Int?, plazaId: Int?, procedenciaTiendaId: Int?, tiendaUsoId: Int?,
        asignadoUsuarioId: Int?, stockDestino: String?, atiUsuarioId: Int? = null,
        reemplazaActivoId: Int? = null, salidaDestino: String? = null,
        salidaUsuarioId: Int? = null, salidaAtiUsuarioId: Int? = null, motivo: String? = null,
        salidaSerie: String? = null, salidaCodigoBarras: String? = null,
        fotoEquipoUri: Uri? = null, fotoSerieUri: Uri? = null, fotoActivoUri: Uri? = null,
    ): Resultado<ApiResultado> = try {
        val datos = mapaDatos(
            serie = serie, codigoBarras = codigoBarras, numActivo = numActivo, modeloId = modeloId,
            status = status, negocioId = negocioId, plazaId = plazaId,
            procedenciaTiendaId = procedenciaTiendaId, tiendaUsoId = tiendaUsoId,
            asignadoUsuarioId = asignadoUsuarioId, stockDestino = stockDestino, atiUsuarioId = atiUsuarioId,
            reemplazaActivoId = reemplazaActivoId, salidaDestino = salidaDestino,
            salidaUsuarioId = salidaUsuarioId, salidaAtiUsuarioId = salidaAtiUsuarioId, motivo = motivo,
            salidaSerie = salidaSerie, salidaCodigoBarras = salidaCodigoBarras,
        )
        val r = api.guardarActivo(
            datos,
            ImagenUtil.parte(context, fotoEquipoUri, "foto_equipo"),
            ImagenUtil.parte(context, fotoSerieUri, "foto_serie"),
            ImagenUtil.parte(context, fotoActivoUri, "foto_activo"),
        )
        if (r.success) Resultado.Exito(r) else Resultado.Error(r.message ?: "No se pudo guardar el activo.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun actualizar(
        context: Context,
        id: Int, serie: String, codigoBarras: String?, numActivo: String?, modeloId: Int?, status: String,
        procedenciaTiendaId: Int?, tiendaUsoId: Int?, asignadoUsuarioId: Int?,
        atiUsuarioId: Int? = null, reemplazaActivoId: Int? = null, salidaDestino: String? = null,
        salidaUsuarioId: Int? = null, salidaAtiUsuarioId: Int? = null, motivo: String? = null,
        salidaSerie: String? = null, salidaCodigoBarras: String? = null,
        fotoEquipoUri: Uri? = null, fotoSerieUri: Uri? = null, fotoActivoUri: Uri? = null,
    ): Resultado<ApiResultado> = try {
        val datos = mapaDatos(
            id = id, serie = serie, codigoBarras = codigoBarras, numActivo = numActivo, modeloId = modeloId,
            status = status, procedenciaTiendaId = procedenciaTiendaId, tiendaUsoId = tiendaUsoId,
            asignadoUsuarioId = asignadoUsuarioId, atiUsuarioId = atiUsuarioId,
            reemplazaActivoId = reemplazaActivoId, salidaDestino = salidaDestino,
            salidaUsuarioId = salidaUsuarioId, salidaAtiUsuarioId = salidaAtiUsuarioId, motivo = motivo,
            salidaSerie = salidaSerie, salidaCodigoBarras = salidaCodigoBarras,
        )
        val r = api.actualizarActivo(
            datos,
            ImagenUtil.parte(context, fotoEquipoUri, "foto_equipo"),
            ImagenUtil.parte(context, fotoSerieUri, "foto_serie"),
            ImagenUtil.parte(context, fotoActivoUri, "foto_activo"),
        )
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

    /** Arma el mapa de campos de texto para guardarActivo/actualizarActivo (multipart).
     *  Los null se omiten, igual que antes hacía Retrofit con @Field nullable. */
    private fun mapaDatos(
        id: Int? = null, serie: String, codigoBarras: String?, numActivo: String?, modeloId: Int?,
        status: String, negocioId: Int? = null, plazaId: Int? = null,
        procedenciaTiendaId: Int?, tiendaUsoId: Int?, asignadoUsuarioId: Int?,
        stockDestino: String? = null, atiUsuarioId: Int?, reemplazaActivoId: Int?,
        salidaDestino: String?, salidaUsuarioId: Int?, salidaAtiUsuarioId: Int?, motivo: String? = null,
        salidaSerie: String? = null, salidaCodigoBarras: String? = null,
    ): Map<String, okhttp3.RequestBody> {
        val campos = linkedMapOf<String, String?>(
            "id" to id?.toString(),
            "serie" to serie,
            "codigo_barras" to codigoBarras,
            "num_activo" to numActivo,
            "modelo_id" to modeloId?.toString(),
            "status" to status,
            "negocio_id" to negocioId?.toString(),
            "plaza_id" to plazaId?.toString(),
            "procedencia_tienda_id" to procedenciaTiendaId?.toString(),
            "tienda_uso_id" to tiendaUsoId?.toString(),
            "asignado_usuario_id" to asignadoUsuarioId?.toString(),
            "stock_destino" to stockDestino,
            "ati_usuario_id" to atiUsuarioId?.toString(),
            "reemplaza_activo_id" to reemplazaActivoId?.toString(),
            "salida_destino" to salidaDestino,
            "salida_usuario_id" to salidaUsuarioId?.toString(),
            "salida_ati_usuario_id" to salidaAtiUsuarioId?.toString(),
            "motivo" to motivo,
            "salida_serie" to salidaSerie,
            "salida_codigo_barras" to salidaCodigoBarras,
        )
        return campos.mapNotNull { (k, v) -> ImagenUtil.texto(v)?.let { k to it } }.toMap()
    }
}

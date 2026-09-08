package com.kernel94.inventario123.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kernel94.inventario123.data.model.Activo
import com.kernel94.inventario123.data.model.ApiResultado
import com.kernel94.inventario123.data.model.ListadoActivosResponse
import com.kernel94.inventario123.data.remote.ApiService
import com.kernel94.inventario123.data.remote.ImagenUtil
import java.io.File

class ActivoRepository(private val api: ApiService, private val context: Context? = null) {

    private val gson = Gson()
    // Caché de "activos en uso" por tienda, para poder armar un reemplazo sin señal.
    private val reemplazosCache: File? get() = context?.let { File(it.filesDir, "reemplazos_cache.json") }
    private val reemplazosTipo = object : TypeToken<MutableMap<String, List<Activo>>>() {}.type

    private fun leerReemplazosCache(): MutableMap<String, List<Activo>> =
        reemplazosCache?.takeIf { it.exists() }?.let {
            runCatching { gson.fromJson<MutableMap<String, List<Activo>>>(it.readText(), reemplazosTipo) }.getOrNull()
        } ?: mutableMapOf()

    private fun guardarReemplazosCache(tiendaId: Int, lista: List<Activo>) {
        val f = reemplazosCache ?: return
        val mapa = leerReemplazosCache()
        mapa[tiendaId.toString()] = lista
        runCatching { f.writeText(gson.toJson(mapa)) }
    }

    suspend fun listar(
        modulo: String? = null,
        vista: String? = null, negocioId: Int? = null, regionId: Int? = null,
        plazaId: Int? = null, tiendaId: Int? = null, usuarioId: Int? = null, status: String? = null,
        busqueda: String? = null, pagina: Int = 1, porPagina: Int = 5000,
    ): Resultado<ListadoActivosResponse> = try {
        Resultado.Exito(api.listarActivos(modulo, vista, negocioId, regionId, plazaId, tiendaId, usuarioId, status, busqueda, pagina, porPagina))
    } catch (e: Exception) {
        Resultado.Error("No se pudo cargar el listado.")
    }

    suspend fun obtener(id: Int): Resultado<Activo> = try {
        Resultado.Exito(api.obtenerActivo(id))
    } catch (e: Exception) {
        Resultado.Error("No se pudo cargar el detalle del activo.")
    }

    suspend fun consultar(q: String): Resultado<com.kernel94.inventario123.data.model.ConsultaResponse> = try {
        Resultado.Exito(api.consultar(q))
    } catch (e: retrofit2.HttpException) {
        if (e.code() == 404) Resultado.Exito(com.kernel94.inventario123.data.model.ConsultaResponse(encontrado = false))
        else Resultado.Error("No se pudo consultar.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo consultar. Revisa tu conexión.")
    }

    suspend fun resolverSerie(serie: String, tiendaId: Int? = null): Resultado<com.kernel94.inventario123.data.model.ResolverSerieResponse> = try {
        Resultado.Exito(api.resolverSerie(serie, tiendaId))
    } catch (e: Exception) {
        Resultado.Error("No se pudo verificar la serie.")
    }

    /** Activos "en uso" de una tienda para el selector "¿Reemplaza a?".
     *  dispositivoId null → todas las categorías.
     *  Con señal se traen del servidor y se cachean todas las categorías de la
     *  tienda; sin señal se sirven del caché (filtrando por categoría en local)
     *  para poder armar un reemplazo offline. */
    suspend fun activosEnTiendaPorDispositivo(
        tiendaId: Int, dispositivoId: Int? = null, exceptoId: Int? = null,
    ): List<Activo> = try {
        // Cacheamos SIEMPRE la lista completa de la tienda (sin filtro de categoría).
        val completa = api.obtenerActivosEnTiendaPorDispositivo(tiendaId, null, exceptoId)
        guardarReemplazosCache(tiendaId, completa)
        if (dispositivoId == null) completa else completa.filter { it.dispositivo_id == dispositivoId }
    } catch (e: Exception) {
        leerReemplazosCache()[tiendaId.toString()].orEmpty()
            .filter { exceptoId == null || it.id != exceptoId }
            .filter { dispositivoId == null || it.dispositivo_id == dispositivoId }
    }

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

    // ── Soporte offline: la cola de pendientes reusa el mismo contrato ────────

    /** Campos de texto (map plano) para un alta de activo, listos para persistir/reenviar.
     *  Incluye los campos de reemplazo (opcionales) para poder encolar un
     *  reemplazo offline: el servidor los procesa igual que en el alta directa. */
    fun camposTexto(
        serie: String, codigoBarras: String?, numActivo: String?, modeloId: Int?, status: String,
        negocioId: Int?, plazaId: Int?, procedenciaTiendaId: Int?, tiendaUsoId: Int?,
        asignadoUsuarioId: Int?, stockDestino: String?, atiUsuarioId: Int?, motivo: String?,
        reemplazaActivoId: Int? = null, salidaDestino: String? = null, salidaUsuarioId: Int? = null,
        salidaAtiUsuarioId: Int? = null, salidaSerie: String? = null, salidaCodigoBarras: String? = null,
    ): Map<String, String> = linkedMapOf<String, String?>(
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
        "motivo" to motivo,
        "reemplaza_activo_id" to reemplazaActivoId?.toString(),
        "salida_destino" to salidaDestino,
        "salida_usuario_id" to salidaUsuarioId?.toString(),
        "salida_ati_usuario_id" to salidaAtiUsuarioId?.toString(),
        "salida_serie" to salidaSerie,
        "salida_codigo_barras" to salidaCodigoBarras,
    ).mapNotNull { (k, v) -> v?.let { k to it } }.toMap()

    /** Reenvía un pendiente de la cola. Lanza excepción si falla la red. */
    suspend fun enviarPendiente(
        campos: Map<String, String>,
        fotoEquipoBytes: ByteArray?, fotoSerieBytes: ByteArray?, fotoActivoBytes: ByteArray?,
    ): ApiResultado = api.guardarActivo(
        campos.mapNotNull { (k, v) -> ImagenUtil.texto(v)?.let { k to it } }.toMap(),
        fotoEquipoBytes?.let { ImagenUtil.parteBytes(it, "foto_equipo", "foto_equipo.jpg", "image/jpeg") },
        fotoSerieBytes?.let { ImagenUtil.parteBytes(it, "foto_serie", "foto_serie.jpg", "image/jpeg") },
        fotoActivoBytes?.let { ImagenUtil.parteBytes(it, "foto_activo", "foto_activo.jpg", "image/jpeg") },
    )

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

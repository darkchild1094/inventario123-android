package com.kernel94.inventario123.data.repository

import com.kernel94.inventario123.data.model.ApiResultado
import com.kernel94.inventario123.data.model.ListaSolicitudesResponse
import com.kernel94.inventario123.data.model.SolicitudTraslado
import com.kernel94.inventario123.data.remote.ApiService
import com.kernel94.inventario123.data.remote.ImagenUtil
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Solicitudes de traslado a bodega con doble firma digital.
 * Espeja SolicitudTrasladoController (web) y sus endpoints en ApiController.
 */
class SolicitudRepository(private val api: ApiService) {

    private fun texto(v: String) = v.toRequestBody("text/plain".toMediaTypeOrNull())

    suspend fun listar(estado: String? = null): Resultado<ListaSolicitudesResponse> = try {
        Resultado.Exito(api.listarSolicitudes(estado))
    } catch (e: Exception) {
        Resultado.Error("No se pudieron cargar las solicitudes.")
    }

    suspend fun obtener(id: Int): Resultado<SolicitudTraslado> = try {
        Resultado.Exito(api.obtenerSolicitud(id))
    } catch (e: Exception) {
        Resultado.Error("No se pudo cargar la solicitud.")
    }

    suspend fun contarPendientes(): Int = try {
        api.contarSolicitudesPendientes().pendientes
    } catch (e: Exception) { 0 }

    /**
     * @param destino  "asignado" | "en_bodega" | "baja" | "garantia"
     * @param origenTipo "asignado" (mi stock) | "tienda"
     */
    suspend fun crear(
        destino: String,
        origenTipo: String,
        activos: List<Int>,
        nota: String?,
        firmaPng: ByteArray,
        origenTiendaId: Int? = null,
        destinoBodegaId: Int? = null,
        destinoUsuarioId: Int? = null,
    ): Resultado<ApiResultado> = try {
        val r = api.crearSolicitud(
            destino = texto(destino),
            origenTipo = texto(origenTipo),
            nota = nota?.takeIf { it.isNotBlank() }?.let { texto(it) },
            origenTiendaId = origenTiendaId?.let { texto(it.toString()) },
            destinoBodegaId = destinoBodegaId?.let { texto(it.toString()) },
            destinoUsuarioId = destinoUsuarioId?.let { texto(it.toString()) },
            activos = activos.map { texto(it.toString()) },
            firma = ImagenUtil.parteBytes(firmaPng, "firma", "firma_solicitante.png"),
        )
        if (r.success) Resultado.Exito(r) else Resultado.Error(r.message ?: "No se pudo enviar la solicitud.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun aprobar(id: Int, firmaPng: ByteArray): Resultado<ApiResultado> = try {
        val r = api.aprobarSolicitud(
            id = texto(id.toString()),
            firma = ImagenUtil.parteBytes(firmaPng, "firma", "firma_aprobador.png"),
        )
        if (r.success) Resultado.Exito(r) else Resultado.Error(r.message ?: "No se pudo aprobar.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun rechazar(id: Int, motivo: String): Resultado<ApiResultado> = try {
        val r = api.rechazarSolicitud(mapOf("id" to id, "motivo" to motivo))
        if (r.success) Resultado.Exito(r) else Resultado.Error(r.message ?: "No se pudo rechazar.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    suspend fun cancelar(id: Int): Resultado<ApiResultado> = try {
        val r = api.cancelarSolicitud(mapOf("id" to id))
        if (r.success) Resultado.Exito(r) else Resultado.Error(r.message ?: "No se pudo cancelar.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }
}

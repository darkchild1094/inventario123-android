package com.kernel94.inventario123.data.repository

import com.google.gson.Gson
import com.kernel94.inventario123.data.model.ApiResultado
import com.kernel94.inventario123.data.model.ListaTransferencias
import com.kernel94.inventario123.data.remote.ApiService
import retrofit2.HttpException

/**
 * Entrega de equipo entre personas. Se manda y el que recibe lo acepta; el
 * equipo no cambia de manos hasta entonces. Sustituye a las viejas solicitudes
 * de traslado con firma.
 */
class TransferenciaRepository(private val api: ApiService) {

    /**
     * @param origen   "mi_stock" (lo que traigo a mi nombre) o "bodega"
     * @param bodegaId obligatorio cuando el origen es una bodega
     */
    suspend fun transferir(
        activos: List<Int>,
        destinoUsuarioId: Int,
        nota: String?,
        origen: String = "mi_stock",
        bodegaId: Int? = null,
    ): Resultado<String> = conMensajeDelServidor {
        api.transferirActivo(
            buildMap {
                put("activos", activos)
                put("destino_usuario_id", destinoUsuarioId)
                put("origen", origen)
                nota?.takeIf { it.isNotBlank() }?.let { put("nota", it) }
                bodegaId?.let { put("bodega_id", it) }
            }
        )
    }

    suspend fun listar(): Resultado<ListaTransferencias> = try {
        Resultado.Exito(api.listarTransferencias())
    } catch (e: Exception) {
        Resultado.Error("No se pudieron cargar las transferencias.")
    }

    suspend fun contarPendientes(): Int = try {
        api.contarTransferenciasPendientes().pendientes
    } catch (e: Exception) {
        0
    }

    suspend fun aceptar(id: Int): Resultado<String> =
        conMensajeDelServidor { api.aceptarTransferencia(mapOf("id" to id)) }

    suspend fun rechazar(id: Int, motivo: String): Resultado<String> =
        conMensajeDelServidor { api.rechazarTransferencia(mapOf("id" to id, "motivo" to motivo)) }

    suspend fun cancelar(id: Int): Resultado<String> =
        conMensajeDelServidor { api.cancelarTransferencia(mapOf("id" to id)) }

    /**
     * El servidor explica bien por qué niega una transferencia ("ese equipo ya
     * no está en tu stock", "esa persona no está en tu plaza"), así que su
     * mensaje se muestra tal cual en vez de uno genérico. Los 4xx llegan como
     * HttpException, de ahí que haya que leer el cuerpo del error.
     */
    private suspend fun conMensajeDelServidor(bloque: suspend () -> ApiResultado): Resultado<String> = try {
        val r = bloque()
        if (r.success) Resultado.Exito(r.message ?: "Listo.")
        else Resultado.Error(r.message ?: "No se pudo completar la operación.")
    } catch (e: HttpException) {
        val cuerpo = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val msg = runCatching { Gson().fromJson(cuerpo, ApiResultado::class.java) }.getOrNull()?.message
        Resultado.Error(msg ?: "No se pudo completar la operación.")
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }
}

package com.kernel94.inventario123.data.repository

import android.content.Context
import android.net.Uri
import com.kernel94.inventario123.data.local.PendientesStore
import com.kernel94.inventario123.data.model.ActivoPendiente
import com.kernel94.inventario123.data.remote.ConnectivityObserver
import com.kernel94.inventario123.data.remote.ImagenUtil
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Registro de altas de activo con soporte offline: si hay señal se envía al
 * momento; si no, se guarda en cola (con las fotos comprimidas a archivos
 * locales) y se reintenta al recuperar internet. El módulo de estatus muestra
 * el ID que devolvió el servidor por cada pendiente enviado.
 */
class PendientesRepository(
    private val store: PendientesStore,
    private val activoRepository: ActivoRepository,
    private val conectividad: ConnectivityObserver,
) {
    val pendientes: StateFlow<List<ActivoPendiente>> = store.flow

    /** Serializa sincronizar(): se dispara desde onCreate y desde el observer
     *  de conectividad; sin esto dos corrutinas reenviarían la misma cola. */
    private val syncMutex = Mutex()

    /** Los campos + la clave de idempotencia (= localId): el servidor la usa
     *  para no crear un duplicado si un reenvío repite un alta ya aceptada. */
    private fun camposConClave(p: ActivoPendiente): Map<String, String> =
        p.campos + ("idempotency_key" to p.localId)

    /**
     * @return Resultado.Exito(mensaje) — enviado (con id) o encolado.
     */
    suspend fun registrar(
        context: Context,
        campos: Map<String, String>,
        fotoEquipoUri: Uri?, fotoSerieUri: Uri?, fotoActivoUri: Uri?,
    ): Resultado<String> {
        val local = ActivoPendiente(campos = campos)

        // 1) comprimir y guardar las fotos localmente (sobreviven al cierre)
        fun guardar(uri: Uri?, campo: String): String? {
            if (uri == null) return null
            val bytes = ImagenUtil.comprimirABytes(context, uri) ?: return null
            return store.guardarFotoLocal(local.localId, campo, bytes)
        }
        var p = local.copy(
            fotoEquipoPath = guardar(fotoEquipoUri, "equipo"),
            fotoSeriePath = guardar(fotoSerieUri, "serie"),
            fotoActivoPath = guardar(fotoActivoUri, "activo"),
        )

        // 2) si hay señal, intentar enviarlo ya
        if (conectividad.hayInternet()) {
            try {
                val r = activoRepository.enviarPendiente(
                    camposConClave(p), leer(p.fotoEquipoPath), leer(p.fotoSeriePath), leer(p.fotoActivoPath),
                )
                if (r.success) {
                    borrarFotos(p)
                    return Resultado.Exito("Activo registrado. ID en el servidor: ${r.id ?: "—"}.")
                }
                // el servidor respondió con error de validación: se guarda como 'error'
                p = p.copy(estado = "error", error = r.message ?: "El servidor rechazó el alta.")
            } catch (e: Exception) {
                p = p.copy(estado = "pendiente")
            }
        }

        store.agregar(p)
        return Resultado.Exito(
            if (p.estado == "error") "Guardado en la cola: ${p.error}"
            else "Sin señal: guardado en la cola. Se enviará automáticamente al recuperar internet."
        )
    }

    /** Recorre la cola y envía lo que se pueda. Devuelve cuántos se enviaron. */
    suspend fun sincronizar(): Int = syncMutex.withLock {
        if (!conectividad.hayInternet()) return@withLock 0
        var enviados = 0
        for (p in store.porEnviar()) {
            store.actualizar(p.copy(estado = "enviando", error = null))
            try {
                val r = activoRepository.enviarPendiente(
                    camposConClave(p), leer(p.fotoEquipoPath), leer(p.fotoSeriePath), leer(p.fotoActivoPath),
                )
                if (r.success) {
                    store.actualizar(p.copy(estado = "enviado", serverId = r.id, error = null))
                    enviados++
                } else {
                    store.actualizar(p.copy(estado = "error", error = r.message ?: "Rechazado por el servidor."))
                }
            } catch (e: Exception) {
                store.actualizar(p.copy(estado = "pendiente", error = null))
            }
        }
        enviados
    }

    fun reintentar(localId: String) {
        store.flow.value.find { it.localId == localId }?.let {
            store.actualizar(it.copy(estado = "pendiente", error = null))
        }
    }

    fun descartar(localId: String) = store.eliminar(localId)
    fun limpiarEnviados() = store.flow.value.filter { it.estado == "enviado" }.forEach { store.eliminar(it.localId) }

    private fun leer(path: String?): ByteArray? = path?.let {
        runCatching { File(it).readBytes() }.getOrNull()
    }
    private fun borrarFotos(p: ActivoPendiente) {
        listOfNotNull(p.fotoEquipoPath, p.fotoSeriePath, p.fotoActivoPath).forEach { runCatching { File(it).delete() } }
    }
}

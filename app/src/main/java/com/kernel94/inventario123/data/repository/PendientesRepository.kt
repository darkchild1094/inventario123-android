package com.kernel94.inventario123.data.repository

import android.content.Context
import android.net.Uri
import com.kernel94.inventario123.data.local.PendientesStore
import com.kernel94.inventario123.data.model.ActivoPendiente
import com.kernel94.inventario123.data.model.ApiResultado
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

    /** Estado reactivo de conexión (para la UI). */
    val online: StateFlow<Boolean> = conectividad.online
    fun hayConexion(): Boolean = conectividad.hayInternet()

    /** Serializa sincronizar(): se dispara desde onCreate, desde el observer
     *  de conectividad y desde WorkManager; sin esto varias corrutinas
     *  reenviarían la misma cola. */
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
        // Las tres del equipo que SALE, cuando el alta es un reemplazo.
        fotoSalidaEquipoUri: Uri? = null,
        fotoSalidaSerieUri: Uri? = null,
        fotoSalidaActivoUri: Uri? = null,
        /**
         * Lo llama el repositorio cuando el servidor avisa que el equipo YA
         * estaba registrado (409). En ese caso el alta NO se encola: no es algo
         * pendiente de enviar, es una decisión que toca al usuario — mover el
         * equipo existente o cancelar. Reenviar con "mover_existente" a "1".
         */
        onYaExiste: ((ApiResultado) -> Unit)? = null,
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
            fotoSalidaEquipoPath = guardar(fotoSalidaEquipoUri, "salida_equipo"),
            fotoSalidaSeriePath = guardar(fotoSalidaSerieUri, "salida_serie"),
            fotoSalidaActivoPath = guardar(fotoSalidaActivoUri, "salida_activo"),
        )

        // 2) si hay señal, intentar enviarlo ya
        if (conectividad.hayInternet()) {
            try {
                val r = activoRepository.enviarPendiente(
                    camposConClave(p), leer(p.fotoEquipoPath), leer(p.fotoSeriePath), leer(p.fotoActivoPath),
                    leer(p.fotoSalidaEquipoPath), leer(p.fotoSalidaSeriePath), leer(p.fotoSalidaActivoPath),
                )
                if (r.success) {
                    borrarFotos(p)
                    return Resultado.Exito(
                        if (r.movido) r.message ?: "El equipo ya existía: se movió a la nueva ubicación."
                        else "Activo registrado. ID en el servidor: ${r.id ?: "—"}."
                    )
                }
                // El equipo ya existe: no se encola (no hay nada pendiente de
                // enviar, hay que decidir si se mueve) y se avisa a la UI.
                if (r.ya_existe && onYaExiste != null) {
                    borrarFotos(p)
                    onYaExiste(r)
                    return Resultado.Exito(r.message ?: "Este equipo ya está registrado.")
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
                    leer(p.fotoSalidaEquipoPath), leer(p.fotoSalidaSeriePath), leer(p.fotoSalidaActivoPath),
                )
                if (r.success) {
                    store.actualizar(p.copy(estado = "enviado", serverId = r.id, error = null))
                    // Ya llegó al servidor: las fotos locales no se volverán a
                    // usar. Antes se quedaban en disco hasta que el usuario
                    // tocaba "limpiar enviados".
                    borrarFotos(p)
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
        store.rutasDeFotos(p).forEach { runCatching { File(it).delete() } }
    }
}

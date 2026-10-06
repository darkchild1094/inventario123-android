package com.kernel94.inventario123.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kernel94.inventario123.data.model.ActivoPendiente
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** Cola local de altas de activo pendientes de envío (JSON en filesDir). */
class PendientesStore(private val context: Context) {

    private val gson = Gson()
    private val tipo = object : TypeToken<List<ActivoPendiente>>() {}.type
    private val archivo: File get() = File(context.filesDir, "activos_pendientes.json")

    private val _flow = MutableStateFlow(cargar())
    val flow: StateFlow<List<ActivoPendiente>> = _flow

    private fun cargar(): List<ActivoPendiente> = try {
        if (archivo.exists()) gson.fromJson(archivo.readText(), tipo) ?: emptyList() else emptyList()
    } catch (e: Exception) { emptyList() }

    private fun guardar(lista: List<ActivoPendiente>) {
        try { archivo.writeText(gson.toJson(lista)) } catch (_: Exception) {}
        _flow.value = lista
    }

    /**
     * Leer-modificar-escribir tiene que ser atómico de principio a fin: antes
     * sólo guardar() estaba sincronizado y agregar()/actualizar() calculaban la
     * lista nueva fuera del candado, así que un alta concurrente con el worker
     * podía perder una entrada.
     */
    @Synchronized
    private fun mutar(transformar: (List<ActivoPendiente>) -> List<ActivoPendiente>) {
        guardar(transformar(_flow.value))
    }

    fun agregar(p: ActivoPendiente) = mutar { it + p }
    fun actualizar(p: ActivoPendiente) = mutar { lista -> lista.map { if (it.localId == p.localId) p else it } }

    @Synchronized
    fun eliminar(localId: String) {
        _flow.value.find { it.localId == localId }?.let { borrarFotos(it) }
        guardar(_flow.value.filter { it.localId != localId })
    }

    /**
     * Los que toca reintentar solos: sólo "pendiente" (fallo de red). Antes
     * incluía "error", de modo que un alta rechazada por validación se
     * reenviaba en cada cambio de conectividad para siempre, y dejaba sin
     * sentido el botón de reintentar manual.
     */
    fun porEnviar(): List<ActivoPendiente> = _flow.value.filter { it.estado == "pendiente" }

    fun guardarFotoLocal(localId: String, campo: String, bytes: ByteArray): String? = try {
        val f = File(context.filesDir, "pend_${localId}_$campo.jpg")
        f.writeBytes(bytes)
        f.absolutePath
    } catch (e: Exception) { null }

    private fun borrarFotos(p: ActivoPendiente) {
        rutasDeFotos(p).forEach { runCatching { File(it).delete() } }
    }

    /** Todas las rutas de foto de un pendiente: las del que entra y las del que sale. */
    fun rutasDeFotos(p: ActivoPendiente): List<String> = listOfNotNull(
        p.fotoEquipoPath, p.fotoSeriePath, p.fotoActivoPath,
        p.fotoSalidaEquipoPath, p.fotoSalidaSeriePath, p.fotoSalidaActivoPath,
    )
}

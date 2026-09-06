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

    @Synchronized
    private fun guardar(lista: List<ActivoPendiente>) {
        try { archivo.writeText(gson.toJson(lista)) } catch (_: Exception) {}
        _flow.value = lista
    }

    fun agregar(p: ActivoPendiente) = guardar(_flow.value + p)
    fun actualizar(p: ActivoPendiente) = guardar(_flow.value.map { if (it.localId == p.localId) p else it })
    fun eliminar(localId: String) {
        _flow.value.find { it.localId == localId }?.let { borrarFotos(it) }
        guardar(_flow.value.filter { it.localId != localId })
    }

    /** Los que aún no llegaron al servidor (pendiente / error). */
    fun porEnviar(): List<ActivoPendiente> = _flow.value.filter { it.estado == "pendiente" || it.estado == "error" }

    fun guardarFotoLocal(localId: String, campo: String, bytes: ByteArray): String? = try {
        val f = File(context.filesDir, "pend_${localId}_$campo.jpg")
        f.writeBytes(bytes)
        f.absolutePath
    } catch (e: Exception) { null }

    private fun borrarFotos(p: ActivoPendiente) {
        listOfNotNull(p.fotoEquipoPath, p.fotoSeriePath, p.fotoActivoPath).forEach {
            runCatching { File(it).delete() }
        }
    }
}

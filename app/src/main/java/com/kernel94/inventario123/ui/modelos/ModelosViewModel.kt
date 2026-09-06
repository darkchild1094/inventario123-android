package com.kernel94.inventario123.ui.modelos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.Dispositivo
import com.kernel94.inventario123.data.model.Marca
import com.kernel94.inventario123.data.model.Modelo
import com.kernel94.inventario123.data.repository.CatalogoRepository
import com.kernel94.inventario123.data.repository.ModeloRepository
import com.kernel94.inventario123.data.repository.Resultado
import kotlinx.coroutines.launch

class ModelosViewModel(
    private val modeloRepository: ModeloRepository,
    private val catalogoRepository: CatalogoRepository,
) : ViewModel() {

    var modelos by mutableStateOf<List<Modelo>>(emptyList()); private set
    var dispositivos by mutableStateOf<List<Dispositivo>>(emptyList()); private set
    /** Se derivan de los modelos existentes (no hay endpoint de marcas). */
    var marcas by mutableStateOf<List<Marca>>(emptyList()); private set
    var cargando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    fun cargar() {
        cargando = true
        error = null
        viewModelScope.launch {
            when (val r = modeloRepository.listar()) {
                is Resultado.Exito -> {
                    modelos = r.datos
                    marcas = r.datos
                        .filter { it.marca_id != null && !it.marca_nombre.isNullOrBlank() }
                        .map { Marca(it.marca_id!!, it.marca_nombre!!) }
                        .distinctBy { it.id }
                        .sortedBy { it.nombre.lowercase() }
                }
                is Resultado.Error -> error = r.mensaje
            }
            when (val r = catalogoRepository.obtenerCatalogos()) {
                is Resultado.Exito -> dispositivos = r.datos.dispositivos
                is Resultado.Error -> {}
            }
            cargando = false
        }
    }

    fun guardar(
        id: Int?, nombre: String, dispositivoId: Int, marcaId: Int?, marcaNueva: String?,
        onListo: (Boolean, String) -> Unit,
    ) {
        viewModelScope.launch {
            val datos = mutableMapOf<String, Any?>(
                "nombre" to nombre.trim(),
                "dispositivo_id" to dispositivoId,
            )
            if (!marcaNueva.isNullOrBlank()) datos["marca_nueva"] = marcaNueva.trim()
            else if (marcaId != null && marcaId > 0) datos["marca_id"] = marcaId

            val r = if (id == null) {
                modeloRepository.guardar(datos)
            } else {
                datos["id"] = id
                modeloRepository.actualizar(datos)
            }
            when (r) {
                is Resultado.Exito -> { onListo(true, r.datos.message ?: "Guardado."); cargar() }
                is Resultado.Error -> onListo(false, r.mensaje)
            }
        }
    }

    fun eliminar(id: Int, reasignarA: Int?, onListo: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val r = modeloRepository.eliminar(id, reasignarA)) {
                is Resultado.Exito -> { onListo(true, r.datos.message ?: "Eliminado."); cargar() }
                is Resultado.Error -> onListo(false, r.mensaje)
            }
        }
    }
}

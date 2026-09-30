package com.kernel94.inventario123.ui.bodega

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.Bodega
import com.kernel94.inventario123.data.model.InventarioBodega
import com.kernel94.inventario123.data.repository.BodegaRepository
import com.kernel94.inventario123.data.repository.Resultado
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Histórico de inventarios físicos (por mes) de las bodegas del usuario, y
 * punto de entrada para abrir/retomar el inventario del mes en curso.
 */
class InventarioBodegaViewModel(
    private val bodegaRepository: BodegaRepository,
) : ViewModel() {

    var bodegas by mutableStateOf<List<Bodega>>(emptyList()); private set
    var bodegaId by mutableStateOf<Int?>(null); private set
    var inventarios by mutableStateOf<List<InventarioBodega>>(emptyList()); private set
    var cargando by mutableStateOf(false); private set
    var iniciando by mutableStateOf(false); private set
    var mensaje by mutableStateOf<String?>(null); private set

    private var jHistorico: Job? = null

    fun iniciar() {
        viewModelScope.launch {
            cargando = true
            bodegas = bodegaRepository.bodegas()
            if (bodegaId == null && bodegas.size == 1) bodegaId = bodegas.first().id
            cargarHistorico()
            cargando = false
        }
    }

    fun onBodegaChange(id: Int?) {
        bodegaId = id
        inventarios = emptyList()
        jHistorico?.cancel()
        jHistorico = viewModelScope.launch {
            cargando = true
            cargarHistorico()
            cargando = false
        }
    }

    private suspend fun cargarHistorico() {
        val id = bodegaId ?: return
        when (val r = bodegaRepository.historico(id)) {
            is Resultado.Exito -> inventarios = r.datos
            is Resultado.Error -> mensaje = r.mensaje
        }
    }

    /** Abre (o retoma) el inventario del mes en curso; devuelve su id por callback. */
    fun iniciarInventario(onListo: (Int) -> Unit) {
        val id = bodegaId ?: return
        iniciando = true
        viewModelScope.launch {
            when (val r = bodegaRepository.iniciar(id)) {
                is Resultado.Exito -> onListo(r.datos.id)
                is Resultado.Error -> mensaje = r.mensaje
            }
            iniciando = false
        }
    }

    fun limpiarMensaje() { mensaje = null }
}

package com.kernel94.inventario123.ui.bodega

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.InventarioBodega
import com.kernel94.inventario123.data.repository.BodegaRepository
import com.kernel94.inventario123.data.repository.Resultado
import kotlinx.coroutines.launch

/** Escaneo de un inventario abierto: marca activos como encontrados y anota los faltantes. */
class InventarioBodegaDetalleViewModel(
    private val bodegaRepository: BodegaRepository,
) : ViewModel() {

    var inventario by mutableStateOf<InventarioBodega?>(null); private set
    var cargando by mutableStateOf(false); private set
    var escaneando by mutableStateOf(false); private set
    var mensaje by mutableStateOf<String?>(null); private set

    fun cargar(inventarioId: Int) {
        viewModelScope.launch {
            cargando = true
            when (val r = bodegaRepository.detalle(inventarioId)) {
                is Resultado.Exito -> inventario = r.datos
                is Resultado.Error -> mensaje = r.mensaje
            }
            cargando = false
        }
    }

    fun escanear(codigo: String) {
        val id = inventario?.id ?: return
        if (escaneando) return
        escaneando = true
        viewModelScope.launch {
            when (val r = bodegaRepository.escanear(id, codigo)) {
                is Resultado.Exito -> inventario = r.datos
                is Resultado.Error -> mensaje = r.mensaje
            }
            escaneando = false
        }
    }

    fun guardarNota(detalleId: Int, nota: String) {
        viewModelScope.launch {
            when (val r = bodegaRepository.guardarNota(detalleId, nota)) {
                is Resultado.Exito -> inventario?.id?.let { cargar(it) }
                is Resultado.Error -> mensaje = r.mensaje
            }
        }
    }

    fun cerrar(onCerrado: () -> Unit) {
        val id = inventario?.id ?: return
        viewModelScope.launch {
            when (val r = bodegaRepository.cerrar(id)) {
                is Resultado.Exito -> { inventario = r.datos; onCerrado() }
                is Resultado.Error -> mensaje = r.mensaje
            }
        }
    }

    fun limpiarMensaje() { mensaje = null }
}

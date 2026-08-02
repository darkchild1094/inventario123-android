package com.kernel94.inventario123.ui.detalle

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.Activo
import com.kernel94.inventario123.data.repository.ActivoRepository
import com.kernel94.inventario123.data.repository.Resultado
import kotlinx.coroutines.launch

class DetalleViewModel(private val activoRepository: ActivoRepository) : ViewModel() {
    var activo by mutableStateOf<Activo?>(null); private set
    var cargando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var eliminado by mutableStateOf(false); private set

    fun cargar(id: Int) {
        cargando = true
        error = null
        viewModelScope.launch {
            when (val r = activoRepository.obtener(id)) {
                is Resultado.Exito -> { activo = r.datos; cargando = false }
                is Resultado.Error -> { error = r.mensaje; cargando = false }
            }
        }
    }

    fun eliminar(id: Int, onListo: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val r = activoRepository.eliminar(id)) {
                is Resultado.Exito -> { eliminado = true; onListo(true, r.datos.message ?: "Eliminado.") }
                is Resultado.Error -> onListo(false, r.mensaje)
            }
        }
    }
}

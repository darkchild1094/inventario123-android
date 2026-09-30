package com.kernel94.inventario123.ui.rentec

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.ProyectoRentec
import com.kernel94.inventario123.data.repository.RentecRepository
import com.kernel94.inventario123.data.repository.Resultado
import kotlinx.coroutines.launch

/** Lista de proyectos RENTEC visibles para el usuario, con conteos. */
class RentecListaViewModel(
    private val rentecRepository: RentecRepository,
) : ViewModel() {

    var proyectos by mutableStateOf<List<ProyectoRentec>>(emptyList()); private set
    var cargando by mutableStateOf(false); private set
    var creando by mutableStateOf(false); private set
    var mensaje by mutableStateOf<String?>(null); private set

    fun iniciar() {
        viewModelScope.launch {
            cargando = true
            when (val r = rentecRepository.listar()) {
                is Resultado.Exito -> proyectos = r.datos
                is Resultado.Error -> mensaje = r.mensaje
            }
            cargando = false
        }
    }

    fun crear(nombre: String, onCreado: (Int) -> Unit) {
        if (nombre.isBlank()) { mensaje = "Dale un nombre al proyecto."; return }
        creando = true
        viewModelScope.launch {
            when (val r = rentecRepository.crear(nombre.trim())) {
                is Resultado.Exito -> { creando = false; onCreado(r.datos.id) }
                is Resultado.Error -> { creando = false; mensaje = r.mensaje }
            }
        }
    }

    fun limpiarMensaje() { mensaje = null }
}

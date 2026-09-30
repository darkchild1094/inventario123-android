package com.kernel94.inventario123.ui.bodega

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.Usuario
import com.kernel94.inventario123.data.repository.BodegaRepository
import kotlinx.coroutines.launch

/** Landing del módulo "Stock PFS": lista de ingenieros con 1+ activos a su
 *  nombre. Se toca uno para ver su stock (ListadoScreen filtrado por él). */
class StockPfsListaViewModel(
    private val bodegaRepository: BodegaRepository,
) : ViewModel() {

    var usuarios by mutableStateOf<List<Usuario>>(emptyList()); private set
    var cargando by mutableStateOf(false); private set

    fun iniciar() {
        viewModelScope.launch {
            cargando = true
            usuarios = bodegaRepository.stockPfsUsuarios()
            cargando = false
        }
    }
}

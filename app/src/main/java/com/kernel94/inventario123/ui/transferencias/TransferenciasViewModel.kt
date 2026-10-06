package com.kernel94.inventario123.ui.transferencias

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.Transferencia
import com.kernel94.inventario123.data.repository.Resultado
import com.kernel94.inventario123.data.repository.TransferenciaRepository
import kotlinx.coroutines.launch

/**
 * Equipo que alguien te mandó y está esperando que lo aceptes, más lo que tú
 * mandaste. El equipo no cambia de manos hasta que el receptor acepta: hasta
 * entonces sigue siendo responsabilidad de quien lo envió.
 */
class TransferenciasViewModel(
    private val repo: TransferenciaRepository,
) : ViewModel() {

    var porAceptar by mutableStateOf<List<Transferencia>>(emptyList()); private set
    var enviadas by mutableStateOf<List<Transferencia>>(emptyList()); private set
    var cargando by mutableStateOf(false); private set
    var ocupado by mutableStateOf(false); private set
    var mensaje by mutableStateOf<String?>(null); private set

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            when (val r = repo.listar()) {
                is Resultado.Exito -> { porAceptar = r.datos.por_aceptar; enviadas = r.datos.enviadas }
                is Resultado.Error -> mensaje = r.mensaje
            }
            cargando = false
        }
    }

    fun aceptar(id: Int) = operar { repo.aceptar(id) }
    fun rechazar(id: Int, motivo: String) = operar { repo.rechazar(id, motivo) }
    fun cancelar(id: Int) = operar { repo.cancelar(id) }

    /** Toda acción recarga la lista: el estado lo decide el servidor, no la app. */
    private fun operar(accion: suspend () -> Resultado<String>) {
        if (ocupado) return
        ocupado = true
        viewModelScope.launch {
            mensaje = when (val r = accion()) {
                is Resultado.Exito -> r.datos
                is Resultado.Error -> r.mensaje
            }
            ocupado = false
            cargar()
        }
    }

    fun limpiarMensaje() { mensaje = null }
}

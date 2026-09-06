package com.kernel94.inventario123.ui.pendientes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.ActivoPendiente
import com.kernel94.inventario123.data.repository.PendientesRepository
import kotlinx.coroutines.launch

class PendientesViewModel(private val repo: PendientesRepository) : ViewModel() {

    val pendientes get() = repo.pendientes
    var sincronizando by mutableStateOf(false); private set
    var mensaje by mutableStateOf<String?>(null)

    fun sincronizar() {
        sincronizando = true
        viewModelScope.launch {
            val n = repo.sincronizar()
            sincronizando = false
            mensaje = if (n > 0) "$n enviado(s)." else "Nada por enviar o sin señal."
        }
    }

    fun reintentar(p: ActivoPendiente) { repo.reintentar(p.localId); sincronizar() }
    fun descartar(p: ActivoPendiente) = repo.descartar(p.localId)
    fun limpiarEnviados() = repo.limpiarEnviados()
}

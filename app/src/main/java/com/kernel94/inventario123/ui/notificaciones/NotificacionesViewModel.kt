package com.kernel94.inventario123.ui.notificaciones

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.Notificacion
import com.kernel94.inventario123.data.repository.AuthRepository
import com.kernel94.inventario123.data.repository.NotificacionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Lista de "Enviar" recibidos/enviados (reemplaza Historial/Transferencias/
 * SolicitudTraslado/Altas pendientes de envío). Offline-first: observa Room
 * directo; [actualizar] fuerza un refresh contra el backend (pull-to-refresh
 * o al entrar a la pantalla) — el refresco en vivo normal lo dispara
 * SyncManager cuando Firebase avisa.
 */
class NotificacionesViewModel(
    private val repo: NotificacionRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val notificaciones = repo.observar()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Notificacion>())

    var cargando by mutableStateOf(false); private set
    private var miUsuarioId: Int? = null

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            if (miUsuarioId == null) miUsuarioId = authRepository.obtenerPerfil()?.usuario?.id
            repo.sincronizar()
            cargando = false
        }
    }

    fun marcarLeida(id: Int?) {
        val uid = miUsuarioId ?: return
        viewModelScope.launch { repo.marcarLeida(id, uid) }
    }
}

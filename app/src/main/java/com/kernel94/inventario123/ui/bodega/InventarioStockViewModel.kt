package com.kernel94.inventario123.ui.bodega

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.InventarioBodega
import com.kernel94.inventario123.data.model.Usuario
import com.kernel94.inventario123.data.repository.BodegaRepository
import com.kernel94.inventario123.data.repository.Resultado
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Histórico de inventarios físicos (por mes) del stock personal de un
 * usuario (Mi Stock / Stock PFS), y punto de entrada para abrir/retomar el
 * del mes en curso. El backend siempre incluye al propio usuario en sesión
 * en la lista de "a quién puedo auditar" — si es la única opción (caso
 * normal para un PFS auditando su propio stock) se preselecciona sola,
 * igual que ya hace InventarioBodegaViewModel con una sola bodega.
 */
class InventarioStockViewModel(
    private val bodegaRepository: BodegaRepository,
) : ViewModel() {

    var usuarios by mutableStateOf<List<Usuario>>(emptyList()); private set
    var usuarioId by mutableStateOf<Int?>(null); private set
    var inventarios by mutableStateOf<List<InventarioBodega>>(emptyList()); private set
    var cargando by mutableStateOf(false); private set
    var iniciando by mutableStateOf(false); private set
    var mensaje by mutableStateOf<String?>(null); private set

    private var jHistorico: Job? = null

    fun iniciar() {
        viewModelScope.launch {
            cargando = true
            usuarios = bodegaRepository.stockUsuarios()
            if (usuarioId == null && usuarios.size == 1) usuarioId = usuarios.first().id
            cargarHistorico()
            cargando = false
        }
    }

    fun onUsuarioChange(id: Int?) {
        usuarioId = id
        inventarios = emptyList()
        jHistorico?.cancel()
        jHistorico = viewModelScope.launch {
            cargando = true
            cargarHistorico()
            cargando = false
        }
    }

    private suspend fun cargarHistorico() {
        val id = usuarioId ?: return
        when (val r = bodegaRepository.historicoUsuario(id)) {
            is Resultado.Exito -> inventarios = r.datos
            is Resultado.Error -> mensaje = r.mensaje
        }
    }

    /** Abre (o retoma) el inventario del mes en curso; devuelve su id por callback. */
    fun iniciarInventario(onListo: (Int) -> Unit) {
        val id = usuarioId ?: return
        iniciando = true
        viewModelScope.launch {
            when (val r = bodegaRepository.iniciarUsuario(id)) {
                is Resultado.Exito -> onListo(r.datos.id)
                is Resultado.Error -> mensaje = r.mensaje
            }
            iniciando = false
        }
    }

    fun limpiarMensaje() { mensaje = null }
}

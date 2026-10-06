package com.kernel94.inventario123.ui.rentec

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.ProyectoRentec
import com.kernel94.inventario123.data.repository.AuthRepository
import com.kernel94.inventario123.data.repository.ExportRepository
import com.kernel94.inventario123.data.repository.RentecRepository
import com.kernel94.inventario123.data.repository.Resultado
import java.io.File
import kotlinx.coroutines.launch

/** Detalle de un proyecto RENTEC: recibidos en bodega + instalados en tienda. */
class RentecDetalleViewModel(
    private val rentecRepository: RentecRepository,
    private val exportRepository: ExportRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var proyecto by mutableStateOf<ProyectoRentec?>(null); private set
    /**
     * Recibir equipo bajo el folio es sólo de coordinador y admin: es la entrada
     * del material al sistema. Instalar lo ya recibido lo hace cualquier rol, por
     * eso el folio es de toda la plaza. El servidor lo valida igual; esto sólo
     * evita mostrar un botón que va a fallar.
     */
    var puedeRecibir by mutableStateOf(false); private set
    var miUsuarioId by mutableStateOf(0); private set
    var cargando by mutableStateOf(false); private set
    var exportando by mutableStateOf(false); private set
    var borrando by mutableStateOf(false); private set
    var mensaje by mutableStateOf<String?>(null); private set

    fun cargar(id: Int) {
        viewModelScope.launch {
            cargando = true
            authRepository.obtenerPerfil()?.let {
                puedeRecibir = it.permisos.puedeRecibirRentec
                miUsuarioId = it.usuario?.id ?: 0
            }
            when (val r = rentecRepository.detalle(id)) {
                is Resultado.Exito -> proyecto = r.datos
                is Resultado.Error -> mensaje = r.mensaje
            }
            cargando = false
        }
    }

    fun cerrar(onCerrado: () -> Unit) {
        val id = proyecto?.id ?: return
        viewModelScope.launch {
            when (val r = rentecRepository.cerrar(id)) {
                is Resultado.Exito -> { proyecto = r.datos; onCerrado() }
                is Resultado.Error -> mensaje = r.mensaje
            }
        }
    }

    /**
     * Borra el folio. Sólo se puede si no tiene huella (equipo o bitácora); si la
     * tiene, el servidor lo niega y su mensaje explica qué hacer, así que se
     * muestra tal cual en vez de inventar uno genérico.
     */
    fun eliminar(onBorrado: () -> Unit) {
        val id = proyecto?.id ?: return
        if (borrando) return
        borrando = true
        viewModelScope.launch {
            when (val r = rentecRepository.eliminar(id)) {
                is Resultado.Exito -> { borrando = false; onBorrado() }
                is Resultado.Error -> { borrando = false; mensaje = r.mensaje }
            }
        }
    }

    fun exportar(context: Context, onListo: (File) -> Unit) {
        val p = proyecto ?: return
        if (exportando) return
        exportando = true
        viewModelScope.launch {
            when (val r = exportRepository.exportarRentec(context, p.id, p.folio)) {
                is Resultado.Exito -> { exportando = false; onListo(r.datos) }
                is Resultado.Error -> { exportando = false; mensaje = r.mensaje }
            }
        }
    }

    fun limpiarMensaje() { mensaje = null }
}

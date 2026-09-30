package com.kernel94.inventario123.ui.rentec

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.ProyectoRentec
import com.kernel94.inventario123.data.repository.ExportRepository
import com.kernel94.inventario123.data.repository.RentecRepository
import com.kernel94.inventario123.data.repository.Resultado
import java.io.File
import kotlinx.coroutines.launch

/** Detalle de un proyecto RENTEC: recibidos en bodega + instalados en tienda. */
class RentecDetalleViewModel(
    private val rentecRepository: RentecRepository,
    private val exportRepository: ExportRepository,
) : ViewModel() {

    var proyecto by mutableStateOf<ProyectoRentec?>(null); private set
    var cargando by mutableStateOf(false); private set
    var exportando by mutableStateOf(false); private set
    var mensaje by mutableStateOf<String?>(null); private set

    fun cargar(id: Int) {
        viewModelScope.launch {
            cargando = true
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

package com.kernel94.inventario123.ui.tiendas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.InventarioTiendaResponse
import com.kernel94.inventario123.data.model.ResumenInventarioTienda
import com.kernel94.inventario123.data.repository.ActivoRepository
import com.kernel94.inventario123.data.repository.Resultado
import com.kernel94.inventario123.data.repository.TiendaRepository
import kotlinx.coroutines.launch

/**
 * Checklist de inventario de UNA tienda (migración 036): abre/retoma el
 * inventario de la tienda, cada activo se marca ✓/✗ (manual o escaneando), y
 * al finalizar se cachea el resumen en la tienda (ver Tienda.guardarUltimoInventario).
 * Es por-visita (no mensual como InventarioBodega) y el "X" cancela y
 * descarta: no tiene sentido retomar un inventario a medias de otra visita.
 */
class InventarioTiendaViewModel(
    private val tiendaRepository: TiendaRepository,
    private val activoRepository: ActivoRepository,
) : ViewModel() {

    var inventario by mutableStateOf<InventarioTiendaResponse?>(null); private set
    var cargando by mutableStateOf(false); private set
    var procesando by mutableStateOf(false); private set
    var mensaje by mutableStateOf<String?>(null); private set

    /** Fila esperando el dialog de comentario (tocó ✗, o reabre uno ya marcado ✗). */
    var detalleParaNota by mutableStateOf<Int?>(null); private set

    fun cargar(tiendaId: Int) {
        cargando = true
        viewModelScope.launch {
            val abiertoId = tiendaRepository.inventarioAbierto(tiendaId)
            val r = if (abiertoId != null) tiendaRepository.inventarioDetalle(abiertoId)
                    else tiendaRepository.inventarioIniciar(tiendaId)
            when (r) {
                is Resultado.Exito -> inventario = r.datos
                is Resultado.Error -> mensaje = r.mensaje
            }
            cargando = false
        }
    }

    private fun recargar() {
        inventario?.id?.let { id ->
            viewModelScope.launch {
                when (val r = tiendaRepository.inventarioDetalle(id)) {
                    is Resultado.Exito -> inventario = r.datos
                    is Resultado.Error -> mensaje = r.mensaje
                }
            }
        }
    }

    fun escanear(codigo: String) {
        val id = inventario?.id ?: return
        if (procesando) return
        procesando = true
        viewModelScope.launch {
            when (val r = tiendaRepository.inventarioEscanear(id, codigo)) {
                is Resultado.Exito -> recargar()
                is Resultado.Error -> mensaje = r.mensaje
            }
            procesando = false
        }
    }

    /** Toque directo en el icono ✓ de una fila (no abre dialog). */
    fun marcarEncontrado(detalleId: Int) {
        viewModelScope.launch {
            when (val r = tiendaRepository.inventarioMarcar(detalleId, true)) {
                is Resultado.Exito -> recargar()
                is Resultado.Error -> mensaje = r.mensaje
            }
        }
    }

    /** Toque en ✗: abre el dialog de comentario (se confirma con [guardarNoEncontrado]). */
    fun pedirNota(detalleId: Int) { detalleParaNota = detalleId }
    fun cerrarDialogNota() { detalleParaNota = null }

    fun guardarNoEncontrado(detalleId: Int, nota: String) {
        viewModelScope.launch {
            when (val r = tiendaRepository.inventarioMarcar(detalleId, false)) {
                is Resultado.Exito -> {
                    if (nota.isNotBlank()) tiendaRepository.inventarioNota(detalleId, nota)
                    recargar()
                }
                is Resultado.Error -> mensaje = r.mensaje
            }
            detalleParaNota = null
        }
    }

    /**
     * Se llama cada vez que la pantalla vuelve a primer plano (el FAB "Agregar
     * activo" navega al alta normal — CrearEditarActivoScreen —, que no
     * devuelve el id creado porque puede quedar encolada offline). En vez de
     * depender de ese id, aquí se compara el stock ACTUAL de la tienda contra
     * lo ya snapshotteado en este checklist y se engancha (ya marcado
     * encontrado=1, "NUEVO") lo que aparezca de más.
     */
    fun sincronizarNuevos(tiendaId: Int) {
        val inv = inventario ?: return
        if (!inv.abierto) return
        viewModelScope.launch {
            val actuales = activoRepository.activosEnTiendaPorDispositivo(tiendaId)
            val yaEnChecklist = inv.detalle.map { it.activo_id }.toSet()
            val nuevos = actuales.filter { it.id !in yaEnChecklist }
            if (nuevos.isEmpty()) return@launch
            for (a in nuevos) tiendaRepository.inventarioAgregarNuevo(inv.id, a.id)
            recargar()
        }
    }

    fun finalizar(onCerrado: (ResumenInventarioTienda) -> Unit) {
        val inv = inventario ?: return
        if (inv.pendientes > 0) {
            mensaje = "Faltan ${inv.pendientes} activo(s) por revisar antes de finalizar."
            return
        }
        viewModelScope.launch {
            when (val r = tiendaRepository.inventarioCerrar(inv.id)) {
                is Resultado.Exito -> onCerrado(r.datos)
                is Resultado.Error -> mensaje = r.mensaje
            }
        }
    }

    fun cancelar(onCancelado: () -> Unit) {
        val id = inventario?.id
        if (id == null) { onCancelado(); return }
        viewModelScope.launch {
            tiendaRepository.inventarioCancelar(id)
            onCancelado()
        }
    }

    fun limpiarMensaje() { mensaje = null }
}

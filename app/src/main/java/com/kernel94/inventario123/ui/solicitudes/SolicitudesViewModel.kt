package com.kernel94.inventario123.ui.solicitudes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.Activo
import com.kernel94.inventario123.data.model.Bodega
import com.kernel94.inventario123.data.model.Perfil
import com.kernel94.inventario123.data.model.SolicitudTraslado
import com.kernel94.inventario123.data.repository.ActivoRepository
import com.kernel94.inventario123.data.repository.AuthRepository
import com.kernel94.inventario123.data.repository.CatalogoRepository
import com.kernel94.inventario123.data.repository.Resultado
import com.kernel94.inventario123.data.repository.SolicitudRepository
import kotlinx.coroutines.launch

/**
 * Cubre las 3 pantallas de Traslados (lista, crear, detalle), igual que
 * ModelosViewModel cubre el catálogo de modelos.
 */
class SolicitudesViewModel(
    private val solicitudRepository: SolicitudRepository,
    private val activoRepository: ActivoRepository,
    private val catalogoRepository: CatalogoRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var perfil by mutableStateOf<Perfil?>(null); private set

    // Lista
    var solicitudes by mutableStateOf<List<SolicitudTraslado>>(emptyList()); private set
    var puedeAprobar by mutableStateOf(false); private set
    var puedeCrear by mutableStateOf(false); private set
    var pendientes by mutableStateOf(0); private set
    var filtroEstado by mutableStateOf<String?>(null); private set

    // Crear
    var misAsignados by mutableStateOf<List<Activo>>(emptyList()); private set
    var bodegas by mutableStateOf<List<Bodega>>(emptyList()); private set

    // Detalle
    var detalle by mutableStateOf<SolicitudTraslado?>(null); private set

    var cargando by mutableStateOf(false); private set
    var enviando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    fun cargarLista(estado: String? = filtroEstado) {
        filtroEstado = estado
        cargando = true
        error = null
        viewModelScope.launch {
            if (perfil == null) perfil = authRepository.obtenerPerfil()
            when (val r = solicitudRepository.listar(estado)) {
                is Resultado.Exito -> {
                    solicitudes = r.datos.solicitudes
                    puedeAprobar = r.datos.puedeAprobar
                    puedeCrear = r.datos.puedeCrear
                    pendientes = r.datos.solicitudes.count { it.estado == "pendiente" }
                }
                is Resultado.Error -> error = r.mensaje
            }
            cargando = false
        }
    }

    fun cargarFormulario() {
        cargando = true
        error = null
        viewModelScope.launch {
            if (perfil == null) perfil = authRepository.obtenerPerfil()
            val plazaId = perfil?.permisos?.plazaId ?: perfil?.usuario?.plaza_id ?: 0
            when (val r = activoRepository.listar(vista = "mi_stock", status = "asignado", porPagina = 1000)) {
                is Resultado.Exito -> misAsignados = r.datos.activos
                is Resultado.Error -> error = r.mensaje
            }
            when (val r = catalogoRepository.obtenerCatalogos()) {
                is Resultado.Exito -> bodegas = r.datos.bodegas.filter { b ->
                    plazaId == 0 || (b.plazas_ids?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.contains(plazaId) ?: true)
                }.ifEmpty { r.datos.bodegas }
                is Resultado.Error -> {}
            }
            cargando = false
        }
    }

    fun crear(activos: List<Int>, bodegaId: Int, nota: String, firmaPng: ByteArray, onListo: (Boolean, String) -> Unit) {
        if (activos.isEmpty()) { onListo(false, "Selecciona al menos un activo."); return }
        if (bodegaId <= 0) { onListo(false, "Selecciona la bodega destino."); return }
        enviando = true
        viewModelScope.launch {
            val r = solicitudRepository.crear(activos, bodegaId, nota, firmaPng)
            enviando = false
            when (r) {
                is Resultado.Exito -> onListo(true, r.datos.message ?: "Solicitud enviada.")
                is Resultado.Error -> onListo(false, r.mensaje)
            }
        }
    }

    fun cargarDetalle(id: Int) {
        cargando = true
        error = null
        detalle = null
        viewModelScope.launch {
            if (perfil == null) perfil = authRepository.obtenerPerfil()
            when (val r = solicitudRepository.obtener(id)) {
                is Resultado.Exito -> detalle = r.datos
                is Resultado.Error -> error = r.mensaje
            }
            cargando = false
        }
    }

    fun aprobar(id: Int, firmaPng: ByteArray, onListo: (Boolean, String) -> Unit) {
        enviando = true
        viewModelScope.launch {
            val r = solicitudRepository.aprobar(id, firmaPng)
            enviando = false
            when (r) {
                is Resultado.Exito -> { onListo(true, r.datos.message ?: "Aprobada."); cargarDetalle(id) }
                is Resultado.Error -> onListo(false, r.mensaje)
            }
        }
    }

    fun rechazar(id: Int, motivo: String, onListo: (Boolean, String) -> Unit) {
        if (motivo.isBlank()) { onListo(false, "Indica el motivo del rechazo."); return }
        enviando = true
        viewModelScope.launch {
            val r = solicitudRepository.rechazar(id, motivo.trim())
            enviando = false
            when (r) {
                is Resultado.Exito -> { onListo(true, r.datos.message ?: "Rechazada."); cargarDetalle(id) }
                is Resultado.Error -> onListo(false, r.mensaje)
            }
        }
    }

    fun cancelar(id: Int, onListo: (Boolean, String) -> Unit) {
        enviando = true
        viewModelScope.launch {
            val r = solicitudRepository.cancelar(id)
            enviando = false
            when (r) {
                is Resultado.Exito -> { onListo(true, r.datos.message ?: "Cancelada."); cargarDetalle(id) }
                is Resultado.Error -> onListo(false, r.mensaje)
            }
        }
    }
}

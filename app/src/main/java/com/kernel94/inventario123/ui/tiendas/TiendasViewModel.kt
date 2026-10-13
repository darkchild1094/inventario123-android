package com.kernel94.inventario123.ui.tiendas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.Perfil
import com.kernel94.inventario123.data.model.Plaza
import com.kernel94.inventario123.data.model.TiendaConteo
import com.kernel94.inventario123.data.model.Usuario
import com.kernel94.inventario123.data.repository.AuthRepository
import com.kernel94.inventario123.data.repository.CatalogoRepository
import com.kernel94.inventario123.data.repository.Resultado
import com.kernel94.inventario123.data.repository.TiendaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Módulo "Tiendas": lista de tiendas acotada al rol, con nº de activos por tienda.
 * Al tocar una tienda se ven sus activos (drill-down al módulo). Sólo admin puede
 * asignar el ATI responsable desde aquí.
 */
class TiendasViewModel(
    private val tiendaRepository: TiendaRepository,
    private val catalogoRepository: CatalogoRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var perfil by mutableStateOf<Perfil?>(null); private set
    var plazas by mutableStateOf<List<Plaza>>(emptyList()); private set
    var plazaId by mutableStateOf<Int?>(null)
    var busqueda by mutableStateOf("")

    var tiendas by mutableStateOf<List<TiendaConteo>>(emptyList()); private set
    var puedeAsignarAti by mutableStateOf(false); private set
    var atis by mutableStateOf<List<Usuario>>(emptyList()); private set
    var cargando by mutableStateOf(false); private set
    var mensaje by mutableStateOf<String?>(null); private set

    private var debounceJob: Job? = null

    fun iniciar() {
        viewModelScope.launch {
            perfil = authRepository.obtenerPerfil()
            when (val r = catalogoRepository.obtenerCatalogos()) {
                is Resultado.Exito -> plazas = r.datos.plazas
                is Resultado.Error -> {}
            }
            // El backend ya devuelve solo las plazas del usuario; si es una sola, se
            // preselecciona en vez de dejar "Todas mis plazas" (ambiguo cuando no lo es).
            if (plazaId == null && plazas.size == 1) plazaId = plazas.first().id
            cargar()
        }
    }

    fun onPlazaChange(id: Int?) {
        plazaId = id?.takeIf { it > 0 }
        cargar()
    }

    fun onBusquedaChange(t: String) {
        busqueda = t
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch { delay(350); cargar() }
    }

    fun cargar() {
        cargando = true
        viewModelScope.launch {
            when (val r = tiendaRepository.listar(plazaId, busqueda)) {
                is Resultado.Exito -> {
                    tiendas = r.datos.tiendas
                    puedeAsignarAti = r.datos.puedeAsignarAti
                }
                is Resultado.Error -> mensaje = r.mensaje
            }
            if (puedeAsignarAti) {
                plazaId?.let { atis = tiendaRepository.atisPorPlaza(it) }
            }
            cargando = false
        }
    }

    fun asignarAti(tiendaId: Int, atiUsuarioId: Int?) {
        viewModelScope.launch {
            when (val r = tiendaRepository.asignarAti(tiendaId, atiUsuarioId)) {
                is Resultado.Exito -> {
                    mensaje = r.datos
                    tiendas = tiendas.map {
                        if (it.id == tiendaId) it.copy(
                            ati_usuario_id = atiUsuarioId,
                            ati_nombre = atis.firstOrNull { u -> u.id == atiUsuarioId }?.nombre,
                        ) else it
                    }
                }
                is Resultado.Error -> mensaje = r.mensaje
            }
        }
    }

    // ── Selección múltiple + eliminar tiendas ────────────────────────────
    var seleccion by mutableStateOf<Set<Int>>(emptySet()); private set
    var modoSeleccion by mutableStateOf(false); private set

    fun activarSeleccion(id: Int) { modoSeleccion = true; seleccion = seleccion + id }
    fun alternarSeleccion(id: Int) {
        seleccion = if (id in seleccion) seleccion - id else seleccion + id
        if (seleccion.isEmpty()) modoSeleccion = false
    }
    fun salirDeSeleccion() { seleccion = emptySet(); modoSeleccion = false }

    fun eliminarSeleccion(onListo: (Int, Int) -> Unit) {
        val ids = seleccion.toList()
        viewModelScope.launch {
            var ok = 0
            for (id in ids) {
                if (tiendaRepository.eliminar(id) is Resultado.Exito) ok++
            }
            salirDeSeleccion()
            cargar()
            onListo(ok, ids.size)
        }
    }

    // ── Alta/edición de tienda (TiendaFormScreen) ────────────────────────
    var tiendaEnEdicion by mutableStateOf<com.kernel94.inventario123.data.model.Tienda?>(null); private set
    var cargandoForm by mutableStateOf(false); private set
    var atisForm by mutableStateOf<List<Usuario>>(emptyList()); private set

    /** ATIs de la plaza elegida en el formulario — independiente del filtro de la lista. */
    fun cargarAtisForm(plazaId: Int) {
        viewModelScope.launch { atisForm = tiendaRepository.atisPorPlaza(plazaId) }
    }

    fun cargarParaEditar(id: Int) {
        tiendaEnEdicion = null
        cargandoForm = true
        viewModelScope.launch {
            when (val r = tiendaRepository.obtener(id)) {
                is Resultado.Exito -> tiendaEnEdicion = r.datos
                is Resultado.Error -> mensaje = r.mensaje
            }
            cargandoForm = false
        }
    }

    fun guardarTienda(id: Int?, cr: String, nombre: String, plazaId: Int, atiUsuarioId: Int?, onListo: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val r = if (id == null) {
                when (val rc = tiendaRepository.crear(cr, nombre, plazaId, atiUsuarioId)) {
                    is Resultado.Exito -> Resultado.Exito("Tienda creada.")
                    is Resultado.Error -> rc
                }
            } else {
                tiendaRepository.actualizar(id, cr, nombre, plazaId, atiUsuarioId)
            }
            when (r) {
                is Resultado.Exito -> { cargar(); onListo(true, r.datos) }
                is Resultado.Error -> onListo(false, r.mensaje)
            }
        }
    }

    fun eliminarTienda(id: Int, onListo: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val r = tiendaRepository.eliminar(id)) {
                is Resultado.Exito -> { cargar(); onListo(true, r.datos) }
                is Resultado.Error -> onListo(false, r.mensaje)
            }
        }
    }

    fun limpiarMensaje() { mensaje = null }
}

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

    fun limpiarMensaje() { mensaje = null }
}

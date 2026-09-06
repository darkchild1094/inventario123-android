package com.kernel94.inventario123.ui.tiendas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.Catalogos
import com.kernel94.inventario123.data.model.Perfil
import com.kernel94.inventario123.data.model.Plaza
import com.kernel94.inventario123.data.model.Tienda
import com.kernel94.inventario123.data.model.Usuario
import com.kernel94.inventario123.data.repository.AuthRepository
import com.kernel94.inventario123.data.repository.CatalogoRepository
import com.kernel94.inventario123.data.repository.Resultado
import com.kernel94.inventario123.data.repository.TiendaRepository
import kotlinx.coroutines.launch

/** Pantalla "Tiendas · ATI responsable" (espeja app/views/tiendas/index.php, solo admin). */
class TiendasViewModel(
    private val tiendaRepository: TiendaRepository,
    private val catalogoRepository: CatalogoRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var perfil by mutableStateOf<Perfil?>(null); private set
    var plazas by mutableStateOf<List<Plaza>>(emptyList()); private set
    var plazaId by mutableStateOf<Int?>(null)
    var busqueda by mutableStateOf("")

    private var tiendasTodas by mutableStateOf<List<Tienda>>(emptyList())
    var atis by mutableStateOf<List<Usuario>>(emptyList()); private set
    var cargando by mutableStateOf(false); private set
    var mensaje by mutableStateOf<String?>(null); private set

    val tiendas: List<Tienda>
        get() = if (busqueda.isBlank()) tiendasTodas
        else tiendasTodas.filter {
            it.nombre.contains(busqueda, true) || (it.cr_tienda ?: "").contains(busqueda, true)
        }

    fun iniciar() {
        viewModelScope.launch {
            perfil = authRepository.obtenerPerfil()
            when (val r = catalogoRepository.obtenerCatalogos()) {
                is Resultado.Exito -> plazas = r.datos.plazas
                is Resultado.Error -> {}
            }
            if (plazaId == null) plazaId = perfil?.permisos?.plazaId?.takeIf { it > 0 } ?: plazas.firstOrNull()?.id
            cargar()
        }
    }

    fun onPlazaChange(id: Int?) {
        plazaId = id
        cargar()
    }

    fun cargar() {
        val pid = plazaId ?: return
        cargando = true
        viewModelScope.launch {
            tiendasTodas = tiendaRepository.tiendasPorPlaza(pid)
            atis = tiendaRepository.atisPorPlaza(pid)
            cargando = false
        }
    }

    fun asignarAti(tiendaId: Int, atiUsuarioId: Int?) {
        viewModelScope.launch {
            when (val r = tiendaRepository.asignarAti(tiendaId, atiUsuarioId)) {
                is Resultado.Exito -> {
                    mensaje = r.datos
                    tiendasTodas = tiendasTodas.map {
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

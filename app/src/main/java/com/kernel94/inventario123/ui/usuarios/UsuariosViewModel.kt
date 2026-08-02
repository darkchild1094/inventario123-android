package com.kernel94.inventario123.ui.usuarios

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.Catalogos
import com.kernel94.inventario123.data.model.Usuario
import com.kernel94.inventario123.data.repository.CatalogoRepository
import com.kernel94.inventario123.data.repository.Resultado
import com.kernel94.inventario123.data.repository.UsuarioRepository
import kotlinx.coroutines.launch

class UsuariosViewModel(
    private val usuarioRepository: UsuarioRepository,
    private val catalogoRepository: CatalogoRepository,
) : ViewModel() {
    var usuarios by mutableStateOf<List<Usuario>>(emptyList()); private set
    var catalogos by mutableStateOf(Catalogos()); private set
    var cargando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    fun cargar() {
        cargando = true
        viewModelScope.launch {
            when (val r = usuarioRepository.listar()) {
                is Resultado.Exito -> { usuarios = r.datos; cargando = false }
                is Resultado.Error -> { error = r.mensaje; cargando = false }
            }
            when (val r = catalogoRepository.obtenerCatalogos()) {
                is Resultado.Exito -> catalogos = r.datos
                is Resultado.Error -> {}
            }
        }
    }

    /** plazaIds: selección de checkboxes (varias plazas, igual que en la web) */
    fun guardarUsuario(
        id: Int?, nombre: String, email: String, password: String?,
        tipo: String, plazaIds: List<Int>, onListo: (Boolean, String) -> Unit,
    ) {
        viewModelScope.launch {
            val datos = mutableMapOf<String, Any?>(
                "nombre" to nombre, "email" to email, "tipo" to tipo, "plaza_id" to plazaIds,
            )
            if (!password.isNullOrBlank()) datos["password"] = password

            val resultado = if (id == null) {
                usuarioRepository.crear(datos)
            } else {
                datos["id"] = id
                usuarioRepository.actualizar(datos)
            }
            when (resultado) {
                is Resultado.Exito -> { onListo(true, resultado.datos.message ?: "Guardado."); cargar() }
                is Resultado.Error -> onListo(false, resultado.mensaje)
            }
        }
    }

    fun eliminarUsuario(id: Int, onListo: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val r = usuarioRepository.eliminar(id)) {
                is Resultado.Exito -> { onListo(true, r.datos.message ?: "Eliminado."); cargar() }
                is Resultado.Error -> onListo(false, r.mensaje)
            }
        }
    }
}

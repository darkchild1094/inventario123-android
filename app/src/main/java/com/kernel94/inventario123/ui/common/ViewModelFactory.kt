package com.kernel94.inventario123.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kernel94.inventario123.Inventario123App
import com.kernel94.inventario123.ui.auth.LoginViewModel
import com.kernel94.inventario123.ui.detalle.DetalleViewModel
import com.kernel94.inventario123.ui.form.CrearEditarActivoViewModel
import com.kernel94.inventario123.ui.listado.ListadoViewModel
import com.kernel94.inventario123.ui.usuarios.UsuariosViewModel

/** Factory manual simple (sin Hilt) que arma cada ViewModel con sus repos desde Inventario123App. */
class ViewModelFactory(private val app: Inventario123App) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(app.authRepository) as T
            modelClass.isAssignableFrom(ListadoViewModel::class.java) ->
                ListadoViewModel(app.activoRepository, app.catalogoRepository, app.authRepository, app.exportRepository) as T
            modelClass.isAssignableFrom(DetalleViewModel::class.java) ->
                DetalleViewModel(app.activoRepository) as T
            modelClass.isAssignableFrom(CrearEditarActivoViewModel::class.java) ->
                CrearEditarActivoViewModel(app.activoRepository, app.catalogoRepository, app.authRepository) as T
            modelClass.isAssignableFrom(UsuariosViewModel::class.java) ->
                UsuariosViewModel(app.usuarioRepository, app.catalogoRepository) as T
            else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
        }
    }
}

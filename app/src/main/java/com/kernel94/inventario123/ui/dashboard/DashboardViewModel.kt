package com.kernel94.inventario123.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.Perfil
import com.kernel94.inventario123.data.model.ResumenDashboard
import com.kernel94.inventario123.data.repository.ActivoRepository
import com.kernel94.inventario123.data.repository.AuthRepository
import com.kernel94.inventario123.data.repository.Resultado
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val activoRepository: ActivoRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var perfil by mutableStateOf<Perfil?>(null); private set
    var resumen by mutableStateOf<ResumenDashboard?>(null); private set
    var cargando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    fun cargar() {
        cargando = true
        error = null
        viewModelScope.launch {
            if (perfil == null) perfil = authRepository.obtenerPerfil()
            when (val r = activoRepository.resumenDashboard()) {
                is Resultado.Exito -> resumen = r.datos
                is Resultado.Error -> error = r.mensaje
            }
            cargando = false
        }
    }
}

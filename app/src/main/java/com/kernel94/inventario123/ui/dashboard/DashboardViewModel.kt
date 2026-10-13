package com.kernel94.inventario123.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.Perfil
import com.kernel94.inventario123.data.model.ProyectoRentec
import com.kernel94.inventario123.data.model.ResumenDashboard
import com.kernel94.inventario123.data.model.VersionApp
import com.kernel94.inventario123.data.repository.ActivoRepository
import com.kernel94.inventario123.data.repository.AuthRepository
import com.kernel94.inventario123.data.repository.NotificacionRepository
import com.kernel94.inventario123.data.repository.RentecRepository
import com.kernel94.inventario123.data.repository.Resultado
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val activoRepository: ActivoRepository,
    private val authRepository: AuthRepository,
    private val rentecRepository: RentecRepository,
    notificacionRepository: NotificacionRepository,
) : ViewModel() {

    var perfil by mutableStateOf<Perfil?>(null); private set
    var resumen by mutableStateOf<ResumenDashboard?>(null); private set
    var cargando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var versionApp by mutableStateOf<VersionApp?>(null); private set
    var proyectosRentec by mutableStateOf<List<ProyectoRentec>>(emptyList()); private set

    val notificacionesNoLeidas = notificacionRepository.observarNoLeidas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** % global de avance de los proyectos RENTEC abiertos (instalados / total que ha pasado por ellos). */
    val avanceRentecGlobal: Int get() {
        val abiertos = proyectosRentec.filter { it.abierto }
        val total = abiertos.sumOf { it.recibidos + it.instalados }
        val instalados = abiertos.sumOf { it.instalados }
        return if (total <= 0) 0 else (instalados * 100 / total)
    }

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
            if (perfil?.modulos.orEmpty().any { it.clave == "rentec" }) {
                when (val r = rentecRepository.listar()) {
                    is Resultado.Exito -> proyectosRentec = r.datos
                    is Resultado.Error -> {}
                }
            }
        }
        // Aparte: si falla no debe tumbar el resto del dashboard.
        viewModelScope.launch { versionApp = activoRepository.obtenerUltimaVersionApp() }
    }
}

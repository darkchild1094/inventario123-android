package com.kernel94.inventario123.ui.historial

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.Catalogos
import com.kernel94.inventario123.data.model.Movimiento
import com.kernel94.inventario123.data.model.Perfil
import com.kernel94.inventario123.data.repository.AuthRepository
import com.kernel94.inventario123.data.repository.CatalogoRepository
import com.kernel94.inventario123.data.repository.MovimientoRepository
import com.kernel94.inventario123.data.repository.Resultado
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Pestaña Historial: bitácora filtrable (mismos filtros que historial/index.php). */
class HistorialViewModel(
    private val movimientoRepository: MovimientoRepository,
    private val catalogoRepository: CatalogoRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var perfil by mutableStateOf<Perfil?>(null); private set
    var catalogos by mutableStateOf(Catalogos()); private set

    var serie by mutableStateOf("")
    var evento by mutableStateOf<String?>(null)
    var tiendaId by mutableStateOf<Int?>(null)
    var usuarioId by mutableStateOf<Int?>(null)
    var desde by mutableStateOf("")
    var hasta by mutableStateOf("")

    var movimientos by mutableStateOf<List<Movimiento>>(emptyList()); private set
    var totalResultados by mutableStateOf(0); private set
    var paginaActual by mutableStateOf(1); private set
    var totalPaginas by mutableStateOf(1); private set
    var cargando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    private var debounceJob: Job? = null

    fun iniciar() {
        viewModelScope.launch {
            perfil = authRepository.obtenerPerfil()
            when (val r = catalogoRepository.obtenerCatalogos()) {
                is Resultado.Exito -> catalogos = r.datos
                is Resultado.Error -> {}
            }
            cargar()
        }
    }

    fun onSerieChange(texto: String) {
        serie = texto
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(400)
            cargar()
        }
    }

    fun onFiltroChange() = cargar()

    fun limpiarFiltros() {
        serie = ""; evento = null; tiendaId = null; usuarioId = null; desde = ""; hasta = ""
        cargar()
    }

    fun cargar(pagina: Int = 1) {
        cargando = true
        error = null
        viewModelScope.launch {
            when (val r = movimientoRepository.listar(
                serie = serie, evento = evento, tiendaId = tiendaId, usuarioId = usuarioId,
                desde = desde, hasta = hasta, pagina = pagina, porPagina = 30,
            )) {
                is Resultado.Exito -> {
                    movimientos = r.datos.movimientos
                    totalResultados = r.datos.paginacion.total_resultados
                    paginaActual = r.datos.paginacion.pagina_actual
                    totalPaginas = r.datos.paginacion.total_paginas
                    cargando = false
                }
                is Resultado.Error -> { error = r.mensaje; cargando = false }
            }
        }
    }

    fun paginaSiguiente() { if (paginaActual < totalPaginas) cargar(paginaActual + 1) }
    fun paginaAnterior() { if (paginaActual > 1) cargar(paginaActual - 1) }
}

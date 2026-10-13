package com.kernel94.inventario123.ui.movimientos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.Movimiento
import com.kernel94.inventario123.data.repository.MovimientoRepository
import com.kernel94.inventario123.data.repository.Resultado
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Módulo "Movimientos" (net-new del rediseño): línea de tiempo con pestañas
 * Bodega / Usuario / Tienda — un movimiento aparece en una si su origen O
 * destino es de ese tipo de lugar (ver Movimiento::listar, filtro lugar_tipo).
 * Reusa movimiento/Movimiento::listar() tal cual; sólo cambia el filtro y la
 * presentación (antes era Historial, pestañas distintas, ya borrado).
 */
class MovimientosViewModel(
    private val movimientoRepository: MovimientoRepository,
) : ViewModel() {

    var pestana by mutableStateOf("bodega"); private set
    var busqueda by mutableStateOf("")

    var movimientos by mutableStateOf<List<Movimiento>>(emptyList()); private set
    var paginaActual by mutableStateOf(1); private set
    var totalPaginas by mutableStateOf(1); private set
    var totalResultados by mutableStateOf(0); private set
    var cargando by mutableStateOf(false); private set
    var cargandoMas by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    val puedeCargarMas: Boolean get() = paginaActual < totalPaginas

    private var debounceJob: Job? = null
    private var cargaJob: Job? = null

    fun cambiarPestana(p: String) {
        if (pestana == p) return
        pestana = p
        cargar()
    }

    fun onBusquedaChange(texto: String) {
        busqueda = texto
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch { delay(350); cargar() }
    }

    fun cargar(pagina: Int = 1) {
        cargaJob?.cancel()
        cargando = true
        error = null
        cargaJob = viewModelScope.launch {
            when (val r = pedirPagina(pagina)) {
                is Resultado.Exito -> {
                    movimientos = r.datos.movimientos
                    aplicarPaginacion(r.datos)
                    cargando = false
                }
                is Resultado.Error -> { error = r.mensaje; cargando = false }
            }
        }
    }

    fun cargarMas() {
        if (cargando || cargandoMas || !puedeCargarMas) return
        val siguiente = paginaActual + 1
        cargandoMas = true
        viewModelScope.launch {
            when (val r = pedirPagina(siguiente)) {
                is Resultado.Exito -> {
                    if (r.datos.paginacion.pagina_actual == paginaActual + 1) {
                        movimientos = movimientos + r.datos.movimientos
                        aplicarPaginacion(r.datos)
                    }
                    cargandoMas = false
                }
                is Resultado.Error -> { error = r.mensaje; cargandoMas = false }
            }
        }
    }

    private suspend fun pedirPagina(pagina: Int) = movimientoRepository.listar(
        serie = busqueda.ifBlank { null },
        lugarTipo = pestana,
        pagina = pagina, porPagina = 40,
    )

    private fun aplicarPaginacion(datos: com.kernel94.inventario123.data.model.ListadoMovimientosResponse) {
        paginaActual = datos.paginacion.pagina_actual
        totalPaginas = datos.paginacion.total_paginas
        totalResultados = datos.paginacion.total_resultados
    }
}

package com.kernel94.inventario123.ui.listado

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.Activo
import com.kernel94.inventario123.data.model.Catalogos
import com.kernel94.inventario123.data.model.Perfil
import com.kernel94.inventario123.data.repository.ActivoRepository
import com.kernel94.inventario123.data.repository.AuthRepository
import com.kernel94.inventario123.data.repository.CatalogoRepository
import com.kernel94.inventario123.data.repository.ExportRepository
import com.kernel94.inventario123.data.repository.Resultado
import kotlinx.coroutines.Job
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ListadoViewModel(
    private val activoRepository: ActivoRepository,
    private val catalogoRepository: CatalogoRepository,
    private val authRepository: AuthRepository,
    private val exportRepository: ExportRepository,
) : ViewModel() {

    var perfil by mutableStateOf<Perfil?>(null); private set
    var catalogos by mutableStateOf(Catalogos()); private set

    var vistaActual by mutableStateOf("todos")
    var negocioId by mutableStateOf<Int?>(null)
    var regionId by mutableStateOf<Int?>(null)
    var plazaId by mutableStateOf<Int?>(null)
    var usuarioId by mutableStateOf<Int?>(null)
    var status by mutableStateOf<String?>(null)
    var busqueda by mutableStateOf("")

    var activos by mutableStateOf<List<Activo>>(emptyList()); private set
    var paginaActual by mutableStateOf(1); private set
    var totalPaginas by mutableStateOf(1); private set
    var cargando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    private var debounceJob: Job? = null

    fun iniciar() {
        viewModelScope.launch {
            perfil = authRepository.obtenerPerfil()
            vistaActual = perfil?.vistasDisponibles?.firstOrNull() ?: "todos"
            when (val r = catalogoRepository.obtenerCatalogos()) {
                is Resultado.Exito -> catalogos = r.datos
                is Resultado.Error -> {}
            }
            cargar()
        }
    }

    fun cambiarVista(v: String) {
        vistaActual = v
        cargar()
    }

    fun onFiltroChange() {
        cargar()
    }

    fun onBusquedaChange(texto: String) {
        busqueda = texto
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(400)
            cargar()
        }
    }

    fun cargar(pagina: Int = 1) {
        cargando = true
        error = null
        viewModelScope.launch {
            when (val r = activoRepository.listar(
                vista = vistaActual, negocioId = negocioId, regionId = regionId,
                plazaId = plazaId, usuarioId = usuarioId, status = status,
                busqueda = busqueda.ifBlank { null }, pagina = pagina,
            )) {
                is Resultado.Exito -> {
                    activos = r.datos.activos
                    paginaActual = r.datos.paginacion.pagina_actual
                    totalPaginas = r.datos.paginacion.total_paginas
                    cargando = false
                }
                is Resultado.Error -> {
                    error = r.mensaje
                    cargando = false
                }
            }
        }
    }

    var exportando by mutableStateOf(false); private set

    fun exportar(context: Context, onListo: (Resultado<File>) -> Unit) {
        exportando = true
        viewModelScope.launch {
            val r = exportRepository.exportarInventario(context)
            exportando = false
            onListo(r)
        }
    }

    fun limpiarFiltros() {
        negocioId = null; regionId = null; plazaId = null; usuarioId = null
        status = null; busqueda = ""
        cargar()
    }
}

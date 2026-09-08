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
import com.kernel94.inventario123.data.repository.SolicitudRepository
import kotlinx.coroutines.Job
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ListadoViewModel(
    private val activoRepository: ActivoRepository,
    private val catalogoRepository: CatalogoRepository,
    private val authRepository: AuthRepository,
    private val exportRepository: ExportRepository,
    private val solicitudRepository: SolicitudRepository,
) : ViewModel() {

    var solicitudesPendientes by mutableStateOf(0); private set

    var perfil by mutableStateOf<Perfil?>(null); private set
    var catalogos by mutableStateOf(Catalogos()); private set

    var vistaActual by mutableStateOf("todos")
    // Navegación por módulos: si != null, manda sobre "vista".
    var modulo by mutableStateOf<String?>(null)
    var tiendaId by mutableStateOf<Int?>(null)
    var moduloEditable by mutableStateOf(false); private set
    var negocioId by mutableStateOf<Int?>(null)
    var regionId by mutableStateOf<Int?>(null)
    var plazaId by mutableStateOf<Int?>(null)
    var usuarioId by mutableStateOf<Int?>(null)
    var status by mutableStateOf<String?>(null)
    var busqueda by mutableStateOf("")

    var activos by mutableStateOf<List<Activo>>(emptyList()); private set
    var conteosVistas by mutableStateOf<Map<String, Int>>(emptyMap()); private set
    var paginaActual by mutableStateOf(1); private set
    var totalPaginas by mutableStateOf(1); private set
    var cargando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    private var debounceJob: Job? = null

    /** @param moduloArg si viene, la pantalla lista ese módulo; @param tiendaArg acota a una tienda. */
    fun iniciar(moduloArg: String? = null, tiendaArg: Int? = null) {
        modulo = moduloArg?.takeIf { it.isNotBlank() }
        tiendaId = tiendaArg?.takeIf { it > 0 }
        viewModelScope.launch {
            perfil = authRepository.obtenerPerfil()
            val vistas = perfil?.vistasDisponibles ?: emptyList()
            if (modulo == null) {
                vistaActual = if ("todos" in vistas) "todos" else vistas.firstOrNull() ?: "todos"
            }
            when (val r = catalogoRepository.obtenerCatalogos()) {
                is Resultado.Exito -> catalogos = r.datos
                is Resultado.Error -> {}
            }
            if (perfil?.permisos?.puedeAprobarTraslados == true) {
                solicitudesPendientes = solicitudRepository.contarPendientes()
            }
            cargar()

            // Sólo en modo "vista" (legado): conteos de las demás pestañas.
            if (modulo == null) {
                vistas.forEach { vista ->
                    if (vista != vistaActual) {
                        launch {
                            val res = activoRepository.listar(vista = vista, porPagina = 1)
                            if (res is Resultado.Exito) {
                                conteosVistas = conteosVistas + (vista to res.datos.paginacion.total_resultados)
                            }
                        }
                    }
                }
            }
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
                modulo = modulo,
                vista = if (modulo == null) vistaActual else null,
                negocioId = negocioId, regionId = regionId,
                plazaId = plazaId, tiendaId = tiendaId, usuarioId = usuarioId, status = status,
                busqueda = busqueda.ifBlank { null }, pagina = pagina,
                porPagina = 5000 // Aumentamos el límite para mostrar prácticamente todo sin paginación manual
            )) {
                is Resultado.Exito -> {
                    activos = r.datos.activos
                    paginaActual = r.datos.paginacion.pagina_actual
                    totalPaginas = r.datos.paginacion.total_paginas
                    moduloEditable = r.datos.moduloEditable

                    conteosVistas = conteosVistas + ((modulo ?: vistaActual) to r.datos.paginacion.total_resultados)

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
            val m = modulo
            val r = if (m != null) exportRepository.exportarModulo(context, m, tiendaId)
                    else exportRepository.exportarInventario(context)
            exportando = false
            onListo(r)
        }
    }

    fun limpiarFiltros() {
        negocioId = null; regionId = null; plazaId = null; usuarioId = null
        status = null; busqueda = ""
        cargar()
    }

    fun eliminar(id: Int, onListo: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val r = activoRepository.eliminar(id)) {
                is Resultado.Exito -> {
                    cargar(paginaActual)
                    onListo(true, r.datos.message ?: "Activo eliminado")
                }
                is Resultado.Error -> onListo(false, r.mensaje)
            }
        }
    }
}

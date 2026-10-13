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
import com.kernel94.inventario123.data.repository.NotificacionRepository
import com.kernel94.inventario123.data.repository.Resultado
import com.kernel94.inventario123.data.repository.TiendaRepository
import kotlinx.coroutines.Job
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ListadoViewModel(
    private val activoRepository: ActivoRepository,
    private val catalogoRepository: CatalogoRepository,
    private val authRepository: AuthRepository,
    private val exportRepository: ExportRepository,
    private val tiendaRepository: TiendaRepository,
    private val notificacionRepository: NotificacionRepository,
) : ViewModel() {

    /**
     * Plazas del usuario, para las pestañas de los módulos acotados por plaza.
     * Sin ellas un coordinador con dos plazas veía las dos bodegas revueltas en
     * una sola lista, que es justo lo que no debe pasar con el inventario.
     */
    var misPlazas by mutableStateOf<List<com.kernel94.inventario123.data.model.Plaza>>(emptyList()); private set

    /** ¿Este módulo se acota por plaza y el usuario tiene más de una? */
    val mostrarPestanasPlaza: Boolean
        get() = misPlazas.size > 1 && modulo in listOf("bodega", "tiendas", "stock_pfs", "ati")

    var perfil by mutableStateOf<Perfil?>(null); private set
    var catalogos by mutableStateOf(Catalogos()); private set

    // Todo listado es de un módulo (Bodega, Mi stock, Stock PFS, ATI, Tiendas…):
    // la navegación vieja por `vista=` ya no existe.
    var modulo by mutableStateOf<String?>(null)
    var tiendaId by mutableStateOf<Int?>(null)
    var moduloEditable by mutableStateOf(false); private set
    var negocioId by mutableStateOf<Int?>(null)
    var regionId by mutableStateOf<Int?>(null)
    var plazaId by mutableStateOf<Int?>(null)
    var usuarioId by mutableStateOf<Int?>(null)
    var status by mutableStateOf<String?>(null)
    var busqueda by mutableStateOf("")

    /** Sólo cuando modulo=="tiendas" && tiendaId!=null: nombre, CR y último inventario. */
    var tiendaActual by mutableStateOf<com.kernel94.inventario123.data.model.Tienda?>(null); private set

    var activos by mutableStateOf<List<Activo>>(emptyList()); private set
    var paginaActual by mutableStateOf(1); private set
    var totalPaginas by mutableStateOf(1); private set
    var totalResultados by mutableStateOf(0); private set
    var cargando by mutableStateOf(false); private set
    var cargandoMas by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    /** ¿Queda al menos una página más por traer? */
    val puedeCargarMas: Boolean get() = paginaActual < totalPaginas

    // ── Selección múltiple para "Enviar" (Bodega↔Mi stock↔usuario) ──────────
    // Inmediato, sin aceptación (ver EnvioService/NotificacionRepository). Solo
    // tiene sentido donde el equipo tiene un dueño que lo puede entregar: Mi
    // Stock (lo mío) y Bodega si la tengo editable (coordinador/admin).
    var seleccion by mutableStateOf<Set<Int>>(emptySet()); private set
    var modoSeleccion by mutableStateOf(false); private set
    var enviando by mutableStateOf(false); private set

    val puedeEnviarAqui: Boolean
        get() = perfil?.permisos?.puedeEnviar == true &&
            (modulo == "mi_stock" || (modulo == "bodega" && moduloEditable))

    fun activarSeleccion(id: Int) {
        if (!puedeEnviarAqui) return
        modoSeleccion = true
        seleccion = seleccion + id
    }

    fun alternarSeleccion(id: Int) {
        seleccion = if (id in seleccion) seleccion - id else seleccion + id
        if (seleccion.isEmpty()) modoSeleccion = false
    }

    fun salirDeSeleccion() { seleccion = emptySet(); modoSeleccion = false }
    fun seleccionarTodos() { seleccion = activos.map { it.id }.toSet() }

    /** Plazas a las que se puede enviar: todas menos la actual si se envía desde Bodega. */
    val plazasDestino: List<com.kernel94.inventario123.data.model.Plaza>
        get() = if (modulo == "bodega") catalogos.plazas.filter { it.id != plazaId } else catalogos.plazas

    /** Personas de la plaza a las que se les puede entregar (menos uno mismo). */
    val destinatarios: List<com.kernel94.inventario123.data.model.Usuario>
        get() {
            val yo = perfil?.usuario?.id
            val plaza = plazaId ?: perfil?.permisos?.plazaId
            return catalogos.usuarios.filter { it.id != yo && (plaza == null || it.plaza_id == null || it.plaza_id == plaza) }
        }

    fun enviar(tipo: String, plazaDestinoId: Int?, usuarioDestinoId: Int?, onListo: (Boolean, String) -> Unit) {
        if (seleccion.isEmpty() || enviando) return
        enviando = true
        viewModelScope.launch {
            val r = notificacionRepository.enviar(seleccion.toList(), tipo, plazaDestinoId, usuarioDestinoId)
            enviando = false
            when (r) {
                is Resultado.Exito -> { salirDeSeleccion(); cargar(); onListo(true, r.datos) }
                is Resultado.Error -> onListo(false, r.mensaje)
            }
        }
    }

    private var debounceJob: Job? = null
    private var cargaJob: Job? = null

    private companion object {
        /**
         * Tamaño de página. Antes se pedían 5000 "para no paginar", y como
         * ninguna plaza baja de 5,956 activos en tienda el listado mostraba
         * ~5000 y el resto quedaba invisible, sin aviso. Ahora se pagina de
         * verdad y la pantalla va trayendo páginas al llegar al final.
         */
        const val POR_PAGINA = 50
    }

    /** @param moduloArg módulo a listar; @param tiendaArg acota a
     *  una tienda; @param usuarioArg acota a un usuario (p. ej. desde la lista de Stock PFS). */
    fun iniciar(moduloArg: String? = null, tiendaArg: Int? = null, usuarioArg: Int? = null) {
        modulo = moduloArg?.takeIf { it.isNotBlank() }
        tiendaId = tiendaArg?.takeIf { it > 0 }
        usuarioId = usuarioArg?.takeIf { it > 0 }
        viewModelScope.launch {
            perfil = authRepository.obtenerPerfil()
            when (val r = catalogoRepository.obtenerCatalogos()) {
                is Resultado.Exito -> catalogos = r.datos
                is Resultado.Error -> {}
            }
            // Pestañas por plaza: el catálogo ya viene acotado al alcance del
            // rol, así que sus plazas son exactamente las que puede ver.
            misPlazas = catalogos.plazas
            if (mostrarPestanasPlaza && plazaId == null) {
                plazaId = perfil?.permisos?.plazaId?.takeIf { it > 0 } ?: misPlazas.firstOrNull()?.id
            }
            cargarTiendaActual()
            cargar()
        }
    }

    /** Refresca el nombre/CR/último-inventario de la tienda actual (tras editar o cerrar un checklist). */
    fun cargarTiendaActual() {
        val id = tiendaId
        if (modulo != "tiendas" || id == null) { tiendaActual = null; return }
        viewModelScope.launch {
            when (val r = tiendaRepository.obtener(id)) {
                is Resultado.Exito -> tiendaActual = r.datos
                is Resultado.Error -> {}
            }
        }
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

    /** Carga la primera página y reemplaza la lista. Cualquier cambio de filtro entra por aquí. */
    fun cargar(pagina: Int = 1) {
        cargaJob?.cancel()
        cargando = true
        error = null
        cargaJob = viewModelScope.launch {
            when (val r = pedirPagina(pagina)) {
                is Resultado.Exito -> {
                    activos = r.datos.activos
                    aplicarPaginacion(r.datos)
                    cargando = false
                }
                is Resultado.Error -> {
                    error = r.mensaje
                    cargando = false
                }
            }
        }
    }

    /**
     * Trae la página siguiente y la añade al final. La llama la pantalla cuando
     * el usuario se acerca al final de la lista.
     */
    fun cargarMas() {
        if (cargando || cargandoMas || !puedeCargarMas) return
        val siguiente = paginaActual + 1
        cargandoMas = true
        viewModelScope.launch {
            when (val r = pedirPagina(siguiente)) {
                is Resultado.Exito -> {
                    // Por si llegó una recarga desde cero mientras esta petición
                    // viajaba: sólo se concatena si sigue siendo la continuación.
                    if (r.datos.paginacion.pagina_actual == paginaActual + 1) {
                        activos = activos + r.datos.activos
                        aplicarPaginacion(r.datos)
                    }
                    cargandoMas = false
                }
                is Resultado.Error -> {
                    error = r.mensaje
                    cargandoMas = false
                }
            }
        }
    }

    private suspend fun pedirPagina(pagina: Int) = activoRepository.listar(
        modulo = modulo,
        negocioId = negocioId, regionId = regionId,
        plazaId = plazaId, tiendaId = tiendaId, usuarioId = usuarioId, status = status,
        busqueda = busqueda.ifBlank { null }, pagina = pagina,
        porPagina = POR_PAGINA,
    )

    private fun aplicarPaginacion(datos: com.kernel94.inventario123.data.model.ListadoActivosResponse) {
        paginaActual = datos.paginacion.pagina_actual
        totalPaginas = datos.paginacion.total_paginas
        totalResultados = datos.paginacion.total_resultados
        moduloEditable = datos.moduloEditable
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
                    // Desde la primera página: al paginar de verdad, recargar
                    // sólo la página actual descartaría las anteriores.
                    cargar()
                    onListo(true, r.datos.message ?: "Activo eliminado")
                }
                is Resultado.Error -> onListo(false, r.mensaje)
            }
        }
    }
}

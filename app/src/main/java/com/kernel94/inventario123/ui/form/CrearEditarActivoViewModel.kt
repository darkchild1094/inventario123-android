package com.kernel94.inventario123.ui.form

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.*
import com.kernel94.inventario123.data.repository.ActivoRepository
import com.kernel94.inventario123.data.repository.AuthRepository
import com.kernel94.inventario123.data.repository.CatalogoRepository
import com.kernel94.inventario123.data.repository.Resultado
import kotlinx.coroutines.launch

/**
 * Espeja la lógica de app/views/home/crear.php + ActivoGuardado (web):
 * los campos visibles/requeridos cambian según status y rol.
 *   · asignado          → "Asignado a" (usuario)
 *   · en_uso            → "Tienda en uso" + "¿Reemplaza a?" (y destino del que sale)
 *   · garantia / baja   → "ATI responsable"
 * Solo serie / código de barras / procedencia se limpian entre un registro y el
 * siguiente; el resto de la configuración se conserva para agilizar altas en serie.
 */
class CrearEditarActivoViewModel(
    private val activoRepository: ActivoRepository,
    private val catalogoRepository: CatalogoRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var perfil by mutableStateOf<Perfil?>(null); private set
    var catalogos by mutableStateOf(Catalogos()); private set

    // Campos del formulario (persisten entre registros, excepto los marcados abajo)
    var serie by mutableStateOf("")            // se limpia después de guardar
    var codigoBarras by mutableStateOf("")     // se limpia después de guardar
    var numActivo by mutableStateOf("")
    var motivo by mutableStateOf("")           // motivo del movimiento -> movimiento.nota; se limpia después de guardar
    var procedenciaTiendaId by mutableStateOf<Int?>(null) // se limpia después de guardar

    var negocioId by mutableStateOf<Int?>(null)
    var plazaId by mutableStateOf<Int?>(null)
    var dispositivoId by mutableStateOf<Int?>(null)
    var modeloId by mutableStateOf<Int?>(null)
    var status by mutableStateOf("en_bodega")
    var asignadoUsuarioId by mutableStateOf<Int?>(null)
    var tiendaUsoId by mutableStateOf<Int?>(null)
    var stockDestino by mutableStateOf<String?>(null)

    // garantía / baja
    var atiUsuarioId by mutableStateOf<Int?>(null)
    // reemplazo (status = en_uso)
    var reemplazaActivoId by mutableStateOf<Int?>(null)
    var salidaDestino by mutableStateOf("asignado")
    var salidaUsuarioId by mutableStateOf<Int?>(null)
    var salidaAtiUsuarioId by mutableStateOf<Int?>(null)
    // serie / código de barras del activo que sale — prellenados, editables para corregir
    var salidaSerie by mutableStateOf("")
    var salidaCodigoBarras by mutableStateOf("")
    // reemplazar equipo de otra categoría (no solo el mismo dispositivo)
    var reemplazoOtraCategoria by mutableStateOf(false)

    // Fotos (migración 007). *Actual = nombre de archivo ya en el servidor (edición).
    // *Uri = nueva foto elegida en este formulario (cámara/galería), aún no subida.
    var fotoEquipoActual by mutableStateOf<String?>(null); private set
    var fotoSerieActual by mutableStateOf<String?>(null); private set
    var fotoActivoActual by mutableStateOf<String?>(null); private set
    var fotoEquipoUri by mutableStateOf<Uri?>(null)
    var fotoSerieUri by mutableStateOf<Uri?>(null)
    var fotoActivoUri by mutableStateOf<Uri?>(null)

    var modelosFiltrados by mutableStateOf<List<Modelo>>(emptyList()); private set
    var plazasFiltradas by mutableStateOf<List<Plaza>>(emptyList()); private set
    var usuariosAsignables by mutableStateOf<List<Usuario>>(emptyList()); private set
    var atisPlaza by mutableStateOf<List<Usuario>>(emptyList()); private set
    var reemplazosDisponibles by mutableStateOf<List<Activo>>(emptyList()); private set

    var idEdicion by mutableStateOf<Int?>(null); private set
    var cargando by mutableStateOf(false); private set
    var guardando by mutableStateOf(false); private set
    var mensaje by mutableStateOf<String?>(null); private set
    var esError by mutableStateOf(false); private set

    fun iniciar(idActivoAEditar: Int? = null) {
        viewModelScope.launch {
            perfil = authRepository.obtenerPerfil()
            when (val r = catalogoRepository.obtenerCatalogos()) {
                is Resultado.Exito -> catalogos = r.datos
                is Resultado.Error -> {}
            }

            if (idActivoAEditar != null) {
                idEdicion = idActivoAEditar
                cargando = true
                when (val r = activoRepository.obtener(idActivoAEditar)) {
                    is Resultado.Exito -> {
                        val a = r.datos
                        serie = a.serie ?: ""
                        codigoBarras = a.codigoBarras ?: ""
                        numActivo = a.numActivo ?: ""
                        fotoEquipoActual = a.foto_equipo
                        fotoSerieActual = a.foto_serie
                        fotoActivoActual = a.foto_activo
                        procedenciaTiendaId = a.procedencia_tienda_id
                        plazaId = a.plaza_id
                        // Buscar el negocio_id a través de la plaza
                        negocioId = catalogos.plazas.find { it.id == a.plaza_id }?.negocio_id
                        dispositivoId = a.dispositivo_id
                        modeloId = a.modelo_id
                        status = a.status
                        tiendaUsoId = a.tienda_uso_id
                        asignadoUsuarioId = if (a.stock_tipo == "usuario") a.usuario_stock_id else null
                    }
                    is Resultado.Error -> { mensaje = r.mensaje; esError = true }
                }
                cargando = false
            } else {
                // Pre-cargar desde el perfil del usuario para nuevos activos
                perfil?.usuario?.plaza_id?.let { userPlazaId ->
                    val plaza = catalogos.plazas.find { it.id == userPlazaId }
                    if (plaza != null) {
                        negocioId = plaza.negocio_id
                        plazaId = plaza.id
                    }
                }
            }

            aplicarCascadaNegocio()
            aplicarCascadaDispositivo()
            actualizarUsuariosAsignables()
            cargarAtis()
            cargarReemplazos()
        }
    }

    fun onNegocioChange(id: Int?) {
        negocioId = id
        plazaId = null
        aplicarCascadaNegocio()
    }

    private fun aplicarCascadaNegocio() {
        plazasFiltradas = if (negocioId != null) {
            catalogos.plazas.filter { it.negocio_id == negocioId }
        } else catalogos.plazas
        if (plazaId == null && plazasFiltradas.size == 1) plazaId = plazasFiltradas[0].id
        actualizarUsuariosAsignables()
        cargarAtis()
    }

    fun onPlazaChange(id: Int?) {
        plazaId = id
        actualizarUsuariosAsignables()
        cargarAtis()
        cargarReemplazos()
    }

    fun onDispositivoChange(id: Int?) {
        dispositivoId = id
        modeloId = null
        aplicarCascadaDispositivo()
        cargarReemplazos()
    }

    fun onStatusChange(nuevo: String) {
        status = nuevo
        if (nuevo != "en_uso") {
            reemplazaActivoId = null
            reemplazosDisponibles = emptyList()
        } else {
            cargarReemplazos()
        }
    }

    fun onTiendaUsoChange(id: Int?) {
        tiendaUsoId = id
        cargarReemplazos()
    }

    /** Al elegir el activo que sale, prellena su serie / código de barras (editables). */
    fun onReemplazaChange(id: Int?) {
        reemplazaActivoId = id
        val a = reemplazosDisponibles.find { it.id == id }
        salidaSerie = a?.serie ?: ""
        salidaCodigoBarras = a?.codigoBarras ?: ""
    }

    private fun aplicarCascadaDispositivo() {
        modelosFiltrados = if (dispositivoId != null) {
            catalogos.modelos.filter { it.dispositivo_id == dispositivoId }
        } else catalogos.modelos
    }

    private fun actualizarUsuariosAsignables() {
        usuariosAsignables = if (plazaId != null) {
            catalogos.usuarios.filter { it.plaza_id == plazaId || it.tipo == "admin" }
        } else {
            catalogos.usuarios
        }
    }

    private fun cargarAtis() {
        val pid = plazaId ?: return
        viewModelScope.launch {
            atisPlaza = catalogoRepository.atisPorPlaza(pid)
        }
    }

    /** Al marcar "otra categoría" el selector "¿Reemplaza a?" lista todo el
     *  equipo en_uso de la tienda, no solo el de la misma categoría. */
    fun onReemplazoOtraCategoriaChange(v: Boolean) {
        reemplazoOtraCategoria = v
        cargarReemplazos()
    }

    private fun cargarReemplazos() {
        val tId = tiendaUsoId
        val dId = dispositivoId
        if (status != "en_uso" || tId == null || dId == null) {
            reemplazosDisponibles = emptyList()
            return
        }
        viewModelScope.launch {
            reemplazosDisponibles = activoRepository.activosEnTiendaPorDispositivo(
                tId, if (reemplazoOtraCategoria) null else dId, idEdicion,
            )
        }
    }

    /** Campos visibles según status + rol, mismo criterio que manejarEstatus() en crear.php */
    fun requiereAsignadoUsuario(): Boolean = status == "asignado"
    fun requiereTiendaUso(): Boolean = status == "en_uso"
    fun requiereAti(): Boolean = status == "garantia" || status == "baja"
    fun permiteElegirOtroUsuario(): Boolean {
        val tipo = perfil?.permisos?.tipo ?: return false
        return tipo == "admin" || tipo == "coordinador" || tipo == "ati"
    }

    fun guardar(context: Context, onExito: () -> Unit) {
        if (serie.isBlank()) {
            mensaje = "La serie es obligatoria."; esError = true; return
        }
        guardando = true
        mensaje = null
        val hayReemplazo = status == "en_uso" && reemplazaActivoId != null
        viewModelScope.launch {
            val resultado = if (idEdicion == null) {
                activoRepository.crear(
                    context = context,
                    serie = serie.trim(), codigoBarras = codigoBarras.ifBlank { null },
                    numActivo = numActivo.ifBlank { null }, modeloId = modeloId,
                    status = status, negocioId = negocioId, plazaId = plazaId,
                    procedenciaTiendaId = procedenciaTiendaId, tiendaUsoId = tiendaUsoId,
                    asignadoUsuarioId = asignadoUsuarioId, stockDestino = stockDestino,
                    atiUsuarioId = if (requiereAti()) atiUsuarioId else null,
                    reemplazaActivoId = if (hayReemplazo) reemplazaActivoId else null,
                    salidaDestino = if (hayReemplazo) salidaDestino else null,
                    salidaUsuarioId = if (hayReemplazo) salidaUsuarioId else null,
                    salidaAtiUsuarioId = if (hayReemplazo) salidaAtiUsuarioId else null,
                    salidaSerie = if (hayReemplazo) salidaSerie.trim().ifBlank { null } else null,
                    salidaCodigoBarras = if (hayReemplazo) salidaCodigoBarras.trim().ifBlank { null } else null,
                    motivo = motivo.trim().ifBlank { null },
                    fotoEquipoUri = fotoEquipoUri, fotoSerieUri = fotoSerieUri, fotoActivoUri = fotoActivoUri,
                )
            } else {
                activoRepository.actualizar(
                    context = context,
                    id = idEdicion!!, serie = serie.trim(), codigoBarras = codigoBarras.ifBlank { null },
                    numActivo = numActivo.ifBlank { null }, modeloId = modeloId, status = status,
                    procedenciaTiendaId = procedenciaTiendaId, tiendaUsoId = tiendaUsoId,
                    asignadoUsuarioId = asignadoUsuarioId,
                    atiUsuarioId = if (requiereAti()) atiUsuarioId else null,
                    reemplazaActivoId = if (hayReemplazo) reemplazaActivoId else null,
                    salidaDestino = if (hayReemplazo) salidaDestino else null,
                    salidaUsuarioId = if (hayReemplazo) salidaUsuarioId else null,
                    salidaAtiUsuarioId = if (hayReemplazo) salidaAtiUsuarioId else null,
                    salidaSerie = if (hayReemplazo) salidaSerie.trim().ifBlank { null } else null,
                    salidaCodigoBarras = if (hayReemplazo) salidaCodigoBarras.trim().ifBlank { null } else null,
                    motivo = motivo.trim().ifBlank { null },
                    fotoEquipoUri = fotoEquipoUri, fotoSerieUri = fotoSerieUri, fotoActivoUri = fotoActivoUri,
                )
            }
            when (resultado) {
                is Resultado.Exito -> {
                    guardando = false
                    esError = false
                    mensaje = resultado.datos.message ?: "Guardado correctamente."
                    if (idEdicion == null) {
                        // Solo se limpian estos campos; todo lo demás se conserva
                        serie = ""; codigoBarras = ""; procedenciaTiendaId = null
                        reemplazaActivoId = null; motivo = ""; salidaSerie = ""; salidaCodigoBarras = ""; reemplazoOtraCategoria = false
                        fotoEquipoUri = null; fotoSerieUri = null; fotoActivoUri = null
                        onExito()
                    } else {
                        onExito()
                    }
                }
                is Resultado.Error -> {
                    guardando = false
                    esError = true
                    mensaje = resultado.mensaje
                }
            }
        }
    }

    fun limpiarMensaje() { mensaje = null }

    /** Nombre del dispositivo actualmente seleccionado (ej. "UPS", "REGULADOR"),
     *  usado para decidir el modo del escáner de serie sin depender de IDs
     *  hardcodeados que podrían cambiar entre entornos/plazas. */
    fun nombreDispositivoSeleccionado(): String? =
        catalogos.dispositivos.find { it.id == dispositivoId }?.nombre
}

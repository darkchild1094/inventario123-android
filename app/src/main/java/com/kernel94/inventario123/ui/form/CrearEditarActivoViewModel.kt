package com.kernel94.inventario123.ui.form

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
 * Espeja la lógica de app/views/home/crear.php + HomeController::guardar():
 * los campos visibles/requeridos cambian según status y rol, y solo serie/
 * placa/procedencia se limpian entre un registro y el siguiente (el resto
 * de la configuración -negocio, plaza, dispositivo, modelo, estatus,
 * asignación- se conserva para agilizar el registro de varios activos
 * seguidos con la misma configuración).
 */
class CrearEditarActivoViewModel(
    private val activoRepository: ActivoRepository,
    private val catalogoRepository: CatalogoRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var perfil by mutableStateOf<Perfil?>(null); private set
    var catalogos by mutableStateOf(Catalogos()); private set

    // Campos del formulario (persisten entre registros, excepto los marcados abajo)
    var serie by mutableStateOf("")       // se limpia después de guardar
    var placa by mutableStateOf("")       // se limpia después de guardar
    var procedenciaTiendaId by mutableStateOf<Int?>(null) // se limpia después de guardar

    var negocioId by mutableStateOf<Int?>(null)
    var plazaId by mutableStateOf<Int?>(null)
    var dispositivoId by mutableStateOf<Int?>(null)
    var modeloId by mutableStateOf<Int?>(null)
    var status by mutableStateOf("en_bodega")
    var asignadoUsuarioId by mutableStateOf<Int?>(null)
    var tiendaUsoId by mutableStateOf<Int?>(null)
    var stockDestino by mutableStateOf<String?>(null)

    var modelosFiltrados by mutableStateOf<List<Modelo>>(emptyList()); private set
    var plazasFiltradas by mutableStateOf<List<Plaza>>(emptyList()); private set
    var usuariosAsignables by mutableStateOf<List<Usuario>>(emptyList()); private set

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
                        placa = a.placa ?: ""
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
    }

    fun onPlazaChange(id: Int?) {
        plazaId = id
        actualizarUsuariosAsignables()
    }

    fun onDispositivoChange(id: Int?) {
        dispositivoId = id
        modeloId = null
        aplicarCascadaDispositivo()
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

    /** Campos visibles según status + rol, mismo criterio que manejarEstatus() en crear.php */
    fun requiereAsignadoUsuario(): Boolean = status == "asignado"
    fun requiereTiendaUso(): Boolean = status == "en_uso"
    fun permiteElegirOtroUsuario(): Boolean {
        val tipo = perfil?.permisos?.tipo ?: return false
        return tipo == "admin" || tipo == "coordinador" || tipo == "ati"
    }

    fun guardar(onExito: () -> Unit) {
        if (serie.isBlank()) {
            mensaje = "La serie es obligatoria."; esError = true; return
        }
        guardando = true
        mensaje = null
        viewModelScope.launch {
            val resultado = if (idEdicion == null) {
                activoRepository.crear(
                    serie = serie.trim(), placa = placa.ifBlank { null }, modeloId = modeloId,
                    status = status, negocioId = negocioId, plazaId = plazaId,
                    procedenciaTiendaId = procedenciaTiendaId, tiendaUsoId = tiendaUsoId,
                    asignadoUsuarioId = asignadoUsuarioId, stockDestino = stockDestino,
                )
            } else {
                activoRepository.actualizar(
                    id = idEdicion!!, serie = serie.trim(), placa = placa.ifBlank { null },
                    modeloId = modeloId, status = status, procedenciaTiendaId = procedenciaTiendaId,
                    tiendaUsoId = tiendaUsoId, asignadoUsuarioId = asignadoUsuarioId,
                )
            }
            when (resultado) {
                is Resultado.Exito -> {
                    guardando = false
                    esError = false
                    mensaje = resultado.datos.message ?: "Guardado correctamente."
                    if (idEdicion == null) {
                        // Solo se limpian estos 3 campos; todo lo demás se conserva
                        serie = ""; placa = ""; procedenciaTiendaId = null
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
}

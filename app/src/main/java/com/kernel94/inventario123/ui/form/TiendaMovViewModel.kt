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
import com.kernel94.inventario123.data.repository.PendientesRepository
import com.kernel94.inventario123.data.repository.Resultado
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Form "Movimiento en tienda" (módulo Tiendas). Tres modos:
 *  - instalacion: si la serie está en mi stock personal -> mover ese activo a la
 *    tienda (actualizar en_uso). Si no existe -> alta nueva (pide dispositivo/modelo).
 *  - retiro: identifica el activo en_uso de la tienda por serie/CB -> pasa a mi
 *    stock (actualizar asignado a mí). Sin firma (excepción del backend).
 *  - reemplazo: alta en_uso del que entra + salida del que sale a mi stock.
 */
enum class ModoMov { INSTALACION, RETIRO, REEMPLAZO }

class TiendaMovViewModel(
    private val activoRepository: ActivoRepository,
    private val catalogoRepository: CatalogoRepository,
    private val authRepository: AuthRepository,
    private val pendientesRepository: PendientesRepository,
) : ViewModel() {

    var perfil by mutableStateOf<Perfil?>(null); private set
    var catalogos by mutableStateOf(Catalogos()); private set
    var tiendas by mutableStateOf<List<Tienda>>(emptyList()); private set

    var modo by mutableStateOf(ModoMov.INSTALACION)
    var tiendaId by mutableStateOf<Int?>(null)
    var tiendaFija by mutableStateOf(false); private set

    var serie by mutableStateOf("")
    var codigoBarras by mutableStateOf("")
    var motivo by mutableStateOf("")
    var fotoEquipoUri by mutableStateOf<Uri?>(null)   // equipo instalado / entrante
    var fotoSalidaUri by mutableStateOf<Uri?>(null)   // equipo retirado (modo reemplazo)

    // Sólo cuando la instalación/reemplazo es un alta nueva.
    var dispositivoId by mutableStateOf<Int?>(null)
    var modeloId by mutableStateOf<Int?>(null)
    val modelosFiltrados: List<Modelo>
        get() = dispositivoId?.let { d -> catalogos.modelos.filter { it.dispositivo_id == d } } ?: catalogos.modelos

    // Equipo que sale (modo reemplazo).
    var salidaSerie by mutableStateOf("")
    var salidaCodigoBarras by mutableStateOf("")

    // Estado del lookup de la serie.
    var lookup by mutableStateOf<ResolverSerieResponse?>(null); private set
    var lookupSalida by mutableStateOf<ResolverSerieResponse?>(null); private set
    var necesitaAltaNueva by mutableStateOf(true); private set  // instalación: false si la serie está en mi stock

    var cargando by mutableStateOf(false); private set
    var guardando by mutableStateOf(false); private set
    var mensaje by mutableStateOf<String?>(null); private set
    var esError by mutableStateOf(false); private set

    val online get() = pendientesRepository.online
    private val miId get() = perfil?.usuario?.id ?: 0

    private var jSerie: Job? = null
    private var jSalida: Job? = null

    fun iniciar(tiendaFijaId: Int?) {
        viewModelScope.launch {
            cargando = true
            perfil = authRepository.obtenerPerfil()
            when (val r = catalogoRepository.obtenerCatalogos()) {
                is Resultado.Exito -> catalogos = r.datos
                is Resultado.Error -> {}
            }
            val misPlazas = perfil?.permisos?.plazasIds?.toSet().orEmpty() +
                listOfNotNull(perfil?.permisos?.plazaId?.takeIf { it > 0 })
            tiendas = if (perfil?.permisos?.tipo == "admin" || misPlazas.isEmpty()) catalogos.tiendas
                      else catalogos.tiendas.filter { it.plaza_id in misPlazas }
            if (tiendaFijaId != null && tiendaFijaId > 0) {
                tiendaId = tiendaFijaId
                tiendaFija = true
            }
            cargando = false
        }
    }

    fun onModoChange(m: ModoMov) {
        modo = m
        lookup = null; lookupSalida = null
        necesitaAltaNueva = m != ModoMov.RETIRO
        verificarSerie()
    }

    fun onSerieChange(v: String) { serie = v; verificarSerie() }
    fun onSalidaSerieChange(v: String) { salidaSerie = v; verificarSalida() }

    private fun verificarSerie() {
        jSerie?.cancel()
        val q = serie.trim()
        if (q.length < 3) { lookup = null; necesitaAltaNueva = modo != ModoMov.RETIRO; return }
        jSerie = viewModelScope.launch {
            delay(350)
            when (val r = activoRepository.resolverSerie(q, tiendaId)) {
                is Resultado.Exito -> {
                    lookup = r.datos
                    necesitaAltaNueva = when (modo) {
                        ModoMov.RETIRO -> false
                        else -> !(r.datos.encontrado && r.datos.en_mi_stock)
                    }
                }
                is Resultado.Error -> {}
            }
        }
    }

    private fun verificarSalida() {
        jSalida?.cancel()
        val q = salidaSerie.trim()
        if (q.length < 3) { lookupSalida = null; return }
        jSalida = viewModelScope.launch {
            delay(350)
            when (val r = activoRepository.resolverSerie(q, tiendaId)) {
                is Resultado.Exito -> lookupSalida = r.datos
                is Resultado.Error -> {}
            }
        }
    }

    fun guardar(context: Context, onExito: () -> Unit) {
        val tId = tiendaId
        if (tId == null || tId <= 0) { mensaje = "Elige la tienda."; esError = true; return }
        if (serie.isBlank()) { mensaje = "La serie es obligatoria."; esError = true; return }
        guardando = true; mensaje = null

        viewModelScope.launch {
            val res: Resultado<*> = when (modo) {
                ModoMov.RETIRO -> {
                    val lk = lookup
                    val a = lk?.activo
                    if (lk == null || !lk.encontrado || !lk.en_esta_tienda || a == null) {
                        guardando = false; esError = true
                        mensaje = "Esa serie no está instalada en esta tienda."
                        return@launch
                    }
                    activoRepository.actualizar(
                        context = context, id = a.id, serie = a.serie ?: serie.trim(),
                        codigoBarras = a.codigoBarras, numActivo = a.numActivo, modeloId = a.modelo_id,
                        status = "asignado", procedenciaTiendaId = tId, tiendaUsoId = null,
                        asignadoUsuarioId = miId, atiUsuarioId = null,
                        reemplazaActivoId = null, salidaDestino = null, salidaUsuarioId = null,
                        salidaAtiUsuarioId = null, salidaSerie = null, salidaCodigoBarras = null,
                        motivo = motivo.trim().ifBlank { null },
                        fotoEquipoUri = fotoEquipoUri, fotoSerieUri = null, fotoActivoUri = null,
                    )
                }
                ModoMov.INSTALACION -> {
                    val lk = lookup
                    if (lk?.encontrado == true && lk.en_mi_stock && lk.activo != null) {
                        // mover mi activo a la tienda
                        val a = lk.activo!!
                        activoRepository.actualizar(
                            context = context, id = a.id, serie = a.serie ?: serie.trim(),
                            codigoBarras = a.codigoBarras, numActivo = a.numActivo, modeloId = a.modelo_id,
                            status = "en_uso", procedenciaTiendaId = a.procedencia_tienda_id, tiendaUsoId = tId,
                            asignadoUsuarioId = null, atiUsuarioId = null,
                            reemplazaActivoId = null, salidaDestino = null, salidaUsuarioId = null,
                            salidaAtiUsuarioId = null, salidaSerie = null, salidaCodigoBarras = null,
                            motivo = motivo.trim().ifBlank { null },
                            fotoEquipoUri = fotoEquipoUri, fotoSerieUri = null, fotoActivoUri = null,
                        )
                    } else {
                        // alta nueva en_uso (encolable offline)
                        val campos = activoRepository.camposTexto(
                            serie = serie.trim(), codigoBarras = codigoBarras.ifBlank { null },
                            numActivo = null, modeloId = modeloId, status = "en_uso",
                            negocioId = null, plazaId = null, procedenciaTiendaId = null,
                            tiendaUsoId = tId, asignadoUsuarioId = null, stockDestino = null,
                            atiUsuarioId = null, motivo = motivo.trim().ifBlank { null },
                        )
                        pendientesRepository.registrar(context, campos, fotoEquipoUri, null, null)
                    }
                }
                ModoMov.REEMPLAZO -> {
                    val sale = lookupSalida
                    val saleId = sale?.activo?.id
                    if (sale == null || !sale.encontrado || !sale.en_esta_tienda || saleId == null) {
                        guardando = false; esError = true
                        mensaje = "El equipo que sale no está instalado en esta tienda."
                        return@launch
                    }
                    // Online: lleva 2 fotos (instalado + retirado); no pasa por la cola.
                    activoRepository.crear(
                        context = context,
                        serie = serie.trim(), codigoBarras = codigoBarras.ifBlank { null },
                        numActivo = null, modeloId = modeloId, status = "en_uso",
                        negocioId = null, plazaId = null, procedenciaTiendaId = null, tiendaUsoId = tId,
                        asignadoUsuarioId = null, stockDestino = null, atiUsuarioId = null,
                        reemplazaActivoId = saleId, salidaDestino = "asignado", salidaUsuarioId = miId,
                        salidaSerie = salidaSerie.trim().ifBlank { null },
                        salidaCodigoBarras = salidaCodigoBarras.trim().ifBlank { null },
                        motivo = motivo.trim().ifBlank { null },
                        fotoEquipoUri = fotoEquipoUri, fotoEquipoSalidaUri = fotoSalidaUri,
                    )
                }
            }

            guardando = false
            when (res) {
                is Resultado.Exito<*> -> {
                    esError = false
                    mensaje = when (modo) {
                        ModoMov.RETIRO -> "Equipo retirado a tu stock."
                        ModoMov.REEMPLAZO -> "Reemplazo registrado."
                        ModoMov.INSTALACION -> "Equipo instalado en la tienda."
                    }
                    limpiar()
                    onExito()
                }
                is Resultado.Error -> { esError = true; mensaje = res.mensaje }
            }
        }
    }

    private fun limpiar() {
        serie = ""; codigoBarras = ""; motivo = ""; fotoEquipoUri = null; fotoSalidaUri = null
        salidaSerie = ""; salidaCodigoBarras = ""
        modeloId = null; lookup = null; lookupSalida = null
    }

    fun limpiarMensaje() { mensaje = null }
}

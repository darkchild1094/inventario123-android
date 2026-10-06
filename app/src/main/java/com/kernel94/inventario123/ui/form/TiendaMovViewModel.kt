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
import com.kernel94.inventario123.data.repository.RentecRepository
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
    private val rentecRepository: RentecRepository,
) : ViewModel() {

    var perfil by mutableStateOf<Perfil?>(null); private set
    var catalogos by mutableStateOf(Catalogos()); private set
    var hintsEscaner by mutableStateOf(HintsEscaner()); private set
    var tiendas by mutableStateOf<List<Tienda>>(emptyList()); private set

    var modo by mutableStateOf(ModoMov.INSTALACION)
    var tiendaId by mutableStateOf<Int?>(null)
    var tiendaFija by mutableStateOf(false); private set

    // Contexto RENTEC (instalación de equipo recibido para un proyecto de
    // renovación): fija motivo y etiqueta el activo/movimiento con el proyecto.
    var proyectoRentecId by mutableStateOf<Int?>(null); private set
    var proyectoRentecFolio by mutableStateOf<String?>(null); private set
    // Equipo ya recibido en bodega bajo este proyecto — para elegirlo en vez
    // de escribir la serie a mano (sigue siendo editable después de elegir).
    var activosRecibidosRentec by mutableStateOf<List<RentecRecibido>>(emptyList()); private set

    var serie by mutableStateOf("")
    var codigoBarras by mutableStateOf("")
    var motivo by mutableStateOf("")
    // Tres fotos por equipo (las mismas columnas que lleva cualquier activo):
    // el aparato, su etiqueta de serie y su código de barras. Un juego para el
    // equipo que ENTRA y otro para el que SALE.
    var fotoEquipoUri by mutableStateOf<Uri?>(null)
    var fotoSerieUri by mutableStateOf<Uri?>(null)
    var fotoActivoUri by mutableStateOf<Uri?>(null)

    var fotoSalidaUri by mutableStateOf<Uri?>(null)        // equipo retirado
    var fotoSalidaSerieUri by mutableStateOf<Uri?>(null)
    var fotoSalidaActivoUri by mutableStateOf<Uri?>(null)

    // Sólo cuando la instalación/reemplazo es un alta nueva.
    var dispositivoId by mutableStateOf<Int?>(null)
    var modeloId by mutableStateOf<Int?>(null)
    val modelosFiltrados: List<Modelo>
        get() = dispositivoId?.let { d -> catalogos.modelos.filter { it.dispositivo_id == d } } ?: catalogos.modelos

    // Equipo que sale (modo reemplazo).
    var salidaSerie by mutableStateOf("")
    var salidaCodigoBarras by mutableStateOf("")
    // N° de activo del que sale — no se edita a mano en el form (se llena
    // solo al elegirlo de la lista); no se muestra en ningún lado de la app,
    // pero viaja al backend y sale en el Excel de RENTEC.
    var salidaNumActivo by mutableStateOf<String?>(null); private set
    // Activos "en uso" en la tienda elegida, para elegir el que sale en vez
    // de escribirlo a mano (con fallback manual si no aparece en la lista).
    var activosEnTiendaSalida by mutableStateOf<List<Activo>>(emptyList()); private set

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

    private fun hintCodigoBarras(): HintCodigoBarras =
        hintsEscaner.por_dispositivo[dispositivoId?.toString()]?.codigo_barras ?: HintCodigoBarras()
    // El equipo que SALE puede ser de otro tipo de dispositivo que el que entra
    // (dispositivoId es del que entra) — validar contra su propio hint, no el
    // del entrante, para no rechazar/aceptar formatos del dispositivo equivocado.
    private fun hintCodigoBarrasSalida(): HintCodigoBarras {
        val dSalida = lookupSalida?.activo?.dispositivo_id
        return if (dSalida != null) hintsEscaner.por_dispositivo[dSalida.toString()]?.codigo_barras ?: HintCodigoBarras()
        else hintCodigoBarras()
    }
    private fun cbInvalido(v: String, hint: HintCodigoBarras = hintCodigoBarras()): Boolean {
        if (v.isBlank()) return false
        val regex = if (hint.solo_digitos) Regex("^\\d{${hint.longitud}}$") else Regex("^[A-Za-z0-9]{${hint.longitud}}$")
        return !regex.matches(v.trim())
    }
    /** Mensaje de validación acorde al hint del dispositivo elegido (por defecto 8 dígitos). */
    fun cbMensajeInvalido(hint: HintCodigoBarras = hintCodigoBarras()): String =
        "El código de barras debe ser ${hint.longitud} ${if (hint.solo_digitos) "dígitos numéricos" else "caracteres"}."

    private fun nombreDisp() = catalogos.dispositivos.find { it.id == dispositivoId }?.nombre?.uppercase() ?: ""
    private fun hintSerie(): HintSerie =
        hintsEscaner.por_dispositivo[dispositivoId?.toString()]?.serie ?: HintSerie()
    /** Prefijos (coma-separados) a priorizar en el lector de series. */
    fun prefijoEscanerSerie(): String? = hintSerie().prefijos.joinToString(",").ifBlank {
        if (nombreDisp().contains("UPS")) "3S,SM" else null
    }
    fun ocrEscanerSerie(): Boolean = hintSerie().modo_ocr ||
        (nombreDisp().contains("REGULADOR") && !nombreDisp().contains("UPS"))
    /** Código diminuto (ej. UPS): conviene arrancar con más zoom. */
    fun zoomAltoEscanerSerie(): Boolean = hintSerie().zoom_alto || nombreDisp().contains("UPS") || hintSerie().prefijos.isNotEmpty()
    fun cbLongitudEscaner(): Int = hintCodigoBarras().longitud
    fun cbSoloDigitosEscaner(): Boolean = hintCodigoBarras().solo_digitos

    private var jSerie: Job? = null
    private var jSalida: Job? = null

    fun iniciar(tiendaFijaId: Int?, proyectoRentecContexto: Int? = null, proyectoRentecFolioContexto: String? = null) {
        proyectoRentecId = proyectoRentecContexto
        proyectoRentecFolio = proyectoRentecFolioContexto
        if (proyectoRentecContexto != null) {
            modo = ModoMov.REEMPLAZO
            motivo = "Renovación tecnológica"
        }
        viewModelScope.launch {
            cargando = true
            perfil = authRepository.obtenerPerfil()
            when (val r = catalogoRepository.obtenerCatalogos()) {
                is Resultado.Exito -> catalogos = r.datos
                is Resultado.Error -> {}
            }
            hintsEscaner = catalogoRepository.obtenerHintsEscaner()
            val misPlazas = perfil?.permisos?.plazasIds?.toSet().orEmpty() +
                listOfNotNull(perfil?.permisos?.plazaId?.takeIf { it > 0 })
            tiendas = if (perfil?.permisos?.tipo == "admin" || misPlazas.isEmpty()) catalogos.tiendas
                      else catalogos.tiendas.filter { it.plaza_id in misPlazas }
            if (tiendaFijaId != null && tiendaFijaId > 0) {
                tiendaId = tiendaFijaId
                tiendaFija = true
            }
            if (proyectoRentecContexto != null) {
                when (val r = rentecRepository.detalle(proyectoRentecContexto)) {
                    is Resultado.Exito -> activosRecibidosRentec = r.datos.detalle_recibidos
                    is Resultado.Error -> {}
                }
            }
            if (tiendaId != null && modo == ModoMov.REEMPLAZO) cargarActivosEnTiendaSalida()
            cargando = false
        }
    }

    fun onTiendaChange(id: Int?) {
        tiendaId = id
        activosEnTiendaSalida = emptyList()
        if (id != null && modo == ModoMov.REEMPLAZO) cargarActivosEnTiendaSalida()
    }

    private fun cargarActivosEnTiendaSalida() {
        val tId = tiendaId ?: return
        viewModelScope.launch {
            activosEnTiendaSalida = activoRepository.activosEnTiendaPorDispositivo(tId, null, null)
        }
    }

    /** Elige el equipo recibido (bodega, este proyecto) como el que entra —
     *  sigue siendo editable después: solo prellena serie/código. */
    fun onSeleccionarRecibido(item: RentecRecibido) {
        codigoBarras = item.codigo_barras ?: ""
        onSerieChange(item.serie ?: item.codigo_barras ?: item.num_activo ?: "")
    }

    /** Elige de la lista el equipo que sale de la tienda — prellena serie/CB/
     *  N° de activo (este último nunca se edita a mano, solo viaja al guardar).
     *  Si después corrige la serie a mano, onSalidaSerieChange() limpia el N°. */
    fun onSeleccionarSalida(item: Activo) {
        salidaSerie = item.serie ?: item.codigoBarras ?: item.numActivo ?: ""
        salidaCodigoBarras = item.codigoBarras ?: ""
        salidaNumActivo = item.numActivo
        verificarSalida()
    }

    fun onModoChange(m: ModoMov) {
        modo = m
        lookup = null; lookupSalida = null
        necesitaAltaNueva = m != ModoMov.RETIRO
        if (m == ModoMov.REEMPLAZO && activosEnTiendaSalida.isEmpty()) cargarActivosEnTiendaSalida()
        verificarSerie()
    }

    fun onSerieChange(v: String) { serie = v; verificarSerie() }
    fun onSalidaSerieChange(v: String) {
        salidaSerie = v
        salidaNumActivo = null // tecleado a mano: ya no es el de la lista, no se adivina.
        verificarSalida()
    }

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
                        // En mi stock, o ya recibido en bodega (p.ej. bajo un proyecto
                        // RENTEC): en ambos casos se MUEVE el activo existente en vez
                        // de darlo de alta otra vez.
                        else -> !(r.datos.encontrado && (r.datos.en_mi_stock || r.datos.en_bodega))
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
        if (cbInvalido(codigoBarras)) {
            mensaje = cbMensajeInvalido(); esError = true; return
        }
        if (modo == ModoMov.REEMPLAZO && cbInvalido(salidaCodigoBarras, hintCodigoBarrasSalida())) {
            mensaje = cbMensajeInvalido(hintCodigoBarrasSalida()); esError = true; return
        }
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
                        fotoEquipoUri = fotoEquipoUri, fotoSerieUri = fotoSerieUri, fotoActivoUri = fotoActivoUri,
                    )
                }
                ModoMov.INSTALACION -> {
                    val lk = lookup
                    val yaExiste = lk?.encontrado == true && (lk.en_mi_stock || lk.en_bodega) && lk.activo != null
                    if (yaExiste) {
                        // mover el activo existente (mi stock, o ya recibido en bodega) a la tienda
                        val a = lk!!.activo!!
                        activoRepository.actualizar(
                            context = context, id = a.id, serie = a.serie ?: serie.trim(),
                            codigoBarras = a.codigoBarras ?: codigoBarras.ifBlank { null }, numActivo = a.numActivo, modeloId = a.modelo_id,
                            status = "en_uso", procedenciaTiendaId = a.procedencia_tienda_id, tiendaUsoId = tId,
                            asignadoUsuarioId = null, atiUsuarioId = null,
                            reemplazaActivoId = null, salidaDestino = null, salidaUsuarioId = null,
                            salidaAtiUsuarioId = null, salidaSerie = null, salidaCodigoBarras = null,
                            motivo = motivo.trim().ifBlank { null },
                            fotoEquipoUri = fotoEquipoUri, fotoSerieUri = fotoSerieUri, fotoActivoUri = fotoActivoUri,
                            proyectoRentecId = proyectoRentecId ?: lk.proyecto_rentec_id,
                        )
                    } else {
                        // alta nueva en_uso (encolable offline)
                        val campos = activoRepository.camposTexto(
                            serie = serie.trim(), codigoBarras = codigoBarras.ifBlank { null },
                            numActivo = null, modeloId = modeloId, status = "en_uso",
                            negocioId = null, plazaId = null, procedenciaTiendaId = null,
                            tiendaUsoId = tId, asignadoUsuarioId = null, stockDestino = null,
                            atiUsuarioId = null, motivo = motivo.trim().ifBlank { null },
                            proyectoRentecId = proyectoRentecId,
                        )
                        pendientesRepository.registrar(context, campos, fotoEquipoUri, fotoSerieUri, fotoActivoUri)
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
                    val lk = lookup
                    val yaRecibido = lk?.encontrado == true && (lk.en_bodega || lk.en_mi_stock) && lk.activo != null
                    if (yaRecibido) {
                        // El equipo que entra ya existía (en bodega, p.ej. fase 1 de
                        // RENTEC, o en mi stock personal): se MUEVE en vez de
                        // duplicarlo. actualizar() dispara el mismo procesarReemplazo()
                        // que crear() para el que sale.
                        val a = lk!!.activo!!
                        activoRepository.actualizar(
                            context = context, id = a.id, serie = a.serie ?: serie.trim(),
                            codigoBarras = a.codigoBarras ?: codigoBarras.ifBlank { null }, numActivo = a.numActivo,
                            modeloId = a.modelo_id, status = "en_uso",
                            procedenciaTiendaId = a.procedencia_tienda_id, tiendaUsoId = tId,
                            asignadoUsuarioId = null, atiUsuarioId = null,
                            reemplazaActivoId = saleId, salidaDestino = "asignado", salidaUsuarioId = miId,
                            salidaSerie = salidaSerie.trim().ifBlank { null },
                            salidaCodigoBarras = salidaCodigoBarras.trim().ifBlank { null },
                            salidaNumActivo = salidaNumActivo,
                            motivo = motivo.trim().ifBlank { null },
                            fotoEquipoUri = fotoEquipoUri, fotoSerieUri = fotoSerieUri, fotoActivoUri = fotoActivoUri,
                            fotoEquipoSalidaUri = fotoSalidaUri, fotoSerieSalidaUri = fotoSalidaSerieUri,
                            fotoActivoSalidaUri = fotoSalidaActivoUri,
                            proyectoRentecId = proyectoRentecId ?: lk.proyecto_rentec_id,
                        )
                    } else {
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
                            salidaNumActivo = salidaNumActivo,
                            motivo = motivo.trim().ifBlank { null },
                            fotoEquipoUri = fotoEquipoUri, fotoSerieUri = fotoSerieUri, fotoActivoUri = fotoActivoUri,
                            fotoEquipoSalidaUri = fotoSalidaUri, fotoSerieSalidaUri = fotoSalidaSerieUri,
                            fotoActivoSalidaUri = fotoSalidaActivoUri,
                            proyectoRentecId = proyectoRentecId,
                        )
                    }
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
        serie = ""; codigoBarras = ""
        fotoEquipoUri = null; fotoSerieUri = null; fotoActivoUri = null
        fotoSalidaUri = null; fotoSalidaSerieUri = null; fotoSalidaActivoUri = null
        salidaSerie = ""; salidaCodigoBarras = ""; salidaNumActivo = null
        modeloId = null; lookup = null; lookupSalida = null
        // En un lote RENTEC el motivo se conserva entre instalaciones sucesivas.
        if (proyectoRentecId == null) motivo = ""
    }

    fun limpiarMensaje() { mensaje = null }
}

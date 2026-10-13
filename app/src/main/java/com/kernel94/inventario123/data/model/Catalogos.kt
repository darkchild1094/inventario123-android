package com.kernel94.inventario123.data.model

data class Usuario(
    val id: Int = 0, val nombre: String = "", val email: String? = null,
    val foto: String? = null, val plaza_id: Int? = null, val plaza_nombre: String? = null,
    val tipo: String = "pfs",
    /**
     * TODAS las plazas asignadas (usuario_plaza), no sólo la principal. El
     * formulario de edición debe premarcarlas desde aquí: el servidor borra y
     * reinserta la lista completa, así que guardar con una lista incompleta le
     * quita al usuario el resto de sus plazas sin avisar.
     */
    val plaza_ids: List<Int> = emptyList(),
    // Solo viene poblado por stockPfsUsuarios() (landing de "Stock PFS").
    val activos_count: Int = 0,
)

/** Etiqueta legible para un rol de usuario. */
fun rolLabel(tipo: String?): String = when (tipo?.lowercase()) {
    "admin"       -> "Admin"
    "coordinador" -> "Coordinador"
    "ati"         -> "ATI"
    "pfs", "fs"   -> "PFS"
    else          -> tipo?.uppercase() ?: "—"
}

data class CuentaGuardada(
    val id: Int,
    val nombre: String,
    val email: String,
    val foto: String?,
    val ultimoLogin: Long = System.currentTimeMillis()
)

data class Negocio(val id: Int = 0, val nombre: String = "")
data class Region(val id: Int = 0, val nombre: String = "", val negocio_id: Int? = null, val negocio_nombre: String? = null)
data class Plaza(
    val id: Int = 0, val cr_plaza: String? = null, val nombre: String = "",
    val region_id: Int? = null, val region_nombre: String? = null,
    val negocio_id: Int? = null, val negocio_nombre: String? = null,
)
data class Dispositivo(val id: Int = 0, val nombre: String = "")
data class Modelo(
    val id: Int = 0, val nombre: String = "", val dispositivo_id: Int? = null,
    val marca_id: Int? = null, val marca_nombre: String? = null,
    val dispositivo_nombre: String? = null, val activos_count: Int = 0,
)
data class Marca(val id: Int = 0, val nombre: String = "")
data class Tienda(
    val id: Int = 0, val nombre: String = "", val plaza_id: Int? = null,
    val cr_tienda: String? = null, val coordenadas: String? = null,
    val ati_usuario_id: Int? = null, val ati_nombre: String? = null,
    val plaza_nombre: String? = null,
    val ultimo_inv_fecha: String? = null,
    val ultimo_inv_encontrados: Int? = null,
    val ultimo_inv_no_encontrados: Int? = null,
)
data class Bodega(val id: Int = 0, val nombre: String = "", val usuario_id: Int? = null, val plazas_ids: String? = null)
data class StatusOpcion(val value: String = "", val label: String = "")

data class Catalogos(
    val dispositivos: List<Dispositivo> = emptyList(),
    val modelos: List<Modelo> = emptyList(),
    val tiendas: List<Tienda> = emptyList(),
    val plazas: List<Plaza> = emptyList(),
    val regiones: List<Region> = emptyList(),
    val negocios: List<Negocio> = emptyList(),
    val usuarios: List<Usuario> = emptyList(),
    val bodegas: List<Bodega> = emptyList(),
    val status_opts: List<StatusOpcion> = emptyList(),
)

data class Permisos(
    val tipo: String = "", val puedeVerTodasPlazas: Boolean = false,
    val puedeFiltrarPorPlaza: Boolean = false, val puedeCrearActivo: Boolean = false,
    val puedeEditarActivo: Boolean = false, val puedeGestionarUsuarios: Boolean = false,
    val puedeExportar: Boolean = false, val puedeVerBodega: Boolean = false,
    val puedeGestionarTiendas: Boolean = false,
    val puedeGestionarModelos: Boolean = false,
    // Recibir equipo nuevo bajo un folio RENTEC: solo coordinador y admin.
    // Instalar lo ya recibido lo hace cualquier rol del modulo.
    val puedeRecibirRentec: Boolean = false,
    // Enviar equipo (Bodega↔Mi stock↔usuario) — inmediato, sin aceptación.
    val puedeEnviar: Boolean = false,
    val plazaId: Int = 0, val plazasIds: List<Int> = emptyList(),
)

// ── Dashboard ───────────────────────────────────────────────────────────────

data class DashPorNombre(val nombre: String = "", val n: Int = 0)
data class DashMovimiento(
    val evento: String = "", val creado_en: String? = null,
    val equipo: String? = null, val serie: String? = null,
)
data class TecnicoResumen(
    val usuarios: Int = 0, val tiendas: Int = 0, val modelos: Int = 0, val bodegas: Int = 0,
    val solicitudes_por_estado: Map<String, Int> = emptyMap(),
    val activos_sin_modelo: Int = 0,
)
data class ResumenDashboard(
    val total: Int = 0,
    val por_status: Map<String, Int> = emptyMap(),
    val por_dispositivo: List<DashPorNombre> = emptyList(),
    val por_plaza: List<DashPorNombre> = emptyList(),
    val por_modulo: Map<String, Int> = emptyMap(),
    val traslados_pendientes: Int = 0,
    val movimientos: List<DashMovimiento> = emptyList(),
    val tecnico: TecnicoResumen? = null,
)

/** Última versión de APK subida por un admin — sección "Actualizar app" del dashboard. */
data class VersionApp(
    val hay_version: Boolean = false,
    val version_code: Int = 0,
    val version_name: String? = null,
    val notas: String? = null,
    val creado_en: String? = null,
    val url_descarga: String? = null,
)

// ── Navegación por módulos ──────────────────────────────────────────────────

data class Modulo(
    val clave: String = "",
    val etiqueta: String = "",
    val icono: String = "",
    val editable: Boolean = false,
)

data class Perfil(
    val usuario: Usuario? = null, val permisos: Permisos = Permisos(),
    val modulos: List<Modulo> = emptyList(),
)

// ── Consulta (módulo global de identificación de un equipo) ─────────────────

data class ConsultaUbicacion(
    val stock_tipo: String? = null,
    val tienda_stock: String? = null,
    val bodega: String? = null,
    val usuario: String? = null,
    val tienda_uso: String? = null,
    val procedencia: String? = null,
    val plaza_nombre: String? = null,
    val region_nombre: String? = null,
    val negocio_nombre: String? = null,
    val status: String? = null,
    val resumen: String? = null,
)
data class ConsultaCoincidencia(
    val id: Int = 0,
    val serie: String? = null,
    val codigo_barras: String? = null,
    val num_activo: String? = null,
    val dispositivo_nombre: String? = null,
    val modelo_nombre: String? = null,
    val marca_nombre: String? = null,
    val status: String? = null,
    val ubicacion_corta: String? = null,
)
data class ConsultaResponse(
    val encontrado: Boolean = false,
    val message: String? = null,
    val activo: Activo? = null,
    val ubicacion: ConsultaUbicacion? = null,
    val historial: List<Movimiento> = emptyList(),
    val coincidencias: List<ConsultaCoincidencia> = emptyList(),
)

// ── Módulo Tiendas ─────────────────────────────────────────────────────────

data class TiendaConteo(
    val id: Int = 0,
    val cr_tienda: String? = null,
    val nombre: String = "",
    val plaza_id: Int? = null,
    val plaza_nombre: String? = null,
    val region_nombre: String? = null,
    val negocio_nombre: String? = null,
    val ati_usuario_id: Int? = null,
    val ati_nombre: String? = null,
    val activos_count: Int = 0,
    val ultimo_inv_fecha: String? = null,
    val ultimo_inv_encontrados: Int? = null,
    val ultimo_inv_no_encontrados: Int? = null,
)
data class ListaTiendasResponse(
    val tiendas: List<TiendaConteo> = emptyList(),
    val puedeAsignarAti: Boolean = false,
)

// ── Pistas para el escáner de series (derivadas de las series ya registradas) ──

data class HintSerie(
    val prefijos: List<String> = emptyList(),
    val modo_ocr: Boolean = false,
    val zoom_alto: Boolean = false,
)
data class HintCodigoBarras(val longitud: Int = 8, val solo_digitos: Boolean = true)
data class HintDispositivo(
    val serie: HintSerie = HintSerie(),
    val codigo_barras: HintCodigoBarras = HintCodigoBarras(),
)
data class HintsEscaner(
    val por_dispositivo: Map<String, HintDispositivo> = emptyMap(),
)

/** Motivos de movimiento (select en los formularios). El texto se guarda tal cual en movimiento.nota. */
val MOTIVOS_MOVIMIENTO = listOf(
    "Renovación tecnológica", "Daño", "Garantía", "Alta", "Baja", "Traspaso",
)

data class ResolverSerieResponse(
    val encontrado: Boolean = false,
    val activo: Activo? = null,
    val en_mi_stock: Boolean = false,
    val en_esta_tienda: Boolean = false,
    // En bodega bajo un proyecto RENTEC: la instalación debe MOVER este
    // activo (actualizar) en vez de darlo de alta otra vez (evita duplicarlo).
    val en_bodega: Boolean = false,
    val proyecto_rentec_id: Int? = null,
    val ubicacion_corta: String? = null,
    val coincidencias: List<ConsultaCoincidencia> = emptyList(),
)

data class ApiResultado(
    val success: Boolean = false,
    val message: String? = null,
    val id: Int? = null,
    // El servidor detectó que el equipo YA estaba registrado y NO lo duplicó.
    // Responde 409 pidiendo confirmación: para mover el existente a la nueva
    // ubicación hay que reenviar el alta con mover_existente = true.
    val ya_existe: Boolean = false,
    val movido: Boolean = false,
    val coincidio_por: String? = null,
    val ubicacion: String? = null,
    val activo: Activo? = null,
)
data class LoginResponse(val success: Boolean = false, val message: String? = null, val usuario: Usuario? = null, val session_id: String? = null)
data class LoginRequest(val email: String, val password: String)

// ── Notificaciones + "Enviar" (reemplaza Transferencias/SolicitudTraslado) ──

data class NotifActivo(
    val id: Int = 0,
    val serie: String? = null,
    val codigo_barras: String? = null,
    val num_activo: String? = null,
    val modelo_nombre: String? = null,
    val marca_nombre: String? = null,
    val dispositivo_nombre: String? = null,
)

data class Notificacion(
    val id: Int = 0,
    val usuario_destino_id: Int = 0,
    val de_usuario_id: Int = 0,
    val de_nombre: String = "",
    val direccion: String = "", // 'a_bodega' | 'a_stock'
    val lugar: String = "",
    val grupo_id: String = "",
    val leida: Boolean = false,
    val creado_en: String = "",
    val activos: List<NotifActivo> = emptyList(),
)

/** Un aviso por destinatario — la app escribe este mismo nodo en Firebase tras un "Enviar" exitoso. */
data class AvisoEnvio(
    val usuario_destino_id: Int = 0,
    val notificacion_id: Int = 0,
    val datos: Map<String, @JvmSuppressWildcards Any?> = emptyMap(),
)

data class EnviarActivosResponse(
    val success: Boolean = false,
    val message: String? = null,
    val avisos: List<AvisoEnvio> = emptyList(),
)

// ── Inventario (checklist) de tienda — migración 036 ────────────────────────
// 3 estados visuales a partir de 2 campos: sin tocar (escaneado_en null) =
// pendiente/gris; encontrado=1 = verde; encontrado=0 con escaneado_en = rojo.

data class InventarioTiendaDetalle(
    val detalle_id: Int = 0,
    val activo_id: Int = 0,
    val encontrado: Int = 0,
    val escaneado_en: String? = null,
    val nota: String? = null,
    val serie: String? = null,
    val codigo_barras: String? = null,
    val num_activo: String? = null,
    val modelo_nombre: String? = null,
    val marca_nombre: String? = null,
    val dispositivo_nombre: String? = null,
) {
    val revisado: Boolean get() = escaneado_en != null
    val encontradoBool: Boolean get() = encontrado != 0
}

data class InventarioTiendaResponse(
    val id: Int = 0,
    val tienda_id: Int = 0,
    val estado: String = "abierto",
    val usuario_id: Int = 0,
    val total_esperado: Int = 0,
    val total_encontrado: Int = 0,
    val creado_en: String? = null,
    val cerrado_en: String? = null,
    val detalle: List<InventarioTiendaDetalle> = emptyList(),
) {
    val abierto: Boolean get() = estado == "abierto"
    val pendientes: Int get() = detalle.count { !it.revisado }
}

data class CerrarInventarioTiendaResponse(
    val success: Boolean = false,
    val resumen: ResumenInventarioTienda? = null,
)
data class ResumenInventarioTienda(val encontrados: Int = 0, val no_encontrados: Int = 0)

data class InventarioTiendaAbierto(
    val id: Int = 0, val tienda_id: Int = 0, val estado: String = "abierto",
    val usuario_id: Int = 0, val total_esperado: Int = 0, val total_encontrado: Int = 0,
    val creado_en: String? = null, val cerrado_en: String? = null,
)
data class InventarioTiendaAbiertoResponse(val abierto: InventarioTiendaAbierto? = null)
data class InventarioTiendaIniciarResponse(val success: Boolean = false, val inventario: InventarioTiendaResponse? = null)
data class InventarioTiendaEscanearResponse(val success: Boolean = false, val message: String? = null, val activo_id: Int? = null)

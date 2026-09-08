package com.kernel94.inventario123.data.model

data class Usuario(
    val id: Int = 0, val nombre: String = "", val email: String? = null,
    val foto: String? = null, val plaza_id: Int? = null, val plaza_nombre: String? = null,
    val tipo: String = "pfs",
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
)
data class Bodega(val id: Int = 0, val nombre: String = "", val usuario_id: Int? = null, val plazas_ids: String? = null)
data class Area(val id: Int = 0, val nombre: String = "")
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
    val areas: List<Area> = emptyList(),
    val status_opts: List<StatusOpcion> = emptyList(),
)

data class Permisos(
    val tipo: String = "", val puedeVerTodasPlazas: Boolean = false,
    val puedeFiltrarPorPlaza: Boolean = false, val puedeCrearActivo: Boolean = false,
    val puedeEditarActivo: Boolean = false, val puedeGestionarUsuarios: Boolean = false,
    val puedeExportar: Boolean = false, val puedeVerBodega: Boolean = false,
    val puedeVerHistorial: Boolean = false, val puedeGestionarTiendas: Boolean = false,
    val puedeGestionarModelos: Boolean = false,
    val puedeCrearSolicitudTraslado: Boolean = false,
    val puedeAprobarTraslados: Boolean = false,
    val puedeVerTraslados: Boolean = false,
    val plazaId: Int = 0, val plazasIds: List<Int> = emptyList(),
)

// ── Solicitudes de traslado a bodega (doble firma) ───────────────────────────

data class SolicitudActivo(
    val id: Int = 0,
    val serie: String? = null,
    val codigo_barras: String? = null,
    val num_activo: String? = null,
    val status: String? = null,
    val modelo_nombre: String? = null,
    val marca_nombre: String? = null,
    val dispositivo_nombre: String? = null,
)

data class SolicitudTraslado(
    val id: Int = 0,
    val estado: String = "pendiente",
    val destino: String = "en_bodega",           // asignado | en_bodega | baja | garantia
    val plaza_id: Int = 0,
    val plaza_nombre: String? = null,
    val bodega_nombre: String? = null,
    val origen_nombre: String? = null,
    val origen_tienda_nombre: String? = null,
    val origen_bodega_nombre: String? = null,
    val destino_usuario_nombre: String? = null,
    val solicitante_nombre: String? = null,
    val aprobador_nombre: String? = null,
    val aprobador2_nombre: String? = null,
    val firma_solicitante: String? = null,
    val firma_aprobador: String? = null,
    val firma_aprobador2: String? = null,
    val nota: String? = null,
    val motivo_rechazo: String? = null,
    val grupo_id: String? = null,
    val creado_en: String? = null,
    val resuelto_en: String? = null,
    val activos_count: Int = 0,
    val activos: List<SolicitudActivo> = emptyList(),
    val puedeFirmar: Boolean = false,
    val puedeCancelar: Boolean = false,
    val porFirmar: Boolean = false,
) {
    val destinoLabel: String get() = when (destino) {
        "asignado"  -> "Traspaso a otro ingeniero"
        "en_bodega" -> "Devolución a bodega"
        "baja"      -> "Baja"
        "garantia"  -> "Garantía"
        else        -> destino
    }
    val dobleFirma: Boolean get() = destino == "garantia"
    val origenLabel: String get() = origen_nombre
        ?: origen_tienda_nombre?.let { "Tienda $it" }
        ?: origen_bodega_nombre?.let { "Bodega $it" }
        ?: "—"
}

data class ListaSolicitudesResponse(
    val solicitudes: List<SolicitudTraslado> = emptyList(),
    val pendientes: Int = 0,
    val puedeAprobar: Boolean = false,
    val puedeCrear: Boolean = false,
)

data class ConteoPendientes(val pendientes: Int = 0)

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
    val vistasDisponibles: List<String> = emptyList(),
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
)
data class ListaTiendasResponse(
    val tiendas: List<TiendaConteo> = emptyList(),
    val puedeAsignarAti: Boolean = false,
)

data class ResolverSerieResponse(
    val encontrado: Boolean = false,
    val activo: Activo? = null,
    val en_mi_stock: Boolean = false,
    val en_esta_tienda: Boolean = false,
    val ubicacion_corta: String? = null,
    val coincidencias: List<ConsultaCoincidencia> = emptyList(),
)

data class ApiResultado(val success: Boolean = false, val message: String? = null, val id: Int? = null)
data class LoginResponse(val success: Boolean = false, val message: String? = null, val usuario: Usuario? = null, val session_id: String? = null)
data class LoginRequest(val email: String, val password: String)

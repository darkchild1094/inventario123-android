package com.kernel94.inventario123.data.model

data class Usuario(
    val id: Int = 0, val nombre: String = "", val email: String? = null,
    val foto: String? = null, val plaza_id: Int? = null, val plaza_nombre: String? = null,
    val tipo: String = "fs",
)

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
}

data class ListaSolicitudesResponse(
    val solicitudes: List<SolicitudTraslado> = emptyList(),
    val pendientes: Int = 0,
    val puedeAprobar: Boolean = false,
    val puedeCrear: Boolean = false,
)

data class ConteoPendientes(val pendientes: Int = 0)

data class Perfil(
    val usuario: Usuario? = null, val permisos: Permisos = Permisos(),
    val vistasDisponibles: List<String> = emptyList(),
)

data class ApiResultado(val success: Boolean = false, val message: String? = null, val id: Int? = null)
data class LoginResponse(val success: Boolean = false, val message: String? = null, val usuario: Usuario? = null, val session_id: String? = null)
data class LoginRequest(val email: String, val password: String)

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
data class Modelo(val id: Int = 0, val nombre: String = "", val dispositivo_id: Int? = null)
data class Tienda(val id: Int = 0, val nombre: String = "", val plaza_id: Int? = null)
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
    val plazaId: Int = 0, val plazasIds: List<Int> = emptyList(),
)

data class Perfil(
    val usuario: Usuario? = null, val permisos: Permisos = Permisos(),
    val vistasDisponibles: List<String> = emptyList(),
)

data class ApiResultado(val success: Boolean = false, val message: String? = null, val id: Int? = null)
data class LoginResponse(val success: Boolean = false, val message: String? = null, val usuario: Usuario? = null, val session_id: String? = null)
data class LoginRequest(val email: String, val password: String)

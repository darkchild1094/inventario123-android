package com.kernel94.inventario123.data.model

/**
 * Proyecto de Renovación Tecnológica (RENTEC): agrupa bajo un folio los
 * activos recibidos en bodega y luego instalados en tienda. No es un flujo
 * nuevo — reutiliza alta normal (status=en_bodega) y el modo Reemplazo de
 * Tiendas, solo les añade este folio para poder listarlos/exportarlos.
 */
data class ProyectoRentec(
    val id: Int = 0,
    val folio: String = "",
    val nombre: String = "",
    val estado: String = "abierto",
    val usuario_id: Int = 0,
    val usuario_nombre: String? = null,
    val creado_en: String? = null,
    val cerrado_en: String? = null,
    val recibidos: Int = 0,
    val instalados: Int = 0,
    val detalle_recibidos: List<RentecRecibido> = emptyList(),
    val detalle_instalados: List<RentecInstalado> = emptyList(),
) {
    val abierto: Boolean get() = estado == "abierto"
}

/** Equipo ya recibido en bodega bajo el proyecto, pendiente de instalar. */
data class RentecRecibido(
    val activo_id: Int = 0,
    val serie: String? = null,
    val codigo_barras: String? = null,
    val num_activo: String? = null,
    val modelo_nombre: String? = null,
    val marca_nombre: String? = null,
    val dispositivo_nombre: String? = null,
)

/** Un equipo ya instalado en tienda bajo el proyecto (con o sin relacionado). */
data class RentecInstalado(
    val movimiento_id: Int = 0,
    val evento: String = "",
    val creado_en: String? = null,
    val tienda_id: Int? = null,
    val tienda_nombre: String? = null,
    val cr_tienda: String? = null,
    val activo_id: Int = 0,
    val activo_relacionado_id: Int? = null,
    val usuario_id: Int? = null,
    val usuario_nombre: String? = null,
    val serie_entra: String? = null,
    val codigo_entra: String? = null,
    val modelo_nombre: String? = null,
    val marca_nombre: String? = null,
    val dispositivo_nombre: String? = null,
    val serie_sale: String? = null,
    val codigo_sale: String? = null,
)

data class RentecCrearResponse(
    val success: Boolean = false,
    val message: String? = null,
    val id: Int? = null,
    val folio: String? = null,
)

data class RentecCerrarResponse(
    val success: Boolean = false,
    val message: String? = null,
    val proyecto: ProyectoRentec? = null,
)

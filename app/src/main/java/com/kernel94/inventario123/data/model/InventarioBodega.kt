package com.kernel94.inventario123.data.model

/** Auditoría física mensual: snapshot de activos + qué se escaneó. Objetivo es
 *  una bodega o el stock personal de un usuario (uno de los dos, no ambos). */
data class InventarioBodega(
    val id: Int = 0,
    val bodega_id: Int? = null,
    val stock_usuario_id: Int? = null,
    val periodo: String = "",
    val estado: String = "abierto",
    val usuario_id: Int = 0,
    val usuario_nombre: String? = null,
    val total_esperado: Int = 0,
    val total_encontrado: Int = 0,
    val creado_en: String? = null,
    val cerrado_en: String? = null,
    val detalle: List<InventarioBodegaDetalle> = emptyList(),
) {
    val abierto: Boolean get() = estado == "abierto"
}

data class InventarioBodegaDetalle(
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
    val encontradoBool: Boolean get() = encontrado != 0
}

data class InventarioBodegaResponse(
    val success: Boolean = false,
    val message: String? = null,
    val activo_id: Int? = null,
    val inventario: InventarioBodega? = null,
)

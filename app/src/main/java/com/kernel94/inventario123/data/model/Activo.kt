package com.kernel94.inventario123.data.model

data class Activo(
    val id: Int = 0,
    val serie: String? = null,
    val placa: String? = null,
    val modelo_id: Int? = null,
    val status: String = "en_bodega",
    val procedencia_tienda_id: Int? = null,
    val tienda_uso_id: Int? = null,
    val stock_id: Int? = null,
    val fecha_alta: String? = null,
    val fecha_modificacion: String? = null,
    val modelo_nombre: String? = null,
    val dispositivo_id: Int? = null,
    val dispositivo_nombre: String? = null,
    val area_nombre: String? = null,
    val stock_tipo: String? = null,
    val usuario_stock_id: Int? = null,
    val usuario_nombre: String? = null,
    val bodega_stock_id: Int? = null,
    val bodega_nombre: String? = null,
    val plaza_id: Int? = null,
    val plaza_nombre: String? = null,
    val region_nombre: String? = null,
    val negocio_nombre: String? = null,
    val tienda_uso_nombre: String? = null,
    val procedencia_nombre: String? = null,
    val puedeEditar: Boolean = false,
    val puedeEliminar: Boolean = false,
) {
    val statusLabel: String
        get() = when (status) {
            "en_bodega" -> "En Bodega"; "en_uso" -> "En Uso"; "baja" -> "Baja"
            "garantia" -> "Garantía"; "asignado" -> "Asignado"; else -> "Desconocido"
        }
    val asignadoOBodega: String
        get() = if (stock_tipo == "usuario") (usuario_nombre ?: "—") else (bodega_nombre ?: "—")
}

data class Paginacion(
    val pagina_actual: Int = 1, val total_paginas: Int = 1,
    val total_resultados: Int = 0, val por_pagina: Int = 20,
)

data class ListadoActivosResponse(
    val activos: List<Activo> = emptyList(),
    val paginacion: Paginacion = Paginacion(),
    val vista: String? = null,
)

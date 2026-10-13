package com.kernel94.inventario123.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caché local (offline-first) de lo que ya trae la API. El backend PHP sigue
 * siendo la fuente de autoridad — estas filas se pueblan/reemplazan desde
 * `data/sync/SyncManager.kt`, nunca se editan a mano. Los `id` son los
 * mismos enteros autoincrementales de MySQL (a diferencia de POS123, que
 * genera sus ids localmente y usa Firebase como base de datos; aquí Firebase
 * solo es el canal de "avisa ya", no guarda el dato de negocio).
 *
 * Reflejan 1:1 los modelos de `data/model/Catalogos.kt`/`Activo.kt` para que
 * convertir API↔Room sea directo — ver `*Dao.kt` los `fromApi()`/`toApi()`.
 */

@Entity(tableName = "activos_cache")
data class ActivoEntity(
    @PrimaryKey val id: Int,
    val serie: String? = null,
    val codigoBarras: String? = null,
    val numActivo: String? = null,
    val fotoEquipo: String? = null,
    val fotoSerie: String? = null,
    val fotoActivo: String? = null,
    val modeloId: Int? = null,
    val status: String = "en_bodega",
    val procedenciaTiendaId: Int? = null,
    val tiendaUsoId: Int? = null,
    val stockId: Int? = null,
    val proyectoRentecId: Int? = null,
    val fechaAlta: String? = null,
    val fechaModificacion: String? = null,
    val modeloNombre: String? = null,
    val marcaNombre: String? = null,
    val dispositivoId: Int? = null,
    val dispositivoNombre: String? = null,
    val stockTipo: String? = null,
    val usuarioStockId: Int? = null,
    val usuarioNombre: String? = null,
    val bodegaStockId: Int? = null,
    val bodegaNombre: String? = null,
    val tiendaStockId: Int? = null,
    val tiendaStockNombre: String? = null,
    val plazaId: Int? = null,
    val plazaNombre: String? = null,
    val regionNombre: String? = null,
    val negocioNombre: String? = null,
    val tiendaUsoNombre: String? = null,
    val procedenciaNombre: String? = null,
    val puedeEditar: Boolean = false,
    val puedeEliminar: Boolean = false,
    // Para qué "balde" local se guardó esta copia (p.ej. "bodega", "mi_stock",
    // "tienda:14", "stock_pfs:9") — permite que varias pantallas convivan en
    // la misma tabla sin pisarse: el refresco de una lista solo reemplaza las
    // filas de su propio balde (ver ActivoDao.reemplazarBalde).
    val balde: String,
    val actualizadoEn: Long = System.currentTimeMillis(),
)

@Entity(tableName = "notificaciones_cache")
data class NotificacionEntity(
    @PrimaryKey val id: Int,
    val usuarioDestinoId: Int,
    val deUsuarioId: Int,
    val deNombre: String,
    val direccion: String,
    val lugar: String,
    val grupoId: String,
    val leida: Boolean,
    val creadoEn: String,
    /** JSON de la lista de activos (tipo·modelo·serie) — se arma/lee en el repositorio, no aquí. */
    val activosJson: String,
)

@Entity(tableName = "tiendas_cache")
data class TiendaEntity(
    @PrimaryKey val id: Int,
    val crTienda: String,
    val nombre: String,
    val plazaId: Int,
    val plazaNombre: String? = null,
    val regionNombre: String? = null,
    val negocioNombre: String? = null,
    val atiUsuarioId: Int? = null,
    val atiNombre: String? = null,
    val activosCount: Int = 0,
    val ultimoInvFecha: String? = null,
    val ultimoInvEncontrados: Int? = null,
    val ultimoInvNoEncontrados: Int? = null,
)

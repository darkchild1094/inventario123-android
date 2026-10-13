package com.kernel94.inventario123.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivoDao {
    @Query("SELECT * FROM activos_cache WHERE balde = :balde ORDER BY id DESC")
    fun observarBalde(balde: String): Flow<List<ActivoEntity>>

    @Query("SELECT * FROM activos_cache WHERE id = :id LIMIT 1")
    suspend fun obtener(id: Int): ActivoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(activos: List<ActivoEntity>)

    @Query("DELETE FROM activos_cache WHERE balde = :balde")
    suspend fun borrarBalde(balde: String)

    /**
     * Reemplaza el contenido completo de un "balde" (p.ej. todo lo de
     * Bodega-Valles) por lo que acaba de traer el backend — borra+inserta en
     * una sola transacción para que ninguna pantalla vea el balde a medias.
     */
    @Transaction
    suspend fun reemplazarBalde(balde: String, activos: List<ActivoEntity>) {
        borrarBalde(balde)
        insertarTodos(activos)
    }
}

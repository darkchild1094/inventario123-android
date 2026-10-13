package com.kernel94.inventario123.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TiendaDao {
    @Query("SELECT * FROM tiendas_cache WHERE plazaId = :plazaId ORDER BY nombre")
    fun observarPorPlaza(plazaId: Int): Flow<List<TiendaEntity>>

    @Query("SELECT * FROM tiendas_cache ORDER BY nombre")
    fun observarTodas(): Flow<List<TiendaEntity>>

    @Query("SELECT * FROM tiendas_cache WHERE id = :id LIMIT 1")
    suspend fun obtener(id: Int): TiendaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(tiendas: List<TiendaEntity>)

    @Query("DELETE FROM tiendas_cache")
    suspend fun borrarTodas()

    @Transaction
    suspend fun reemplazarTodas(tiendas: List<TiendaEntity>) {
        borrarTodas()
        insertarTodas(tiendas)
    }
}

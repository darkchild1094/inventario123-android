package com.kernel94.inventario123.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacionDao {
    @Query("SELECT * FROM notificaciones_cache ORDER BY id DESC")
    fun observarTodas(): Flow<List<NotificacionEntity>>

    @Query("SELECT COUNT(*) FROM notificaciones_cache WHERE leida = 0")
    fun observarNoLeidas(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(notifs: List<NotificacionEntity>)

    @Query("DELETE FROM notificaciones_cache")
    suspend fun borrarTodas()

    @Query("UPDATE notificaciones_cache SET leida = 1 WHERE id = :id")
    suspend fun marcarLeidaLocal(id: Int)

    @Query("UPDATE notificaciones_cache SET leida = 1")
    suspend fun marcarTodasLeidasLocal()

    @Transaction
    suspend fun reemplazarTodas(notifs: List<NotificacionEntity>) {
        borrarTodas()
        insertarTodas(notifs)
    }
}

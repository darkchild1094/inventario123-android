package com.kernel94.inventario123.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ActivoEntity::class,
        NotificacionEntity::class,
        TiendaEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activoDao(): ActivoDao
    abstract fun notificacionDao(): NotificacionDao
    abstract fun tiendaDao(): TiendaDao

    companion object {
        @Volatile private var instancia: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inventario123.db",
                ).build().also { instancia = it }
            }
    }
}

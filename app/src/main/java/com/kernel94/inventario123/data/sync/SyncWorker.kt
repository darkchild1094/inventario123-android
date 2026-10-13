package com.kernel94.inventario123.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kernel94.inventario123.Inventario123App

/**
 * Respaldo periódico: refresca la caché local (Room) aunque el listener en
 * vivo de [SyncManager] no haya avisado (app cerrada del todo, el listener
 * de Firebase se perdió por cualquier motivo, etc.). El canal rápido sigue
 * siendo Firebase; esto es la red de seguridad, no el camino principal.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? Inventario123App ?: return Result.success()
        return try {
            app.notificacionRepository.sincronizar()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val NOMBRE_UNICO = "sync-general"
    }
}

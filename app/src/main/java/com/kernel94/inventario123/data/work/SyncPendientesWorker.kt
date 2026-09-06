package com.kernel94.inventario123.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kernel94.inventario123.Inventario123App

/**
 * Vacía la cola de altas pendientes aunque la app esté cerrada. Se programa como
 * trabajo periódico con restricción de red en Inventario123App; WorkManager lo
 * ejecuta cuando hay conexión. La idempotencia del servidor evita duplicados si
 * este worker y el reintento en primer plano coinciden.
 */
class SyncPendientesWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? Inventario123App ?: return Result.success()
        return try {
            app.pendientesRepository.sincronizar()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val NOMBRE_UNICO = "sync-pendientes"
    }
}

package com.kernel94.inventario123.data.repository

import android.content.Context
import com.kernel94.inventario123.data.remote.ApiService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportRepository(private val api: ApiService) {

    /**
     * Descarga el Excel del backend (mismo endpoint que usa la web,
     * ExportController::inventario(), ya con el fix de alcance por rol)
     * y lo guarda en cache/exportados/ para poder compartirlo via FileProvider.
     */
    suspend fun exportarInventario(context: Context): Resultado<File> {
        return try {
            val response = api.exportarInventario()
            if (!response.isSuccessful || response.body() == null) {
                return Resultado.Error(
                    if (response.code() == 403) "No tienes permiso para exportar."
                    else "No se pudo generar el archivo."
                )
            }

            val carpeta = File(context.cacheDir, "exportados").apply { mkdirs() }
            val nombreArchivo = "Inventario_" + SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale("es", "MX")).format(Date()) + ".xlsx"
            val archivo = File(carpeta, nombreArchivo)

            response.body()!!.byteStream().use { entrada ->
                archivo.outputStream().use { salida -> entrada.copyTo(salida) }
            }

            Resultado.Exito(archivo)
        } catch (e: Exception) {
            Resultado.Error("No se pudo conectar al servidor para exportar.")
        }
    }
}

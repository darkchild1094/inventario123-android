package com.kernel94.inventario123.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

/**
 * Convierte una foto elegida (cámara/galería) en la parte multipart que espera
 * ImageHelper (web).
 *
 * Antes se subían los bytes CRUDOS del archivo (3–5 MB por foto). Ahora se
 * comprime en el cliente: se decodifica con `inSampleSize` para no cargar el
 * bitmap completo en memoria, se escala al lado mayor <= `maxLado`, se corrige
 * la orientación EXIF y se re-encoda a JPEG de calidad `calidad`. Resultado
 * típico: ~200–500 KB, subida mucho más rápida. El servidor igual re-optimiza.
 */
object ImagenUtil {

    private const val MAX_LADO = 1600
    private const val CALIDAD = 80

    fun parte(context: Context, uri: Uri?, campo: String): MultipartBody.Part? =
        comprimir(context, uri, campo, MAX_LADO, CALIDAD)

    fun comprimir(
        context: Context,
        uri: Uri?,
        campo: String,
        maxLado: Int = MAX_LADO,
        calidad: Int = CALIDAD,
    ): MultipartBody.Part? {
        if (uri == null) return null
        return try {
            val jpeg = comprimirABytes(context, uri, maxLado, calidad)
                ?: return crudo(context, uri, campo)
            val body = jpeg.toRequestBody("image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData(campo, "$campo.jpg", body)
        } catch (e: Exception) {
            crudo(context, uri, campo)
        }
    }

    /** Comprime una imagen a un ByteArray JPEG. null si no se pudo decodificar. */
    fun comprimirABytes(context: Context, uri: Uri, maxLado: Int = MAX_LADO, calidad: Int = CALIDAD): ByteArray? {
        val cr = context.contentResolver

        // 1) medir sin cargar
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) } ?: return null
        val (w, h) = opciones.outWidth to opciones.outHeight
        if (w <= 0 || h <= 0) return null

        // 2) inSampleSize (potencia de 2) para acercarse a maxLado
        var sample = 1
        val mayor = maxOf(w, h)
        while (mayor / (sample * 2) >= maxLado) sample *= 2

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) } ?: return null

        // 3) escalar fino al lado mayor <= maxLado
        val lado = maxOf(bmp.width, bmp.height)
        if (lado > maxLado) {
            val escala = maxLado.toFloat() / lado
            val nb = Bitmap.createScaledBitmap(bmp, (bmp.width * escala).toInt().coerceAtLeast(1), (bmp.height * escala).toInt().coerceAtLeast(1), true)
            if (nb != bmp) bmp.recycle()
            bmp = nb
        }

        // 4) orientación EXIF
        bmp = corregirOrientacion(context, uri, bmp)

        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, calidad.coerceIn(1, 100), out)
        bmp.recycle()
        return out.toByteArray()
    }

    private fun corregirOrientacion(context: Context, uri: Uri, bmp: Bitmap): Bitmap {
        return try {
            val orientacion = context.contentResolver.openInputStream(uri)?.use { ins ->
                ExifInterface(ins).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            val m = Matrix()
            when (orientacion) {
                ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
                else -> return bmp
            }
            val rotado = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (rotado != bmp) bmp.recycle()
            rotado
        } catch (e: Exception) {
            bmp
        }
    }

    private fun crudo(context: Context, uri: Uri, campo: String): MultipartBody.Part? = try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = when (mime) {
            "image/png" -> "png"; "image/webp" -> "webp"; "image/gif" -> "gif"; else -> "jpg"
        }
        MultipartBody.Part.createFormData(campo, "$campo.$ext", bytes.toRequestBody(mime.toMediaTypeOrNull()))
    } catch (e: Exception) {
        null
    }

    /** Envuelve un ByteArray PNG/JPEG ya listo (p.ej. una firma) como parte multipart. */
    fun parteBytes(bytes: ByteArray, campo: String, nombre: String, mime: String = "image/png"): MultipartBody.Part =
        MultipartBody.Part.createFormData(campo, nombre, bytes.toRequestBody(mime.toMediaTypeOrNull()))

    fun texto(valor: String?): okhttp3.RequestBody? =
        valor?.toRequestBody("text/plain".toMediaTypeOrNull())
}

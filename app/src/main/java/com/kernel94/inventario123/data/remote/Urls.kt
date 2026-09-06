package com.kernel94.inventario123.data.remote

import com.kernel94.inventario123.BuildConfig

/** Arma URLs absolutas de archivos servidos por /uploads/ (fuera del API). */
object Urls {
    private val base get() = BuildConfig.BASE_URL.trimEnd('/')

    fun usuarioFoto(nombre: String): String = "$base/uploads/usuarios/$nombre"
    fun activoFoto(nombre: String): String = "$base/uploads/$nombre"
    fun activoFotoThumb(nombre: String): String = "$base/uploads/thumbs/$nombre"
    fun firma(nombre: String): String = "$base/uploads/firmas/$nombre"
}

package com.kernel94.inventario123.ui.consulta

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernel94.inventario123.data.model.ConsultaResponse
import com.kernel94.inventario123.data.repository.ActivoRepository
import com.kernel94.inventario123.data.repository.Resultado
import kotlinx.coroutines.launch

/** Módulo "Consulta": identifica un equipo por serie / código / N° de activo (global). */
class ConsultaViewModel(private val activoRepository: ActivoRepository) : ViewModel() {

    var texto by mutableStateOf("")
    var cargando by mutableStateOf(false); private set
    var resultado by mutableStateOf<ConsultaResponse?>(null); private set
    var error by mutableStateOf<String?>(null); private set

    fun consultar() {
        val q = texto.trim()
        if (q.isEmpty()) return
        cargando = true
        error = null
        resultado = null
        viewModelScope.launch {
            when (val r = activoRepository.consultar(q)) {
                is Resultado.Exito -> resultado = r.datos
                is Resultado.Error -> error = r.mensaje
            }
            cargando = false
        }
    }

    fun limpiar() {
        texto = ""; resultado = null; error = null
    }
}

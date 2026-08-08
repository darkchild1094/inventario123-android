package com.kernel94.inventario123.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta de colores extraída de las capturas del backend (Dark Theme / Professional)
val BsDark = Color(0xFF212529)       // Fondo de encabezados y botones principales
val BsPrimary = Color(0xFF0D6EFD)     // Azul de botones de acción y badges (Asignado)
val BsSecondary = Color(0xFF6C757D)   // Gris para textos secundarios y placeholders
val BsSuccess = Color(0xFF198754)     // Verde para éxitos
val BsDanger = Color(0xFFDC3545)      // Rojo para errores/bajas
val BsWarning = Color(0xFFFFC107)     // Amarillo para coordinadores
val BsLight = Color(0xFFF8F9FA)      // Blanco/Gris muy claro para fondos de tarjetas
val BsWhite = Color(0xFFFFFFFF)
val BsBorder = Color(0xFFDEE2E6)      // Color de bordes

// Colores de estatus específicos de las tarjetas
val StatusEnBodega = BsSecondary
val StatusEnUso = BsSuccess
val StatusBaja = BsDanger
val StatusGarantia = Color(0xFFB08800)
val StatusAsignado = BsPrimary

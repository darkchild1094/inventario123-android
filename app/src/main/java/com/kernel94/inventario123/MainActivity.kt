package com.kernel94.inventario123

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kernel94.inventario123.ui.navigation.Inventario123NavGraph
import com.kernel94.inventario123.ui.theme.Inventario123Theme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as Inventario123App

        setContent {
            Inventario123Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var listo by remember { mutableStateOf(false) }
                    var sesionActiva by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        sesionActiva = app.authRepository.haySesionActiva()
                        listo = true
                    }

                    if (listo) {
                        Inventario123NavGraph(app = app, sesionActivaInicial = sesionActiva)
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

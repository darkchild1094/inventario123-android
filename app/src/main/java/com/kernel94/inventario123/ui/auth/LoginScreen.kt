package com.kernel94.inventario123.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kernel94.inventario123.data.model.CuentaGuardada
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsPrimary
import com.kernel94.inventario123.ui.theme.BsWhite

@Composable
fun LoginScreen(viewModel: LoginViewModel, onLoginExitoso: () -> Unit) {
    val cuentas by viewModel.cuentasGuardadas.collectAsState(initial = emptyList())
    val fondoOscuro = Color(0xFF1B1E21)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fondoOscuro),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (cuentas.isNotEmpty()) {
                Text(
                    "Cuentas recientes",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(cuentas) { cuenta ->
                        ItemCuentaGuardada(cuenta, onClick = { 
                            viewModel.email = it.email
                            viewModel.password = ""
                        }, onRemove = { viewModel.eliminarCuenta(it.email) })
                    }
                }
            }

            // Contenedor principal estilo Backend
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BsWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Encabezado Oscuro
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BsDark)
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Logo Institucional Inventario123
                            AsyncImage(
                                model = "file:///android_asset/logo_login.png",
                                contentDescription = "Logo Inventario123",
                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Inventario123",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Sistema de Gestión de Activos",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Formulario
                    Column(modifier = Modifier.padding(24.dp)) {
                        OutlinedTextField(
                            value = viewModel.email,
                            onValueChange = { viewModel.email = it },
                            label = { Text("Correo Electrónico") },
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFE9F0FF),
                                unfocusedContainerColor = Color(0xFFE9F0FF),
                                focusedBorderColor = BsPrimary
                            )
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = viewModel.password,
                            onValueChange = { viewModel.password = it },
                            label = { Text("Contraseña") },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFE9F0FF),
                                unfocusedContainerColor = Color(0xFFE9F0FF),
                                focusedBorderColor = BsPrimary
                            )
                        )

                        if (viewModel.error != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                viewModel.error!!, 
                                color = MaterialTheme.colorScheme.error, 
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(24.dp))
                        
                        Button(
                            onClick = { viewModel.login(onLoginExitoso) },
                            enabled = !viewModel.cargando,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BsDark)
                        ) {
                            if (viewModel.cargando) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Iniciar Sesión", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    
                    // Footer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8F9FA))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "© 2026 Kernel94 · Todos los derechos reservados.",
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemCuentaGuardada(cuenta: CuentaGuardada, onClick: (CuentaGuardada) -> Unit, onRemove: (CuentaGuardada) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp).clickable { onClick(cuenta) }
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            AsyncImage(
                model = cuenta.foto ?: "https://ui-avatars.com/api/?name=${cuenta.nombre}&background=0D6EFD&color=fff",
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.Gray),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = { onRemove(cuenta) },
                modifier = Modifier.size(20.dp).offset(x = 4.dp, y = (-4).dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Eliminar", tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
        Text(
            cuenta.nombre.split(" ").firstOrNull() ?: "",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

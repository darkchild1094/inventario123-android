package com.kernel94.inventario123.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.ui.theme.BsPrimary

@Composable
fun LoginScreen(viewModel: LoginViewModel, onLoginExitoso: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BsPrimary),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Inventory2, contentDescription = null, tint = BsPrimary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text("Inventario123", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Control de activos FEMSA/OXXO", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = viewModel.email,
                    onValueChange = { viewModel.email = it },
                    label = { Text("Correo electrónico") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = { viewModel.password = it },
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (viewModel.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(viewModel.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.login(onLoginExitoso) },
                    enabled = !viewModel.cargando,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BsPrimary)
                ) {
                    if (viewModel.cargando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Iniciar sesión")
                    }
                }
            }
        }
    }
}

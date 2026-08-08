package com.kernel94.inventario123.ui.listado.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kernel94.inventario123.data.model.Activo
import com.kernel94.inventario123.ui.theme.*

@Composable
fun ActivoCard(
    activo: Activo,
    onClick: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Cabecera comentada hasta nuevo aviso de habilitar manejo de imagenes
            /*
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color(0xFFF1F3F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Computer,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.LightGray
                )

                // Badge de Estatus (Esquina Superior Derecha)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colorDeEstatus(activo.status))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        activo.statusLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            */

            // Badge de Estatus (Ahora arriba de los datos, ya que la imagen está comentada)
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    color = colorDeEstatus(activo.status),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = activo.statusLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Contenido de la Tarjeta
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                
                // Badges de Negocio y Plaza (Fila superior)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeaderBadge(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Store, 
                        text = activo.negocio_nombre ?: "N/A", 
                        color = Color(0xFF0056B3)
                    )
                    HeaderBadge(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.LocationOn, 
                        text = activo.plaza_nombre ?: "N/A", 
                        color = Color(0xFFDC3545)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Título y Subtítulo
                Text(
                    text = activo.dispositivo_nombre?.uppercase() ?: "SIN DISPOSITIVO",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1A1C1E),
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = activo.modelo_nombre ?: "Modelo no especificado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                // Detalles con alineación derecha (como en la imagen)
                DetailRow(Icons.Filled.ViewHeadline, "Serie:", activo.serie ?: "—")
                DetailRow(Icons.Filled.Label, "Placa:", activo.placa ?: "—")
                DetailRow(Icons.Filled.Storefront, "Técnico:", activo.usuario_nombre ?: "—")

                Spacer(Modifier.height(16.dp))

                // Footer: ID y Acciones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID: #${activo.id.toString().padStart(4, '0')}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedIconButton(
                            onClick = onEditar,
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.LightGray),
                            colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = BsPrimary)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar", modifier = Modifier.size(20.dp))
                        }
                        OutlinedIconButton(
                            onClick = onEliminar,
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.LightGray),
                            colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = Color.Red)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderBadge(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    text: String, 
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF8F9FA),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.DarkGray)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.5f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

fun colorDeEstatus(status: String): Color = when (status) {
    "asignado" -> StatusAsignado
    "baja" -> StatusBaja
    "en_uso" -> StatusEnUso
    "garantia" -> StatusGarantia
    else -> StatusEnBodega
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ParkingTicket
import com.example.data.model.RateConfig
import com.example.util.CalendarHelper
import com.example.util.Formatters

@Composable
fun TicketReceiptDialog(
    ticket: ParkingTicket,
    rateConfig: RateConfig,
    onDismiss: () -> Unit,
    onSharePdf: (ParkingTicket) -> Unit,
    onShareText: (ParkingTicket) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(20.dp))
                .testTag("ticket_receipt_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (ticket.status == "PAID_EXIT") "Comprobante de Salida" else "Ticket de Ingreso",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_ticket_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Receipt Canvas Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Business Title
                        Text(
                            text = rateConfig.businessName.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = rateConfig.businessNitOrPhone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = rateConfig.businessAddress,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Ticket ID & Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TICKET #${ticket.id.toString().padStart(5, '0')}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            val statusBg = if (ticket.status == "PAID_EXIT") Color(0xFF2E7D32) else Color(0xFFE65100)
                            val statusText = if (ticket.status == "PAID_EXIT") "PAGADO / SALIDA" else "EN PARQUEADERO"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Large Plate Box
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = ticket.plateNumber.uppercase(),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "VEHÍCULO: ${ticket.vehicleType.uppercase()}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Details Rows
                        if (ticket.clientName.isNotBlank()) {
                            DetailReceiptRow("Cliente:", ticket.clientName)
                        }
                        if (ticket.clientPhone.isNotBlank()) {
                            DetailReceiptRow("Teléfono:", ticket.clientPhone)
                        }

                        DetailReceiptRow("Ingreso:", Formatters.formatDateTime(ticket.entryTimeMillis))

                        if (ticket.exitTimeMillis != null) {
                            DetailReceiptRow("Salida:", Formatters.formatDateTime(ticket.exitTimeMillis))
                            DetailReceiptRow(
                                "Tiempo Total:",
                                Formatters.formatDuration(ticket.entryTimeMillis, ticket.exitTimeMillis)
                            )
                        }

                        // Helmet status
                        val helmetDesc = if (ticket.hasHelmet) {
                            "SÍ (${ticket.helmetCount} cascos${if (ticket.helmetLockerNumber.isNotBlank()) " | Locker: ${ticket.helmetLockerNumber}" else ""})"
                        } else "NO"
                        DetailReceiptRow("Casco en custodia:", helmetDesc)

                        if (!ticket.ocrExtractedText.isNullOrBlank()) {
                            DetailReceiptRow("Info OCR:", ticket.ocrExtractedText)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Financial Breakdown
                        if (ticket.status == "PAID_EXIT") {
                            DetailReceiptRow("Subtotal:", Formatters.formatCurrency(ticket.calculatedAmount))
                            if (ticket.discount > 0) {
                                DetailReceiptRow("Descuento:", "-${Formatters.formatCurrency(ticket.discount)}")
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "TOTAL PAGADO:",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = Formatters.formatCurrency(ticket.finalAmountPaid),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            DetailReceiptRow("Método de Pago:", ticket.paymentMethod)
                        } else {
                            val ratePreview = when (ticket.vehicleType.uppercase()) {
                                "MOTO" -> "${Formatters.formatCurrency(rateConfig.motoRatePerHour)} / hora"
                                "CARRO" -> "${Formatters.formatCurrency(rateConfig.carRatePerHour)} / hora"
                                else -> "${Formatters.formatCurrency(rateConfig.motoRatePerHour)} / hora"
                            }
                            DetailReceiptRow("Tarifa Base:", ratePreview)
                            if (ticket.hasHelmet && rateConfig.isHelmetFeeActive) {
                                DetailReceiptRow("Tarifa Casco:", "${Formatters.formatCurrency(rateConfig.helmetStorageFee)} (${rateConfig.helmetFeeType})")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = rateConfig.businessTerms,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Share PDF & Share Text & Open Calendar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSharePdf(ticket) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_pdf_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF Ticket")
                    }

                    OutlinedButton(
                        onClick = { onShareText(ticket) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_text_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir")
                    }
                }

                if (ticket.syncedToCalendar) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { CalendarHelper.openCalendarApp(context, ticket.entryTimeMillis) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_calendar_event_button")
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver en Agenda del Teléfono")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ParkingTicket
import com.example.data.model.RateConfig
import com.example.ui.ParkingViewModel
import com.example.ui.components.PhotoCaptureCard
import com.example.util.CalendarHelper
import com.example.util.FeeCalculator
import com.example.util.Formatters
import com.example.util.OcrHelper
import com.example.util.PhotoStorageHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlateEntryExitScreen(
    viewModel: ParkingViewModel,
    onNavigateToActiveList: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val activeTickets by viewModel.activeTickets.collectAsStateWithLifecycle()
    val filteredHistoryTickets by viewModel.filteredHistoryTickets.collectAsStateWithLifecycle()
    val rateConfig by viewModel.rateConfig.collectAsStateWithLifecycle()

    var plateQuery by remember { mutableStateOf("") }
    val normalizedPlate = plateQuery.trim().uppercase()

    // Find active ticket matching plate
    val activeTicket = remember(normalizedPlate, activeTickets) {
        if (normalizedPlate.isBlank()) null
        else activeTickets.find { it.plateNumber.equals(normalizedPlate, ignoreCase = true) }
    }

    // Camera OCR quick scanner launcher for license plate
    var isOcrScanningPlate by remember { mutableStateOf(false) }
    val plateOcrCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            isOcrScanningPlate = true
            coroutineScope.launch {
                try {
                    val ocrResult = OcrHelper.recognizeTextFromBitmap(bitmap)
                    val foundPlate = ocrResult.candidatePlate ?: OcrHelper.extractPlateCandidate(ocrResult.rawText)
                    if (!foundPlate.isNullOrBlank()) {
                        plateQuery = foundPlate.uppercase()
                        Toast.makeText(context, "Placa detectada: $foundPlate", Toast.LENGTH_SHORT).show()
                    } else if (ocrResult.rawText.isNotBlank()) {
                        val simpleTokens = ocrResult.rawText.replace("\n", " ").split(" ")
                        val candidate = simpleTokens.firstOrNull { it.length in 5..7 }
                        if (candidate != null) {
                            plateQuery = candidate.uppercase().replace("[^A-Z0-9]".toRegex(), "")
                        } else {
                            Toast.makeText(context, "Texto leído: ${ocrResult.rawText.take(20)}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "No se reconoció la placa en la imagen", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isOcrScanningPlate = false
                }
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Title Header
        Text(
            text = "Entrada / Salida por Placa",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Escribe o escanea la placa para registrar ingreso o cobrar salida inmediatamente",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 🔍 High-contrast Plate Input Box with OCR Scanner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "DIGITE O ESCANEE LA PLACA:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = plateQuery,
                        onValueChange = { plateQuery = it.uppercase() },
                        placeholder = { Text("ABC123 / ABC12D") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (plateQuery.isNotBlank()) {
                                IconButton(onClick = { plateQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("plate_search_input")
                    )

                    // OCR Camera Button
                    Button(
                        onClick = {
                            plateOcrCameraLauncher.launch(null)
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("scan_plate_camera_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isOcrScanningPlate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Escanear Placa OCR")
                        }
                    }
                }

                // Quick Chips for Active Vehicles
                if (activeTickets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Vehículos en parqueadero (${activeTickets.size}):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeTickets.take(10).forEach { ticket ->
                            val isSelected = ticket.plateNumber.equals(normalizedPlate, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { plateQuery = ticket.plateNumber },
                                label = {
                                    Text(
                                        text = ticket.plateNumber,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (ticket.vehicleType.uppercase()) {
                                            "CARRO" -> Icons.Default.DirectionsCar
                                            "BICICLETA" -> Icons.AutoMirrored.Filled.DirectionsBike
                                            else -> Icons.Default.TwoWheeler
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic State Screen depending on whether vehicle is currently ACTIVE or NEW
        if (normalizedPlate.isBlank()) {
            // Prompt guide when no plate entered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Ingrese o escanee una placa",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• Si el vehículo está adentro ➔ Se abrirá la liquidación de salida y cobro.\n• Si es un nuevo ingreso ➔ Se abrirá el formulario con fotos, cascos y agenda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (activeTicket != null) {
            // ==========================================
            // 🟢 VEHICLE IS ACTIVE -> EXIT & BILLING FLOW
            // ==========================================
            ActiveVehicleExitSection(
                ticket = activeTicket,
                rateConfig = rateConfig,
                viewModel = viewModel,
                onExitCompleted = {
                    plateQuery = ""
                }
            )
        } else {
            // ==========================================
            // 🔵 VEHICLE IS NOT ACTIVE -> NEW ENTRY FLOW
            // ==========================================
            NewVehicleEntrySection(
                initialPlate = normalizedPlate,
                rateConfig = rateConfig,
                viewModel = viewModel,
                onEntryCompleted = { createdTicket ->
                    viewModel.selectTicketForReceipt(createdTicket)
                    plateQuery = ""
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * EXIT & BILLING SECTION FOR AN ACTIVE VEHICLE
 */
@Composable
private fun ActiveVehicleExitSection(
    ticket: ParkingTicket,
    rateConfig: RateConfig,
    viewModel: ParkingViewModel,
    onExitCompleted: () -> Unit
) {
    val context = LocalContext.current
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMs = System.currentTimeMillis()
            delay(1000)
        }
    }

    var selectedPaymentMethod by remember { mutableStateOf("EFECTIVO") }
    var discountText by remember { mutableStateOf("0") }
    var cashReceivedText by remember { mutableStateOf("") }

    val discountAmount = discountText.toDoubleOrNull() ?: 0.0
    val feeCalculation = remember(ticket, currentTimeMs, rateConfig, discountAmount) {
        FeeCalculator.calculateFee(
            ticket = ticket,
            config = rateConfig,
            exitTimeMillis = currentTimeMs,
            customDiscount = discountAmount
        )
    }

    val cashReceived = cashReceivedText.toDoubleOrNull() ?: 0.0
    val changeDue = (cashReceived - feeCalculation.totalAmount).coerceAtLeast(0.0)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "VEHÍCULO ACTIVO (EN PARQUEADERO)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Ticket #${ticket.id} | Ingresó: ${Formatters.formatTimeOnly(ticket.entryTimeMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Vehicle Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = ticket.plateNumber,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${ticket.vehicleType} | ${ticket.clientName.ifBlank { "Cliente General" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (ticket.clientPhone.isNotBlank()) {
                            Text(
                                text = "📞 Tel: ${ticket.clientPhone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Live Time Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Tiempo Estadía",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = feeCalculation.durationFormatted,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                if (ticket.hasHelmet) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.SportsMotorsports, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Cascos en custodia: ${ticket.helmetCount} ${if (ticket.helmetLockerNumber.isNotBlank()) "(Casillero: ${ticket.helmetLockerNumber})" else ""}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                if (!ticket.ocrExtractedText.isNullOrBlank() || ticket.notes.isNotBlank()) {
                    val notes = listOfNotNull(ticket.ocrExtractedText.takeIf { !it.isNullOrBlank() }, ticket.notes.takeIf { it.isNotBlank() }).joinToString(" | ")
                    Text(
                        text = "Notas / OCR: $notes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Share Entry Ticket Button
                OutlinedButton(
                    onClick = {
                        viewModel.shareTicketPdf(context, ticket)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compartir Ticket de Entrada (PDF / WhatsApp)")
                }
            }
        }

        // Billing Calculation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "LIQUIDACIÓN Y COBRO DE SALIDA",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary
                )

                // Breakdown rows
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tarifa Base Tiempo:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        Formatters.formatCurrency(feeCalculation.baseFee, rateConfig),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (feeCalculation.helmetFee > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Custodia de Cascos (${ticket.helmetCount}):", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Formatters.formatCurrency(feeCalculation.helmetFee, rateConfig),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Discount Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Descuento ($):", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .width(130.dp)
                            .testTag("exit_discount_input"),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Total to pay
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL A COBRAR:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                    Text(
                        text = Formatters.formatCurrency(feeCalculation.totalAmount, rateConfig),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                // Payment Method Selector
                Text(
                    text = "Método de Pago:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val methods = listOf("EFECTIVO", "TRANSFERENCIA", "TARJETA", "OTRO")
                    methods.forEach { method ->
                        val isSelected = selectedPaymentMethod == method
                        val label = when (method) {
                            "EFECTIVO" -> "Efectivo"
                            "TRANSFERENCIA" -> "Transf./Nequi"
                            "TARJETA" -> "Tarjeta"
                            else -> "Otro"
                        }
                        val icon = when (method) {
                            "EFECTIVO" -> Icons.Default.LocalAtm
                            "TRANSFERENCIA" -> Icons.Default.Payments
                            "TARJETA" -> Icons.Default.CreditCard
                            else -> Icons.Default.Payments
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPaymentMethod = method },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Cash change calculator if Cash selected
                if (selectedPaymentMethod == "EFECTIVO") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = cashReceivedText,
                            onValueChange = { cashReceivedText = it },
                            label = { Text("Efectivo Recibido") },
                            placeholder = { Text(rateConfig.currencySymbol) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("exit_cash_received_input"),
                            singleLine = true
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(8.dp)
                        ) {
                            Text("Cambio / Vueltas:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = Formatters.formatCurrency(changeDue, rateConfig),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (changeDue > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Final Action Button: Process Exit & Open Ticket Dialog
                Button(
                    onClick = {
                        viewModel.completeExitAndPayment(
                            ticket = ticket,
                            paymentMethod = selectedPaymentMethod,
                            discount = discountAmount
                        ) { completedTicket ->
                            viewModel.selectTicketForReceipt(completedTicket)
                            onExitCompleted()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("confirm_exit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("COBRAR SALIDA Y GENERAR TICKET", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

/**
 * NEW ENTRY REGISTRATION SECTION FOR A VEHICLE
 */
@Composable
private fun NewVehicleEntrySection(
    initialPlate: String,
    rateConfig: RateConfig,
    viewModel: ParkingViewModel,
    onEntryCompleted: (ParkingTicket) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var vehicleType by remember { mutableStateOf("MOTO") }
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var hasHelmet by remember { mutableStateOf(vehicleType == "MOTO") }
    var helmetCount by remember { mutableIntStateOf(1) }
    var helmetLocker by remember { mutableStateOf("") }

    // Photos & OCR
    var motoPhotoPath by remember { mutableStateOf<String?>(null) }
    var platePhotoPath by remember { mutableStateOf<String?>(null) }
    var helmetPhotoPath by remember { mutableStateOf<String?>(null) }
    var ocrVehicleText by remember { mutableStateOf("") }

    // Auto-search past visits to fill client
    LaunchedEffect(initialPlate) {
        if (initialPlate.length >= 3) {
            val customer = viewModel.findCustomerByPlate(initialPlate)
            if (customer != null) {
                if (customer.clientName.isNotBlank()) clientName = customer.clientName
                if (customer.clientPhone.isNotBlank()) clientPhone = customer.clientPhone
                if (customer.vehicleType.isNotBlank()) vehicleType = customer.vehicleType
                if (customer.motoPhotoPath != null) motoPhotoPath = customer.motoPhotoPath
                if (customer.ocrExtractedText != null) ocrVehicleText = customer.ocrExtractedText
            }
        }
    }

    // Calendar permission launcher
    var syncCalendar by remember { mutableStateOf(rateConfig.autoSyncCalendar) }
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_CALENDAR] == true &&
                permissions[Manifest.permission.WRITE_CALENDAR] == true
        if (granted) {
            syncCalendar = true
            Toast.makeText(context, "Permiso de agenda concedido", Toast.LENGTH_SHORT).show()
        } else {
            syncCalendar = false
            Toast.makeText(context, "Permiso de agenda denegado", Toast.LENGTH_SHORT).show()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "REGISTRAR ENTRADA PARA: $initialPlate",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Complete fotos, cascos y cliente para emitir el ticket de ingreso",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Vehicle Type Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Tipo de Vehículo:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf("MOTO" to "Moto", "CARRO" to "Carro", "BICICLETA" to "Bicicleta")
                    types.forEach { (typeCode, typeLabel) ->
                        val isSelected = vehicleType == typeCode
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                vehicleType = typeCode
                                if (typeCode == "MOTO") hasHelmet = true
                            },
                            label = { Text(typeLabel, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (typeCode) {
                                        "MOTO" -> Icons.Default.TwoWheeler
                                        "CARRO" -> Icons.Default.DirectionsCar
                                        else -> Icons.AutoMirrored.Filled.DirectionsBike
                                    },
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Photos Section (Moto, Placa, Casco)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Fotos de Entrada (Moto, Placa y Casco):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Photo 1: Moto with automatic OCR extraction
                PhotoCaptureCard(
                    title = "1. Foto de la Moto",
                    subtitle = "Extrae texto / modelo por OCR",
                    icon = Icons.Default.TwoWheeler,
                    photoPath = motoPhotoPath,
                    tag = "moto_entry",
                    onPhotoCaptured = { savedPath ->
                        motoPhotoPath = savedPath
                        viewModel.processOcrFromFile(context, savedPath) { ocrResult ->
                            if (ocrResult.rawText.isNotBlank()) {
                                ocrVehicleText = ocrResult.rawText
                                Toast.makeText(context, "Texto detectado en la moto", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onPhotoCleared = { motoPhotoPath = null }
                )

                // OCR text editable field
                if (!ocrVehicleText.isBlank() || motoPhotoPath != null) {
                    OutlinedTextField(
                        value = ocrVehicleText,
                        onValueChange = { ocrVehicleText = it },
                        label = { Text("Texto extraído de la foto (OCR editable)") },
                        placeholder = { Text("Marca, modelo, cilindraje, calcomanías...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ocr_editable_input"),
                        minLines = 2,
                        maxLines = 3
                    )
                }

                // Photo 2: Plate
                PhotoCaptureCard(
                    title = "2. Foto de la Placa",
                    subtitle = "Evidencia visual de matrícula",
                    icon = Icons.Default.CameraAlt,
                    photoPath = platePhotoPath,
                    tag = "plate_entry",
                    onPhotoCaptured = { platePhotoPath = it },
                    onPhotoCleared = { platePhotoPath = null }
                )

                // Photo 3: Helmet
                if (hasHelmet) {
                    PhotoCaptureCard(
                        title = "3. Foto del Casco",
                        subtitle = "Custodia y estado del casco",
                        icon = Icons.Default.SportsMotorsports,
                        photoPath = helmetPhotoPath,
                        tag = "helmet_entry",
                        onPhotoCaptured = { helmetPhotoPath = it },
                        onPhotoCleared = { helmetPhotoPath = null }
                    )
                }
            }
        }

        // Helmet Custody Option
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.SportsMotorsports, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(
                                text = "Custodia de Casco",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (rateConfig.isHelmetFeeActive) "Tarifa: ${Formatters.formatCurrency(rateConfig.helmetStorageFee, rateConfig)}" else "Incluido en servicio",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = hasHelmet,
                        onCheckedChange = { hasHelmet = it }
                    )
                }

                if (hasHelmet) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = helmetCount.toString(),
                            onValueChange = { helmetCount = it.toIntOrNull()?.coerceIn(1, 4) ?: 1 },
                            label = { Text("Cant. Cascos") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = helmetLocker,
                            onValueChange = { helmetLocker = it },
                            label = { Text("Casillero / Ficha") },
                            placeholder = { Text("Ej: 04") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Customer & Agenda Integration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Datos del Cliente:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nombre del Cliente") },
                        placeholder = { Text("Ej: Carlos Pérez") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("entry_customer_name"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Teléfono") },
                        placeholder = { Text("Ej: 3001234567") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("entry_customer_phone"),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observaciones / Estado del Vehículo") },
                    placeholder = { Text("Rayones, accesorios, repuestos...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Agenda Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Guardar en Agenda del Teléfono",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Switch(
                        checked = syncCalendar,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (!CalendarHelper.hasCalendarPermissions(context)) {
                                    calendarPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                                    )
                                } else {
                                    syncCalendar = true
                                }
                            } else {
                                syncCalendar = false
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Save & Share Entry Ticket Button
        Button(
            onClick = {
                viewModel.registerNewEntry(
                    context = context,
                    plate = initialPlate,
                    vehicleType = vehicleType,
                    clientName = clientName.trim(),
                    clientPhone = clientPhone.trim(),
                    hasHelmet = hasHelmet,
                    helmetCount = helmetCount,
                    helmetLocker = helmetLocker.trim(),
                    motoPhotoPath = motoPhotoPath,
                    platePhotoPath = platePhotoPath,
                    helmetPhotoPath = helmetPhotoPath,
                    ocrNotes = ocrVehicleText.trim(),
                    syncToCalendar = syncCalendar,
                    onSuccess = { createdTicket ->
                        onEntryCompleted(createdTicket)
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("submit_entry_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("REGISTRAR ENTRADA Y GENERAR TICKET", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

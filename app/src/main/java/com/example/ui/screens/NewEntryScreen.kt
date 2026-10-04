package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ParkingTicket
import com.example.ui.ParkingViewModel
import com.example.ui.components.PhotoCaptureCard
import com.example.util.CalendarHelper
import com.example.util.OcrHelper
import com.example.util.PhotoStorageHelper
import kotlinx.coroutines.launch

@Composable
fun NewEntryScreen(
    viewModel: ParkingViewModel,
    onEntryCompleted: (ParkingTicket) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val rateConfig by viewModel.rateConfig.collectAsStateWithLifecycle()
    val isOcrLoading by viewModel.isOcrLoading.collectAsStateWithLifecycle()

    var plateText by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("MOTO") }
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }

    // Helmet options
    var hasHelmet by remember { mutableStateOf(true) } // Default true for moto
    var helmetCount by remember { mutableIntStateOf(1) }
    var helmetLocker by remember { mutableStateOf("") }

    // Photos
    var motoPhotoPath by remember { mutableStateOf<String?>(null) }
    var platePhotoPath by remember { mutableStateOf<String?>(null) }
    var helmetPhotoPath by remember { mutableStateOf<String?>(null) }

    // OCR Information
    var ocrExtractedText by remember { mutableStateOf("") }
    var isOcrProcessed by remember { mutableStateOf(false) }

    // Calendar sync
    var syncWithCalendar by remember { mutableStateOf(rateConfig.autoSyncCalendar) }

    val scrollState = rememberScrollState()

    // Permission launcher for calendar
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_CALENDAR] ?: false
        val writeGranted = permissions[Manifest.permission.WRITE_CALENDAR] ?: false
        if (readGranted && writeGranted) {
            syncWithCalendar = true
            Toast.makeText(context, "Permisos de calendario concedidos", Toast.LENGTH_SHORT).show()
        } else {
            syncWithCalendar = false
            Toast.makeText(context, "Permiso denegado. No se podrá sincronizar con la agenda.", Toast.LENGTH_LONG).show()
        }
    }

    // Direct Plate Quick Scan camera launcher
    val quickScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            coroutineScope.launch {
                val path = PhotoStorageHelper.saveBitmapToFile(context, bitmap, "plate_scan")
                platePhotoPath = path
                viewModel.processOcrFromBitmap(bitmap) { ocrResult ->
                    isOcrProcessed = true
                    ocrExtractedText = ocrResult.rawText
                    if (ocrResult.candidatePlate != null) {
                        plateText = ocrResult.candidatePlate
                    }
                }
            }
        }
    }

    // Lookup customer when typing plate
    LaunchedEffect(plateText) {
        if (plateText.length >= 4) {
            val customer = viewModel.findCustomerByPlate(plateText)
            if (customer != null) {
                if (clientName.isBlank()) clientName = customer.clientName
                if (clientPhone.isBlank()) clientPhone = customer.clientPhone
                if (motoPhotoPath == null && customer.motoPhotoPath != null) motoPhotoPath = customer.motoPhotoPath
                if (platePhotoPath == null && customer.platePhotoPath != null) platePhotoPath = customer.platePhotoPath
                if (helmetPhotoPath == null && customer.helmetPhotoPath != null) helmetPhotoPath = customer.helmetPhotoPath
                if (ocrExtractedText.isBlank() && customer.ocrExtractedText != null) {
                    ocrExtractedText = customer.ocrExtractedText
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Title Header
        Text(
            text = "Registro de Ingreso",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Captura de datos, fotos del cliente, OCR y custodia de cascos",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Vehicle Type Selector Chips
        Text(
            text = "Tipo de Vehículo",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VehicleTypeChip(
                label = "Moto",
                icon = Icons.Default.TwoWheeler,
                isSelected = vehicleType == "MOTO",
                onClick = {
                    vehicleType = "MOTO"
                    hasHelmet = true
                },
                modifier = Modifier.weight(1f)
            )
            VehicleTypeChip(
                label = "Carro",
                icon = Icons.Default.DirectionsCar,
                isSelected = vehicleType == "CARRO",
                onClick = {
                    vehicleType = "CARRO"
                    hasHelmet = false
                },
                modifier = Modifier.weight(1f)
            )
            VehicleTypeChip(
                label = "Bicicleta",
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                isSelected = vehicleType == "BICICLETA",
                onClick = {
                    vehicleType = "BICICLETA"
                    hasHelmet = false
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Plate Input with Scan button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = plateText,
                onValueChange = { plateText = it.uppercase() },
                label = { Text("Placa del Vehículo *") },
                placeholder = { Text("Ej: ABC12D / ABC123") },
                leadingIcon = {
                    Icon(Icons.Default.Numbers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Text
                ),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("plate_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Quick Camera Scan Plate Button
            OutlinedButton(
                onClick = { quickScanLauncher.launch(null) },
                modifier = Modifier
                    .height(56.dp)
                    .testTag("scan_plate_camera_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear Placa")
                    Text("OCR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Photos of Motorcycle, Plate and Helmet
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fotografías del Cliente y Vehículo",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isOcrLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    text = "Toma foto a la moto, placa y casco para inventario y extracción OCR.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Photo Moto (with OCR auto extraction)
                PhotoCaptureCard(
                    title = "1. Foto de la Moto / Vehículo",
                    subtitle = "Detecta modelo, marcas y texto mediante OCR",
                    icon = Icons.Default.TwoWheeler,
                    photoPath = motoPhotoPath,
                    tag = "moto",
                    onPhotoCaptured = { savedPath ->
                        motoPhotoPath = savedPath
                        // Auto-run OCR on Moto photo
                        viewModel.processOcrFromFile(context, savedPath) { ocrResult ->
                            isOcrProcessed = true
                            if (ocrExtractedText.isBlank()) {
                                ocrExtractedText = ocrResult.rawText
                            } else {
                                ocrExtractedText += "\n" + ocrResult.rawText
                            }
                            if (plateText.isBlank() && ocrResult.candidatePlate != null) {
                                plateText = ocrResult.candidatePlate
                            }
                        }
                    },
                    onPhotoCleared = { motoPhotoPath = null }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Photo Placa
                PhotoCaptureCard(
                    title = "2. Foto de la Placa",
                    subtitle = "Identificación visual de la placa",
                    icon = Icons.Default.Numbers,
                    photoPath = platePhotoPath,
                    tag = "plate",
                    onPhotoCaptured = { savedPath ->
                        platePhotoPath = savedPath
                        // Run OCR to confirm plate
                        viewModel.processOcrFromFile(context, savedPath) { ocrResult ->
                            if (ocrResult.candidatePlate != null) {
                                plateText = ocrResult.candidatePlate
                            }
                        }
                    },
                    onPhotoCleared = { platePhotoPath = null }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Photo Casco
                PhotoCaptureCard(
                    title = "3. Foto del Casco",
                    subtitle = "Registro del estado y accesorios del casco",
                    icon = Icons.Default.SportsMotorsports,
                    photoPath = helmetPhotoPath,
                    tag = "helmet",
                    onPhotoCaptured = { savedPath ->
                        helmetPhotoPath = savedPath
                        hasHelmet = true
                    },
                    onPhotoCleared = { helmetPhotoPath = null }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Editable OCR & Vehicle Details Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Información Capturada por OCR (Editable)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "El texto detectado automáticamente en la foto de la moto se guarda aquí y puedes corregirlo o agregar notas:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = ocrExtractedText,
                    onValueChange = { ocrExtractedText = it },
                    placeholder = { Text("Texto OCR / Color / Marca / Detalles (Ej: Pulsar NS200 Negra, calcomanía lateral)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ocr_text_input"),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Casco Checkbox / Switch Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasHelmet)
                    Color(0xFF2E7D32).copy(alpha = 0.08f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (hasHelmet) Color(0xFF2E7D32).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.SportsMotorsports,
                            contentDescription = null,
                            tint = if (hasHelmet) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column {
                            Text(
                                text = "¿Deja Casco en Custodia?",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (rateConfig.isHelmetFeeActive)
                                    "Tarifa custodia: ${rateConfig.helmetFeeType} (${com.example.util.Formatters.formatCurrency(rateConfig.helmetStorageFee)})"
                                else "Servicio sin costo adicional",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = hasHelmet,
                        onCheckedChange = { hasHelmet = it },
                        modifier = Modifier.testTag("helmet_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2E7D32)
                        )
                    )
                }

                if (hasHelmet) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF2E7D32).copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cantidad de Cascos:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { if (helmetCount > 1) helmetCount-- },
                                modifier = Modifier
                                    .size(32.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp))
                            }

                            Text(
                                text = "$helmetCount",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            IconButton(
                                onClick = { if (helmetCount < 5) helmetCount++ },
                                modifier = Modifier
                                    .size(32.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = helmetLocker,
                        onValueChange = { helmetLocker = it },
                        label = { Text("Número de Casillero / Descripción del Casco") },
                        placeholder = { Text("Ej: Casillero #4 / Casco MT negro mate") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("helmet_locker_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Customer Info (Optional)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "Datos del Cliente (Opcional)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Nombre del Cliente") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = clientPhone,
                    onValueChange = { clientPhone = it },
                    label = { Text("Teléfono de Contacto") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_phone_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calendar Agenda Sync Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Conectar con Agenda del Teléfono",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Guarda el evento y recordatorio en el calendario",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = syncWithCalendar,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            // Check permissions
                            if (!CalendarHelper.hasCalendarPermissions(context)) {
                                calendarPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_CALENDAR,
                                        Manifest.permission.WRITE_CALENDAR
                                    )
                                )
                            } else {
                                syncWithCalendar = true
                            }
                        } else {
                            syncWithCalendar = false
                        }
                    },
                    modifier = Modifier.testTag("calendar_sync_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Submit Button
        Button(
            onClick = {
                viewModel.registerNewEntry(
                    context = context,
                    plate = plateText,
                    vehicleType = vehicleType,
                    clientName = clientName,
                    clientPhone = clientPhone,
                    hasHelmet = hasHelmet,
                    helmetCount = helmetCount,
                    helmetLocker = helmetLocker,
                    motoPhotoPath = motoPhotoPath,
                    platePhotoPath = platePhotoPath,
                    helmetPhotoPath = helmetPhotoPath,
                    ocrNotes = ocrExtractedText,
                    syncToCalendar = syncWithCalendar,
                    onSuccess = { createdTicket ->
                        onEntryCompleted(createdTicket)
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("submit_entry_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "REGISTRAR INGRESO Y GENERAR TICKET",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun VehicleTypeChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                color = contentColor
            )
        }
    }
}

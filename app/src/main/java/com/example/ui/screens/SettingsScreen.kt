package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.RateConfig
import com.example.ui.ParkingViewModel
import com.example.util.Formatters

@Composable
fun SettingsScreen(
    viewModel: ParkingViewModel,
    modifier: Modifier = Modifier
) {
    val rateConfig by viewModel.rateConfig.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Workshop & Business info
    var businessName by remember { mutableStateOf(rateConfig.businessName) }
    var businessNit by remember { mutableStateOf(rateConfig.businessNitOrPhone) }
    var businessAddress by remember { mutableStateOf(rateConfig.businessAddress) }
    var workshopSlogan by remember { mutableStateOf(rateConfig.workshopSlogan) }
    var workshopAttendant by remember { mutableStateOf(rateConfig.workshopAttendantName) }
    var businessTerms by remember { mutableStateOf(rateConfig.businessTerms) }

    // Currency & Formatting
    var selectedCurrencyPreset by remember { mutableStateOf(rateConfig.currencyType) }
    var currencySymbol by remember { mutableStateOf(rateConfig.currencySymbol) }
    var currencyCode by remember { mutableStateOf(rateConfig.currencyCode) }
    var thousandsSeparator by remember { mutableStateOf(rateConfig.thousandsSeparator) } // "POINT", "COMMA", "NONE"
    var decimalPlaces by remember { mutableIntStateOf(rateConfig.decimalPlaces) }

    // Rates
    var motoPerHour by remember { mutableStateOf(rateConfig.motoRatePerHour.toString()) }
    var motoPerMinute by remember { mutableStateOf(rateConfig.motoRatePerMinute.toString()) }
    var motoPerDay by remember { mutableStateOf(rateConfig.motoRatePerDay.toString()) }

    var carPerHour by remember { mutableStateOf(rateConfig.carRatePerHour.toString()) }
    var carPerMinute by remember { mutableStateOf(rateConfig.carRatePerMinute.toString()) }
    var carPerDay by remember { mutableStateOf(rateConfig.carRatePerDay.toString()) }

    var bikePerHour by remember { mutableStateOf(rateConfig.bikeRatePerHour.toString()) }
    var bikePerDay by remember { mutableStateOf(rateConfig.bikeRatePerDay.toString()) }

    var helmetFee by remember { mutableStateOf(rateConfig.helmetStorageFee.toString()) }
    var isHelmetFeeActive by remember { mutableStateOf(rateConfig.isHelmetFeeActive) }

    var gracePeriod by remember { mutableStateOf(rateConfig.gracePeriodMinutes.toString()) }
    var fractionMinutes by remember { mutableStateOf(rateConfig.fractionMinutes.toString()) }
    var autoSyncCalendar by remember { mutableStateOf(rateConfig.autoSyncCalendar) }

    LaunchedEffect(rateConfig) {
        businessName = rateConfig.businessName
        businessNit = rateConfig.businessNitOrPhone
        businessAddress = rateConfig.businessAddress
        workshopSlogan = rateConfig.workshopSlogan
        workshopAttendant = rateConfig.workshopAttendantName
        businessTerms = rateConfig.businessTerms

        selectedCurrencyPreset = rateConfig.currencyType
        currencySymbol = rateConfig.currencySymbol
        currencyCode = rateConfig.currencyCode
        thousandsSeparator = rateConfig.thousandsSeparator
        decimalPlaces = rateConfig.decimalPlaces

        motoPerHour = if (rateConfig.motoRatePerHour % 1.0 == 0.0) rateConfig.motoRatePerHour.toLong().toString() else rateConfig.motoRatePerHour.toString()
        motoPerMinute = if (rateConfig.motoRatePerMinute % 1.0 == 0.0) rateConfig.motoRatePerMinute.toLong().toString() else rateConfig.motoRatePerMinute.toString()
        motoPerDay = if (rateConfig.motoRatePerDay % 1.0 == 0.0) rateConfig.motoRatePerDay.toLong().toString() else rateConfig.motoRatePerDay.toString()

        carPerHour = if (rateConfig.carRatePerHour % 1.0 == 0.0) rateConfig.carRatePerHour.toLong().toString() else rateConfig.carRatePerHour.toString()
        carPerMinute = if (rateConfig.carRatePerMinute % 1.0 == 0.0) rateConfig.carRatePerMinute.toLong().toString() else rateConfig.carRatePerMinute.toString()
        carPerDay = if (rateConfig.carRatePerDay % 1.0 == 0.0) rateConfig.carRatePerDay.toLong().toString() else rateConfig.carRatePerDay.toString()

        bikePerHour = if (rateConfig.bikeRatePerHour % 1.0 == 0.0) rateConfig.bikeRatePerHour.toLong().toString() else rateConfig.bikeRatePerHour.toString()
        bikePerDay = if (rateConfig.bikeRatePerDay % 1.0 == 0.0) rateConfig.bikeRatePerDay.toLong().toString() else rateConfig.bikeRatePerDay.toString()

        helmetFee = if (rateConfig.helmetStorageFee % 1.0 == 0.0) rateConfig.helmetStorageFee.toLong().toString() else rateConfig.helmetStorageFee.toString()
        isHelmetFeeActive = rateConfig.isHelmetFeeActive
        gracePeriod = rateConfig.gracePeriodMinutes.toString()
        fractionMinutes = rateConfig.fractionMinutes.toString()
        autoSyncCalendar = rateConfig.autoSyncCalendar
    }

    // Live preview formatting sample
    val sampleAmount = 12500.50
    val sampleConfig = remember(currencySymbol, thousandsSeparator, decimalPlaces) {
        RateConfig(
            currencySymbol = currencySymbol,
            thousandsSeparator = thousandsSeparator,
            decimalPlaces = decimalPlaces
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Configuración del Taller y Tarifas",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Datos del taller, moneda (USD, COL, SOL), separador de miles y cálculo automático",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Workshop & Business Profile Card
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Datos del Taller y Establecimiento",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Nombre del Taller / Parqueadero *") },
                    placeholder = { Text("Ej: Taller & Parqueadero MotoExpert") },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_business_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = businessNit,
                    onValueChange = { businessNit = it },
                    label = { Text("NIT / RUC / RUT / Teléfono de Contacto") },
                    placeholder = { Text("Ej: NIT: 900.123.456-7 | Cel: 300 987 6543") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_business_nit"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = businessAddress,
                    onValueChange = { businessAddress = it },
                    label = { Text("Dirección / Ubicación del Taller") },
                    placeholder = { Text("Ej: Carrera 15 # 45-20, Zona Centro") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_business_address"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = workshopAttendant,
                        onValueChange = { workshopAttendant = it },
                        label = { Text("Encargado / Mecánico") },
                        placeholder = { Text("Ej: Maestro Carlos") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_workshop_attendant"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = workshopSlogan,
                        onValueChange = { workshopSlogan = it },
                        label = { Text("Lema / Slogan") },
                        placeholder = { Text("Ej: Calidad y Confianza") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_workshop_slogan"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = businessTerms,
                    onValueChange = { businessTerms = it },
                    label = { Text("Términos, Garantías y Condiciones del Ticket") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_business_terms"),
                    minLines = 2,
                    maxLines = 3
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Currency & Thousands Separator Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Moneda y Formato Numérico",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Currency Presets Chips (COL/COP, USD, SOL/PEN, Custom)
                Text(
                    text = "Seleccionar Moneda:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurrencyPresetChip(
                        title = "COL (COP)",
                        subtitle = "Pesos ($)",
                        isSelected = selectedCurrencyPreset == "COP",
                        onClick = {
                            selectedCurrencyPreset = "COP"
                            currencySymbol = "$"
                            currencyCode = "COP"
                            thousandsSeparator = "POINT"
                            decimalPlaces = 0
                        },
                        modifier = Modifier.weight(1f)
                    )

                    CurrencyPresetChip(
                        title = "USD",
                        subtitle = "Dólar ($)",
                        isSelected = selectedCurrencyPreset == "USD",
                        onClick = {
                            selectedCurrencyPreset = "USD"
                            currencySymbol = "$"
                            currencyCode = "USD"
                            thousandsSeparator = "COMMA"
                            decimalPlaces = 2
                        },
                        modifier = Modifier.weight(1f)
                    )

                    CurrencyPresetChip(
                        title = "SOL (PEN)",
                        subtitle = "Soles (S/)",
                        isSelected = selectedCurrencyPreset == "PEN",
                        onClick = {
                            selectedCurrencyPreset = "PEN"
                            currencySymbol = "S/"
                            currencyCode = "PEN"
                            thousandsSeparator = "COMMA"
                            decimalPlaces = 2
                        },
                        modifier = Modifier.weight(1f)
                    )

                    CurrencyPresetChip(
                        title = "Otro",
                        subtitle = "Personal.",
                        isSelected = selectedCurrencyPreset == "CUSTOM",
                        onClick = {
                            selectedCurrencyPreset = "CUSTOM"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Currency Inputs (if needed or editable)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = currencySymbol,
                        onValueChange = {
                            currencySymbol = it
                            selectedCurrencyPreset = "CUSTOM"
                        },
                        label = { Text("Símbolo") },
                        placeholder = { Text("$, S/, USD") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_currency_symbol"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = currencyCode,
                        onValueChange = {
                            currencyCode = it.uppercase()
                            selectedCurrencyPreset = "CUSTOM"
                        },
                        label = { Text("Código") },
                        placeholder = { Text("COP, USD, PEN") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_currency_code"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = decimalPlaces.toString(),
                        onValueChange = { decimalPlaces = it.toIntOrNull()?.coerceIn(0, 3) ?: 0 },
                        label = { Text("Decimales") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_decimal_places"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // Thousands Separator Option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.FormatListNumbered, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Separación de Miles:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SeparatorOptionChip(
                        title = "Con Punto (.)",
                        example = if (decimalPlaces > 0) "12.500,50" else "12.500",
                        isSelected = thousandsSeparator == "POINT",
                        onClick = { thousandsSeparator = "POINT" },
                        modifier = Modifier.weight(1f)
                    )
                    SeparatorOptionChip(
                        title = "Con Coma (,)",
                        example = if (decimalPlaces > 0) "12,500.50" else "12,500",
                        isSelected = thousandsSeparator == "COMMA",
                        onClick = { thousandsSeparator = "COMMA" },
                        modifier = Modifier.weight(1f)
                    )
                    SeparatorOptionChip(
                        title = "Sin Separador",
                        example = if (decimalPlaces > 0) "12500.50" else "12500",
                        isSelected = thousandsSeparator == "NONE",
                        onClick = { thousandsSeparator = "NONE" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "👁️ Vista Previa en App y Tickets:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = Formatters.formatCurrency(sampleAmount, sampleConfig),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Motorcycle Rates Card
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Tarifas de Motos ($currencySymbol)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = motoPerHour,
                        onValueChange = { motoPerHour = it },
                        label = { Text("Por Hora ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_moto_hour"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = motoPerMinute,
                        onValueChange = { motoPerMinute = it },
                        label = { Text("Por Minuto ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_moto_min"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = motoPerDay,
                    onValueChange = { motoPerDay = it },
                    label = { Text("Día Completo / 24 Horas ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_moto_day"),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Car & Other Rates Card
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Tarifas de Carros y Camionetas ($currencySymbol)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = carPerHour,
                        onValueChange = { carPerHour = it },
                        label = { Text("Carro Hora ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_car_hour"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = carPerMinute,
                        onValueChange = { carPerMinute = it },
                        label = { Text("Carro Minuto ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_car_min"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = carPerDay,
                    onValueChange = { carPerDay = it },
                    label = { Text("Carro Día Completo ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_car_day"),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Bicycle Rates Card
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Tarifas de Bicicletas ($currencySymbol)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = bikePerHour,
                        onValueChange = { bikePerHour = it },
                        label = { Text("Bici Hora ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_bike_hour"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = bikePerDay,
                        onValueChange = { bikePerDay = it },
                        label = { Text("Bici Día ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_bike_day"),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Helmet Custody Settings
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.SportsMotorsports, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Custodia de Cascos",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Switch(
                        checked = isHelmetFeeActive,
                        onCheckedChange = { isHelmetFeeActive = it },
                        modifier = Modifier.testTag("settings_helmet_fee_switch")
                    )
                }

                if (isHelmetFeeActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = helmetFee,
                        onValueChange = { helmetFee = it },
                        label = { Text("Tarifa por Casco ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_helmet_fee_input"),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7. Calculation & Billing Parameters
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Parámetros de Cobro",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = gracePeriod,
                        onValueChange = { gracePeriod = it },
                        label = { Text("Minutos de Gracia (Gratis)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_grace_period"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = fractionMinutes,
                        onValueChange = { fractionMinutes = it },
                        label = { Text("Fracción (Minutos)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_fraction_min"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sincronizar con Agenda por defecto",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Activar por defecto el guardado en calendario",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoSyncCalendar,
                        onCheckedChange = { autoSyncCalendar = it }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
        Button(
            onClick = {
                val updatedConfig = rateConfig.copy(
                    businessName = businessName.trim(),
                    businessNitOrPhone = businessNit.trim(),
                    businessAddress = businessAddress.trim(),
                    workshopSlogan = workshopSlogan.trim(),
                    workshopAttendantName = workshopAttendant.trim(),
                    businessTerms = businessTerms.trim(),

                    currencyType = selectedCurrencyPreset,
                    currencySymbol = currencySymbol.trim(),
                    currencyCode = currencyCode.trim().uppercase(),
                    thousandsSeparator = thousandsSeparator,
                    decimalPlaces = decimalPlaces,

                    motoRatePerHour = motoPerHour.toDoubleOrNull() ?: rateConfig.motoRatePerHour,
                    motoRatePerMinute = motoPerMinute.toDoubleOrNull() ?: rateConfig.motoRatePerMinute,
                    motoRatePerDay = motoPerDay.toDoubleOrNull() ?: rateConfig.motoRatePerDay,

                    carRatePerHour = carPerHour.toDoubleOrNull() ?: rateConfig.carRatePerHour,
                    carRatePerMinute = carPerMinute.toDoubleOrNull() ?: rateConfig.carRatePerMinute,
                    carRatePerDay = carPerDay.toDoubleOrNull() ?: rateConfig.carRatePerDay,

                    bikeRatePerHour = bikePerHour.toDoubleOrNull() ?: rateConfig.bikeRatePerHour,
                    bikeRatePerDay = bikePerDay.toDoubleOrNull() ?: rateConfig.bikeRatePerDay,

                    helmetStorageFee = helmetFee.toDoubleOrNull() ?: rateConfig.helmetStorageFee,
                    isHelmetFeeActive = isHelmetFeeActive,
                    gracePeriodMinutes = gracePeriod.toIntOrNull() ?: rateConfig.gracePeriodMinutes,
                    fractionMinutes = fractionMinutes.toIntOrNull() ?: rateConfig.fractionMinutes,
                    autoSyncCalendar = autoSyncCalendar
                )
                viewModel.updateRateConfig(updatedConfig)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_settings_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("GUARDAR TODA LA CONFIGURACIÓN", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun CurrencyPresetChip(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = contentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = contentColor.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun SeparatorOptionChip(
    title: String,
    example: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = contentColor
            )
            Text(
                text = example,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                ),
                color = contentColor
            )
        }
    }
}


package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rate_config")
data class RateConfig(
    @PrimaryKey
    val id: Int = 1,
    val businessName: String = "Taller y Parqueadero MotoPark",
    val businessNitOrPhone: String = "NIT: 900.876.543-1 | Tel: 300 123 4567",
    val businessAddress: String = "Zona Centro - Taller y Parqueadero Principal",
    val businessTerms: String = "Conserve su ticket para retirar el vehículo. No respondemos por objetos no inventariados dejados en el vehículo.",
    val workshopAttendantName: String = "Administrador / Encargado",
    val workshopSlogan: String = "Servicio especializado, seguro y garantizado",
    
    // Currency & Formatting
    val currencyType: String = "COP", // "COP", "USD", "PEN", "CUSTOM"
    val currencySymbol: String = "$", // "$", "S/", "US$", etc.
    val currencyCode: String = "COP", // "COP", "USD", "PEN", "SOL", "COL"
    val thousandsSeparator: String = "POINT", // "POINT" (1.000), "COMMA" (1,000), "NONE" (1000)
    val decimalPlaces: Int = 0, // 0 for COP, 2 for USD/PEN
    
    // Rates
    val motoRatePerHour: Double = 2000.0,
    val motoRatePerMinute: Double = 35.0,
    val motoRatePerDay: Double = 12000.0,
    val carRatePerHour: Double = 4000.0,
    val carRatePerMinute: Double = 70.0,
    val carRatePerDay: Double = 25000.0,
    val bikeRatePerHour: Double = 1000.0,
    val bikeRatePerDay: Double = 5000.0,
    val helmetStorageFee: Double = 1000.0, // Additional fee per helmet if enabled
    val isHelmetFeeActive: Boolean = true,
    val helmetFeeType: String = "FLAT", // FLAT (tarifa única), PER_HOUR (por hora), FREE (gratis)
    val fractionMinutes: Int = 15,
    val gracePeriodMinutes: Int = 10,
    val rateCalculationMode: String = "HOURLY_FRACTION", // HOURLY_FRACTION, PER_MINUTE, FLAT_HOUR
    val autoSyncCalendar: Boolean = true
)

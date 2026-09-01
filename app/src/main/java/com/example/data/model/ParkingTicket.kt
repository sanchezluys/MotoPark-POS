package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parking_tickets")
data class ParkingTicket(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plateNumber: String,
    val vehicleType: String = "MOTO", // MOTO, CARRO, BICICLETA, OTRO
    val clientName: String = "",
    val clientPhone: String = "",
    val entryTimeMillis: Long = System.currentTimeMillis(),
    val exitTimeMillis: Long? = null,
    val hasHelmet: Boolean = false,
    val helmetCount: Int = 0,
    val helmetLockerNumber: String = "",
    val motoPhotoPath: String? = null,
    val platePhotoPath: String? = null,
    val helmetPhotoPath: String? = null,
    val ocrExtractedText: String? = null,
    val calculatedAmount: Double = 0.0,
    val discount: Double = 0.0,
    val finalAmountPaid: Double = 0.0,
    val paymentMethod: String = "EFECTIVO", // EFECTIVO, TRANSFERENCIA, TARJETA, OTRO
    val status: String = "ACTIVE", // ACTIVE, PAID_EXIT, CANCELLED
    val notes: String = "",
    val syncedToCalendar: Boolean = false,
    val calendarEventId: Long? = null
)

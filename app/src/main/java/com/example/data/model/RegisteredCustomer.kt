package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registered_customers",
    indices = [Index(value = ["plateNumber"], unique = true)]
)
data class RegisteredCustomer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plateNumber: String,
    val clientName: String = "",
    val clientPhone: String = "",
    val vehicleType: String = "MOTO",
    val brandModelColor: String = "",
    val motoPhotoPath: String? = null,
    val platePhotoPath: String? = null,
    val helmetPhotoPath: String? = null,
    val ocrExtractedText: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val totalVisits: Int = 1
)

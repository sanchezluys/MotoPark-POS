package com.example.util

import com.example.data.model.ParkingTicket
import com.example.data.model.RateConfig
import kotlin.math.ceil

data class FeeCalculationResult(
    val durationMinutes: Long,
    val durationFormatted: String,
    val baseFee: Double,
    val helmetFee: Double,
    val subtotal: Double,
    val discount: Double,
    val totalAmount: Double,
    val breakdownDescription: String
)

object FeeCalculator {

    fun calculateFee(
        ticket: ParkingTicket,
        config: RateConfig,
        exitTimeMillis: Long = System.currentTimeMillis(),
        customDiscount: Double = 0.0
    ): FeeCalculationResult {
        val entryTime = ticket.entryTimeMillis
        val durationMillis = (exitTimeMillis - entryTime).coerceAtLeast(0)
        val totalMinutes = durationMillis / (1000 * 60)

        val durationFormatted = Formatters.formatDuration(entryTime, exitTimeMillis)

        // If within initial grace period and less than grace time, free or minimum
        if (totalMinutes <= config.gracePeriodMinutes) {
            val helmetFee = if (ticket.hasHelmet && config.isHelmetFeeActive && config.helmetFeeType == "FLAT") {
                (ticket.helmetCount.coerceAtLeast(1)) * config.helmetStorageFee
            } else 0.0

            val total = (helmetFee - customDiscount).coerceAtLeast(0.0)
            return FeeCalculationResult(
                durationMinutes = totalMinutes,
                durationFormatted = durationFormatted,
                baseFee = 0.0,
                helmetFee = helmetFee,
                subtotal = helmetFee,
                discount = customDiscount,
                totalAmount = total,
                breakdownDescription = "Período de Gracia (${totalMinutes}m <= ${config.gracePeriodMinutes}m de cortesía)"
            )
        }

        // Get hourly/daily rates based on vehicle type
        val hourlyRate = when (ticket.vehicleType.uppercase()) {
            "MOTO" -> config.motoRatePerHour
            "CARRO" -> config.carRatePerHour
            "BICICLETA" -> config.bikeRatePerHour
            else -> config.motoRatePerHour
        }

        val dailyRate = when (ticket.vehicleType.uppercase()) {
            "MOTO" -> config.motoRatePerDay
            "CARRO" -> config.carRatePerDay
            "BICICLETA" -> config.bikeRatePerDay
            else -> config.motoRatePerDay
        }

        val minuteRate = when (ticket.vehicleType.uppercase()) {
            "MOTO" -> config.motoRatePerMinute
            "CARRO" -> config.carRatePerMinute
            else -> config.motoRatePerMinute
        }

        // Calculate base fee
        val days = totalMinutes / (24 * 60)
        val remainingMinutes = totalMinutes % (24 * 60)

        val baseFee: Double = when (config.rateCalculationMode) {
            "PER_MINUTE" -> {
                totalMinutes * minuteRate
            }
            "HOURLY_FRACTION" -> {
                var fee = days * dailyRate
                val fraction = config.fractionMinutes.coerceAtLeast(1)
                val billableFractions = ceil(remainingMinutes.toDouble() / fraction).toInt()
                val fractionsPerHour = 60.0 / fraction
                val feeForRemaining = (billableFractions / fractionsPerHour) * hourlyRate
                // Cap daily if remaining fee exceeds daily rate
                fee += if (dailyRate > 0 && feeForRemaining > dailyRate) dailyRate else feeForRemaining
                fee
            }
            else -> { // HOURLY / FULL HOUR
                val hours = ceil(totalMinutes.toDouble() / 60.0).toInt().coerceAtLeast(1)
                hours * hourlyRate
            }
        }

        // Calculate helmet fee
        val helmetFee: Double = if (ticket.hasHelmet && config.isHelmetFeeActive) {
            val count = ticket.helmetCount.coerceAtLeast(1)
            when (config.helmetFeeType) {
                "PER_HOUR" -> {
                    val hours = ceil(totalMinutes.toDouble() / 60.0).toInt().coerceAtLeast(1)
                    count * config.helmetStorageFee * hours
                }
                "FREE" -> 0.0
                else -> count * config.helmetStorageFee // FLAT
            }
        } else {
            0.0
        }

        val subtotal = baseFee + helmetFee
        val total = (subtotal - customDiscount).coerceAtLeast(0.0)

        val helmetText = if (helmetFee > 0) " + Casco: ${Formatters.formatCurrency(helmetFee, config)}" else ""
        val breakdown = "Tiempo: $durationFormatted | Tarifa: ${Formatters.formatCurrency(baseFee, config)}$helmetText"

        return FeeCalculationResult(
            durationMinutes = totalMinutes,
            durationFormatted = durationFormatted,
            baseFee = baseFee,
            helmetFee = helmetFee,
            subtotal = subtotal,
            discount = customDiscount,
            totalAmount = total,
            breakdownDescription = breakdown
        )
    }
}

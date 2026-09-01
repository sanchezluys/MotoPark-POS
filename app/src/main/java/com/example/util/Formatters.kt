package com.example.util

import com.example.data.model.RateConfig
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    @Volatile
    var activeConfig: RateConfig = RateConfig()

    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
    private val timeOnlyFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun formatNumber(amount: Double, thousandsSeparator: String = "POINT", decimalPlaces: Int = 0): String {
        return try {
            val symbols = DecimalFormatSymbols(Locale.US).apply {
                when (thousandsSeparator.uppercase()) {
                    "POINT", "PUNTO_DECIMAL_COMA", "PUNTO" -> {
                        groupingSeparator = '.'
                        decimalSeparator = ','
                    }
                    "COMMA", "COMA_DECIMAL_PUNTO", "COMA" -> {
                        groupingSeparator = ','
                        decimalSeparator = '.'
                    }
                    else -> {
                        // NONE
                        groupingSeparator = ' '
                        decimalSeparator = '.'
                    }
                }
            }

            val pattern = buildString {
                if (thousandsSeparator == "NONE") {
                    append("0")
                } else {
                    append("#,##0")
                }
                if (decimalPlaces > 0) {
                    append(".")
                    repeat(decimalPlaces) { append("0") }
                }
            }

            val df = DecimalFormat(pattern, symbols)
            if (thousandsSeparator == "NONE") {
                df.isGroupingUsed = false
            }
            df.format(amount)
        } catch (e: Exception) {
            String.format(Locale.US, "%.${decimalPlaces}f", amount)
        }
    }

    fun formatCurrency(amount: Double, config: RateConfig? = null): String {
        val cfg = config ?: activeConfig
        val symbol = cfg.currencySymbol.trim()
        val numFormatted = formatNumber(
            amount = Math.abs(amount),
            thousandsSeparator = cfg.thousandsSeparator,
            decimalPlaces = cfg.decimalPlaces
        )
        val prefix = if (amount < 0) "-" else ""
        return if (symbol.isNotBlank()) {
            "$prefix$symbol $numFormatted"
        } else {
            "$prefix$numFormatted"
        }
    }

    fun formatDateTime(millis: Long): String {
        return dateTimeFormat.format(Date(millis))
    }

    fun formatTimeOnly(millis: Long): String {
        return timeOnlyFormat.format(Date(millis))
    }

    fun formatDateOnly(millis: Long): String {
        return dateOnlyFormat.format(Date(millis))
    }

    fun formatFileTimestamp(): String {
        return fileNameDateFormat.format(Date())
    }

    fun formatDuration(startMillis: Long, endMillis: Long): String {
        val diffMillis = (endMillis - startMillis).coerceAtLeast(0)
        val minutes = (diffMillis / (1000 * 60)) % 60
        val hours = (diffMillis / (1000 * 60 * 60)) % 24
        val days = diffMillis / (1000 * 60 * 60 * 24)

        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}


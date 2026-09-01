package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.ParkingTicket
import com.example.data.model.RateConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfExportHelper {

    suspend fun generateTicketPdf(
        context: Context,
        ticket: ParkingTicket,
        config: RateConfig
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageWidth = 400 // Standard thermal receipt / compact doc width
        val pageHeight = 650
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Outer Border
        val borderPaint = Paint().apply {
            color = Color.rgb(20, 30, 50)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(RectF(15f, 15f, pageWidth - 15f, pageHeight - 15f), 12f, 12f, borderPaint)

        // Header Background Banner
        val headerPaint = Paint().apply {
            color = Color.rgb(20, 40, 80)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(15f, 15f, pageWidth - 15f, 95f), 12f, 12f, headerPaint)
        // Rect to cover bottom corners of header
        canvas.drawRect(RectF(15f, 50f, pageWidth - 15f, 95f), headerPaint)

        // Header Text
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(config.businessName.take(35), pageWidth / 2f, 44f, titlePaint)

        val subTitlePaint = Paint().apply {
            color = Color.rgb(220, 230, 255)
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(config.businessNitOrPhone.take(45), pageWidth / 2f, 62f, subTitlePaint)
        canvas.drawText(config.businessAddress.take(45), pageWidth / 2f, 76f, subTitlePaint)
        if (config.workshopSlogan.isNotBlank()) {
            val sloganPaint = Paint().apply {
                color = Color.rgb(180, 210, 255)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(config.workshopSlogan.take(50), pageWidth / 2f, 89f, sloganPaint)
        }

        // Ticket Title & Number
        val ticketTitlePaint = Paint().apply {
            color = Color.rgb(20, 30, 50)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val ticketTypeLabel = if (ticket.status == "PAID_EXIT") "COMPROBANTE DE PAGO / SALIDA" else "TICKET DE INGRESO"
        canvas.drawText(ticketTypeLabel, pageWidth / 2f, 125f, ticketTitlePaint)

        val ticketNumPaint = Paint().apply {
            color = Color.rgb(180, 80, 20)
            textSize = 12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Nº #${ticket.id.toString().padStart(6, '0')}", pageWidth / 2f, 142f, ticketNumPaint)

        // Divider
        val dividerPaint = Paint().apply {
            color = Color.rgb(210, 215, 225)
            strokeWidth = 1f
        }
        canvas.drawLine(30f, 155f, pageWidth - 30f, 155f, dividerPaint)

        // Plate Highlight Box
        val plateBoxPaint = Paint().apply {
            color = Color.rgb(245, 247, 250)
            style = Paint.Style.FILL
        }
        val plateStroke = Paint().apply {
            color = Color.rgb(40, 60, 100)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val plateRect = RectF(60f, 168f, pageWidth - 60f, 220f)
        canvas.drawRoundRect(plateRect, 8f, 8f, plateBoxPaint)
        canvas.drawRoundRect(plateRect, 8f, 8f, plateStroke)

        val platePaint = Paint().apply {
            color = Color.rgb(10, 25, 60)
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(ticket.plateNumber.uppercase(), pageWidth / 2f, 204f, platePaint)

        // Info Rows
        var currentY = 248f
        val labelPaint = Paint().apply {
            color = Color.rgb(100, 110, 125)
            textSize = 11f
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = Color.rgb(20, 30, 45)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        fun drawRow(label: String, value: String) {
            canvas.drawText(label, 35f, currentY, labelPaint)
            val valWidth = valuePaint.measureText(value)
            canvas.drawText(value, pageWidth - 35f - valWidth, currentY, valuePaint)
            currentY += 22f
        }

        drawRow("Tipo de Vehículo:", ticket.vehicleType)
        if (ticket.clientName.isNotBlank()) {
            drawRow("Cliente:", ticket.clientName)
        }
        if (ticket.clientPhone.isNotBlank()) {
            drawRow("Teléfono:", ticket.clientPhone)
        }

        drawRow("Fecha/Hora Ingreso:", Formatters.formatDateTime(ticket.entryTimeMillis))

        if (ticket.exitTimeMillis != null) {
            drawRow("Fecha/Hora Salida:", Formatters.formatDateTime(ticket.exitTimeMillis))
            drawRow("Tiempo Total:", Formatters.formatDuration(ticket.entryTimeMillis, ticket.exitTimeMillis))
        }

        // Helmet section
        val helmetVal = if (ticket.hasHelmet) {
            "SÍ (${ticket.helmetCount} cascos" + (if (ticket.helmetLockerNumber.isNotBlank()) " | Locker: ${ticket.helmetLockerNumber}" else "") + ")"
        } else "NO"
        drawRow("Servicio Casco:", helmetVal)

        if (!ticket.ocrExtractedText.isNullOrBlank()) {
            val ocrPreview = ticket.ocrExtractedText.take(28)
            drawRow("OCR / Observaciones:", ocrPreview)
        }

        canvas.drawLine(30f, currentY + 4f, pageWidth - 30f, currentY + 4f, dividerPaint)
        currentY += 24f

        // Payment Details if finished or active estimated
        if (ticket.status == "PAID_EXIT") {
            drawRow("Subtotal:", Formatters.formatCurrency(ticket.calculatedAmount))
            if (ticket.discount > 0) {
                drawRow("Descuento:", "-${Formatters.formatCurrency(ticket.discount)}")
            }
            val totalPaint = Paint().apply {
                color = Color.rgb(20, 100, 40)
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("TOTAL PAGADO:", 35f, currentY + 4f, totalPaint)
            val totalStr = Formatters.formatCurrency(ticket.finalAmountPaid)
            val totalWidth = totalPaint.measureText(totalStr)
            canvas.drawText(totalStr, pageWidth - 35f - totalWidth, currentY + 4f, totalPaint)
            currentY += 24f

            drawRow("Método de Pago:", ticket.paymentMethod)
        } else {
            val rateText = when (ticket.vehicleType.uppercase()) {
                "MOTO" -> "${Formatters.formatCurrency(config.motoRatePerHour)} / hora"
                "CARRO" -> "${Formatters.formatCurrency(config.carRatePerHour)} / hora"
                "BICICLETA" -> "${Formatters.formatCurrency(config.bikeRatePerHour)} / hora"
                else -> "${Formatters.formatCurrency(config.motoRatePerHour)} / hora"
            }
            drawRow("Tarifa Base:", rateText)
            if (ticket.hasHelmet && config.isHelmetFeeActive) {
                drawRow("Tarifa Casco:", "${Formatters.formatCurrency(config.helmetStorageFee)} (${config.helmetFeeType})")
            }
        }

        // Barcode / Simulated QR Strip
        currentY += 10f
        val barcodePaint = Paint().apply {
            color = Color.rgb(30, 40, 60)
            strokeWidth = 2f
        }
        val barcodeXStart = 80f
        val barcodeWidth = pageWidth - 160f
        val numBars = 35
        for (i in 0..numBars) {
            val barX = barcodeXStart + (i * (barcodeWidth / numBars))
            val width = if (i % 3 == 0 || i % 7 == 0) 3.5f else 1.5f
            barcodePaint.strokeWidth = width
            canvas.drawLine(barX, currentY, barX, currentY + 30f, barcodePaint)
        }
        currentY += 38f

        // Terms Footer
        val termsPaint = Paint().apply {
            color = Color.rgb(120, 130, 140)
            textSize = 8f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val termsLine1 = config.businessTerms.take(70)
        val termsLine2 = if (config.businessTerms.length > 70) config.businessTerms.substring(70).take(70) else "¡Gracias por su visita! Software MotoPark Offline"
        canvas.drawText(termsLine1, pageWidth / 2f, currentY + 12f, termsPaint)
        canvas.drawText(termsLine2, pageWidth / 2f, currentY + 24f, termsPaint)

        pdfDocument.finishPage(page)

        // Save File
        val reportsDir = File(context.cacheDir, "tickets")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val file = File(reportsDir, "Ticket_${ticket.plateNumber}_${ticket.id}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        file
    }

    suspend fun generateReportPdf(
        context: Context,
        reportTitle: String,
        dateRangeText: String,
        tickets: List<ParkingTicket>,
        config: RateConfig
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // Standard A4 width (points)
        val pageHeight = 842 // Standard A4 height (points)
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Header Background Banner
        val headerPaint = Paint().apply {
            color = Color.rgb(15, 30, 65)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, headerPaint)

        // Title & Business Header
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(config.businessName, 30f, 38f, titlePaint)

        val subTitlePaint = Paint().apply {
            color = Color.rgb(210, 225, 255)
            textSize = 10f
            isAntiAlias = true
        }
        canvas.drawText("${config.businessNitOrPhone} | ${config.businessAddress}", 30f, 56f, subTitlePaint)
        canvas.drawText("Reporte: $reportTitle - $dateRangeText", 30f, 74f, subTitlePaint)

        val genDatePaint = Paint().apply {
            color = Color.rgb(200, 215, 240)
            textSize = 9f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("Generado: ${Formatters.formatDateTime(System.currentTimeMillis())}", pageWidth - 30f, 74f, genDatePaint)

        // Summary Calculations
        val paidTickets = tickets.filter { it.status == "PAID_EXIT" }
        val activeTickets = tickets.filter { it.status == "ACTIVE" }
        val totalRevenue = paidTickets.sumOf { it.finalAmountPaid }
        val cashRevenue = paidTickets.filter { it.paymentMethod == "EFECTIVO" }.sumOf { it.finalAmountPaid }
        val digitalRevenue = totalRevenue - cashRevenue
        val totalMotos = tickets.count { it.vehicleType.equals("MOTO", ignoreCase = true) }
        val totalCarros = tickets.count { it.vehicleType.equals("CARRO", ignoreCase = true) }
        val totalHelmets = tickets.count { it.hasHelmet }

        // KPI Summary Cards
        var startY = 110f
        val kpiCardBg = Paint().apply {
            color = Color.rgb(245, 248, 253)
            style = Paint.Style.FILL
        }
        val kpiBorder = Paint().apply {
            color = Color.rgb(215, 225, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        fun drawKpiCard(x: Float, y: Float, width: Float, height: Float, title: String, value: String, valueColor: Int) {
            val rect = RectF(x, y, x + width, y + height)
            canvas.drawRoundRect(rect, 6f, 6f, kpiCardBg)
            canvas.drawRoundRect(rect, 6f, 6f, kpiBorder)

            val kpiTitlePaint = Paint().apply {
                color = Color.rgb(100, 115, 130)
                textSize = 9f
                isAntiAlias = true
            }
            val kpiValPaint = Paint().apply {
                color = valueColor
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(title, x + 10f, y + 20f, kpiTitlePaint)
            canvas.drawText(value, x + 10f, y + 42f, kpiValPaint)
        }

        val cardW = 122f
        val cardH = 55f
        drawKpiCard(30f, startY, cardW, cardH, "TOTAL INGRESOS", Formatters.formatCurrency(totalRevenue), Color.rgb(20, 120, 50))
        drawKpiCard(162f, startY, cardW, cardH, "EFECTIVO", Formatters.formatCurrency(cashRevenue), Color.rgb(25, 60, 140))
        drawKpiCard(294f, startY, cardW, cardH, "TRANSFERENCIA / OTRO", Formatters.formatCurrency(digitalRevenue), Color.rgb(140, 60, 20))
        drawKpiCard(426f, startY, cardW, cardH, "TOTAL VEHÍCULOS", "${tickets.size} ($totalMotos motos)", Color.rgb(20, 30, 50))

        startY += 75f

        // Table Header
        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(30, 50, 90)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(30f, startY, pageWidth - 30f, startY + 24f), 4f, 4f, tableHeaderPaint)

        val thTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawText("TICKET", 38f, startY + 16f, thTextPaint)
        canvas.drawText("PLACA", 90f, startY + 16f, thTextPaint)
        canvas.drawText("TIPO", 160f, startY + 16f, thTextPaint)
        canvas.drawText("ENTRADA", 215f, startY + 16f, thTextPaint)
        canvas.drawText("SALIDA / TIEMPO", 310f, startY + 16f, thTextPaint)
        canvas.drawText("CASCO", 430f, startY + 16f, thTextPaint)
        canvas.drawText("TOTAL", 500f, startY + 16f, thTextPaint)

        startY += 26f

        // Table Rows
        val rowBgAlt = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        val rowDivider = Paint().apply {
            color = Color.rgb(230, 235, 242)
            strokeWidth = 1f
        }
        val tdPaint = Paint().apply {
            color = Color.rgb(30, 40, 50)
            textSize = 8.5f
            isAntiAlias = true
        }
        val tdBold = Paint().apply {
            color = Color.rgb(15, 25, 45)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val statusActivePaint = Paint().apply {
            color = Color.rgb(190, 80, 10)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val rowHeight = 22f
        val maxRows = 24
        tickets.take(maxRows).forEachIndexed { index, ticket ->
            val rowY = startY + (index * rowHeight)
            if (index % 2 == 1) {
                canvas.drawRect(30f, rowY, pageWidth - 30f, rowY + rowHeight, rowBgAlt)
            }
            canvas.drawLine(30f, rowY + rowHeight, pageWidth - 30f, rowY + rowHeight, rowDivider)

            val textY = rowY + 15f
            canvas.drawText("#${ticket.id}", 38f, textY, tdPaint)
            canvas.drawText(ticket.plateNumber, 90f, textY, tdBold)
            canvas.drawText(ticket.vehicleType.take(8), 160f, textY, tdPaint)
            canvas.drawText(Formatters.formatTimeOnly(ticket.entryTimeMillis), 215f, textY, tdPaint)

            if (ticket.status == "PAID_EXIT" && ticket.exitTimeMillis != null) {
                val dur = Formatters.formatDuration(ticket.entryTimeMillis, ticket.exitTimeMillis)
                canvas.drawText(dur, 310f, textY, tdPaint)
            } else {
                canvas.drawText("EN PARQUEO", 310f, textY, statusActivePaint)
            }

            val helmetStr = if (ticket.hasHelmet) "SI (${ticket.helmetCount})" else "NO"
            canvas.drawText(helmetStr, 430f, textY, tdPaint)

            val amountStr = if (ticket.status == "PAID_EXIT") Formatters.formatCurrency(ticket.finalAmountPaid) else "Pendiente"
            canvas.drawText(amountStr, 500f, textY, tdBold)
        }

        // Footer note
        val footerY = pageHeight - 35f
        canvas.drawLine(30f, footerY - 10f, pageWidth - 30f, footerY - 10f, rowDivider)
        val footerPaint = Paint().apply {
            color = Color.rgb(120, 130, 145)
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(
            "Software de Parqueadero Offline MotoPark | Registros mostrados: ${tickets.take(maxRows).size} de ${tickets.size}",
            pageWidth / 2f,
            footerY + 5f,
            footerPaint
        )

        pdfDocument.finishPage(page)

        // Save File
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val file = File(reportsDir, "Reporte_MotoPark_${Formatters.formatFileTimestamp()}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        file
    }

    fun sharePdfFile(context: Context, file: File, chooserTitle: String = "Compartir Documento PDF") {
        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareTicketAsText(context: Context, ticket: ParkingTicket, config: RateConfig) {
        try {
            val helmetText = if (ticket.hasHelmet) "SÍ (${ticket.helmetCount} casco(s), Casillero: ${ticket.helmetLockerNumber})" else "NO"
            val text = buildString {
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("🅿️ ${config.businessName.uppercase()}")
                appendLine("${config.businessNitOrPhone}")
                appendLine("${config.businessAddress}")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("TICKET #${ticket.id.toString().padStart(6, '0')}")
                appendLine("PLACA: ${ticket.plateNumber.uppercase()}")
                appendLine("VEHÍCULO: ${ticket.vehicleType}")
                if (ticket.clientName.isNotBlank()) appendLine("CLIENTE: ${ticket.clientName}")
                if (ticket.clientPhone.isNotBlank()) appendLine("TELÉFONO: ${ticket.clientPhone}")
                appendLine("INGRESO: ${Formatters.formatDateTime(ticket.entryTimeMillis)}")
                if (ticket.exitTimeMillis != null) {
                    appendLine("SALIDA:  ${Formatters.formatDateTime(ticket.exitTimeMillis)}")
                    appendLine("TIEMPO:  ${Formatters.formatDuration(ticket.entryTimeMillis, ticket.exitTimeMillis)}")
                    appendLine("TOTAL PAGADO: ${Formatters.formatCurrency(ticket.finalAmountPaid)}")
                    appendLine("MÉTODO: ${ticket.paymentMethod}")
                }
                appendLine("DEJA CASCO: $helmetText")
                if (!ticket.ocrExtractedText.isNullOrBlank()) {
                    appendLine("OCR/DETALLE: ${ticket.ocrExtractedText}")
                }
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine(config.businessTerms)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, "Ticket MotoPark #${ticket.id} - ${ticket.plateNumber}")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir Ticket de Parqueadero"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

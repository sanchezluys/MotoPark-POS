package com.example.util

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.data.model.ParkingTicket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone

object CalendarHelper {

    fun hasCalendarPermissions(context: Context): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    suspend fun addParkingEventToCalendar(
        context: Context,
        ticket: ParkingTicket
    ): Long? = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions(context)) {
            return@withContext null
        }

        try {
            // Find a valid primary calendar ID
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
            )
            val uri: Uri = CalendarContract.Calendars.CONTENT_URI
            var calendarId: Long = 1

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    calendarId = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                }
            }

            val startMillis = ticket.entryTimeMillis
            val endMillis = startMillis + (2 * 60 * 60 * 1000) // Default 2h block or reminder

            val helmetText = if (ticket.hasHelmet) "SI (${ticket.helmetCount} casco(s), Casillero: ${ticket.helmetLockerNumber})" else "NO"
            val description = buildString {
                appendLine("=== INGRESO PARQUEADERO MOTOPARK ===")
                appendLine("Ticket #: ${ticket.id}")
                appendLine("Placa: ${ticket.plateNumber}")
                appendLine("Tipo: ${ticket.vehicleType}")
                appendLine("Cliente: ${ticket.clientName.ifBlank { "Sin nombre" }}")
                appendLine("Teléfono: ${ticket.clientPhone.ifBlank { "N/A" }}")
                appendLine("Deja Casco: $helmetText")
                if (!ticket.ocrExtractedText.isNullOrBlank()) {
                    appendLine("OCR/Detalles: ${ticket.ocrExtractedText}")
                }
                appendLine("Hora Ingreso: ${Formatters.formatDateTime(startMillis)}")
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, "🅿️ MotoPark: ${ticket.plateNumber} (${ticket.vehicleType})")
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val eventUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = eventUri?.lastPathSegment?.toLongOrNull()

            // Add a reminder 15 minutes before or notification
            if (eventId != null) {
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, 30)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }

            eventId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun openCalendarApp(context: Context, eventTimeMillis: Long = System.currentTimeMillis()) {
        try {
            val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
            ContentUris.appendId(builder, eventTimeMillis)
            val intent = Intent(Intent.ACTION_VIEW).setData(builder.build())
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

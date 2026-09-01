package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ParkingTicket
import com.example.data.model.RateConfig
import com.example.data.model.RegisteredCustomer
import com.example.data.repository.ParkingRepository
import com.example.util.CalendarHelper
import com.example.util.FeeCalculationResult
import com.example.util.FeeCalculator
import com.example.util.Formatters
import com.example.util.OcrExtractionResult
import com.example.util.OcrHelper
import com.example.util.PdfExportHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DateFilter(val label: String) {
    TODAY("Hoy"),
    YESTERDAY("Ayer"),
    THIS_WEEK("Esta Semana"),
    THIS_MONTH("Este Mes"),
    ALL("Todo el Historial")
}

class ParkingViewModel(
    private val repository: ParkingRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _dateFilter = MutableStateFlow(DateFilter.TODAY)
    val dateFilter: StateFlow<DateFilter> = _dateFilter.asStateFlow()

    private val _rateConfig = MutableStateFlow(RateConfig())
    val rateConfig: StateFlow<RateConfig> = _rateConfig.asStateFlow()

    private val _isOcrLoading = MutableStateFlow(false)
    val isOcrLoading: StateFlow<Boolean> = _isOcrLoading.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent = _snackbarEvent.asSharedFlow()

    private val _selectedTicketForExit = MutableStateFlow<ParkingTicket?>(null)
    val selectedTicketForExit: StateFlow<ParkingTicket?> = _selectedTicketForExit.asStateFlow()

    private val _selectedTicketForReceipt = MutableStateFlow<ParkingTicket?>(null)
    val selectedTicketForReceipt: StateFlow<ParkingTicket?> = _selectedTicketForReceipt.asStateFlow()

    // Observe active tickets filtered by search query
    val activeTickets: StateFlow<List<ParkingTicket>> = repository.activeTickets
        .combine(_searchQuery) { tickets, query ->
            if (query.isBlank()) tickets
            else tickets.filter {
                it.plateNumber.contains(query, ignoreCase = true) ||
                        it.clientName.contains(query, ignoreCase = true) ||
                        it.vehicleType.contains(query, ignoreCase = true)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeCount: StateFlow<Int> = repository.activeCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Filtered history tickets
    val filteredHistoryTickets: StateFlow<List<ParkingTicket>> = combine(
        repository.allTickets,
        _dateFilter,
        _searchQuery
    ) { allTickets, filter, query ->
        val now = Calendar.getInstance()

        val filteredByDate = when (filter) {
            DateFilter.TODAY -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                allTickets.filter { it.entryTimeMillis >= start }
            }
            DateFilter.YESTERDAY -> {
                val startYesterday = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val endYesterday = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                allTickets.filter { it.entryTimeMillis in startYesterday..endYesterday }
            }
            DateFilter.THIS_WEEK -> {
                val startWeek = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                allTickets.filter { it.entryTimeMillis >= startWeek }
            }
            DateFilter.THIS_MONTH -> {
                val startMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                allTickets.filter { it.entryTimeMillis >= startMonth }
            }
            DateFilter.ALL -> allTickets
        }

        if (query.isBlank()) filteredByDate
        else filteredByDate.filter {
            it.plateNumber.contains(query, ignoreCase = true) ||
                    it.clientName.contains(query, ignoreCase = true) ||
                    it.vehicleType.contains(query, ignoreCase = true)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.rateConfig.collect { config ->
                if (config != null) {
                    _rateConfig.value = config
                    Formatters.activeConfig = config
                }
            }
        }
    }

    suspend fun getActiveTicketByPlate(plate: String): ParkingTicket? {
        val cleanPlate = plate.trim().uppercase()
        return repository.findActiveTicketByPlate(cleanPlate)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDateFilter(filter: DateFilter) {
        _dateFilter.value = filter
    }

    fun selectTicketForExit(ticket: ParkingTicket?) {
        _selectedTicketForExit.value = ticket
    }

    fun selectTicketForReceipt(ticket: ParkingTicket?) {
        _selectedTicketForReceipt.value = ticket
    }

    fun calculateFeeForTicket(ticket: ParkingTicket, discount: Double = 0.0): FeeCalculationResult {
        return FeeCalculator.calculateFee(
            ticket = ticket,
            config = _rateConfig.value,
            customDiscount = discount
        )
    }

    fun registerNewEntry(
        context: Context,
        plate: String,
        vehicleType: String,
        clientName: String,
        clientPhone: String,
        hasHelmet: Boolean,
        helmetCount: Int,
        helmetLocker: String,
        motoPhotoPath: String?,
        platePhotoPath: String?,
        helmetPhotoPath: String?,
        ocrNotes: String?,
        syncToCalendar: Boolean,
        onSuccess: (ParkingTicket) -> Unit
    ) {
        viewModelScope.launch {
            val cleanPlate = plate.trim().uppercase()
            if (cleanPlate.isBlank()) {
                _snackbarEvent.emit("Por favor ingresa o escanea la placa del vehículo.")
                return@launch
            }

            // Check if already in parking
            val existing = repository.findActiveTicketByPlate(cleanPlate)
            if (existing != null) {
                _snackbarEvent.emit("¡El vehículo con placa $cleanPlate ya está registrado como ACTIVO en el parqueadero!")
                return@launch
            }

            val newTicket = ParkingTicket(
                plateNumber = cleanPlate,
                vehicleType = vehicleType.uppercase(),
                clientName = clientName.trim(),
                clientPhone = clientPhone.trim(),
                entryTimeMillis = System.currentTimeMillis(),
                hasHelmet = hasHelmet,
                helmetCount = if (hasHelmet) helmetCount.coerceAtLeast(1) else 0,
                helmetLockerNumber = helmetLocker.trim(),
                motoPhotoPath = motoPhotoPath,
                platePhotoPath = platePhotoPath,
                helmetPhotoPath = helmetPhotoPath,
                ocrExtractedText = ocrNotes?.trim(),
                status = "ACTIVE",
                syncedToCalendar = syncToCalendar
            )

            val insertedId = repository.insertTicket(newTicket)
            val savedTicket = newTicket.copy(id = insertedId)

            // Save or update customer record for recurring visitors
            val existingCustomer = repository.getCustomerByPlateSync(cleanPlate)
            if (existingCustomer != null) {
                repository.saveCustomer(
                    existingCustomer.copy(
                        clientName = if (clientName.isNotBlank()) clientName else existingCustomer.clientName,
                        clientPhone = if (clientPhone.isNotBlank()) clientPhone else existingCustomer.clientPhone,
                        motoPhotoPath = motoPhotoPath ?: existingCustomer.motoPhotoPath,
                        platePhotoPath = platePhotoPath ?: existingCustomer.platePhotoPath,
                        helmetPhotoPath = helmetPhotoPath ?: existingCustomer.helmetPhotoPath,
                        ocrExtractedText = ocrNotes ?: existingCustomer.ocrExtractedText,
                        totalVisits = existingCustomer.totalVisits + 1
                    )
                )
            } else {
                repository.saveCustomer(
                    RegisteredCustomer(
                        plateNumber = cleanPlate,
                        clientName = clientName.trim(),
                        clientPhone = clientPhone.trim(),
                        vehicleType = vehicleType.uppercase(),
                        motoPhotoPath = motoPhotoPath,
                        platePhotoPath = platePhotoPath,
                        helmetPhotoPath = helmetPhotoPath,
                        ocrExtractedText = ocrNotes?.trim(),
                        totalVisits = 1
                    )
                )
            }

            // Sync with Calendar if requested
            if (syncToCalendar) {
                val eventId = CalendarHelper.addParkingEventToCalendar(context, savedTicket)
                if (eventId != null) {
                    repository.updateTicket(savedTicket.copy(calendarEventId = eventId, syncedToCalendar = true))
                }
            }

            _snackbarEvent.emit("✅ Ingreso registrado exitosamente: $cleanPlate")
            onSuccess(savedTicket)
        }
    }

    fun completeExitAndPayment(
        ticket: ParkingTicket,
        paymentMethod: String,
        discount: Double,
        onComplete: (ParkingTicket) -> Unit
    ) {
        viewModelScope.launch {
            val exitTime = System.currentTimeMillis()
            val feeResult = FeeCalculator.calculateFee(
                ticket = ticket,
                config = _rateConfig.value,
                exitTimeMillis = exitTime,
                customDiscount = discount
            )

            val updatedTicket = ticket.copy(
                exitTimeMillis = exitTime,
                calculatedAmount = feeResult.subtotal,
                discount = feeResult.discount,
                finalAmountPaid = feeResult.totalAmount,
                paymentMethod = paymentMethod,
                status = "PAID_EXIT"
            )

            repository.updateTicket(updatedTicket)
            _selectedTicketForExit.value = null
            _selectedTicketForReceipt.value = updatedTicket
            _snackbarEvent.emit("✅ Salida registrada: Total ${Formatters.formatCurrency(feeResult.totalAmount)}")
            onComplete(updatedTicket)
        }
    }

    fun cancelTicket(ticket: ParkingTicket) {
        viewModelScope.launch {
            repository.updateTicket(ticket.copy(status = "CANCELLED"))
            _snackbarEvent.emit("Ticket #${ticket.id} cancelado.")
        }
    }

    fun processOcrFromFile(context: Context, filePath: String, onResult: (OcrExtractionResult) -> Unit) {
        viewModelScope.launch {
            _isOcrLoading.value = true
            val result = OcrHelper.recognizeTextFromFile(context, filePath)
            _isOcrLoading.value = false
            onResult(result)
        }
    }

    fun processOcrFromBitmap(bitmap: Bitmap, onResult: (OcrExtractionResult) -> Unit) {
        viewModelScope.launch {
            _isOcrLoading.value = true
            val result = OcrHelper.recognizeTextFromBitmap(bitmap)
            _isOcrLoading.value = false
            onResult(result)
        }
    }

    fun updateRateConfig(newConfig: RateConfig) {
        viewModelScope.launch {
            repository.saveRateConfig(newConfig)
            _rateConfig.value = newConfig
            _snackbarEvent.emit("✅ Configuración de tarifas guardada.")
        }
    }

    fun shareTicketPdf(context: Context, ticket: ParkingTicket) {
        viewModelScope.launch {
            try {
                val pdfFile = PdfExportHelper.generateTicketPdf(context, ticket, _rateConfig.value)
                PdfExportHelper.sharePdfFile(context, pdfFile, "Compartir Ticket #${ticket.id}")
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarEvent.emit("Error al generar PDF del ticket: ${e.localizedMessage}")
            }
        }
    }

    fun shareTicketText(context: Context, ticket: ParkingTicket) {
        PdfExportHelper.shareTicketAsText(context, ticket, _rateConfig.value)
    }

    fun exportHistoryPdfReport(context: Context) {
        viewModelScope.launch {
            try {
                val tickets = filteredHistoryTickets.value
                val filter = _dateFilter.value
                val pdfFile = PdfExportHelper.generateReportPdf(
                    context = context,
                    reportTitle = "Control de Parqueadero - ${filter.label}",
                    dateRangeText = Formatters.formatDateOnly(System.currentTimeMillis()),
                    tickets = tickets,
                    config = _rateConfig.value
                )
                PdfExportHelper.sharePdfFile(context, pdfFile, "Exportar Reporte de Parqueadero")
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarEvent.emit("Error al exportar reporte PDF: ${e.localizedMessage}")
            }
        }
    }

    suspend fun findCustomerByPlate(plate: String): RegisteredCustomer? {
        return repository.getCustomerByPlateSync(plate.trim().uppercase())
    }
}

class ParkingViewModelFactory(
    private val repository: ParkingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ParkingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ParkingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

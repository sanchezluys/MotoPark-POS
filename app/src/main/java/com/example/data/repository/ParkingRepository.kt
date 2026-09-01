package com.example.data.repository

import com.example.data.dao.ParkingTicketDao
import com.example.data.dao.RateConfigDao
import com.example.data.dao.RegisteredCustomerDao
import com.example.data.model.ParkingTicket
import com.example.data.model.RateConfig
import com.example.data.model.RegisteredCustomer
import kotlinx.coroutines.flow.Flow

class ParkingRepository(
    private val ticketDao: ParkingTicketDao,
    private val customerDao: RegisteredCustomerDao,
    private val rateConfigDao: RateConfigDao
) {
    constructor(database: com.example.data.AppDatabase) : this(
        database.parkingTicketDao(),
        database.registeredCustomerDao(),
        database.rateConfigDao()
    )
    val activeTickets: Flow<List<ParkingTicket>> = ticketDao.getActiveTickets()
    val allTickets: Flow<List<ParkingTicket>> = ticketDao.getAllTickets()
    val activeCount: Flow<Int> = ticketDao.getActiveCount()
    val rateConfig: Flow<RateConfig?> = rateConfigDao.getRateConfig()
    val allCustomers: Flow<List<RegisteredCustomer>> = customerDao.getAllCustomers()

    fun getTicketById(id: Long): Flow<ParkingTicket?> = ticketDao.getTicketById(id)

    suspend fun getTicketByIdSync(id: Long): ParkingTicket? = ticketDao.getTicketByIdSync(id)

    fun getTicketsBetween(start: Long, end: Long): Flow<List<ParkingTicket>> =
        ticketDao.getTicketsBetween(start, end)

    fun getPaidTicketsBetween(start: Long, end: Long): Flow<List<ParkingTicket>> =
        ticketDao.getPaidTicketsBetween(start, end)

    fun searchTickets(query: String): Flow<List<ParkingTicket>> = ticketDao.searchTickets(query)

    suspend fun findActiveTicketByPlate(plate: String): ParkingTicket? =
        ticketDao.findActiveTicketByPlate(plate)

    suspend fun insertTicket(ticket: ParkingTicket): Long = ticketDao.insertTicket(ticket)

    suspend fun updateTicket(ticket: ParkingTicket) = ticketDao.updateTicket(ticket)

    suspend fun deleteTicket(ticket: ParkingTicket) = ticketDao.deleteTicket(ticket)

    fun getCustomerByPlate(plate: String): Flow<RegisteredCustomer?> =
        customerDao.getCustomerByPlate(plate)

    suspend fun getCustomerByPlateSync(plate: String): RegisteredCustomer? =
        customerDao.getCustomerByPlateSync(plate)

    suspend fun saveCustomer(customer: RegisteredCustomer): Long =
        customerDao.insertOrUpdateCustomer(customer)

    suspend fun getRateConfigSync(): RateConfig =
        rateConfigDao.getRateConfigSync() ?: RateConfig()

    suspend fun saveRateConfig(config: RateConfig) =
        rateConfigDao.insertOrUpdate(config)
}

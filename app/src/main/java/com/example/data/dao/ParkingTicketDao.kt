package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ParkingTicket
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingTicketDao {
    @Query("SELECT * FROM parking_tickets WHERE status = 'ACTIVE' ORDER BY entryTimeMillis DESC")
    fun getActiveTickets(): Flow<List<ParkingTicket>>

    @Query("SELECT * FROM parking_tickets WHERE id = :id")
    fun getTicketById(id: Long): Flow<ParkingTicket?>

    @Query("SELECT * FROM parking_tickets WHERE id = :id")
    suspend fun getTicketByIdSync(id: Long): ParkingTicket?

    @Query("SELECT * FROM parking_tickets ORDER BY entryTimeMillis DESC")
    fun getAllTickets(): Flow<List<ParkingTicket>>

    @Query("SELECT * FROM parking_tickets WHERE entryTimeMillis >= :startMillis AND entryTimeMillis <= :endMillis ORDER BY entryTimeMillis DESC")
    fun getTicketsBetween(startMillis: Long, endMillis: Long): Flow<List<ParkingTicket>>

    @Query("SELECT * FROM parking_tickets WHERE status = 'PAID_EXIT' AND exitTimeMillis >= :startMillis AND exitTimeMillis <= :endMillis ORDER BY exitTimeMillis DESC")
    fun getPaidTicketsBetween(startMillis: Long, endMillis: Long): Flow<List<ParkingTicket>>

    @Query("SELECT * FROM parking_tickets WHERE plateNumber LIKE '%' || :query || '%' OR clientName LIKE '%' || :query || '%' ORDER BY entryTimeMillis DESC")
    fun searchTickets(query: String): Flow<List<ParkingTicket>>

    @Query("SELECT COUNT(*) FROM parking_tickets WHERE status = 'ACTIVE'")
    fun getActiveCount(): Flow<Int>

    @Query("SELECT * FROM parking_tickets WHERE plateNumber = :plate AND status = 'ACTIVE' LIMIT 1")
    suspend fun findActiveTicketByPlate(plate: String): ParkingTicket?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: ParkingTicket): Long

    @Update
    suspend fun updateTicket(ticket: ParkingTicket)

    @Delete
    suspend fun deleteTicket(ticket: ParkingTicket)
}

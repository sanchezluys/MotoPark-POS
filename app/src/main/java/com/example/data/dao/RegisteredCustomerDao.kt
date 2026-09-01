package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RegisteredCustomer
import kotlinx.coroutines.flow.Flow

@Dao
interface RegisteredCustomerDao {
    @Query("SELECT * FROM registered_customers ORDER BY totalVisits DESC, createdAt DESC")
    fun getAllCustomers(): Flow<List<RegisteredCustomer>>

    @Query("SELECT * FROM registered_customers WHERE plateNumber = :plate LIMIT 1")
    fun getCustomerByPlate(plate: String): Flow<RegisteredCustomer?>

    @Query("SELECT * FROM registered_customers WHERE plateNumber = :plate LIMIT 1")
    suspend fun getCustomerByPlateSync(plate: String): RegisteredCustomer?

    @Query("SELECT * FROM registered_customers WHERE plateNumber LIKE '%' || :query || '%' OR clientName LIKE '%' || :query || '%' ORDER BY totalVisits DESC")
    fun searchCustomers(query: String): Flow<List<RegisteredCustomer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCustomer(customer: RegisteredCustomer): Long

    @Update
    suspend fun updateCustomer(customer: RegisteredCustomer)

    @Delete
    suspend fun deleteCustomer(customer: RegisteredCustomer)
}

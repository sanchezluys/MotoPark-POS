package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.RateConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface RateConfigDao {
    @Query("SELECT * FROM rate_config WHERE id = 1 LIMIT 1")
    fun getRateConfig(): Flow<RateConfig?>

    @Query("SELECT * FROM rate_config WHERE id = 1 LIMIT 1")
    suspend fun getRateConfigSync(): RateConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rateConfig: RateConfig)
}

package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ParkingTicketDao
import com.example.data.dao.RateConfigDao
import com.example.data.dao.RegisteredCustomerDao
import com.example.data.model.ParkingTicket
import com.example.data.model.RateConfig
import com.example.data.model.RegisteredCustomer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ParkingTicket::class,
        RegisteredCustomer::class,
        RateConfig::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun parkingTicketDao(): ParkingTicketDao
    abstract fun registeredCustomerDao(): RegisteredCustomerDao
    abstract fun rateConfigDao(): RateConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "motopark_database.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initialize default rate config
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.rateConfigDao()?.insertOrUpdate(RateConfig())
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

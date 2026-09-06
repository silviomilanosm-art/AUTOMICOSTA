package com.automicosta.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VehicleEntity::class, ExpenseEntity::class, ReminderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AutoMiCostaDatabase : RoomDatabase() {
    abstract fun dao(): AutoMiCostaDao

    companion object {
        @Volatile private var INSTANCE: AutoMiCostaDatabase? = null

        fun get(context: Context): AutoMiCostaDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AutoMiCostaDatabase::class.java,
                "automicosta.db"
            ).build().also { INSTANCE = it }
        }
    }
}

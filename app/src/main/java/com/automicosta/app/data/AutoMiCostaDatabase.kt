package com.automicosta.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [VehicleEntity::class, ExpenseEntity::class, ReminderEntity::class, MaintenanceEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AutoMiCostaDatabase : RoomDatabase() {
    abstract fun dao(): AutoMiCostaDao

    companion object {
        @Volatile private var INSTANCE: AutoMiCostaDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vehicles ADD COLUMN vin TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN registrationYear INTEGER")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN firstRegistrationDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN purchaseDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN tireCount INTEGER NOT NULL DEFAULT 4")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN frontTireSize TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN rearTireSize TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN engineDisplacementCc INTEGER")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN powerKw REAL")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN bodyType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN color TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN countryOfOrigin TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS maintenance (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        vehicleId INTEGER NOT NULL,
                        dateEpochDay INTEGER NOT NULL,
                        component TEXT NOT NULL,
                        workType TEXT NOT NULL,
                        cost REAL NOT NULL,
                        odometerKm INTEGER,
                        workshop TEXT NOT NULL,
                        partBrand TEXT NOT NULL,
                        partCode TEXT NOT NULL,
                        nextDueEpochDay INTEGER,
                        nextDueKm INTEGER,
                        note TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun get(context: Context): AutoMiCostaDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AutoMiCostaDatabase::class.java,
                "automicosta.db"
            ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}

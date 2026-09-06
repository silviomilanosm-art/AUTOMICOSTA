package com.automicosta.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String,
    val brand: String = "",
    val model: String = "",
    val plate: String = "",
    val fuelType: String = "Benzina",
    val currentKm: Int = 0,
    val purchasePrice: Double? = null
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateEpochDay: Long,
    val category: String,
    val amount: Double,
    val odometerKm: Int? = null,
    val litersOrKwh: Double? = null,
    val unitPrice: Double? = null,
    val note: String = ""
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val title: String,
    val dueEpochDay: Long? = null,
    val dueKm: Int? = null,
    val completed: Boolean = false
)

val defaultCategories = listOf(
    "Carburante", "Ricarica", "Manutenzione", "Assicurazione", "Bollo",
    "Revisione", "Pneumatici", "Pedaggi", "Parcheggi", "Lavaggio", "Altro"
)

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
    val vin: String = "",
    val registrationYear: Int? = null,
    val firstRegistrationDate: String = "",
    val fuelType: String = "Benzina",
    val currentKm: Int = 0,
    val purchaseDate: String = "",
    val purchasePrice: Double? = null,
    val tireCount: Int = 4,
    val frontTireSize: String = "",
    val rearTireSize: String = "",
    val engineDisplacementCc: Int? = null,
    val powerKw: Double? = null,
    val bodyType: String = "",
    val color: String = "",
    val countryOfOrigin: String = "",
    val notes: String = ""
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

@Entity(tableName = "maintenance")
data class MaintenanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateEpochDay: Long,
    val component: String,
    val workType: String = "Sostituzione",
    val cost: Double = 0.0,
    val odometerKm: Int? = null,
    val workshop: String = "",
    val partBrand: String = "",
    val partCode: String = "",
    val nextDueEpochDay: Long? = null,
    val nextDueKm: Int? = null,
    val note: String = ""
)

val defaultCategories = listOf(
    "Carburante", "Ricarica", "Manutenzione", "Assicurazione", "Bollo",
    "Revisione", "Pneumatici", "Pedaggi", "Parcheggi", "Lavaggio", "Altro"
)

val maintenanceComponents = listOf(
    "Tagliando", "Olio motore", "Filtro olio", "Filtro aria", "Filtro abitacolo",
    "Pastiglie freni", "Dischi freni", "Alternatore", "Batteria", "Cinghia distribuzione",
    "Catena distribuzione", "Frizione", "Ammortizzatori", "Pneumatici", "Candele",
    "Liquido refrigerante", "Cambio", "Tergicristalli", "Lampadine", "Altro"
)

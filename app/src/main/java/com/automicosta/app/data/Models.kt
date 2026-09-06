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
    "Revisione periodica", "Tagliando completo",
    "Olio motore", "Filtro olio", "Filtro aria motore", "Filtro carburante", "Filtro abitacolo/antipolline",
    "Candele", "Candelette diesel", "Bobine accensione", "Iniettori", "Pompa carburante", "Pompa alta pressione",
    "Cinghia distribuzione", "Kit distribuzione", "Catena distribuzione", "Tendicatena", "Cinghia servizi", "Tendicinghia servizi",
    "Pompa acqua", "Termostato", "Radiatore", "Ventola radiatore", "Vaso espansione", "Liquido refrigerante",
    "Turbina/turbocompressore", "Intercooler", "Valvola EGR", "Filtro antiparticolato DPF/FAP", "Catalizzatore", "Sonda lambda",
    "Marmitta/silenziatore", "Collettore aspirazione", "Collettore scarico", "Supporti motore", "Guarnizione testata",
    "Frizione", "Volano bimassa", "Cambio manuale", "Cambio automatico", "Olio cambio", "Differenziale", "Semiasse", "Giunto omocinetico",
    "Pastiglie freni anteriori", "Pastiglie freni posteriori", "Dischi freni anteriori", "Dischi freni posteriori", "Ganasce freno", "Tamburi freno",
    "Pinze freno", "Tubi freno", "Pompa freno", "Servofreno", "Liquido freni", "Freno di stazionamento/cavo freno a mano",
    "Ammortizzatori anteriori", "Ammortizzatori posteriori", "Molle sospensione", "Bracci oscillanti", "Testine sterzo", "Tiranti sterzo",
    "Biellette barra stabilizzatrice", "Silent block", "Cuscinetti ruota", "Mozzi ruota", "Scatola sterzo", "Pompa servosterzo",
    "Pneumatici anteriori", "Pneumatici posteriori", "Set pneumatici", "Equilibratura ruote", "Convergenza/geometria", "Cerchio ruota",
    "Batteria 12V", "Batteria trazione HV", "Alternatore", "Motorino avviamento", "Regolatore tensione", "Fusibili", "Relè",
    "Cablaggio elettrico", "Sensore ABS", "Sensore pressione pneumatici TPMS", "Sensore temperatura", "Sensore parcheggio", "Telecamera retromarcia",
    "Compressore climatizzatore", "Condensatore climatizzatore", "Evaporatore climatizzatore", "Gas climatizzatore", "Ventola abitacolo",
    "Tergicristallo anteriore", "Tergicristallo posteriore", "Motorino tergicristalli", "Pompa lavavetri", "Ugelli lavavetri",
    "Lampadina anabbagliante H1", "Lampadina anabbagliante H4", "Lampadina anabbagliante H7", "Lampadina anabbagliante H11",
    "Lampadina abbagliante H1", "Lampadina abbagliante H4", "Lampadina abbagliante H7", "Lampadina HB3/9005", "Lampadina HB4/9006",
    "Lampadina xenon D1S", "Lampadina xenon D2S", "Lampadina xenon D3S", "Lampadina xenon D4S", "Modulo/faro LED",
    "Lampadina fendinebbia", "Lampadina posizione W5W/T10", "Lampadina freccia PY21W", "Lampadina freccia WY5W",
    "Lampadina stop P21W", "Lampadina stop/posizione P21/5W", "Lampadina retromarcia W16W", "Lampadina retronebbia P21W",
    "Lampadina targa C5W", "Lampadina targa W5W", "Lampadina plafoniera C5W", "Lampadina abitacolo W5W",
    "Faro anteriore completo", "Fanale posteriore completo", "Terzo stop", "Indicatore di direzione/specchietto",
    "Parabrezza", "Lunotto", "Vetro laterale", "Specchietto retrovisore", "Serratura porta", "Alzacristallo", "Maniglia porta",
    "Cofano", "Portellone", "Paraurti anteriore", "Paraurti posteriore", "Parafango", "Sottoscocca/protezione motore",
    "Chiave/telecomando", "Batteria telecomando", "Centralina motore ECU", "Centralina ABS", "Centralina airbag", "Airbag",
    "Altro componente"
)

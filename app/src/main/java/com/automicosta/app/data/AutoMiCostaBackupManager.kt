package com.automicosta.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

object AutoMiCostaBackupManager {
    private const val FORMAT = "AUTOMICOSTA_BACKUP"
    private const val VERSION = 1

    suspend fun exportBackup(context: Context, uri: Uri) {
        val dao = AutoMiCostaDatabase.get(context).dao()
        val root = JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("createdAt", System.currentTimeMillis())
            put("vehicles", JSONArray().apply { dao.getAllVehicles().forEach { put(vehicleToJson(it)) } })
            put("expenses", JSONArray().apply { dao.getAllExpenses().forEach { put(expenseToJson(it)) } })
            put("reminders", JSONArray().apply { dao.getAllReminders().forEach { put(reminderToJson(it)) } })
            put("maintenance", JSONArray().apply { dao.getAllMaintenance().forEach { put(maintenanceToJson(it)) } })
            val pinHash = context.getSharedPreferences("automicosta_security", Context.MODE_PRIVATE)
                .getString("pin_hash", null)
            if (pinHash != null) put("pinHash", pinHash)
        }
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
            writer.write(root.toString(2))
        } ?: error("Impossibile creare il file di backup")
    }

    suspend fun importBackup(context: Context, uri: Uri): BackupSummary {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Impossibile leggere il file di backup")
        val root = JSONObject(text)
        require(root.optString("format") == FORMAT) { "Il file selezionato non è un backup AUTOMICOSTA valido" }
        require(root.optInt("version", 0) in 1..VERSION) { "Versione backup non supportata" }

        val vehicles = parseArray(root.optJSONArray("vehicles")) { jsonToVehicle(it) }
        val expenses = parseArray(root.optJSONArray("expenses")) { jsonToExpense(it) }
        val reminders = parseArray(root.optJSONArray("reminders")) { jsonToReminder(it) }
        val maintenance = parseArray(root.optJSONArray("maintenance")) { jsonToMaintenance(it) }

        val vehicleIds = vehicles.map { it.id }.toSet()
        require(expenses.all { it.vehicleId in vehicleIds } && reminders.all { it.vehicleId in vehicleIds } && maintenance.all { it.vehicleId in vehicleIds }) {
            "Backup danneggiato: alcuni dati fanno riferimento a veicoli mancanti"
        }

        AutoMiCostaDatabase.get(context).dao().replaceAllData(vehicles, expenses, reminders, maintenance)

        root.optString("pinHash").takeIf { it.isNotBlank() }?.let { pinHash ->
            context.getSharedPreferences("automicosta_security", Context.MODE_PRIVATE)
                .edit().putString("pin_hash", pinHash).apply()
        }

        return BackupSummary(vehicles.size, expenses.size, maintenance.size, reminders.size)
    }

    data class BackupSummary(
        val vehicles: Int,
        val expenses: Int,
        val maintenance: Int,
        val reminders: Int
    )

    private fun vehicleToJson(v: VehicleEntity) = JSONObject().apply {
        put("id", v.id); put("nickname", v.nickname); put("brand", v.brand); put("model", v.model)
        put("plate", v.plate); put("vin", v.vin); putNullable("registrationYear", v.registrationYear)
        put("firstRegistrationDate", v.firstRegistrationDate); put("inspectionDate", v.inspectionDate); put("fuelType", v.fuelType); put("currentKm", v.currentKm)
        put("purchaseDate", v.purchaseDate); putNullable("purchasePrice", v.purchasePrice); put("tireCount", v.tireCount)
        put("frontTireSize", v.frontTireSize); put("rearTireSize", v.rearTireSize)
        putNullable("engineDisplacementCc", v.engineDisplacementCc); putNullable("powerKw", v.powerKw)
        put("bodyType", v.bodyType); put("color", v.color); put("countryOfOrigin", v.countryOfOrigin); put("notes", v.notes)
    }

    private fun expenseToJson(e: ExpenseEntity) = JSONObject().apply {
        put("id", e.id); put("vehicleId", e.vehicleId); put("dateEpochDay", e.dateEpochDay); put("category", e.category)
        put("amount", e.amount); putNullable("odometerKm", e.odometerKm); putNullable("litersOrKwh", e.litersOrKwh)
        putNullable("unitPrice", e.unitPrice); put("note", e.note)
    }

    private fun reminderToJson(r: ReminderEntity) = JSONObject().apply {
        put("id", r.id); put("vehicleId", r.vehicleId); put("title", r.title); putNullable("dueEpochDay", r.dueEpochDay)
        putNullable("dueKm", r.dueKm); put("completed", r.completed)
    }

    private fun maintenanceToJson(m: MaintenanceEntity) = JSONObject().apply {
        put("id", m.id); put("vehicleId", m.vehicleId); put("dateEpochDay", m.dateEpochDay); put("component", m.component)
        put("workType", m.workType); put("cost", m.cost); putNullable("odometerKm", m.odometerKm); put("workshop", m.workshop)
        put("partBrand", m.partBrand); put("partCode", m.partCode); putNullable("nextDueEpochDay", m.nextDueEpochDay)
        putNullable("nextDueKm", m.nextDueKm); put("note", m.note)
    }

    private fun jsonToVehicle(o: JSONObject) = VehicleEntity(
        id = o.getLong("id"), nickname = o.optString("nickname"), brand = o.optString("brand"), model = o.optString("model"),
        plate = o.optString("plate"), vin = o.optString("vin"), registrationYear = o.optIntOrNull("registrationYear"),
        firstRegistrationDate = o.optString("firstRegistrationDate"), inspectionDate = o.optString("inspectionDate"), fuelType = o.optString("fuelType", "Benzina"), currentKm = o.optInt("currentKm", 0),
        purchaseDate = o.optString("purchaseDate"), purchasePrice = o.optDoubleOrNull("purchasePrice"), tireCount = o.optInt("tireCount", 4),
        frontTireSize = o.optString("frontTireSize"), rearTireSize = o.optString("rearTireSize"), engineDisplacementCc = o.optIntOrNull("engineDisplacementCc"),
        powerKw = o.optDoubleOrNull("powerKw"), bodyType = o.optString("bodyType"), color = o.optString("color"),
        countryOfOrigin = o.optString("countryOfOrigin"), notes = o.optString("notes")
    )

    private fun jsonToExpense(o: JSONObject) = ExpenseEntity(
        id = o.getLong("id"), vehicleId = o.getLong("vehicleId"), dateEpochDay = o.getLong("dateEpochDay"), category = o.optString("category"),
        amount = o.getDouble("amount"), odometerKm = o.optIntOrNull("odometerKm"), litersOrKwh = o.optDoubleOrNull("litersOrKwh"),
        unitPrice = o.optDoubleOrNull("unitPrice"), note = o.optString("note")
    )

    private fun jsonToReminder(o: JSONObject) = ReminderEntity(
        id = o.getLong("id"), vehicleId = o.getLong("vehicleId"), title = o.optString("title"), dueEpochDay = o.optLongOrNull("dueEpochDay"),
        dueKm = o.optIntOrNull("dueKm"), completed = o.optBoolean("completed", false)
    )

    private fun jsonToMaintenance(o: JSONObject) = MaintenanceEntity(
        id = o.getLong("id"), vehicleId = o.getLong("vehicleId"), dateEpochDay = o.getLong("dateEpochDay"), component = o.optString("component"),
        workType = o.optString("workType", "Sostituzione"), cost = o.optDouble("cost", 0.0), odometerKm = o.optIntOrNull("odometerKm"),
        workshop = o.optString("workshop"), partBrand = o.optString("partBrand"), partCode = o.optString("partCode"),
        nextDueEpochDay = o.optLongOrNull("nextDueEpochDay"), nextDueKm = o.optIntOrNull("nextDueKm"), note = o.optString("note")
    )

    private inline fun <T> parseArray(array: JSONArray?, parser: (JSONObject) -> T): List<T> {
        if (array == null) return emptyList()
        return buildList { for (i in 0 until array.length()) add(parser(array.getJSONObject(i))) }
    }

    private fun JSONObject.putNullable(key: String, value: Any?) { if (value == null) put(key, JSONObject.NULL) else put(key, value) }
    private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key) || !has(key)) null else getInt(key)
    private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key) || !has(key)) null else getLong(key)
    private fun JSONObject.optDoubleOrNull(key: String): Double? = if (isNull(key) || !has(key)) null else getDouble(key)
}

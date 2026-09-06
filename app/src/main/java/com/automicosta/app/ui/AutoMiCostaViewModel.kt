package com.automicosta.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.automicosta.app.data.AutoMiCostaDao
import com.automicosta.app.data.ExpenseEntity
import com.automicosta.app.data.MaintenanceEntity
import com.automicosta.app.data.ReminderEntity
import com.automicosta.app.data.VehicleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets

sealed interface VinLookupState {
    data object Idle : VinLookupState
    data object Loading : VinLookupState
    data class Success(val vehicle: VehicleEntity) : VinLookupState
    data class Error(val message: String) : VinLookupState
}

class AutoMiCostaViewModel(private val dao: AutoMiCostaDao) : ViewModel() {
    val vehicles = dao.observeVehicles().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val selectedVehicleId = MutableStateFlow<Long?>(null)

    val expenses: StateFlow<List<ExpenseEntity>> = selectedVehicleId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else dao.observeExpenses(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = selectedVehicleId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else dao.observeReminders(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val maintenance: StateFlow<List<MaintenanceEntity>> = selectedVehicleId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else dao.observeMaintenance(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _vinLookupState = MutableStateFlow<VinLookupState>(VinLookupState.Idle)
    val vinLookupState: StateFlow<VinLookupState> = _vinLookupState

    fun selectVehicle(id: Long?) { selectedVehicleId.value = id }

    fun saveVehicle(vehicle: VehicleEntity, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            if (vehicle.id == 0L && dao.vehicleCount() >= 5) {
                onResult(false)
                return@launch
            }
            val id = dao.upsertVehicle(vehicle)
            selectVehicle(if (vehicle.id == 0L) id else vehicle.id)
            onResult(true)
        }
    }

    fun deleteVehicle(vehicleId: Long) {
        viewModelScope.launch {
            dao.deleteVehicleCompletely(vehicleId)
            if (selectedVehicleId.value == vehicleId) selectedVehicleId.value = null
        }
    }

    fun addExpense(vehicleId: Long, category: String, amount: Double, km: Int?, quantity: Double?, unitPrice: Double?, note: String, dateEpochDay: Long) {
        viewModelScope.launch {
            dao.insertExpense(
                ExpenseEntity(
                    vehicleId = vehicleId,
                    dateEpochDay = dateEpochDay,
                    category = category,
                    amount = amount,
                    odometerKm = km,
                    litersOrKwh = quantity,
                    unitPrice = unitPrice,
                    note = note
                )
            )
        }
    }

    fun addMaintenance(item: MaintenanceEntity) {
        viewModelScope.launch { dao.insertMaintenance(item) }
    }

    fun addReminder(vehicleId: Long, title: String, dueKm: Int?, dueEpochDay: Long?) {
        viewModelScope.launch {
            dao.insertReminder(ReminderEntity(vehicleId = vehicleId, title = title, dueKm = dueKm, dueEpochDay = dueEpochDay))
        }
    }

    fun resetVinLookup() { _vinLookupState.value = VinLookupState.Idle }

    fun lookupVin(vin: String, modelYear: Int? = null) {
        val cleanVin = vin.trim().uppercase()
        if (cleanVin.length < 11) {
            _vinLookupState.value = VinLookupState.Error("Inserisci un VIN valido (preferibilmente 17 caratteri).")
            return
        }
        viewModelScope.launch {
            _vinLookupState.value = VinLookupState.Loading
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val encoded = URLEncoder.encode(cleanVin, StandardCharsets.UTF_8.toString())
                    val yearParam = modelYear?.let { "&modelyear=$it" }.orEmpty()
                    val json = URL("https://vpic.nhtsa.dot.gov/api/vehicles/DecodeVinValues/$encoded?format=json$yearParam").readText()
                    val obj = JSONObject(json).getJSONArray("Results").getJSONObject(0)
                    val make = obj.optString("Make")
                    val model = obj.optString("Model")
                    val year = obj.optString("ModelYear").toIntOrNull()
                    if (make.isBlank() && model.isBlank()) error("Nessun dato trovato per questo VIN")
                    VehicleEntity(
                        nickname = listOf(make, model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Veicolo" },
                        brand = make,
                        model = model,
                        vin = cleanVin,
                        registrationYear = year,
                        fuelType = obj.optString("FuelTypePrimary").ifBlank { "Benzina" },
                        engineDisplacementCc = obj.optString("DisplacementCC").toDoubleOrNull()?.toInt(),
                        bodyType = obj.optString("BodyClass"),
                        countryOfOrigin = obj.optString("PlantCountry")
                    )
                }
            }
            result.onSuccess { _vinLookupState.value = VinLookupState.Success(it) }
                .onFailure { _vinLookupState.value = VinLookupState.Error(it.message ?: "Ricerca VIN non riuscita") }
        }
    }

    companion object {
        fun factory(dao: AutoMiCostaDao) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AutoMiCostaViewModel(dao) as T
        }
    }
}

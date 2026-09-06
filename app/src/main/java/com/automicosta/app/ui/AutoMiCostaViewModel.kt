package com.automicosta.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.automicosta.app.data.AutoMiCostaDao
import com.automicosta.app.data.ExpenseEntity
import com.automicosta.app.data.ReminderEntity
import com.automicosta.app.data.VehicleEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AutoMiCostaViewModel(private val dao: AutoMiCostaDao) : ViewModel() {
    val vehicles = dao.observeVehicles().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val selectedVehicleId = MutableStateFlow<Long?>(null)

    val expenses: StateFlow<List<ExpenseEntity>> = selectedVehicleId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else dao.observeExpenses(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = selectedVehicleId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else dao.observeReminders(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectVehicle(id: Long?) { selectedVehicleId.value = id }

    fun addVehicle(nickname: String, brand: String, model: String, fuelType: String, km: Int) {
        viewModelScope.launch {
            val id = dao.upsertVehicle(VehicleEntity(nickname = nickname, brand = brand, model = model, fuelType = fuelType, currentKm = km))
            selectVehicle(id)
        }
    }

    fun addExpense(vehicleId: Long, category: String, amount: Double, km: Int?, quantity: Double?, unitPrice: Double?, note: String) {
        viewModelScope.launch {
            dao.insertExpense(
                ExpenseEntity(
                    vehicleId = vehicleId,
                    dateEpochDay = System.currentTimeMillis() / 86_400_000L,
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

    fun addReminder(vehicleId: Long, title: String, dueKm: Int?) {
        viewModelScope.launch {
            dao.insertReminder(ReminderEntity(vehicleId = vehicleId, title = title, dueKm = dueKm))
        }
    }

    companion object {
        fun factory(dao: AutoMiCostaDao) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AutoMiCostaViewModel(dao) as T
        }
    }
}

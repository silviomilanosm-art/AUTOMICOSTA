package com.automicosta.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoMiCostaDao {
    @Query("SELECT * FROM vehicles ORDER BY id")
    fun observeVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles ORDER BY id")
    suspend fun getAllVehicles(): List<VehicleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVehicle(vehicle: VehicleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreVehicles(items: List<VehicleEntity>)

    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun vehicleCount(): Int

    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY dateEpochDay DESC, id DESC")
    fun observeExpenses(vehicleId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY id")
    suspend fun getAllExpenses(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreExpenses(items: List<ExpenseEntity>)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM reminders WHERE vehicleId = :vehicleId AND completed = 0 ORDER BY dueEpochDay ASC")
    fun observeReminders(vehicleId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY id")
    suspend fun getAllReminders(): List<ReminderEntity>

    @Insert
    suspend fun insertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreReminders(items: List<ReminderEntity>)

    @Query("SELECT * FROM maintenance WHERE vehicleId = :vehicleId ORDER BY dateEpochDay DESC, id DESC")
    fun observeMaintenance(vehicleId: Long): Flow<List<MaintenanceEntity>>

    @Query("SELECT * FROM maintenance ORDER BY id")
    suspend fun getAllMaintenance(): List<MaintenanceEntity>

    @Insert
    suspend fun insertMaintenance(item: MaintenanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreMaintenance(items: List<MaintenanceEntity>)

    @Query("DELETE FROM expenses WHERE vehicleId = :vehicleId")
    suspend fun deleteVehicleExpenses(vehicleId: Long)

    @Query("DELETE FROM reminders WHERE vehicleId = :vehicleId")
    suspend fun deleteVehicleReminders(vehicleId: Long)

    @Query("DELETE FROM maintenance WHERE vehicleId = :vehicleId")
    suspend fun deleteVehicleMaintenance(vehicleId: Long)

    @Query("DELETE FROM vehicles WHERE id = :vehicleId")
    suspend fun deleteVehicleRow(vehicleId: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()

    @Query("DELETE FROM maintenance")
    suspend fun deleteAllMaintenance()

    @Query("DELETE FROM vehicles")
    suspend fun deleteAllVehicles()

    @Transaction
    suspend fun deleteVehicleCompletely(vehicleId: Long) {
        deleteVehicleExpenses(vehicleId)
        deleteVehicleReminders(vehicleId)
        deleteVehicleMaintenance(vehicleId)
        deleteVehicleRow(vehicleId)
    }

    @Transaction
    suspend fun replaceAllData(
        vehicles: List<VehicleEntity>,
        expenses: List<ExpenseEntity>,
        reminders: List<ReminderEntity>,
        maintenance: List<MaintenanceEntity>
    ) {
        deleteAllExpenses()
        deleteAllReminders()
        deleteAllMaintenance()
        deleteAllVehicles()
        if (vehicles.isNotEmpty()) restoreVehicles(vehicles)
        if (expenses.isNotEmpty()) restoreExpenses(expenses)
        if (reminders.isNotEmpty()) restoreReminders(reminders)
        if (maintenance.isNotEmpty()) restoreMaintenance(maintenance)
    }
}

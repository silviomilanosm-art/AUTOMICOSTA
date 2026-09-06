package com.automicosta.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoMiCostaDao {
    @Query("SELECT * FROM vehicles ORDER BY id")
    fun observeVehicles(): Flow<List<VehicleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVehicle(vehicle: VehicleEntity): Long

    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun vehicleCount(): Int

    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY dateEpochDay DESC, id DESC")
    fun observeExpenses(vehicleId: Long): Flow<List<ExpenseEntity>>

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM reminders WHERE vehicleId = :vehicleId AND completed = 0 ORDER BY dueEpochDay ASC")
    fun observeReminders(vehicleId: Long): Flow<List<ReminderEntity>>

    @Insert
    suspend fun insertReminder(reminder: ReminderEntity)

    @Query("SELECT * FROM maintenance WHERE vehicleId = :vehicleId ORDER BY dateEpochDay DESC, id DESC")
    fun observeMaintenance(vehicleId: Long): Flow<List<MaintenanceEntity>>

    @Insert
    suspend fun insertMaintenance(item: MaintenanceEntity)
}

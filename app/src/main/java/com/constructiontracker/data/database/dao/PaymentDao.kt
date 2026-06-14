package com.constructiontracker.data.database.dao

import androidx.room.*
import com.constructiontracker.data.database.entities.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY date DESC, createdAt DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments")
    fun getTotalPaid(): Flow<Double>

    @Insert
    suspend fun insertPayment(payment: PaymentEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)
}

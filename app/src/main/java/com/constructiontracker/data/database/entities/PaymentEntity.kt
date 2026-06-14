package com.constructiontracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contractorId: Int,
    val date: Long,
    val amount: Double,
    val bankReference: String,
    val workDescription: String,
    val receiptReceived: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

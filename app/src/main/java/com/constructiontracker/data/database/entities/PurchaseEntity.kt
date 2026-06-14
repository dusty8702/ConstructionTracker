package com.constructiontracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    val date: Long,
    val amount: Double,
    val category: String,
    val shopName: String,
    val receiptReceived: Boolean,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

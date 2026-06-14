package com.constructiontracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contractors")
data class ContractorEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val contractType: String,   // "FIXED" or "OPEN_ENDED"
    val contractAmount: Double = 0.0,
    val contactNumber: String = "",
    val bankAccountNumber: String = "",
    val bankName: String = "",
    val bankBranch: String = "",
    val photoUri: String = ""
)

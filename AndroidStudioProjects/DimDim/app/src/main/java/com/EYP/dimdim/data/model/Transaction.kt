package com.EYP.dimdim.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey
    val id: String,
    val amount: Double,
    val merchantName: String,
    val category: String,
    val date: Date,
    val description: String,
    val imageUri: String? = null,
    val isIncome: Boolean = false,
    val isSynced: Boolean = false,
    val confidence: Float = 0f,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
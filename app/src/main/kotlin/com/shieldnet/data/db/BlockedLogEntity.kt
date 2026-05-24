package com.shieldnet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_logs")
data class BlockedLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val timestamp: Long = System.currentTimeMillis(),
    val appPackage: String? = null
)

package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val appPackage: String,
    val appLabel: String,
    val timestamp: Long,
    val isStarred: Boolean = false
)

package com.meridians.scribolt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val fontFamily: String = "default", // "default", "serif", "monospace"
    val fontSize: Int = 16, // Font size in sp
    val textColor: String = "#000000", // Hex color code for text
    val backgroundColor: String = "#FFFFFF" // Hex color code for note background
)
package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class ParseStatus {
    PROCESSING,
    COMPLETED,
    FAILED,
    IMPORTED  // Programme has been created from this parse request
}

@Entity(tableName = "parse_requests")
data class ParseRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawText: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val status: ParseStatus = ParseStatus.PROCESSING,
    val error: String? = null,
    val resultJson: String? = null, // JSON representation of ParsedProgramme
    val completedAt: LocalDateTime? = null
)

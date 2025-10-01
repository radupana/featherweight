package com.github.radupana.featherweight.domain

import java.time.LocalDateTime

data class TemplateSummary(
    val id: String,
    val name: String,
    val description: String?,
    val exerciseCount: Int,
    val setCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

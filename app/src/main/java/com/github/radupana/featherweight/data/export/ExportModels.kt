package com.github.radupana.featherweight.data.export

data class ExportOptions(
    val includeBodyweight: Boolean = true,
    val includeOneRepMaxes: Boolean = true,
    val includeNotes: Boolean = true,
    val includeProfile: Boolean = true,
)

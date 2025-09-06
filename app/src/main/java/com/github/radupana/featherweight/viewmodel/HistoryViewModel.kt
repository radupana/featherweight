package com.github.radupana.featherweight.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.export.ExportOptions
import com.github.radupana.featherweight.domain.ProgrammeSummary
import com.github.radupana.featherweight.domain.WorkoutDayInfo
import com.github.radupana.featherweight.domain.WorkoutSummary
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.WorkoutExportService
import com.github.radupana.featherweight.utils.ExportHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale

data class PaginatedHistoryState(
    val programmes: List<ProgrammeSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMoreProgrammes: Boolean = false,
    val hasMoreProgrammes: Boolean = true,
    val currentProgrammePage: Int = 0,
    val pageSize: Int = 20,
    val error: String? = null,
    val exportingWorkoutId: Long? = null,
    val pendingExportFile: java.io.File? = null,
)

data class CalendarState(
    val currentMonth: YearMonth,
    val selectedDate: LocalDate? = null,
    val workoutCounts: Map<LocalDate, Int> = emptyMap(),
    val workoutDayInfo: Map<LocalDate, WorkoutDayInfo> = emptyMap(),
    val isLoading: Boolean = false,
)

data class WeekGroupState(
    val weeks: List<WeekWorkouts> = emptyList(),
    val expandedWeeks: Set<String> = emptySet(),
    val isLoading: Boolean = false,
)

data class WeekWorkouts(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val workouts: List<WorkoutSummary>,
    val totalVolume: Double,
    val totalWorkouts: Int,
)

class HistoryViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)
    private val database = FeatherweightDatabase.getDatabase(application)
    private val exportService = WorkoutExportService(
        database.workoutDao(),
        database.exerciseLogDao(),
        database.setLogDao(),
        database.oneRMDao(),
        repository
    )
    private val exportHandler = ExportHandler(application)

    companion object {
        private const val TAG = "HistoryViewModel"
    }

    private val _historyState = MutableStateFlow(PaginatedHistoryState())
    val historyState: StateFlow<PaginatedHistoryState> = _historyState

    // Calendar state
    private val _calendarState =
        MutableStateFlow(
            CalendarState(
                currentMonth = YearMonth.now(),
                selectedDate = LocalDate.now(),
            ),
        )
    val calendarState: StateFlow<CalendarState> = _calendarState.asStateFlow()

    // Week group state
    private val _weekGroupState = MutableStateFlow(WeekGroupState())
    val weekGroupState: StateFlow<WeekGroupState> = _weekGroupState.asStateFlow()

    // Store selected programme ID for navigation
    var selectedProgrammeId: Long? = null

    init {
        // Seed database if empty and load initial page
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
            loadInitialData()
            loadCalendarData()
            loadWeekGroups()
        }
    }

    private suspend fun loadInitialData() {
        val currentState = _historyState.value
        if (currentState.programmes.isNotEmpty()) {
            return // Already loaded
        }

        _historyState.value = currentState.copy(isLoading = true, error = null)

        try {
            // Load programmes
            val firstPageProgrammes = repository.getCompletedProgrammesPaged(page = 0, pageSize = currentState.pageSize)

            val hasMoreProgrammes = firstPageProgrammes.size == currentState.pageSize

            _historyState.value =
                currentState.copy(
                    programmes = firstPageProgrammes,
                    isLoading = false,
                    isLoadingMoreProgrammes = false,
                    currentProgrammePage = 0,
                    hasMoreProgrammes = hasMoreProgrammes,
                )
        } catch (e: IllegalArgumentException) {
            _historyState.value =
                currentState.copy(
                    isLoading = false,
                    error = "Failed to load programmes: ${e.message}",
                )
        } catch (e: IllegalStateException) {
            _historyState.value =
                currentState.copy(
                    isLoading = false,
                    error = "Failed to load programmes: ${e.message}",
                )
        } catch (e: NumberFormatException) {
            _historyState.value =
                currentState.copy(
                    isLoading = false,
                    error = "Failed to load programmes: ${e.message}",
                )
        }
    }

    fun refreshHistory() {
        viewModelScope.launch {
            val currentState = _historyState.value

            try {
                // Refresh programmes
                val firstPageProgrammes = repository.getCompletedProgrammesPaged(page = 0, pageSize = currentState.pageSize)
                val hasMoreProgrammes = firstPageProgrammes.size == currentState.pageSize

                _historyState.value =
                    currentState.copy(
                        programmes = firstPageProgrammes,
                        isLoadingMoreProgrammes = false,
                        currentProgrammePage = 0,
                        hasMoreProgrammes = hasMoreProgrammes,
                        error = null,
                    )

                // Also refresh calendar and week groups
                loadCalendarData()
                loadWeekGroups()
            } catch (e: IllegalArgumentException) {
                _historyState.value =
                    currentState.copy(
                        error = "Failed to refresh history: ${e.message}",
                    )
            } catch (e: IllegalStateException) {
                _historyState.value =
                    currentState.copy(
                        error = "Failed to refresh history: ${e.message}",
                    )
            } catch (e: NumberFormatException) {
                _historyState.value =
                    currentState.copy(
                        error = "Failed to refresh history: ${e.message}",
                    )
            }
        }
    }

    fun loadNextProgrammePage() {
        viewModelScope.launch {
            val currentState = _historyState.value

            // Don't load if already loading, no more data, or error state
            if (currentState.isLoadingMoreProgrammes || !currentState.hasMoreProgrammes || currentState.error != null) {
                return@launch
            }

            _historyState.value = currentState.copy(isLoadingMoreProgrammes = true)

            try {
                val nextPage = currentState.currentProgrammePage + 1
                val newProgrammes =
                    repository.getCompletedProgrammesPaged(
                        page = nextPage,
                        pageSize = currentState.pageSize,
                    )

                val allProgrammes = currentState.programmes + newProgrammes
                val hasMoreProgrammes = newProgrammes.size == currentState.pageSize

                _historyState.value =
                    currentState.copy(
                        programmes = allProgrammes,
                        isLoadingMoreProgrammes = false,
                        currentProgrammePage = nextPage,
                        hasMoreProgrammes = hasMoreProgrammes,
                    )
            } catch (e: IllegalArgumentException) {
                _historyState.value =
                    currentState.copy(
                        isLoadingMoreProgrammes = false,
                        error = "Failed to load more programmes: ${e.message}",
                    )
            } catch (e: IllegalStateException) {
                _historyState.value =
                    currentState.copy(
                        isLoadingMoreProgrammes = false,
                        error = "Failed to load more programmes: ${e.message}",
                    )
            } catch (e: NumberFormatException) {
                _historyState.value =
                    currentState.copy(
                        isLoadingMoreProgrammes = false,
                        error = "Failed to load more programmes: ${e.message}",
                    )
            }
        }
    }

    fun clearError() {
        val currentState = _historyState.value
        _historyState.value = currentState.copy(error = null)
    }

    // Calendar methods

    fun loadCalendarData() {
        viewModelScope.launch {
            _calendarState.value = _calendarState.value.copy(isLoading = true)
            try {
                val currentMonth = _calendarState.value.currentMonth
                // Load both old counts (for compatibility) and new status-aware counts
                val workoutCounts =
                    repository.getWorkoutCountsByMonth(
                        currentMonth.year,
                        currentMonth.monthValue,
                    )
                val workoutDayInfo =
                    repository.getWorkoutCountsByMonthWithStatus(
                        currentMonth.year,
                        currentMonth.monthValue,
                    )

                _calendarState.value =
                    _calendarState.value.copy(
                        workoutCounts = workoutCounts,
                        workoutDayInfo = workoutDayInfo,
                        isLoading = false,
                    )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to load calendar data", e)
                _calendarState.value = _calendarState.value.copy(isLoading = false)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Failed to load calendar data", e)
                _calendarState.value = _calendarState.value.copy(isLoading = false)
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Failed to load calendar data", e)
                _calendarState.value = _calendarState.value.copy(isLoading = false)
            }
        }
    }

    fun navigateToMonth(yearMonth: YearMonth) {
        _calendarState.value = _calendarState.value.copy(currentMonth = yearMonth)
        loadCalendarData()
        loadWeekGroups() // Also reload week groups for the new month
    }

    fun selectDate(date: LocalDate) {
        _calendarState.value = _calendarState.value.copy(selectedDate = date)
    }

    // Week grouping methods

    fun loadWeekGroups() {
        viewModelScope.launch {
            _weekGroupState.value = _weekGroupState.value.copy(isLoading = true)
            try {
                // Load weeks that overlap with the current calendar month
                val currentMonth = _calendarState.value.currentMonth
                val weekField = WeekFields.of(Locale.getDefault())
                val weeks = mutableListOf<WeekWorkouts>()

                // Find first day of month and last day of month
                val firstDayOfMonth = currentMonth.atDay(1)
                val lastDayOfMonth = currentMonth.atEndOfMonth()

                // Find the week containing the first day of the month
                val firstWeekStart = firstDayOfMonth.with(weekField.dayOfWeek(), 1)

                // Find the week containing the last day of the month
                val lastWeekStart = lastDayOfMonth.with(weekField.dayOfWeek(), 1)

                // Iterate through all weeks that overlap with this month
                var currentWeekStart = firstWeekStart
                while (!currentWeekStart.isAfter(lastWeekStart)) {
                    val weekEnd = currentWeekStart.plusDays(6)
                    val workouts = repository.getWorkoutsByWeek(currentWeekStart)

                    // Only add weeks that have workouts
                    if (workouts.isNotEmpty()) {
                        val totalVolume = workouts.sumOf { it.totalWeight.toDouble() }
                        weeks.add(
                            WeekWorkouts(
                                weekStart = currentWeekStart,
                                weekEnd = weekEnd,
                                workouts = workouts,
                                totalVolume = totalVolume,
                                totalWorkouts = workouts.size,
                            ),
                        )
                    }

                    currentWeekStart = currentWeekStart.plusWeeks(1)
                }

                _weekGroupState.value =
                    WeekGroupState(
                        weeks = weeks,
                        expandedWeeks = emptySet(),
                        isLoading = false,
                    )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to load week groups for calendar month", e)
                _weekGroupState.value = _weekGroupState.value.copy(isLoading = false)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Failed to load week groups for calendar month", e)
                _weekGroupState.value = _weekGroupState.value.copy(isLoading = false)
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Failed to load week groups for calendar month", e)
                _weekGroupState.value = _weekGroupState.value.copy(isLoading = false)
            }
        }
    }

    fun toggleWeekExpanded(weekId: String) {
        val currentExpanded = _weekGroupState.value.expandedWeeks
        _weekGroupState.value =
            _weekGroupState.value.copy(
                expandedWeeks =
                    if (weekId in currentExpanded) {
                        currentExpanded - weekId
                    } else {
                        currentExpanded + weekId
                    },
            )
    }

    // Get workouts for a specific date (used by calendar view)
    fun getWorkoutsForDate(date: LocalDate): List<WorkoutSummary> {
        // Find workouts in the current month's week groups
        val allWorkoutsInMonth = _weekGroupState.value.weeks.flatMap { it.workouts }
        return allWorkoutsInMonth.filter { it.date.toLocalDate() == date }
    }

    fun exportWorkout(workoutId: Long) {
        viewModelScope.launch {
            _historyState.value = _historyState.value.copy(
                exportingWorkoutId = workoutId
            )

            try {
                val exportOptions = ExportOptions(
                    includeBodyweight = false,
                    includeOneRepMaxes = true,
                    includeNotes = true,
                    includeProfile = false
                )

                val file = exportService.exportSingleWorkout(
                    getApplication(),
                    workoutId,
                    exportOptions
                )

                // Store the file for later saving
                _historyState.value = _historyState.value.copy(
                    exportingWorkoutId = null,
                    pendingExportFile = file
                )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to export workout", e)
                _historyState.value = _historyState.value.copy(
                    exportingWorkoutId = null
                )
            }
        }
    }
    
    fun saveExportedFile(uri: android.net.Uri) {
        viewModelScope.launch {
            _historyState.value.pendingExportFile?.let { file ->
                try {
                    exportHandler.copyFileContent(file, uri)
                    // Clean up temp file
                    file.delete()
                    _historyState.value = _historyState.value.copy(
                        pendingExportFile = null
                    )
                } catch (e: java.io.IOException) {
                    Log.e(TAG, "Failed to save exported file", e)
                }
            }
        }
    }
    
    fun clearPendingExport() {
        _historyState.value.pendingExportFile?.delete()
        _historyState.value = _historyState.value.copy(
            pendingExportFile = null
        )
    }
}

package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.github.radupana.featherweight.ui.components.ExerciseDataPoint

class ExerciseProgressViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = FeatherweightRepository(application)
    private val userPreferences = UserPreferences(application)
    
    sealed class ExerciseProgressState {
        object Loading : ExerciseProgressState()
        data class Success(val data: ExerciseProgressData?) : ExerciseProgressState()
        data class Error(val message: String) : ExerciseProgressState()
    }
    
    data class ExerciseProgressData(
        val exerciseName: String,
        val currentMax: Float,
        val allTimePR: Float,
        val monthlyProgress: Float,
        val weeklyFrequency: Float,
        val lastPerformed: LocalDate?,
        val totalSessions: Int,
        val totalVolume: Float,
        val avgSessionVolume: Float,
        val currentTrend: String // "IMPROVING", "STALLING", "DECLINING"
    )
    
    private val _state = MutableStateFlow<ExerciseProgressState>(ExerciseProgressState.Loading)
    val state: StateFlow<ExerciseProgressState> = _state.asStateFlow()
    
    private val _chartData = MutableStateFlow<List<ExerciseDataPoint>>(emptyList())
    val chartData: StateFlow<List<ExerciseDataPoint>> = _chartData.asStateFlow()

    fun loadExerciseData(exerciseName: String) {
        viewModelScope.launch {
            _state.value = ExerciseProgressState.Loading
            
            try {
                val userId = userPreferences.getCurrentUserId()
                if (userId == -1L) {
                    _state.value = ExerciseProgressState.Error("No user selected")
                    return@launch
                }
                
                // Fetch global exercise progress
                val globalProgress = repository.getGlobalExerciseProgress(userId, exerciseName)
                
                if (globalProgress == null) {
                    _state.value = ExerciseProgressState.Success(null)
                    return@launch
                }
                
                // Calculate monthly progress
                val thirtyDaysAgo = LocalDate.now().minusDays(30)
                val monthlyWorkouts = repository.getExerciseWorkoutsInDateRange(
                    userId = userId,
                    exerciseName = exerciseName,
                    startDate = thirtyDaysAgo,
                    endDate = LocalDate.now()
                )
                
                var monthlyProgress = 0f
                if (monthlyWorkouts.isNotEmpty()) {
                    val oldestWeight = monthlyWorkouts.lastOrNull()?.actualWeight ?: 0f
                    val newestWeight = monthlyWorkouts.firstOrNull()?.actualWeight ?: 0f
                    if (oldestWeight > 0) {
                        monthlyProgress = ((newestWeight - oldestWeight) / oldestWeight) * 100
                    }
                }
                
                // Calculate weekly frequency
                val totalDays = if (globalProgress.lastUpdated != null) {
                    ChronoUnit.DAYS.between(
                        monthlyWorkouts.lastOrNull()?.workoutDate?.toLocalDate() ?: LocalDate.now().minusDays(30),
                        LocalDate.now()
                    ).toFloat()
                } else 0f
                
                val weeklyFrequency = if (totalDays > 0) {
                    (monthlyWorkouts.size / (totalDays / 7f))
                } else 0f
                
                // Get all-time PR
                val allTimePR = repository.getPersonalRecordForExercise(userId, exerciseName)?.weight ?: globalProgress.estimatedMax
                
                val data = ExerciseProgressData(
                    exerciseName = exerciseName,
                    currentMax = globalProgress.estimatedMax,
                    allTimePR = allTimePR,
                    monthlyProgress = monthlyProgress,
                    weeklyFrequency = weeklyFrequency,
                    lastPerformed = globalProgress.lastUpdated.toLocalDate(),
                    totalSessions = repository.getTotalSessionsForExercise(userId, exerciseName),
                    totalVolume = globalProgress.totalVolumeLast30Days,
                    avgSessionVolume = globalProgress.avgSessionVolume ?: 0f,
                    currentTrend = globalProgress.trend.name
                )
                
                _state.value = ExerciseProgressState.Success(data)
                
            } catch (e: Exception) {
                _state.value = ExerciseProgressState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadChartData(exerciseName: String) {
        viewModelScope.launch {
            try {
                val userId = userPreferences.getCurrentUserId()
                if (userId == -1L) return@launch
                
                // Get all workout data for this exercise
                val allWorkouts = repository.getExerciseWorkoutsInDateRange(
                    userId = userId,
                    exerciseName = exerciseName,
                    startDate = LocalDate.now().minusYears(2), // Get 2 years of data
                    endDate = LocalDate.now()
                )
                
                // Group by date and get the best performance per day
                val chartPoints = allWorkouts
                    .groupBy { it.workoutDate.toLocalDate() }
                    .map { (date, logs) ->
                        val bestLog = logs.maxByOrNull { 
                            // Calculate estimated 1RM for comparison
                            it.actualWeight * (1 + 0.0333f * it.actualReps)
                        }!!
                        
                        val estimated1RM = bestLog.actualWeight * (1 + 0.0333f * bestLog.actualReps)
                        
                        ExerciseDataPoint(
                            date = date,
                            weight = estimated1RM,
                            reps = bestLog.actualReps,
                            isPR = false // Will be calculated separately
                        )
                    }
                    .sortedBy { it.date }
                
                // Mark PRs
                val dataWithPRs = if (chartPoints.isNotEmpty()) {
                    var currentMax = 0f
                    chartPoints.map { point ->
                        if (point.weight > currentMax) {
                            currentMax = point.weight
                            point.copy(isPR = true)
                        } else {
                            point
                        }
                    }
                } else emptyList()
                
                _chartData.value = dataWithPRs
                
            } catch (e: Exception) {
                // Handle error silently for chart data
                _chartData.value = emptyList()
            }
        }
    }
}
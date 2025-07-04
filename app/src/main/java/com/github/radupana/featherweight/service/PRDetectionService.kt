package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.SetLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import kotlin.math.abs

/**
 * Service responsible for detecting personal records and creating PR entries
 */
class PRDetectionService(
    private val personalRecordDao: PersonalRecordDao
) {
    
    /**
     * Checks if a completed set represents a new PR and creates record if so
     * Returns the PersonalRecord if a PR was detected, null otherwise
     */
    suspend fun checkForPR(setLog: SetLog, exerciseName: String): List<PersonalRecord> = withContext(Dispatchers.IO) {
        val newPRs = mutableListOf<PersonalRecord>()
        
        if (!setLog.isCompleted || setLog.actualReps <= 0 || setLog.actualWeight <= 0) {
            return@withContext newPRs
        }
        
        val currentWeight = setLog.actualWeight
        val currentReps = setLog.actualReps
        val currentVolume = currentWeight * currentReps
        val currentDate = LocalDateTime.now()
        
        // Check for weight PR (higher weight for same or more reps)
        val weightPR = checkWeightPR(exerciseName, currentWeight, currentReps, currentDate)
        weightPR?.let { newPRs.add(it) }
        
        // Check for reps PR (more reps with same or higher weight)
        val repsPR = checkRepsPR(exerciseName, currentWeight, currentReps, currentDate)
        repsPR?.let { newPRs.add(it) }
        
        // Check for volume PR (highest weight × reps)
        val volumePR = checkVolumePR(exerciseName, currentVolume, currentWeight, currentReps, currentDate)
        volumePR?.let { newPRs.add(it) }
        
        // Check for estimated 1RM PR
        val oneRMPR = checkEstimated1RMPR(exerciseName, currentWeight, currentReps, currentDate)
        oneRMPR?.let { newPRs.add(it) }
        
        // For now, only save ESTIMATED_1RM PRs to avoid database clutter
        val estimatedMaxPRs = newPRs.filter { it.recordType == PRType.ESTIMATED_1RM }
        println("🏆 PRDetectionService: Detected ${newPRs.size} total PRs, saving ${estimatedMaxPRs.size} 1RM PRs")
        estimatedMaxPRs.forEach { pr ->
            println("🏆 PRDetectionService: Saving 1RM PR: ${pr.exerciseName} - ${pr.estimated1RM}kg")
            personalRecordDao.insertPersonalRecord(pr)
        }
        
        // Return all detected PRs for potential future use, but only 1RM is saved
        newPRs
    }
    
    private suspend fun checkWeightPR(
        exerciseName: String, 
        weight: Float, 
        reps: Int, 
        date: LocalDateTime
    ): PersonalRecord? {
        val currentMaxWeight = personalRecordDao.getMaxWeightForExercise(exerciseName) ?: 0f
        
        if (weight > currentMaxWeight) {
            // Get previous weight PR for context
            val previousPR = personalRecordDao.getLatestPRForExerciseAndType(exerciseName, PRType.WEIGHT)
            
            val improvementPercentage = if (currentMaxWeight > 0) {
                ((weight - currentMaxWeight) / currentMaxWeight) * 100
            } else 100f
            
            return PersonalRecord(
                exerciseName = exerciseName,
                weight = weight,
                reps = reps,
                recordDate = date,
                previousWeight = previousPR?.weight,
                previousReps = previousPR?.reps,
                previousDate = previousPR?.recordDate,
                improvementPercentage = improvementPercentage,
                recordType = PRType.WEIGHT,
                volume = weight * reps,
                estimated1RM = calculateEstimated1RM(weight, reps),
                notes = "New weight PR: ${weight}kg × ${reps}"
            )
        }
        
        return null
    }
    
    private suspend fun checkRepsPR(
        exerciseName: String,
        weight: Float,
        reps: Int,
        date: LocalDateTime
    ): PersonalRecord? {
        // Find the best reps achieved at this weight or higher
        val allPRs = personalRecordDao.getRecentPRsForExercise(exerciseName, 50)
        val bestRepsAtWeight = allPRs.filter { it.weight >= weight }.maxByOrNull { it.reps }
        
        if (bestRepsAtWeight == null || reps > bestRepsAtWeight.reps) {
            val improvementPercentage = if (bestRepsAtWeight != null) {
                ((reps - bestRepsAtWeight.reps).toFloat() / bestRepsAtWeight.reps) * 100
            } else 100f
            
            return PersonalRecord(
                exerciseName = exerciseName,
                weight = weight,
                reps = reps,
                recordDate = date,
                previousWeight = bestRepsAtWeight?.weight,
                previousReps = bestRepsAtWeight?.reps,
                previousDate = bestRepsAtWeight?.recordDate,
                improvementPercentage = improvementPercentage,
                recordType = PRType.REPS,
                volume = weight * reps,
                estimated1RM = calculateEstimated1RM(weight, reps),
                notes = "New reps PR: ${reps} reps at ${weight}kg"
            )
        }
        
        return null
    }
    
    private suspend fun checkVolumePR(
        exerciseName: String,
        volume: Float,
        weight: Float,
        reps: Int,
        date: LocalDateTime
    ): PersonalRecord? {
        val currentMaxVolume = personalRecordDao.getMaxVolumeForExercise(exerciseName) ?: 0f
        
        if (volume > currentMaxVolume) {
            val previousPR = personalRecordDao.getLatestPRForExerciseAndType(exerciseName, PRType.VOLUME)
            
            val improvementPercentage = if (currentMaxVolume > 0) {
                ((volume - currentMaxVolume) / currentMaxVolume) * 100
            } else 100f
            
            return PersonalRecord(
                exerciseName = exerciseName,
                weight = weight,
                reps = reps,
                recordDate = date,
                previousWeight = previousPR?.weight,
                previousReps = previousPR?.reps,
                previousDate = previousPR?.recordDate,
                improvementPercentage = improvementPercentage,
                recordType = PRType.VOLUME,
                volume = volume,
                estimated1RM = calculateEstimated1RM(weight, reps),
                notes = "New volume PR: ${volume.toInt()}kg total (${weight}kg × ${reps})"
            )
        }
        
        return null
    }
    
    private suspend fun checkEstimated1RMPR(
        exerciseName: String,
        weight: Float,
        reps: Int,
        date: LocalDateTime
    ): PersonalRecord? {
        val estimated1RM = calculateEstimated1RM(weight, reps)
        println("🏆 1RM Check: ${exerciseName} - ${weight}kg x ${reps} = ${estimated1RM}kg estimated 1RM")
        
        // Get the best estimated 1RM from all previous PRs
        val allPRs = personalRecordDao.getRecentPRsForExercise(exerciseName, 50)
        val bestEstimated1RM = allPRs.mapNotNull { it.estimated1RM }.maxOrNull() ?: 0f
        println("🏆 1RM Check: Previous best 1RM = ${bestEstimated1RM}kg from ${allPRs.size} previous PRs")
        
        if (estimated1RM > bestEstimated1RM) { // Remove the stupid weight comparison that breaks 1-rep PRs
            val improvementPercentage = if (bestEstimated1RM > 0) {
                ((estimated1RM - bestEstimated1RM) / bestEstimated1RM) * 100
            } else 100f
            
            println("🏆 1RM Check: ${estimated1RM}kg > ${bestEstimated1RM}kg, improvement = ${improvementPercentage}%")
            
            // Temporarily lower threshold for testing (was 2.0f)
            if (improvementPercentage >= 0.1f) {
                val previousBest = allPRs.find { it.estimated1RM == bestEstimated1RM }
                
                return PersonalRecord(
                    exerciseName = exerciseName,
                    weight = weight,
                    reps = reps,
                    recordDate = date,
                    previousWeight = previousBest?.weight,
                    previousReps = previousBest?.reps,
                    previousDate = previousBest?.recordDate,
                    improvementPercentage = improvementPercentage,
                    recordType = PRType.ESTIMATED_1RM,
                    volume = weight * reps,
                    estimated1RM = estimated1RM,
                    notes = "New estimated 1RM: ${estimated1RM.toInt()}kg (from ${weight}kg × ${reps})"
                )
            } else {
                println("🏆 1RM Check: FAILED - improvement ${improvementPercentage}% < 0.1% threshold")
            }
        } else {
            println("🏆 1RM Check: FAILED - estimated1RM=${estimated1RM}kg <= bestEstimated1RM=${bestEstimated1RM}kg")
        }
        
        return null
    }
    
    /**
     * Calculate estimated 1RM using Brzycki formula
     */
    private fun calculateEstimated1RM(weight: Float, reps: Int): Float {
        if (reps == 1) return weight
        if (reps > 15) return weight // Formula becomes unreliable beyond 15 reps
        
        // Brzycki formula: 1RM = weight / (1.0278 - 0.0278 × reps)
        return weight / (1.0278f - 0.0278f * reps)
    }
    
    /**
     * Get recent PRs for an exercise
     */
    suspend fun getRecentPRsForExercise(exerciseName: String, limit: Int = 5): List<PersonalRecord> = 
        withContext(Dispatchers.IO) {
            personalRecordDao.getRecentPRsForExercise(exerciseName, limit)
        }
    
    /**
     * Get all recent PRs across exercises
     */
    suspend fun getRecentPRs(limit: Int = 10): List<PersonalRecord> = 
        withContext(Dispatchers.IO) {
            personalRecordDao.getRecentPRs(limit)
        }
}
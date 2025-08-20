package com.github.radupana.featherweight.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.radupana.featherweight.dao.TrainingAnalysisDao
import com.github.radupana.featherweight.data.exercise.ExerciseCore
import com.github.radupana.featherweight.data.exercise.ExerciseCoreDao
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseTypeConverters
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.ExerciseVariationDao
import com.github.radupana.featherweight.data.exercise.VariationAlias
import com.github.radupana.featherweight.data.exercise.VariationAliasDao
import com.github.radupana.featherweight.data.exercise.VariationInstruction
import com.github.radupana.featherweight.data.exercise.VariationInstructionDao
import com.github.radupana.featherweight.data.exercise.VariationMuscle
import com.github.radupana.featherweight.data.exercise.VariationMuscleDao
import com.github.radupana.featherweight.data.exercise.VariationRelation
import com.github.radupana.featherweight.data.exercise.VariationRelationDao
import com.github.radupana.featherweight.data.profile.OneRMDao
import com.github.radupana.featherweight.data.profile.OneRMHistory
import com.github.radupana.featherweight.data.profile.ProfileDao
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.data.profile.UserProfile
import com.github.radupana.featherweight.data.programme.ExerciseSubstitution
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.data.programme.ProgrammeWeek
import com.github.radupana.featherweight.data.programme.ProgrammeWorkout

@Database(
    entities = [
        Workout::class,
        ExerciseLog::class,
        SetLog::class,
        // Normalized exercise entities
        ExerciseCore::class,
        ExerciseVariation::class,
        VariationInstruction::class,
        VariationRelation::class,
        VariationAlias::class,
        VariationMuscle::class,
        // Other entities
        Programme::class,
        ProgrammeWeek::class,
        ProgrammeWorkout::class,
        ExerciseSubstitution::class,
        ProgrammeProgress::class,
        UserProfile::class,
        UserExerciseMax::class,
        OneRMHistory::class,
        ExerciseSwapHistory::class,
        ExercisePerformanceTracking::class,
        GlobalExerciseProgress::class,
        ExerciseCorrelation::class,
        PersonalRecord::class,
        TrainingAnalysis::class,
        ParseRequest::class,
    ],
    version = 75,
    exportSchema = false,
)
@TypeConverters(DateConverters::class, ExerciseTypeConverters::class)
abstract class FeatherweightDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    abstract fun exerciseLogDao(): ExerciseLogDao

    abstract fun setLogDao(): SetLogDao

    // Normalized exercise DAOs
    abstract fun exerciseDao(): ExerciseDao // Consolidated DAO

    abstract fun exerciseCoreDao(): ExerciseCoreDao

    abstract fun exerciseVariationDao(): ExerciseVariationDao

    abstract fun variationInstructionDao(): VariationInstructionDao

    abstract fun variationRelationDao(): VariationRelationDao

    abstract fun variationAliasDao(): VariationAliasDao

    abstract fun variationMuscleDao(): VariationMuscleDao

    abstract fun programmeDao(): ProgrammeDao

    abstract fun profileDao(): ProfileDao

    abstract fun oneRMDao(): OneRMDao

    abstract fun exerciseSwapHistoryDao(): ExerciseSwapHistoryDao

    abstract fun exercisePerformanceTrackingDao(): ExercisePerformanceTrackingDao

    abstract fun globalExerciseProgressDao(): GlobalExerciseProgressDao

    abstract fun exerciseCorrelationDao(): ExerciseCorrelationDao

    abstract fun personalRecordDao(): PersonalRecordDao

    abstract fun trainingAnalysisDao(): TrainingAnalysisDao

    abstract fun parseRequestDao(): ParseRequestDao

    companion object {
        @Volatile
        private var INSTANCE: FeatherweightDatabase? = null

        fun getDatabase(context: Context): FeatherweightDatabase =
            INSTANCE ?: synchronized(this) {
                val instance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            FeatherweightDatabase::class.java,
                            "featherweight-db",
                        ).fallbackToDestructiveMigration()
                        .build()
                INSTANCE = instance
                instance
            }
    }
}

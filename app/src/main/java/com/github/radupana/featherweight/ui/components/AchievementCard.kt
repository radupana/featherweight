package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.radupana.featherweight.data.achievement.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun AchievementCard(
    achievement: Achievement,
    userAchievement: UserAchievement? = null,
    onShare: ((Achievement, UserAchievement) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUnlocked = userAchievement != null
    
    // Animation states
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "card_scale"
    )
    
    // Unlock animation
    val unlockScale by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "unlock_scale"
    )
    
    // Color scheme based on achievement difficulty and lock status
    val cardColors = when {
        !isUnlocked -> {
            // Locked achievement
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        achievement.difficulty == AchievementDifficulty.LEGENDARY -> {
            // Legendary achievement - special gradient
            CardDefaults.cardColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
        achievement.difficulty == AchievementDifficulty.HARD -> {
            // Hard achievement - gold-ish
            CardDefaults.cardColors(
                containerColor = Color(0xFFFFD700).copy(alpha = 0.1f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }
        else -> {
            // Standard unlocked achievement
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
    
    Card(
        modifier = modifier
            .scale(scale * unlockScale)
            .clickable(enabled = onClick != null) {
                isPressed = true
                onClick?.invoke()
            }
            .fillMaxWidth(),
        colors = cardColors,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isUnlocked) 4.dp else 1.dp
        )
    ) {
        // Legendary achievements get a special gradient background
        if (isUnlocked && achievement.difficulty == AchievementDifficulty.LEGENDARY) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.2f),
                                Color(0xFFFF6B35).copy(alpha = 0.2f),
                                Color(0xFF8A2BE2).copy(alpha = 0.2f)
                            )
                        )
                    )
            ) {
                AchievementCardContent(
                    achievement = achievement,
                    userAchievement = userAchievement,
                    isUnlocked = isUnlocked,
                    onShare = onShare,
                    onPressed = { isPressed = it }
                )
            }
        } else {
            AchievementCardContent(
                achievement = achievement,
                userAchievement = userAchievement,
                isUnlocked = isUnlocked,
                onShare = onShare,
                onPressed = { isPressed = it }
            )
        }
    }
}

@Composable
private fun AchievementCardContent(
    achievement: Achievement,
    userAchievement: UserAchievement?,
    isUnlocked: Boolean,
    onShare: ((Achievement, UserAchievement) -> Unit)?,
    onPressed: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon and Difficulty Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Achievement Icon with unlock animation
                Text(
                    text = if (isUnlocked) achievement.icon else "🔒",
                    fontSize = 32.sp,
                    modifier = Modifier.animateContentSize()
                )
                
                // Difficulty Badge
                DifficultyBadge(
                    difficulty = achievement.difficulty,
                    isUnlocked = isUnlocked
                )
            }
            
            // Share Button (only for unlocked achievements)
            if (isUnlocked && onShare != null && userAchievement != null) {
                IconButton(
                    onClick = { 
                        onPressed(true)
                        onShare(achievement, userAchievement)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Achievement",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Title and Description
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isUnlocked) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
            
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isUnlocked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
        }
        
        // Unlock Date and Context (for unlocked achievements)
        if (isUnlocked && userAchievement != null) {
            Spacer(modifier = Modifier.height(4.dp))
            
            UnlockInfo(userAchievement = userAchievement)
        } else {
            // Progress hint for locked achievements
            ProgressHint(achievement = achievement)
        }
    }
}

@Composable
private fun DifficultyBadge(
    difficulty: AchievementDifficulty,
    isUnlocked: Boolean
) {
    val (text, color) = when (difficulty) {
        AchievementDifficulty.EASY -> "Easy" to Color(0xFF4CAF50)
        AchievementDifficulty.MEDIUM -> "Medium" to Color(0xFFFF9800)
        AchievementDifficulty.HARD -> "Hard" to Color(0xFFE91E63)
        AchievementDifficulty.LEGENDARY -> "Legendary" to Color(0xFF9C27B0)
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isUnlocked) color.copy(alpha = 0.2f) else color.copy(alpha = 0.1f),
        modifier = Modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (isUnlocked) color else color.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun UnlockInfo(userAchievement: UserAchievement) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "Unlocked ${userAchievement.unlockedDate.format(dateFormatter)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        
        // Show context data if available
        userAchievement.data?.let { contextData ->
            // Parse and display context data
            Text(
                text = parseContextData(contextData),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ProgressHint(achievement: Achievement) {
    // Show hints for achieving locked achievements
    val hintText = when (val condition = achievement.condition) {
        is AchievementCondition.WeightThreshold -> 
            "Reach ${condition.weightKg.toInt()}kg on ${condition.exerciseName}"
        is AchievementCondition.BodyweightMultiple -> 
            "Lift ${condition.multiplier}x your bodyweight on ${condition.exerciseName}"
        is AchievementCondition.WorkoutStreak -> 
            "Workout for ${condition.days} consecutive days"
        is AchievementCondition.SingleWorkoutVolume -> 
            "Lift ${condition.totalKg.toInt()}kg total in one workout"
        is AchievementCondition.SingleSetReps -> 
            "Complete ${condition.reps} reps in a single set"
        is AchievementCondition.StrengthGainPercentage -> 
            "Increase any exercise by ${condition.percentageGain.toInt()}% in ${condition.timeframeDays} days"
        is AchievementCondition.PlateauBreaker -> 
            "Break through a ${condition.stallsRequired}+ workout plateau"
    }
    
    Text(
        text = hintText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        textAlign = TextAlign.Start
    )
}

private fun parseContextData(jsonData: String): String {
    // Simple JSON parsing for context display
    return try {
        when {
            jsonData.contains("weight") && jsonData.contains("exercise") -> {
                val weight = jsonData.substringAfter("\"weight\": ").substringBefore(",").toFloatOrNull()
                val exercise = jsonData.substringAfter("\"exercise\": \"").substringBefore("\"")
                if (weight != null) "${weight.toInt()}kg on $exercise" else ""
            }
            jsonData.contains("streak") -> {
                val streak = jsonData.substringAfter("\"streak\": ").substringBefore(",").toIntOrNull()
                if (streak != null) "$streak day streak" else ""
            }
            jsonData.contains("volume") -> {
                val volume = jsonData.substringAfter("\"volume\": ").substringBefore(",").toFloatOrNull()
                if (volume != null) "${volume.toInt()}kg total volume" else ""
            }
            jsonData.contains("reps") -> {
                val reps = jsonData.substringAfter("\"reps\": ").substringBefore(",").toIntOrNull()
                if (reps != null) "$reps reps in one set" else ""
            }
            else -> ""
        }
    } catch (e: Exception) {
        ""
    }
}
package com.github.radupana.featherweight.ui.components.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.github.radupana.featherweight.domain.WorkoutDayInfo
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarView(
    selectedDate: LocalDate?,
    workoutDayInfo: Map<LocalDate, WorkoutDayInfo>,
    modifier: Modifier = Modifier,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit = {},
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(12) }
    val endMonth = remember { currentMonth.plusMonths(12) }
    val daysOfWeek = remember { daysOfWeek() }
    val coroutineScope = rememberCoroutineScope()

    val state =
        rememberCalendarState(
            startMonth = startMonth,
            endMonth = endMonth,
            firstVisibleMonth = currentMonth,
            firstDayOfWeek = daysOfWeek.first(),
        )

    // Notify when the visible month changes
    LaunchedEffect(state.firstVisibleMonth.yearMonth) {
        onMonthChanged(state.firstVisibleMonth.yearMonth)
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Calendar header with month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.minusMonths(1))
                        }
                    },
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                Text(
                    text =
                        state.firstVisibleMonth.yearMonth.format(
                            DateTimeFormatter.ofPattern("MMMM yyyy"),
                        ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.plusMonths(1))
                        }
                    },
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Days of week header
            Row(modifier = Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { dayOfWeek ->
                    Text(
                        text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar
            HorizontalCalendar(
                state = state,
                dayContent = { calendarDay ->
                    val dayInfo = workoutDayInfo[calendarDay.date]
                    CalendarDayContent(
                        calendarDay = calendarDay,
                        isSelected = selectedDate == calendarDay.date,
                        workoutDayInfo = dayInfo,
                        isToday = calendarDay.date == LocalDate.now(),
                        onClick = { if (calendarDay.position == DayPosition.MonthDate) onDateSelected(calendarDay.date) },
                    )
                },
            )
        }
    }
}

@Composable
private fun CalendarDayContent(
    calendarDay: CalendarDay,
    isSelected: Boolean,
    workoutDayInfo: WorkoutDayInfo?,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val isInCurrentMonth = calendarDay.position == DayPosition.MonthDate

    Box(
        modifier =
            Modifier
                .aspectRatio(1f)
                .padding(2.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primaryContainer
                        else -> Color.Transparent
                    },
                ).clickable(enabled = isInCurrentMonth) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = calendarDay.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color =
                    when {
                        !isInCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )

            // Workout indicators based on status
            if (isInCurrentMonth && workoutDayInfo != null) {
                WorkoutIndicators(
                    dayInfo = workoutDayInfo,
                    isSelected = isSelected,
                    isToday = isToday,
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun WorkoutIndicators(
    dayInfo: WorkoutDayInfo,
    isSelected: Boolean,
    isToday: Boolean,
) {
    val totalWorkouts = dayInfo.completedCount + dayInfo.inProgressCount

    when {
        totalWorkouts == 0 -> {
            // No indicator for days without workouts
            Spacer(modifier = Modifier.height(4.dp))
        }
        totalWorkouts == 1 -> {
            // Single dot indicator
            val isCompleted = dayInfo.completedCount > 0
            Box(
                modifier =
                    Modifier
                        .size(4.dp)
                        .background(
                            color =
                                when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else ->
                                        if (isCompleted) {
                                            MaterialTheme.colorScheme.tertiary
                                        } else {
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                                        }
                                },
                            shape = CircleShape,
                        ).then(
                            if (!isCompleted && !isSelected && !isToday) {
                                // Hollow dot for in-progress workouts
                                Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = CircleShape,
                                    ).padding(0.5.dp)
                            } else {
                                Modifier
                            },
                        ),
            )
        }
        else -> {
            // Multiple workouts - show number badge
            Text(
                text = totalWorkouts.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color =
                    when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.tertiary
                    },
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

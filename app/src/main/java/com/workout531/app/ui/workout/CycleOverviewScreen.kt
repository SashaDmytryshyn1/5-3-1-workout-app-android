package com.workout531.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workout531.app.data.MainLift
import com.workout531.app.data.Week
import com.workout531.app.ui.AppViewModel
import com.workout531.app.util.WorkoutCalculator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleOverviewScreen(
    viewModel: AppViewModel,
    onWorkoutClick: (Week, MainLift) -> Unit,
    onSettingsClick: () -> Unit,
    onNextCycle: () -> Unit
) {
    val cycle = viewModel.state.currentCycle ?: return
    var showNextCycleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cycle ${cycle.number}") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Training maxes summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Training Maxes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("OHP: ${WorkoutCalculator.roundWeight(cycle.maxes.ohp, viewModel.state.roundTo).toInt()} ${viewModel.state.unit}")
                            Text("Squat: ${WorkoutCalculator.roundWeight(cycle.maxes.squat, viewModel.state.roundTo).toInt()} ${viewModel.state.unit}")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bench: ${WorkoutCalculator.roundWeight(cycle.maxes.bench, viewModel.state.roundTo).toInt()} ${viewModel.state.unit}")
                            Text("Deadlift: ${WorkoutCalculator.roundWeight(cycle.maxes.deadlift, viewModel.state.roundTo).toInt()} ${viewModel.state.unit}")
                        }
                    }
                    // Starting weight display
                    viewModel.state.startingBodyWeight?.let { weight ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Text(
                                "Starting Weight: ${weight.toInt()} ${viewModel.state.unit}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                            viewModel.state.profileCreatedDate?.let { date ->
                                Text(
                                    "  (since $date)",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Workout summary stats
            WorkoutSummaryCard(viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            // Calendar tracker
            WorkoutCalendar(viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            // Workout grid
            for (week in Week.entries) {
                Text(
                    week.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                for (lift in WorkoutCalculator.dayOrder) {
                    val isCompleted = viewModel.isWorkoutCompleted(week, lift)
                    val hitTarget = viewModel.didHitTargetReps(week, lift)

                    // Green = completed and hit all target reps
                    // Yellow = completed but missed target reps on the main set
                    // Default = not started
                    val completedAndHit = isCompleted && hitTarget == true
                    val completedButMissed = isCompleted && hitTarget == false

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable { onWorkoutClick(week, lift) },
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                completedAndHit -> Color(0xFF1B5E20).copy(alpha = 0.35f)
                                completedButMissed -> Color(0xFFF9A825).copy(alpha = 0.25f)
                                isCompleted -> Color(0xFF1B5E20).copy(alpha = 0.35f) // deload or no AMRAP
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = when {
                            completedAndHit -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f))
                            completedButMissed -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEB3B).copy(alpha = 0.5f))
                            isCompleted -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f))
                            else -> null
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = when {
                                    completedAndHit -> Color(0xFF4CAF50)
                                    completedButMissed -> Color(0xFFFFEB3B)
                                    isCompleted -> Color(0xFF4CAF50)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    lift.displayName,
                                    fontSize = 16.sp
                                )
                                if (completedButMissed) {
                                    Text(
                                        "Missed target reps",
                                        fontSize = 12.sp,
                                        color = Color(0xFFFFEB3B)
                                    )
                                }
                            }
                            val tm = WorkoutCalculator.getTrainingMax(lift, cycle.maxes)
                            Text(
                                "TM: ${WorkoutCalculator.roundWeight(tm, viewModel.state.roundTo).toInt()}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Next cycle button
            Button(
                onClick = { showNextCycleDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Complete Cycle & Progress", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showNextCycleDialog) {
        NextCycleDialog(
            viewModel = viewModel,
            onDismiss = { showNextCycleDialog = false },
            onConfirm = { liftsToProgress ->
                showNextCycleDialog = false
                viewModel.nextCycle(liftsToProgress)
                onNextCycle()
            }
        )
    }
}

@Composable
fun WorkoutSummaryCard(viewModel: AppViewModel) {
    val cycle = viewModel.state.currentCycle ?: return

    // Count completed workouts in current cycle
    val totalWorkoutsInCycle = Week.entries.size * WorkoutCalculator.dayOrder.size // 4 weeks x 4 lifts = 16
    var completedCount = 0
    var partialCount = 0
    for (week in Week.entries) {
        for (lift in WorkoutCalculator.dayOrder) {
            val allDone = viewModel.areAllSetsCompleted(week, lift)
            val mainDone = viewModel.isWorkoutCompleted(week, lift)
            if (allDone) completedCount++
            else if (mainDone) partialCount++
        }
    }
    val missedCount = totalWorkoutsInCycle - completedCount - partialCount

    // All-time stats from workout log
    val allTimeWorkouts = viewModel.state.workoutLog.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Cycle ${cycle.number} Progress",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            val progress = (completedCount + partialCount).toFloat() / totalWorkoutsInCycle
            @Suppress("DEPRECATION")
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF4CAF50),
                trackColor = MaterialTheme.colorScheme.surface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "$completedCount",
                    label = "Done",
                    color = Color(0xFF4CAF50)
                )
                StatItem(
                    value = "$partialCount",
                    label = "Partial",
                    color = Color(0xFFFF9800)
                )
                StatItem(
                    value = "$missedCount",
                    label = "Remaining",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatItem(
                    value = "$totalWorkoutsInCycle",
                    label = "Total",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (allTimeWorkouts > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "All-time workouts logged: $allTimeWorkouts",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WorkoutCalendar(viewModel: AppViewModel) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // Collect all workout dates from the log
    val workoutDates = remember(viewModel.state.workoutLog) {
        viewModel.state.workoutLog.map { it.date }.toSet()
    }

    // Also include dates from current cycle's completed workouts
    val currentCycleDates = remember(viewModel.state.currentCycle) {
        viewModel.state.currentCycle?.completedWorkouts?.values
            ?.map { it.completedDate }?.toSet() ?: emptySet()
    }

    val allWorkoutDates = workoutDates + currentCycleDates

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month navigation header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Text("<", fontSize = 18.sp)
                }
                Text(
                    "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                TextButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Text(">", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day of week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                for (day in daysOfWeek) {
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Calendar grid
            val firstDay = currentMonth.atDay(1)
            val firstDayOfWeek = firstDay.dayOfWeek
            // Monday = 1, so offset = dayOfWeek.value - 1
            val startOffset = firstDayOfWeek.value - 1
            val daysInMonth = currentMonth.lengthOfMonth()
            val today = LocalDate.now()

            val totalCells = startOffset + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - startOffset + 1

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNum in 1..daysInMonth) {
                                val date = currentMonth.atDay(dayNum)
                                val dateStr = date.toString()
                                val hasWorkout = dateStr in allWorkoutDates
                                val isToday = date == today

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .then(
                                            if (hasWorkout) {
                                                Modifier.background(
                                                    Color(0xFF4CAF50).copy(alpha = 0.3f),
                                                    CircleShape
                                                )
                                            } else Modifier
                                        )
                                        .then(
                                            if (isToday) {
                                                Modifier.border(
                                                    1.5.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    CircleShape
                                                )
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "$dayNum",
                                            fontSize = 13.sp,
                                            fontWeight = if (hasWorkout) FontWeight.Bold else FontWeight.Normal,
                                            color = if (hasWorkout) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Monthly summary
            val monthWorkouts = allWorkoutDates.count { dateStr ->
                try {
                    val date = LocalDate.parse(dateStr)
                    YearMonth.from(date) == currentMonth
                } catch (_: Exception) { false }
            }
            val workoutDaysInMonth = viewModel.state.workoutLog
                .filter {
                    try {
                        YearMonth.from(LocalDate.parse(it.date)) == currentMonth
                    } catch (_: Exception) { false }
                }
                .map { it.date }
                .distinct()
                .size +
                (viewModel.state.currentCycle?.completedWorkouts?.values
                    ?.filter {
                        try {
                            YearMonth.from(LocalDate.parse(it.completedDate)) == currentMonth
                        } catch (_: Exception) { false }
                    }
                    ?.map { it.completedDate }
                    ?.distinct()
                    ?.count { date -> viewModel.state.workoutLog.none { it.date == date } }
                    ?: 0)

            if (monthWorkouts > 0 || workoutDaysInMonth > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "$workoutDaysInMonth workout day${if (workoutDaysInMonth != 1) "s" else ""} this month",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NextCycleDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onConfirm: (Set<MainLift>) -> Unit
) {
    val unit = viewModel.state.unit
    val upperInc = if (unit == "kg") "2.5" else "5"
    val lowerInc = if (unit == "kg") "5" else "10"

    // Pre-select lifts based on whether they hit target reps
    val initialSelection = remember {
        WorkoutCalculator.dayOrder.filter { lift ->
            viewModel.didLiftHitAllTargets(lift) != false
        }.toMutableStateList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Next Cycle?") },
        text = {
            Column {
                Text(
                    "Select which lifts to increase. Lifts where you missed target reps are unchecked.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                for (lift in WorkoutCalculator.dayOrder) {
                    val isUpper = lift == MainLift.OHP || lift == MainLift.BENCH
                    val inc = if (isUpper) upperInc else lowerInc
                    val hitTargets = viewModel.didLiftHitAllTargets(lift)
                    val isChecked = lift in initialSelection

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) initialSelection.remove(lift)
                                else initialSelection.add(lift)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                if (it) initialSelection.add(lift)
                                else initialSelection.remove(lift)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                lift.displayName,
                                fontSize = 15.sp
                            )
                            Text(
                                if (isChecked) "+$inc $unit" else "No change",
                                fontSize = 12.sp,
                                color = if (isChecked) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Show indicator for whether they hit reps
                        when (hitTargets) {
                            true -> Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Hit all targets",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            false -> Text(
                                "Missed",
                                fontSize = 12.sp,
                                color = Color(0xFFFFEB3B)
                            )
                            null -> Text(
                                "N/A",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(initialSelection.toSet())
            }) { Text("Start Next Cycle") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

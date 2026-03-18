package com.workout531.app.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workout531.app.data.MainLift
import com.workout531.app.data.Week
import com.workout531.app.ui.AppViewModel
import com.workout531.app.util.WorkoutCalculator

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
                }
            }

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

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable { onWorkoutClick(week, lift) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCompleted)
                                Color(0xFF1B5E20).copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isCompleted) Icons.Filled.CheckCircle
                                else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (isCompleted) Color(0xFF4CAF50)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                lift.displayName,
                                modifier = Modifier.weight(1f),
                                fontSize = 16.sp
                            )
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
        AlertDialog(
            onDismissRequest = { showNextCycleDialog = false },
            title = { Text("Start Next Cycle?") },
            text = {
                val inc = if (viewModel.state.unit == "kg") "2.5/5" else "5/10"
                Text("Training maxes will increase by $inc ${viewModel.state.unit} (upper/lower).")
            },
            confirmButton = {
                TextButton(onClick = {
                    showNextCycleDialog = false
                    viewModel.nextCycle()
                    onNextCycle()
                }) { Text("Yes, Progress") }
            },
            dismissButton = {
                TextButton(onClick = { showNextCycleDialog = false }) { Text("Cancel") }
            }
        )
    }
}

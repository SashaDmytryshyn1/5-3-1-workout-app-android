package com.workout531.app.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
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
fun WorkoutScreen(
    viewModel: AppViewModel,
    week: Week,
    lift: MainLift,
    onBack: () -> Unit
) {
    val cycle = viewModel.state.currentCycle ?: return
    val trainingMax = WorkoutCalculator.getTrainingMax(lift, cycle.maxes)
    val sets = WorkoutCalculator.generateWorkout(lift, week, trainingMax, viewModel.state.roundTo)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${lift.displayName} - ${week.displayName}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
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
            // TM info
            Text(
                "Training Max: ${WorkoutCalculator.roundWeight(trainingMax, viewModel.state.roundTo).toInt()} ${viewModel.state.unit}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sets
            sets.forEachIndexed { index, set ->
                val completedReps = viewModel.getCompletedReps(week, lift, index)
                val isCompleted = completedReps != null

                SetCard(
                    setNumber = index + 1,
                    weight = set.weight,
                    targetReps = set.reps,
                    isAmrap = set.isAmrap,
                    isWarmup = set.isWarmup,
                    isCompleted = isCompleted,
                    completedReps = completedReps,
                    unit = viewModel.state.unit,
                    barWeight = viewModel.state.barWeight,
                    onComplete = { reps ->
                        viewModel.completeSet(week, lift, index, reps)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.isWorkoutCompleted(week, lift)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1B5E20).copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Workout Complete!",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50),
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SetCard(
    setNumber: Int,
    weight: Double,
    targetReps: Int,
    isAmrap: Boolean,
    isWarmup: Boolean,
    isCompleted: Boolean,
    completedReps: Int?,
    unit: String,
    barWeight: Double,
    onComplete: (Int) -> Unit
) {
    var showRepsDialog by remember { mutableStateOf(false) }
    var repsInput by remember { mutableStateOf(targetReps.toString()) }

    val plates = WorkoutCalculator.calculatePlates(weight, barWeight)
    val platesText = if (plates.isNotEmpty()) {
        plates.joinToString(" + ") { "${it.let { p -> if (p == p.toInt().toDouble()) p.toInt().toString() else p.toString() }}" } + " per side"
    } else {
        "Bar only"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> Color(0xFF1B5E20).copy(alpha = 0.2f)
                isWarmup -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Set info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isWarmup) {
                        Text(
                            "WARMUP",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        "${weight.toInt()} $unit",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = if (isWarmup) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    buildString {
                        append("$targetReps reps")
                        if (isAmrap) append(" (AMRAP)")
                    },
                    color = if (isAmrap) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    platesText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Complete button or completed indicator
            if (isCompleted) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Completed",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(28.dp)
                    )
                    if (completedReps != null) {
                        Text(
                            "$completedReps reps",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                FilledTonalButton(
                    onClick = {
                        if (isAmrap) {
                            repsInput = targetReps.toString()
                            showRepsDialog = true
                        } else {
                            onComplete(targetReps)
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Done")
                }
            }
        }
    }

    // AMRAP reps dialog
    if (showRepsDialog) {
        AlertDialog(
            onDismissRequest = { showRepsDialog = false },
            title = { Text("AMRAP Set - How many reps?") },
            text = {
                Column {
                    Text("Target: $targetReps+ reps at ${weight.toInt()} $unit")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = repsInput,
                        onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) repsInput = it },
                        label = { Text("Reps completed") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val reps = repsInput.toIntOrNull() ?: targetReps
                        onComplete(reps)
                        showRepsDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRepsDialog = false }) { Text("Cancel") }
            }
        )
    }
}

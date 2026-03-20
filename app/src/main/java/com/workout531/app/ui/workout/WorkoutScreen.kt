package com.workout531.app.ui.workout

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workout531.app.data.MainLift
import com.workout531.app.data.Week
import com.workout531.app.ui.AppViewModel
import com.workout531.app.util.RestTimerNotification
import com.workout531.app.util.WorkoutCalculator
import kotlinx.coroutines.delay

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
    val context = LocalContext.current

    // Rest timer state
    var restTimerActive by remember { mutableStateOf(false) }
    var restTimeRemaining by remember { mutableIntStateOf(viewModel.state.restTimerSeconds) }
    var showTimerSettings by remember { mutableStateOf(false) }

    // Rest timer countdown
    LaunchedEffect(restTimerActive) {
        if (restTimerActive) {
            while (restTimeRemaining > 0) {
                delay(1000L)
                restTimeRemaining--
            }
            if (restTimeRemaining <= 0) {
                // Play ding sound
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
                    // Play a second ding after a short pause for a pleasant chime
                    delay(600L)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                    delay(400L)
                    toneGen.release()
                } catch (_: Exception) { }
                // Show system notification (works even if app is in background)
                RestTimerNotification.showTimerDone(context)
                restTimerActive = false
            }
        }
    }

    // Secondary exercises
    val secondaryExercises = viewModel.getSecondaryExercises(lift)
    var showAddExerciseDialog by remember { mutableStateOf(false) }

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

            // Rest Timer Bar
            RestTimerBar(
                isActive = restTimerActive,
                timeRemaining = restTimeRemaining,
                totalTime = viewModel.state.restTimerSeconds,
                onDismiss = {
                    restTimerActive = false
                    restTimeRemaining = viewModel.state.restTimerSeconds
                },
                onSettingsClick = { showTimerSettings = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                        // Start rest timer
                        restTimeRemaining = viewModel.state.restTimerSeconds
                        restTimerActive = true
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Workout completion banner
            val workoutCompleted = viewModel.isWorkoutCompleted(week, lift)
            val allSetsCompleted = viewModel.areAllSetsCompleted(week, lift)

            if (workoutCompleted) {
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
                            tint = if (allSetsCompleted) Color(0xFF4CAF50) else Color(0xFFFFA726)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (allSetsCompleted) "Workout Complete!"
                            else "Workout Complete (some sets skipped)",
                            fontWeight = FontWeight.Bold,
                            color = if (allSetsCompleted) Color(0xFF4CAF50) else Color(0xFFFFA726),
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // --- Secondary Exercises Section ---
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Secondary Exercises",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (secondaryExercises.isEmpty()) {
                Text(
                    "No secondary exercises added yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            secondaryExercises.forEach { exercise ->
                val completedSets = viewModel.getSecondaryCompletedSets(week, lift, exercise.id)

                SecondaryExerciseCard(
                    exercise = exercise,
                    completedSets = completedSets,
                    unit = viewModel.state.unit,
                    onCompleteSet = {
                        viewModel.completeSecondarySet(week, lift, exercise.id)
                        // Start rest timer for secondary exercises too
                        restTimeRemaining = viewModel.state.restTimerSeconds
                        restTimerActive = true
                    },
                    onRemove = {
                        viewModel.removeSecondaryExercise(lift, exercise.id)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showAddExerciseDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Secondary Exercise")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Timer settings dialog
    if (showTimerSettings) {
        TimerSettingsDialog(
            currentSeconds = viewModel.state.restTimerSeconds,
            onDismiss = { showTimerSettings = false },
            onSave = { seconds ->
                viewModel.updateRestTimer(seconds)
                showTimerSettings = false
            }
        )
    }

    // Add secondary exercise dialog
    if (showAddExerciseDialog) {
        AddSecondaryExerciseDialog(
            unit = viewModel.state.unit,
            onDismiss = { showAddExerciseDialog = false },
            onAdd = { name, numSets, reps, weight ->
                viewModel.addSecondaryExercise(lift, name, numSets, reps, weight)
                showAddExerciseDialog = false
            }
        )
    }
}

@Composable
fun RestTimerBar(
    isActive: Boolean,
    timeRemaining: Int,
    totalTime: Int,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))

                if (isActive) {
                    val minutes = timeRemaining / 60
                    val seconds = timeRemaining % 60
                    Text(
                        "Rest: ${minutes}:%02d".format(seconds),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Skip")
                    }
                } else {
                    Text(
                        "Rest Timer: ${totalTime}s",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Timer settings",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (isActive) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = timeRemaining.toFloat() / totalTime.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSettingsDialog(
    currentSeconds: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var input by remember { mutableStateOf(currentSeconds.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rest Timer") },
        text = {
            Column {
                Text("Set default rest time (seconds):")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) input = it },
                    label = { Text("Seconds") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(60, 90, 120, 180).forEach { preset ->
                        FilterChip(
                            selected = input == preset.toString(),
                            onClick = { input = preset.toString() },
                            label = { Text("${preset}s") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val secs = input.toIntOrNull() ?: currentSeconds
                onSave(secs.coerceIn(10, 600))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SecondaryExerciseCard(
    exercise: com.workout531.app.data.SecondaryExercise,
    completedSets: Int,
    unit: String,
    onCompleteSet: () -> Unit,
    onRemove: () -> Unit
) {
    var showRemoveConfirm by remember { mutableStateOf(false) }
    val allDone = completedSets >= exercise.sets

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (allDone) Color(0xFF1B5E20).copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exercise.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                val weightText = if (exercise.weight < 0) "BW" else "${exercise.weight.toInt()} $unit"
                Text(
                    "${exercise.sets} × ${exercise.reps} @ $weightText",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Text(
                    "Sets done: $completedSets / ${exercise.sets}",
                    fontSize = 12.sp,
                    color = if (allDone) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!allDone) {
                FilledTonalButton(onClick = onCompleteSet) {
                    Text("Done")
                }
            } else {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "All sets complete",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = { showRemoveConfirm = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Remove Exercise") },
            text = { Text("Remove \"${exercise.name}\" from ${exercise.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    onRemove()
                    showRemoveConfirm = false
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AddSecondaryExerciseDialog(
    unit: String,
    onDismiss: () -> Unit,
    onAdd: (name: String, sets: Int, reps: Int, weight: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var setsInput by remember { mutableStateOf("3") }
    var repsInput by remember { mutableStateOf("10") }
    var weightInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Secondary Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = setsInput,
                        onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) setsInput = it },
                        label = { Text("Sets") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = repsInput,
                        onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) repsInput = it },
                        label = { Text("Reps") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = {
                        val lower = it.lowercase().trim()
                        if (it.isEmpty() || lower == "b" || lower == "bw" || it.toDoubleOrNull() != null) {
                            weightInput = it
                        }
                    },
                    label = { Text("Weight ($unit) or \"bw\"") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val weight = if (weightInput.lowercase().trim() == "bw") -1.0
                            else weightInput.toDoubleOrNull() ?: 0.0
                        onAdd(
                            name.trim(),
                            setsInput.toIntOrNull() ?: 3,
                            repsInput.toIntOrNull() ?: 10,
                            weight
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
                        if (isAmrap && completedReps > 1) {
                            val estimated1RM = WorkoutCalculator.calculate1RM(weight, completedReps)
                            Text(
                                "e1RM: ${estimated1RM.toInt()} $unit",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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
                    val enteredReps = repsInput.toIntOrNull()
                    if (enteredReps != null && enteredReps > 1) {
                        val estimated1RM = WorkoutCalculator.calculate1RM(weight, enteredReps)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Estimated 1RM: ${estimated1RM.toInt()} $unit",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
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

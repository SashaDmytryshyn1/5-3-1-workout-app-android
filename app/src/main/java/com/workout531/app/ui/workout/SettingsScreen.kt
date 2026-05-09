package com.workout531.app.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workout531.app.data.MainLift
import com.workout531.app.ui.AppViewModel
import com.workout531.app.util.WorkoutCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showCalcDialog by remember { mutableStateOf(false) }
    var calcWeight by remember { mutableStateOf("") }
    var calcReps by remember { mutableStateOf("") }
    var calcResult by remember { mutableStateOf<Double?>(null) }
    var editingLift by remember { mutableStateOf<MainLift?>(null) }
    var editTMInput by remember { mutableStateOf("") }
    var showNotepad by remember { mutableStateOf(false) }
    var newNoteInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            // Current state info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current Program", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cycle: ${viewModel.state.currentCycleNumber}")
                    Text("Unit: ${viewModel.state.unit}")
                    Text("Bar Weight: ${viewModel.state.barWeight.toInt()} ${viewModel.state.unit}")
                    Text("TM Percentage: ${(viewModel.state.tmPercent * 100).toInt()}%")
                    Text("Rounding: ${viewModel.state.roundTo} ${viewModel.state.unit}")
                    viewModel.state.startingBodyWeight?.let { weight ->
                        Text("Starting Body Weight: ${weight.toInt()} ${viewModel.state.unit}")
                    }
                    viewModel.state.profileCreatedDate?.let { date ->
                        Text("Profile Created: $date")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Training maxes (editable)
            viewModel.state.currentCycle?.let { cycle ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Training Maxes", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        val lifts = listOf(
                            MainLift.OHP to cycle.maxes.ohp,
                            MainLift.SQUAT to cycle.maxes.squat,
                            MainLift.BENCH to cycle.maxes.bench,
                            MainLift.DEADLIFT to cycle.maxes.deadlift
                        )
                        lifts.forEach { (lift, tm) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${lift.displayName}: ${WorkoutCalculator.roundWeight(tm, viewModel.state.roundTo).toInt()} ${viewModel.state.unit}",
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        editingLift = lift
                                        editTMInput = WorkoutCalculator.roundWeight(tm, viewModel.state.roundTo).toInt().toString()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Edit ${lift.displayName} TM",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1RM Calculator
            OutlinedButton(
                onClick = { showCalcDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("1RM Calculator")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Improvement Notepad
            OutlinedButton(
                onClick = { showNotepad = !showNotepad },
                modifier = Modifier.fillMaxWidth()
            ) {
                val count = viewModel.state.improvementNotes.size
                Text(if (count > 0) "Improvement Notepad ($count)" else "Improvement Notepad")
            }

            if (showNotepad) {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ideas & Improvements", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        viewModel.state.improvementNotes.forEachIndexed { index, note ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    note,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 14.sp
                                )
                                IconButton(
                                    onClick = { viewModel.removeImprovementNote(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove note",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (viewModel.state.improvementNotes.isEmpty()) {
                            Text(
                                "No notes yet. Add ideas for app improvements!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newNoteInput,
                                onValueChange = { newNoteInput = it },
                                label = { Text("New idea...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (newNoteInput.isNotBlank()) {
                                        viewModel.addImprovementNote(newNoteInput.trim())
                                        newNoteInput = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add note")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Reset
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reset All Data")
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Data?") },
            text = { Text("This will delete all your workout data and maxes. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    viewModel.resetApp()
                    onBack()
                }) { Text("Reset", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit Training Max dialog
    editingLift?.let { lift ->
        AlertDialog(
            onDismissRequest = { editingLift = null },
            title = { Text("Edit ${lift.displayName} TM") },
            text = {
                Column {
                    Text("Enter new training max:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editTMInput,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) editTMInput = it },
                        label = { Text("Training Max (${viewModel.state.unit})") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newTM = editTMInput.toDoubleOrNull()
                    if (newTM != null && newTM > 0) {
                        viewModel.updateTrainingMax(lift, newTM)
                    }
                    editingLift = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingLift = null }) { Text("Cancel") }
            }
        )
    }

    if (showCalcDialog) {
        AlertDialog(
            onDismissRequest = { showCalcDialog = false },
            title = { Text("1RM Calculator") },
            text = {
                Column {
                    Text("Enter weight and reps to estimate your 1 rep max (Epley formula)")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = calcWeight,
                        onValueChange = { calcWeight = it },
                        label = { Text("Weight (${viewModel.state.unit})") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = calcReps,
                        onValueChange = { calcReps = it },
                        label = { Text("Reps") },
                        singleLine = true
                    )
                    calcResult?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Estimated 1RM: ${WorkoutCalculator.roundWeight(it, viewModel.state.roundTo).toInt()} ${viewModel.state.unit}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Training Max (90%): ${WorkoutCalculator.roundWeight(it * 0.9, viewModel.state.roundTo).toInt()} ${viewModel.state.unit}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val w = calcWeight.toDoubleOrNull()
                    val r = calcReps.toIntOrNull()
                    if (w != null && r != null) {
                        calcResult = WorkoutCalculator.calculate1RM(w, r)
                    }
                }) { Text("Calculate") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCalcDialog = false
                    calcResult = null
                    calcWeight = ""
                    calcReps = ""
                }) { Text("Close") }
            }
        )
    }
}

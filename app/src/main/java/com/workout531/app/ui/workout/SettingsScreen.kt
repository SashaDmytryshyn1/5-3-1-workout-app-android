package com.workout531.app.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Training maxes
            viewModel.state.currentCycle?.let { cycle ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Training Maxes", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("OHP: ${WorkoutCalculator.roundWeight(cycle.maxes.ohp, viewModel.state.roundTo).toInt()} ${viewModel.state.unit}")
                        Text("Squat: ${WorkoutCalculator.roundWeight(cycle.maxes.squat, viewModel.state.roundTo).toInt()} ${viewModel.state.unit}")
                        Text("Bench: ${WorkoutCalculator.roundWeight(cycle.maxes.bench, viewModel.state.roundTo).toInt()} ${viewModel.state.unit}")
                        Text("Deadlift: ${WorkoutCalculator.roundWeight(cycle.maxes.deadlift, viewModel.state.roundTo).toInt()} ${viewModel.state.unit}")
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

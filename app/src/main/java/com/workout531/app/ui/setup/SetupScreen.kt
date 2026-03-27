package com.workout531.app.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workout531.app.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(viewModel: AppViewModel, onComplete: () -> Unit) {
    var ohp by remember { mutableStateOf("") }
    var squat by remember { mutableStateOf("") }
    var bench by remember { mutableStateOf("") }
    var deadlift by remember { mutableStateOf("") }
    var bodyWeight by remember { mutableStateOf("") }
    var useTrainingMax by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("5/3/1 Setup") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Enter Your Maxes",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Enter your 1 rep maxes (or known training maxes)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = useTrainingMax,
                    onCheckedChange = { useTrainingMax = it }
                )
                Text("These are already training maxes (don't multiply by 90%)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            MaxInput("Overhead Press (${viewModel.state.unit})", ohp) { ohp = it }
            MaxInput("Squat (${viewModel.state.unit})", squat) { squat = it }
            MaxInput("Bench Press (${viewModel.state.unit})", bench) { bench = it }
            MaxInput("Deadlift (${viewModel.state.unit})", deadlift) { deadlift = it }

            Spacer(modifier = Modifier.height(16.dp))

            MaxInput("Body Weight (${viewModel.state.unit}) - optional", bodyWeight) { bodyWeight = it }

            Spacer(modifier = Modifier.height(24.dp))

            val allFilled = listOf(ohp, squat, bench, deadlift).all {
                it.toDoubleOrNull() != null && it.toDouble() > 0
            }

            Button(
                onClick = {
                    val o = ohp.toDouble()
                    val s = squat.toDouble()
                    val b = bench.toDouble()
                    val d = deadlift.toDouble()
                    val bw = bodyWeight.toDoubleOrNull()
                    if (useTrainingMax) {
                        viewModel.updateSettings(
                            viewModel.state.unit,
                            viewModel.state.barWeight,
                            1.0
                        )
                        viewModel.setupMaxes(o, s, b, d, bw)
                    } else {
                        viewModel.setupMaxes(o, s, b, d, bw)
                    }
                    onComplete()
                },
                enabled = allFilled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Start Program", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun MaxInput(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { newVal ->
            if (newVal.isEmpty() || newVal.toDoubleOrNull() != null) {
                onValueChange(newVal)
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true
    )
}

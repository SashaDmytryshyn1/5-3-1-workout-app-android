package com.workout531.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.workout531.app.data.*
import com.workout531.app.util.WorkoutCalculator
import java.time.LocalDate

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = DataStore(application)

    var state by mutableStateOf(dataStore.loadState())
        private set

    fun save() = dataStore.saveState(state)

    fun setupMaxes(ohp: Double, squat: Double, bench: Double, deadlift: Double) {
        val oneRepMaxes = LiftMaxes(ohp, squat, bench, deadlift)
        val trainingMaxes = oneRepMaxes.trainingMaxes(state.tmPercent)
        state = state.copy(
            oneRepMaxes = oneRepMaxes,
            currentCycle = Cycle(number = state.currentCycleNumber, maxes = trainingMaxes),
            isSetup = true
        )
        save()
    }

    fun completeSet(week: Week, lift: MainLift, setIndex: Int, reps: Int) {
        val cycle = state.currentCycle ?: return
        val key = cycle.key(week, lift)
        val trainingMax = WorkoutCalculator.getTrainingMax(lift, cycle.maxes)
        val sets = WorkoutCalculator.generateWorkout(lift, week, trainingMax, state.roundTo)

        val existing = cycle.completedWorkouts[key]
        val completedSets = existing?.sets?.toMutableList() ?: mutableListOf()

        // Update or add the completed set
        val completedSet = CompletedSet(
            setNumber = setIndex + 1,
            weight = sets[setIndex].weight,
            targetReps = sets[setIndex].reps,
            completedReps = reps,
            isAmrap = sets[setIndex].isAmrap
        )

        val existingIdx = completedSets.indexOfFirst { it.setNumber == completedSet.setNumber }
        if (existingIdx >= 0) {
            completedSets[existingIdx] = completedSet
        } else {
            completedSets.add(completedSet)
        }

        val updatedWorkout = CompletedWorkout(
            mainLift = lift.name,
            week = week.name,
            sets = completedSets,
            completedDate = LocalDate.now().toString()
        )

        val updatedWorkouts = cycle.completedWorkouts.toMutableMap()
        updatedWorkouts[key] = updatedWorkout

        state = state.copy(
            currentCycle = cycle.copy(completedWorkouts = updatedWorkouts)
        )
        save() // Auto-save on every set completion!
    }

    fun isWorkoutCompleted(week: Week, lift: MainLift): Boolean {
        val cycle = state.currentCycle ?: return false
        val key = cycle.key(week, lift)
        val completed = cycle.completedWorkouts[key] ?: return false
        val trainingMax = WorkoutCalculator.getTrainingMax(lift, cycle.maxes)
        val totalSets = WorkoutCalculator.generateWorkout(lift, week, trainingMax, state.roundTo).size
        return completed.sets.size >= totalSets
    }

    fun getCompletedReps(week: Week, lift: MainLift, setIndex: Int): Int? {
        val cycle = state.currentCycle ?: return null
        val key = cycle.key(week, lift)
        val completed = cycle.completedWorkouts[key] ?: return null
        return completed.sets.find { it.setNumber == setIndex + 1 }?.completedReps
    }

    fun nextCycle() {
        val newMaxes = WorkoutCalculator.progressMaxes(
            state.currentCycle?.maxes ?: return,
            state.unit
        )
        val newCycleNumber = state.currentCycleNumber + 1
        state = state.copy(
            currentCycleNumber = newCycleNumber,
            currentCycle = Cycle(number = newCycleNumber, maxes = newMaxes)
        )
        save()
    }

    fun updateSettings(unit: String, barWeight: Double, tmPercent: Double) {
        state = state.copy(
            unit = unit,
            barWeight = barWeight,
            tmPercent = tmPercent,
            roundTo = if (unit == "kg") 2.5 else 5.0
        )
        save()
    }

    fun resetApp() {
        state = AppState()
        save()
    }
}

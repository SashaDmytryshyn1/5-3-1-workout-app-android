package com.workout531.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.workout531.app.data.*
import com.workout531.app.util.WorkoutCalculator
import java.time.LocalDate
import java.util.UUID

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
        save()
    }

    /**
     * Workout is "complete" if the last main (non-warmup) set is done.
     */
    fun isWorkoutCompleted(week: Week, lift: MainLift): Boolean {
        val cycle = state.currentCycle ?: return false
        val key = cycle.key(week, lift)
        val completed = cycle.completedWorkouts[key] ?: return false
        val trainingMax = WorkoutCalculator.getTrainingMax(lift, cycle.maxes)
        val allSets = WorkoutCalculator.generateWorkout(lift, week, trainingMax, state.roundTo)
        // Last set index (0-based) = allSets.size - 1, setNumber = allSets.size
        val lastSetNumber = allSets.size
        return completed.sets.any { it.setNumber == lastSetNumber }
    }

    /**
     * All sets (including warmups) are completed.
     */
    fun areAllSetsCompleted(week: Week, lift: MainLift): Boolean {
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
            currentCycle = Cycle(number = newCycleNumber, maxes = newMaxes),
            // Clear secondary exercise progress for the new cycle but keep templates
            secondaryExerciseProgress = emptyMap()
        )
        save()
    }

    fun updateTrainingMax(lift: MainLift, newTM: Double) {
        val cycle = state.currentCycle ?: return
        val updatedMaxes = when (lift) {
            MainLift.OHP -> cycle.maxes.copy(ohp = newTM)
            MainLift.SQUAT -> cycle.maxes.copy(squat = newTM)
            MainLift.BENCH -> cycle.maxes.copy(bench = newTM)
            MainLift.DEADLIFT -> cycle.maxes.copy(deadlift = newTM)
        }
        state = state.copy(currentCycle = cycle.copy(maxes = updatedMaxes))
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

    fun updateRestTimer(seconds: Int) {
        state = state.copy(restTimerSeconds = seconds)
        save()
    }

    fun resetApp() {
        state = AppState()
        save()
    }

    // --- Secondary Exercise Methods ---

    fun getSecondaryExercises(lift: MainLift): List<SecondaryExercise> {
        return state.secondaryExercises[lift.name] ?: emptyList()
    }

    fun addSecondaryExercise(lift: MainLift, name: String, sets: Int, reps: Int, weight: Double) {
        val current = state.secondaryExercises.toMutableMap()
        val exercises = (current[lift.name] ?: emptyList()).toMutableList()
        exercises.add(SecondaryExercise(
            id = UUID.randomUUID().toString(),
            name = name,
            sets = sets,
            reps = reps,
            weight = weight
        ))
        current[lift.name] = exercises
        state = state.copy(secondaryExercises = current)
        save()
    }

    fun removeSecondaryExercise(lift: MainLift, exerciseId: String) {
        val current = state.secondaryExercises.toMutableMap()
        val exercises = (current[lift.name] ?: emptyList()).filter { it.id != exerciseId }
        current[lift.name] = exercises
        state = state.copy(secondaryExercises = current)
        save()
    }

    fun getSecondaryProgress(week: Week, lift: MainLift): List<SecondaryExercise> {
        val key = "${state.currentCycleNumber}_${week.name}_${lift.name}"
        return state.secondaryExerciseProgress[key] ?: emptyList()
    }

    fun completeSecondarySet(week: Week, lift: MainLift, exerciseId: String) {
        val key = "${state.currentCycleNumber}_${week.name}_${lift.name}"
        val progressMap = state.secondaryExerciseProgress.toMutableMap()
        val progressList = (progressMap[key] ?: emptyList()).toMutableList()

        val templates = getSecondaryExercises(lift)
        val template = templates.find { it.id == exerciseId } ?: return

        val existing = progressList.indexOfFirst { it.id == exerciseId }
        if (existing >= 0) {
            val current = progressList[existing]
            if (current.completedSets < template.sets) {
                progressList[existing] = current.copy(completedSets = current.completedSets + 1)
            }
        } else {
            progressList.add(template.copy(completedSets = 1))
        }

        progressMap[key] = progressList
        state = state.copy(secondaryExerciseProgress = progressMap)
        save()
    }

    fun getSecondaryCompletedSets(week: Week, lift: MainLift, exerciseId: String): Int {
        val progress = getSecondaryProgress(week, lift)
        return progress.find { it.id == exerciseId }?.completedSets ?: 0
    }
}

package com.workout531.app.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.workout531.app.data.*
import com.workout531.app.util.RestTimerNotification
import com.workout531.app.util.WorkoutCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = DataStore(application)

    var state by mutableStateOf(dataStore.loadState())
        private set

    // Rest timer state - lives in ViewModel so it survives navigation
    var restTimerActive by mutableStateOf(false)
        private set
    var restTimeRemaining by mutableIntStateOf(0)
        private set
    private var timerJob: Job? = null

    fun startRestTimer(seconds: Int = state.restTimerSeconds) {
        timerJob?.cancel()
        restTimeRemaining = seconds
        restTimerActive = true
        timerJob = viewModelScope.launch {
            while (restTimeRemaining > 0) {
                delay(1000L)
                restTimeRemaining--
            }
            // Play ding sound
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
                delay(600L)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                delay(400L)
                toneGen.release()
            } catch (_: Exception) { }
            RestTimerNotification.showTimerDone(getApplication())
            restTimerActive = false
        }
    }

    fun cancelRestTimer() {
        timerJob?.cancel()
        restTimerActive = false
        restTimeRemaining = 0
    }

    fun save() = dataStore.saveState(state)

    fun setupMaxes(ohp: Double, squat: Double, bench: Double, deadlift: Double, bodyWeight: Double? = null) {
        val oneRepMaxes = LiftMaxes(ohp, squat, bench, deadlift)
        val trainingMaxes = oneRepMaxes.trainingMaxes(state.tmPercent)
        state = state.copy(
            oneRepMaxes = oneRepMaxes,
            currentCycle = Cycle(number = state.currentCycleNumber, maxes = trainingMaxes),
            isSetup = true,
            startingBodyWeight = bodyWeight ?: state.startingBodyWeight,
            profileCreatedDate = state.profileCreatedDate ?: LocalDate.now().toString()
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

        // Add to persistent workout log if this is the first set completed for this workout today
        val today = LocalDate.now().toString()
        val logEntry = WorkoutLogEntry(
            date = today,
            lift = lift.name,
            week = week.name,
            cycleNumber = state.currentCycleNumber
        )
        val alreadyLogged = state.workoutLog.any {
            it.date == today && it.lift == lift.name && it.week == week.name && it.cycleNumber == state.currentCycleNumber
        }
        val updatedLog = if (alreadyLogged) state.workoutLog else state.workoutLog + logEntry

        state = state.copy(
            currentCycle = cycle.copy(completedWorkouts = updatedWorkouts),
            workoutLog = updatedLog
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

    /**
     * Returns true if the user hit target reps on the last (AMRAP) set.
     * Returns null if the workout hasn't been completed yet.
     */
    fun didHitTargetReps(week: Week, lift: MainLift): Boolean? {
        val cycle = state.currentCycle ?: return null
        val key = cycle.key(week, lift)
        val completed = cycle.completedWorkouts[key] ?: return null
        val trainingMax = WorkoutCalculator.getTrainingMax(lift, cycle.maxes)
        val allSets = WorkoutCalculator.generateWorkout(lift, week, trainingMax, state.roundTo)
        val lastSetNumber = allSets.size
        val lastCompletedSet = completed.sets.find { it.setNumber == lastSetNumber } ?: return null
        return lastCompletedSet.completedReps >= lastCompletedSet.targetReps
    }

    /**
     * Check if a lift hit target reps on the AMRAP set in any non-deload week.
     * Returns true if ALL completed AMRAP sets for this lift hit target reps.
     * Returns null if no AMRAP workouts were completed for this lift.
     */
    fun didLiftHitAllTargets(lift: MainLift): Boolean? {
        val amrapWeeks = listOf(Week.WEEK1, Week.WEEK2, Week.WEEK3)
        val results = amrapWeeks.mapNotNull { week -> didHitTargetReps(week, lift) }
        if (results.isEmpty()) return null
        return results.all { it }
    }

    fun nextCycle(liftsToProgress: Set<MainLift> = MainLift.entries.toSet()) {
        val currentMaxes = state.currentCycle?.maxes ?: return
        val upperInc = if (state.unit == "kg") 2.5 else 5.0
        val lowerInc = if (state.unit == "kg") 5.0 else 10.0

        val newMaxes = LiftMaxes(
            ohp = if (MainLift.OHP in liftsToProgress) currentMaxes.ohp + upperInc else currentMaxes.ohp,
            squat = if (MainLift.SQUAT in liftsToProgress) currentMaxes.squat + lowerInc else currentMaxes.squat,
            bench = if (MainLift.BENCH in liftsToProgress) currentMaxes.bench + upperInc else currentMaxes.bench,
            deadlift = if (MainLift.DEADLIFT in liftsToProgress) currentMaxes.deadlift + lowerInc else currentMaxes.deadlift
        )

        val newCycleNumber = state.currentCycleNumber + 1
        state = state.copy(
            currentCycleNumber = newCycleNumber,
            currentCycle = Cycle(number = newCycleNumber, maxes = newMaxes),
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

    fun updateSecondaryRestTimer(seconds: Int) {
        state = state.copy(secondaryRestTimerSeconds = seconds)
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

    fun updateSecondaryExercise(lift: MainLift, exerciseId: String, name: String, sets: Int, reps: Int, weight: Double) {
        val current = state.secondaryExercises.toMutableMap()
        val exercises = (current[lift.name] ?: emptyList()).toMutableList()
        val idx = exercises.indexOfFirst { it.id == exerciseId }
        if (idx >= 0) {
            exercises[idx] = exercises[idx].copy(name = name, sets = sets, reps = reps, weight = weight)
            current[lift.name] = exercises
            state = state.copy(secondaryExercises = current)
            save()
        }
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

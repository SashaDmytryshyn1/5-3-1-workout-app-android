package com.workout531.app.data

data class LiftMaxes(
    val ohp: Double = 0.0,
    val squat: Double = 0.0,
    val bench: Double = 0.0,
    val deadlift: Double = 0.0
) {
    fun trainingMaxes(tmPercent: Double = 0.90): LiftMaxes = LiftMaxes(
        ohp = ohp * tmPercent,
        squat = squat * tmPercent,
        bench = bench * tmPercent,
        deadlift = deadlift * tmPercent
    )
}

enum class MainLift(val displayName: String) {
    OHP("Overhead Press"),
    SQUAT("Squat"),
    BENCH("Bench Press"),
    DEADLIFT("Deadlift")
}

enum class Week(val displayName: String, val weekNumber: Int) {
    WEEK1("Week 1 - 5/5/5", 1),
    WEEK2("Week 2 - 3/3/3", 2),
    WEEK3("Week 3 - 5/3/1", 3),
    DELOAD("Week 4 - Deload", 4)
}

data class WorkoutSet(
    val setNumber: Int,
    val percentage: Double,
    val reps: Int,
    val isAmrap: Boolean = false,
    val isWarmup: Boolean = false,
    val weight: Double = 0.0,
    val completedReps: Int? = null
) {
    val isCompleted: Boolean get() = completedReps != null
}

data class WorkoutDay(
    val mainLift: MainLift,
    val week: Week,
    val sets: List<WorkoutSet>,
    val isCompleted: Boolean = false,
    val completedDate: String? = null
)

data class Cycle(
    val number: Int,
    val maxes: LiftMaxes,
    val completedWorkouts: Map<String, CompletedWorkout> = emptyMap()
) {
    fun key(week: Week, lift: MainLift) = "${week.name}_${lift.name}"
}

data class CompletedWorkout(
    val mainLift: String,
    val week: String,
    val sets: List<CompletedSet>,
    val completedDate: String
)

data class CompletedSet(
    val setNumber: Int,
    val weight: Double,
    val targetReps: Int,
    val completedReps: Int,
    val isAmrap: Boolean
)

data class SecondaryExercise(
    val id: String = "",
    val name: String = "",
    val sets: Int = 3,
    val reps: Int = 10,
    val weight: Double = 0.0,
    val completedSets: Int = 0
)

data class WorkoutLogEntry(
    val date: String,
    val lift: String,
    val week: String,
    val cycleNumber: Int
)

data class AppState(
    val oneRepMaxes: LiftMaxes = LiftMaxes(),
    val tmPercent: Double = 0.90,
    val currentCycleNumber: Int = 1,
    val currentCycle: Cycle? = null,
    val isSetup: Boolean = false,
    val roundTo: Double = 5.0,
    val unit: String = "lbs",
    val barWeight: Double = 45.0,
    val restTimerSeconds: Int = 90,
    val secondaryRestTimerSeconds: Int = 60,
    // Key: MainLift.name -> list of secondary exercises (persists across cycles)
    val secondaryExercises: Map<String, List<SecondaryExercise>> = emptyMap(),
    // Key: "cycleNum_week_lift" -> list of secondary exercise completion states for current workout
    val secondaryExerciseProgress: Map<String, List<SecondaryExercise>> = emptyMap(),
    // Starting body weight when profile was created
    val startingBodyWeight: Double? = null,
    // Date when the profile was first created
    val profileCreatedDate: String? = null,
    // Persistent log of all completed workout dates (survives cycle transitions)
    val workoutLog: List<WorkoutLogEntry> = emptyList()
)

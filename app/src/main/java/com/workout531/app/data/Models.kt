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

data class AppState(
    val oneRepMaxes: LiftMaxes = LiftMaxes(),
    val tmPercent: Double = 0.90,
    val currentCycleNumber: Int = 1,
    val currentCycle: Cycle? = null,
    val isSetup: Boolean = false,
    val roundTo: Double = 5.0,
    val unit: String = "lbs",
    val barWeight: Double = 45.0
)

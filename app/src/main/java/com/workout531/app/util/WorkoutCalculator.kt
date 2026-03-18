package com.workout531.app.util

import com.workout531.app.data.*
import kotlin.math.round

object WorkoutCalculator {

    fun roundWeight(weight: Double, roundTo: Double): Double {
        return roundTo * round(weight / roundTo)
    }

    fun calculatePlates(totalWeight: Double, barWeight: Double): List<Double> {
        val perSide = (totalWeight - barWeight) / 2.0
        if (perSide <= 0) return emptyList()

        val availablePlates = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)
        val plates = mutableListOf<Double>()
        var remaining = perSide

        for (plate in availablePlates) {
            while (remaining >= plate) {
                plates.add(plate)
                remaining -= plate
            }
        }
        return plates
    }

    fun generateWorkout(
        lift: MainLift,
        week: Week,
        trainingMax: Double,
        roundTo: Double
    ): List<WorkoutSet> {
        val sets = mutableListOf<WorkoutSet>()

        // Warmup sets (skip for deload week - working sets are already light)
        if (week != Week.DELOAD) {
            val warmupPercentages = listOf(0.40, 0.50, 0.60)
            warmupPercentages.forEachIndexed { i, pct ->
                sets.add(WorkoutSet(
                    setNumber = i + 1,
                    percentage = pct,
                    reps = 5,
                    isWarmup = true,
                    weight = roundWeight(trainingMax * pct, roundTo)
                ))
            }
        }

        // Working sets based on week
        val workingSets = when (week) {
            Week.WEEK1 -> listOf(
                Triple(0.65, 5, false),
                Triple(0.75, 5, false),
                Triple(0.85, 5, true)  // AMRAP
            )
            Week.WEEK2 -> listOf(
                Triple(0.70, 3, false),
                Triple(0.80, 3, false),
                Triple(0.90, 3, true)  // AMRAP
            )
            Week.WEEK3 -> listOf(
                Triple(0.75, 5, false),
                Triple(0.85, 3, false),
                Triple(0.95, 1, true)  // AMRAP
            )
            Week.DELOAD -> listOf(
                Triple(0.40, 5, false),
                Triple(0.50, 5, false),
                Triple(0.60, 5, false)
            )
        }

        workingSets.forEachIndexed { i, (pct, reps, amrap) ->
            sets.add(WorkoutSet(
                setNumber = sets.size + 1,
                percentage = pct,
                reps = reps,
                isAmrap = amrap,
                weight = roundWeight(trainingMax * pct, roundTo)
            ))
        }

        return sets
    }

    fun getTrainingMax(lift: MainLift, maxes: LiftMaxes): Double {
        return when (lift) {
            MainLift.OHP -> maxes.ohp
            MainLift.SQUAT -> maxes.squat
            MainLift.BENCH -> maxes.bench
            MainLift.DEADLIFT -> maxes.deadlift
        }
    }

    fun progressMaxes(maxes: LiftMaxes, unit: String): LiftMaxes {
        val upperInc = if (unit == "kg") 2.5 else 5.0
        val lowerInc = if (unit == "kg") 5.0 else 10.0
        return LiftMaxes(
            ohp = maxes.ohp + upperInc,
            squat = maxes.squat + lowerInc,
            bench = maxes.bench + upperInc,
            deadlift = maxes.deadlift + lowerInc
        )
    }

    fun calculate1RM(weight: Double, reps: Int): Double {
        if (reps <= 0) return 0.0
        if (reps == 1) return weight
        return weight / (1.0278 - 0.0278 * reps) // Epley formula
    }

    // Standard 5/3/1 day order
    val dayOrder = listOf(MainLift.OHP, MainLift.SQUAT, MainLift.BENCH, MainLift.DEADLIFT)
}

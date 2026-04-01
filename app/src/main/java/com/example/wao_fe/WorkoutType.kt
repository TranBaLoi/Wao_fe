package com.example.wao_fe

import androidx.annotation.DrawableRes

// These presets are the single source of truth for feature 2 labels, UI icons, and session defaults.
enum class WorkoutType(
    val key: String,
    val title: String,
    @DrawableRes val iconRes: Int,
    val baseSpeedKmh: Double,
    val speedVarianceKmh: Double,
    val caloriesPerMinute: Double,
    val usesStepMetric: Boolean,
    val stepsPerKm: Int,
    val estimatedHeartRateRange: IntRange,
    val exerciseName: String,
) {
    WALKING(
        key = "walking",
        title = "Đi bộ",
        iconRes = R.drawable.ic_walk,
        baseSpeedKmh = 5.2,
        speedVarianceKmh = 0.4,
        caloriesPerMinute = 4.2,
        usesStepMetric = false,
        stepsPerKm = 1350,
        estimatedHeartRateRange = 96..118,
        exerciseName = "Đi bộ",
    ),
    OUTDOOR_RUNNING(
        key = "outdoor_running",
        title = "Chạy bộ ngoài trời",
        iconRes = R.drawable.ic_run,
        baseSpeedKmh = 9.4,
        speedVarianceKmh = 1.0,
        caloriesPerMinute = 10.3,
        usesStepMetric = false,
        stepsPerKm = 1520,
        estimatedHeartRateRange = 138..162,
        exerciseName = "Chạy bộ ngoài trời",
    ),
    INDOOR_RUNNING(
        key = "indoor_running",
        title = "Chạy bộ trong nhà",
        iconRes = R.drawable.ic_run,
        baseSpeedKmh = 8.6,
        speedVarianceKmh = 0.8,
        caloriesPerMinute = 9.6,
        usesStepMetric = true,
        stepsPerKm = 1550,
        estimatedHeartRateRange = 132..156,
        exerciseName = "Chạy bộ trong nhà",
    ),
    CYCLING(
        key = "cycling",
        title = "Đạp xe",
        iconRes = R.drawable.ic_bike,
        baseSpeedKmh = 20.5,
        speedVarianceKmh = 1.6,
        caloriesPerMinute = 8.4,
        usesStepMetric = false,
        stepsPerKm = 0,
        estimatedHeartRateRange = 122..148,
        exerciseName = "Đạp xe",
    );

    companion object {
        const val EXTRA_WORKOUT_TYPE = "extra_workout_type"

        fun fromKey(key: String?): WorkoutType {
            return values().firstOrNull { it.key == key } ?: WALKING
        }
    }
}

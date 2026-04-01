package com.example.wao_fe

import androidx.annotation.DrawableRes

// These presets are the single source of truth for feature 2 labels and live-tracking rules.
enum class WorkoutType(
    val key: String,
    val title: String,
    @DrawableRes val iconRes: Int,
    val usesStepMetric: Boolean,
    val usesGpsDistance: Boolean,
    val strideLengthFactor: Double,
    val metValue: Double,
    val maxReasonableGpsSpeedKmh: Double,
    val defaultCaloriesPerMinute: Double,
    val exerciseName: String,
) {
    WALKING(
        key = "walking",
        title = "Đi bộ",
        iconRes = R.drawable.ic_walk,
        usesStepMetric = false,
        usesGpsDistance = true,
        strideLengthFactor = 0.415,
        metValue = 3.8,
        maxReasonableGpsSpeedKmh = 8.5,
        defaultCaloriesPerMinute = 4.7,
        exerciseName = "Đi bộ",
    ),
    OUTDOOR_RUNNING(
        key = "outdoor_running",
        title = "Chạy bộ ngoài trời",
        iconRes = R.drawable.ic_run,
        usesStepMetric = false,
        usesGpsDistance = true,
        strideLengthFactor = 0.65,
        metValue = 9.8,
        maxReasonableGpsSpeedKmh = 24.0,
        defaultCaloriesPerMinute = 12.0,
        exerciseName = "Chạy bộ ngoài trời",
    ),
    INDOOR_RUNNING(
        key = "indoor_running",
        title = "Chạy bộ trong nhà",
        iconRes = R.drawable.ic_run,
        usesStepMetric = true,
        usesGpsDistance = false,
        strideLengthFactor = 0.65,
        metValue = 8.3,
        maxReasonableGpsSpeedKmh = 0.0,
        defaultCaloriesPerMinute = 10.2,
        exerciseName = "Chạy bộ trong nhà",
    ),
    CYCLING(
        key = "cycling",
        title = "Đạp xe",
        iconRes = R.drawable.ic_bike,
        usesStepMetric = false,
        usesGpsDistance = true,
        strideLengthFactor = 0.0,
        metValue = 7.5,
        maxReasonableGpsSpeedKmh = 45.0,
        defaultCaloriesPerMinute = 9.2,
        exerciseName = "Đạp xe",
    );

    companion object {
        const val EXTRA_WORKOUT_TYPE = "extra_workout_type"

        fun fromKey(key: String?): WorkoutType {
            return values().firstOrNull { it.key == key } ?: WALKING
        }
    }
}

//nam them
package com.example.wao_fe.namstats.models

data class DailyNutritionResponse(
    val userId: Long,
    val date: String,
    val totalCalories: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double
)

enum class StatisticsGroupBy {
    DAY,
    WEEK,
    MONTH
}

data class NutritionPoint(
    val bucketDate: String,
    val totalCalories: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double
)

data class NutritionSeriesResponse(
    val userId: Long,
    val from: String,
    val to: String,
    val groupBy: StatisticsGroupBy,
    val totalCalories: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val points: List<NutritionPoint>
)

data class WeightPoint(
    val bucketDate: String,
    val startWeight: Double? = null,
    val endWeight: Double? = null,
    val changeAmount: Double? = null,
    val logCount: Int
)

data class WeightSeriesResponse(
    val userId: Long,
    val from: String,
    val to: String,
    val groupBy: StatisticsGroupBy,
    val overallChange: Double? = null,
    val points: List<WeightPoint>
)

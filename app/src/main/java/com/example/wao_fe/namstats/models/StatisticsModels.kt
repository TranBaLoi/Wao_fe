//nam them
package com.example.wao_fe.namstats.models

import com.google.gson.annotations.SerializedName

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
    @SerializedName(
        value = "bucketDate",
        alternate = ["bucket_date", "loggedAt", "logged_at", "logDate", "log_date"]
    )
    val bucketDate: String,
    @SerializedName(value = "startWeight", alternate = ["start_weight", "oldWeight", "old_weight"])
    val startWeight: Double? = null,
    @SerializedName(value = "endWeight", alternate = ["end_weight", "newWeight", "new_weight", "weightKg", "weight_kg"])
    val endWeight: Double? = null,
    @SerializedName(value = "changeAmount", alternate = ["change_amount"])
    val changeAmount: Double? = null,
    @SerializedName(value = "logCount", alternate = ["log_count"])
    val logCount: Int = 0
)

data class WeightSeriesResponse(
    val userId: Long,
    val from: String,
    val to: String,
    val groupBy: StatisticsGroupBy,
    val overallChange: Double? = null,
    val points: List<WeightPoint>
)

//namthem
data class CreateWeightLogRequest(
    val date: String,
    val newWeight: Double,
    val note: String? = null
)

//namthem
data class WeightLogUpdateResponse(
    val logId: Long,
    val userId: Long,
    val date: String,
    val oldWeight: Double?,
    val newWeight: Double,
    val changeAmount: Double?,
    val currentProfileWeight: Double?,
    val note: String?,
    val latestKnownWeight: Double? = null,
    val latestKnownDate: String? = null
)

//namthem
data class LatestWeightInfoResponse(
    val userId: Long,
    val latestKnownWeight: Double?,
    val latestKnownDate: String?,
    val source: String?
)

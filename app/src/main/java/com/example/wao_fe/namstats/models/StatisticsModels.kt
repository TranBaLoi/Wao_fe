/*
 * Bài làm của Nguyễn Hải Nam-B22DCCN558
 * Model request/response khớp với DTO backend cho module thống kê.
 */
//nam them
package com.example.wao_fe.namstats.models

import com.google.gson.annotations.SerializedName

/** Tổng dinh dưỡng backend trả về cho một ngày. */
data class DailyNutritionResponse(
    val userId: Long,
    val date: String,
    val totalCalories: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double
)

/** Kiểu group dữ liệu gửi lên backend khi lấy series thống kê. */
enum class StatisticsGroupBy {
    DAY,
    WEEK,
    MONTH
}

/** Một điểm dữ liệu dinh dưỡng trong chuỗi biểu đồ. */
data class NutritionPoint(
    val bucketDate: String,
    val totalCalories: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double
)

/** Chuỗi dinh dưỡng trong khoảng ngày, dùng để vẽ calories/protein/carbs/fat. */
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

/** Một điểm dữ liệu cân nặng; SerializedName giúp tương thích nhiều kiểu tên field backend. */
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

/** Chuỗi cân nặng theo khoảng ngày, gồm các điểm và mức thay đổi tổng. */
data class WeightSeriesResponse(
    val userId: Long,
    val from: String,
    val to: String,
    val groupBy: StatisticsGroupBy,
    val overallChange: Double? = null,
    val points: List<WeightPoint>
)

//namthem
/** Request gửi lên backend khi người dùng lưu cân nặng mới. */
data class CreateWeightLogRequest(
    val date: String,
    val newWeight: Double,
    val note: String? = null
)

//namthem
/** Response sau khi backend tạo log cân nặng thành công. */
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
/** Response cân nặng gần nhất, cho biết dữ liệu lấy từ weight log hay health profile. */
data class LatestWeightInfoResponse(
    val userId: Long,
    val latestKnownWeight: Double?,
    val latestKnownDate: String?,
    val source: String?
)

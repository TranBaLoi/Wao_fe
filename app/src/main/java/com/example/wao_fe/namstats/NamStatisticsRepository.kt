//nam them
package com.example.wao_fe.namstats

import com.example.wao_fe.namstats.models.DailyNutritionResponse
import com.example.wao_fe.namstats.models.NutritionPoint
import com.example.wao_fe.namstats.models.NutritionSeriesResponse
import com.example.wao_fe.namstats.models.StatisticsGroupBy
import com.example.wao_fe.namstats.models.WeightPoint
import com.example.wao_fe.namstats.models.WeightSeriesResponse
import com.example.wao_fe.network.ApiService
import com.example.wao_fe.network.NetworkClient
import com.example.wao_fe.network.models.DailySummaryResponse
import com.example.wao_fe.network.models.HealthProfileResponse
import com.example.wao_fe.network.models.UserResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class NamStatisticsRepository(
    private val apiService: ApiService = NetworkClient.apiService
) {

    suspend fun loadOverviewData(userId: Long): OverviewData = coroutineScope {
        val today = LocalDate.now()

        val userDeferred = async { apiService.getUserById(userId) }
        val profileDeferred = async { apiService.getLatestHealthProfile(userId) }
        val summaryDeferred = async { apiService.getTodaySummary(userId) }
        val nutritionDeferred = async { apiService.getDailyNutrition(userId, today.toString()) }

        OverviewData(
            user = userDeferred.await(),
            latestProfile = profileDeferred.await(),
            todaySummary = summaryDeferred.await(),
            todayNutrition = nutritionDeferred.await()
        )
    }

    suspend fun loadDailySnapshot(userId: Long, date: LocalDate): DailySnapshot = coroutineScope {
        val nutritionDeferred = async { apiService.getDailyNutrition(userId, date.toString()) }
        val profileDeferred = async { apiService.getLatestHealthProfile(userId) }
        val weightDeferred = async {
            apiService.getWeightSeries(
                userId = userId,
                // chỉ dùng để log ra cân nặng hôm đấy
                from = date.toString(),
                to = date.toString(),
                groupBy = StatisticsGroupBy.DAY.name
            )
        }

        val nutrition = nutritionDeferred.await()
        val latestProfile = profileDeferred.await()
        val weights = weightDeferred.await().points
            .filter { it.endWeight != null }
            .sortedBy { it.normalizedBucketDate() }

        val current = weights.lastOrNull()
        val fallbackWeight = latestProfile.weightKg

        DailySnapshot(
            date = date,
            nutrition = nutrition,
            currentWeight = current?.endWeight ?: fallbackWeight,
            //nam them
            previousWeight = current?.startWeight,
            //nam them
            weightChange = current?.changeAmount ?: if (current?.endWeight != null && current.startWeight != null) {
                current.endWeight - current.startWeight
            } else {
                null
            },
            fallbackWeight = fallbackWeight
        )
    }


    //hàm chính để load snapShot....
    suspend fun loadRangeSnapshot(userId: Long, range: DateRange): RangeSnapshot = supervisorScope {
        val safeRange = range.clampToToday()
         // lấy cân nặng cuôi
        val profileDeferred = async { apiService.getLatestHealthProfile(userId) }
//        lấy các dinh dưỡng
        val nutritionDeferred = async {
            runCatching {
                apiService.getNutritionSeries(
                    userId = userId,
                    from = safeRange.start.toString(),
                    to = safeRange.end.toString(),
//                    mặc định
                    groupBy = StatisticsGroupBy.DAY.name
                )
            }.getOrElse {
                emptyNutritionSeries(userId, safeRange)
            }
        }
//        lấy cân nặng
        val weightDeferred = async {
            runCatching {
                loadWeightSeriesForRange(userId, safeRange)
            }.getOrElse {
                emptyWeightSeries(userId, safeRange)
            }
        }

        val fallbackWeight = runCatching { profileDeferred.await().weightKg }.getOrNull()
//tập hợp
        RangeSnapshot(
            range = safeRange,
            nutrition = nutritionDeferred.await(),
            weight = weightDeferred.await(),
            fallbackWeight = fallbackWeight
        )
    }

    fun buildWeekRange(anchorDate: LocalDate): DateRange {
        //nam them
        val safeAnchor = anchorDate.coerceAtMost(LocalDate.now())
        // ép về ngày đầu tuần
        val start = safeAnchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = safeAnchor.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        return DateRange(start, end).clampToToday()
    }

    fun buildMonthRange(anchorDate: LocalDate): DateRange {
        //nam them
        val safeAnchor = anchorDate.coerceAtMost(LocalDate.now())
        //ép về ngày mùng 1
        val start = safeAnchor.withDayOfMonth(1)
        val end = safeAnchor.withDayOfMonth(safeAnchor.lengthOfMonth())
        return DateRange(start, end).clampToToday()
    }

    //nam them
    private fun emptyNutritionSeries(userId: Long, range: DateRange): NutritionSeriesResponse {
        return NutritionSeriesResponse(
            userId = userId,
            from = range.start.toString(),
            to = range.end.toString(),
            groupBy = StatisticsGroupBy.DAY,
            totalCalories = 0.0,
            totalProtein = 0.0,
            totalCarbs = 0.0,
            totalFat = 0.0,
            points = emptyList()
        )
    }

    //nam them
    private fun emptyWeightSeries(userId: Long, range: DateRange): WeightSeriesResponse {
        return WeightSeriesResponse(
            userId = userId,
            from = range.start.toString(),
            to = range.end.toString(),
            groupBy = StatisticsGroupBy.DAY,
            overallChange = null,
            points = emptyList()
        )
    }

    //nam them
    private suspend fun loadWeightSeriesForRange(
        userId: Long,
        range: DateRange
    ): WeightSeriesResponse {

        return apiService.getWeightSeries(
            userId = userId,
            from = range.start.toString(),
            to = range.end.toString(),
            groupBy = StatisticsGroupBy.DAY.name
        )
    }
}

data class OverviewData(
    val user: UserResponse,
    val latestProfile: HealthProfileResponse,
    val todaySummary: DailySummaryResponse,
    val todayNutrition: DailyNutritionResponse
)

data class DateRange(
    val start: LocalDate,
    val end: LocalDate
) {
    //tránh kéo ngày quá tay
    fun clampToToday(today: LocalDate = LocalDate.now()): DateRange {
        val clampedStart = start.coerceAtMost(today)
        val clampedEnd = end.coerceAtMost(today)
        return if (clampedStart <= clampedEnd) {
            copy(start = clampedStart, end = clampedEnd)
        } else {
            copy(start = clampedEnd, end = clampedEnd)
        }
    }

    // trả về dang sách date cho dễ xử lý
    fun dates(): List<LocalDate> {
        val days = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt()
        return (0..days).map { start.plusDays(it.toLong()) }
    }

    //đếm xem có bao nhiêu ngày
    fun daysSpan(): Long = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1

    //nam them
    fun expandToMonthBounds(): DateRange {
        val expandedStart = start.withDayOfMonth(1)
        val expandedEnd = end.withDayOfMonth(end.lengthOfMonth())
        return DateRange(expandedStart, expandedEnd).clampToToday()
    }
}

data class DailySnapshot(
    val date: LocalDate,
    val nutrition: DailyNutritionResponse,
    val currentWeight: Double?,
    val previousWeight: Double?,
    val weightChange: Double?,
    val fallbackWeight: Double?
)

data class RangeSnapshot(
    val range: DateRange,
    val nutrition: NutritionSeriesResponse,
    val weight: WeightSeriesResponse,
    val fallbackWeight: Double?
) {
    fun nutritionPointByDate(date: String): NutritionPoint? = nutrition.points.firstOrNull { it.bucketDate == date }

    fun weightPointByDate(date: String): WeightPoint? =
        weight.points
            .filter { it.normalizedBucketDate() == date }
            .sortedBy { it.bucketDate }
            .lastOrNull { it.endWeight != null }

    fun resolvedWeightByDate(date: String): Double? {
        return weight.points
            .filter { it.normalizedBucketDate() <= date }
            .sortedBy { it.bucketDate }
            .lastOrNull { it.endWeight != null }
            ?.endWeight
            ?: fallbackWeight
    }
}

//nam them
private fun WeightPoint.normalizedBucketDate(): String {
    return if (bucketDate.length >= 10) bucketDate.substring(0, 10) else bucketDate
}

//nam them
private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

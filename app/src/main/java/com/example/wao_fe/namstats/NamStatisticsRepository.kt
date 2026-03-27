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
                from = date.minusDays(60).toString(),
                to = date.toString(),
                groupBy = StatisticsGroupBy.DAY.name
            )
        }

        val nutrition = nutritionDeferred.await()
        val latestProfile = profileDeferred.await()
        val weights = weightDeferred.await().points
            .filter { it.endWeight != null }
            .sortedBy { it.bucketDate }

        val current = weights.lastOrNull()
        val previous = if (weights.size >= 2) weights[weights.size - 2] else null
        val fallbackWeight = latestProfile.weightKg

        DailySnapshot(
            date = date,
            nutrition = nutrition,
            currentWeight = current?.endWeight ?: fallbackWeight,
            previousWeight = previous?.endWeight,
            weightChange = if (current?.endWeight != null && previous?.endWeight != null) {
                current.endWeight - previous.endWeight
            } else {
                null
            },
            fallbackWeight = fallbackWeight
        )
    }

    suspend fun loadRangeSnapshot(userId: Long, range: DateRange): RangeSnapshot = coroutineScope {
        val profileDeferred = async { apiService.getLatestHealthProfile(userId) }
        val nutritionDeferred = async {
            apiService.getNutritionSeries(
                userId = userId,
                from = range.start.toString(),
                to = range.end.toString(),
                groupBy = StatisticsGroupBy.DAY.name
            )
        }
        val weightDeferred = async {
            apiService.getWeightSeries(
                userId = userId,
                from = range.start.toString(),
                to = range.end.toString(),
                groupBy = StatisticsGroupBy.DAY.name
            )
        }

        RangeSnapshot(
            range = range,
            nutrition = nutritionDeferred.await(),
            weight = weightDeferred.await(),
            fallbackWeight = profileDeferred.await().weightKg
        )
    }

    fun buildWeekRange(anchorDate: LocalDate): DateRange {
        val start = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = anchorDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        return DateRange(start, end)
    }

    fun buildMonthRange(anchorDate: LocalDate): DateRange {
        val start = anchorDate.withDayOfMonth(1)
        val end = anchorDate.withDayOfMonth(anchorDate.lengthOfMonth())
        return DateRange(start, end)
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
)

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

    fun weightPointByDate(date: String): WeightPoint? = weight.points.firstOrNull { it.bucketDate == date }

    fun resolvedWeightByDate(date: String): Double? {
        return weight.points
            .filter { it.bucketDate <= date }
            .lastOrNull { it.endWeight != null }
            ?.endWeight
            ?: fallbackWeight
    }
}

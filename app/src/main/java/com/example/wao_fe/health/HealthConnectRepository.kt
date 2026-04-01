package com.example.wao_fe.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

data class HealthConnectSnapshot(
    val stepsToday: Long,
    val activeCaloriesBurnedToday: Double?,
    val totalCaloriesBurnedToday: Double?,
    val latestHeartRateBpm: Long?,
    val latestHeartRateTime: Instant?,
)

data class StepTimelineBucket(
    val hour: Int,
    val label: String,
    val steps: Long,
)

data class DailyStepsTimeline(
    val date: LocalDate,
    val totalSteps: Long,
    val buckets: List<StepTimelineBucket>,
)

class HealthConnectRepository(
    private val healthConnectClient: HealthConnectClient,
) {
    suspend fun readTodaySnapshot(
        zoneId: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ): HealthConnectSnapshot {
        val startOfDay = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant()

        // Steps are cumulative data, so aggregate is the safest way to avoid double counting.
        val aggregateResult = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                ),
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
            ),
        )

        // Heart rate is point-in-time data, so we read records and take the latest sample today.
        val heartRateRecords = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
                ascendingOrder = false,
                pageSize = 10,
            ),
        )

        val latestHeartRateSample = heartRateRecords.records
            .asSequence()
            .flatMap { record -> record.samples.asSequence() }
            .maxByOrNull { sample -> sample.time }

        return HealthConnectSnapshot(
            stepsToday = aggregateResult[StepsRecord.COUNT_TOTAL] ?: 0L,
            activeCaloriesBurnedToday = aggregateResult[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                ?.inKilocalories,
            totalCaloriesBurnedToday = aggregateResult[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
                ?.inKilocalories,
            latestHeartRateBpm = latestHeartRateSample?.beatsPerMinute,
            latestHeartRateTime = latestHeartRateSample?.time,
        )
    }

    suspend fun readDailyStepsTimeline(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ): DailyStepsTimeline {
        val startOfDay = date.atStartOfDay(zoneId).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant()
        val effectiveEnd = minOf(endOfDay, now)
        if (!effectiveEnd.isAfter(startOfDay)) {
            return DailyStepsTimeline(
                date = date,
                totalSteps = 0L,
                buckets = buildHourlyBuckets(LongArray(24)),
            )
        }

        val aggregateResult = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startOfDay, effectiveEnd),
            ),
        )

        val stepRecords = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, effectiveEnd),
                ascendingOrder = true,
                pageSize = 500,
            ),
        )

        // A single StepsRecord can span multiple hours, so we spread its count across overlapping hourly buckets
        // instead of dumping the full value into the record's end hour.
        val hourlySteps = DoubleArray(24)
        stepRecords.records.forEach { record ->
            val boundedStart = maxOf(record.startTime, startOfDay)
            val boundedEnd = minOf(record.endTime, effectiveEnd)
            if (!boundedEnd.isAfter(boundedStart)) return@forEach

            val recordDurationMillis = Duration.between(record.startTime, record.endTime)
                .toMillis()
                .coerceAtLeast(1L)

            var cursor = boundedStart
            while (cursor.isBefore(boundedEnd)) {
                val cursorZoned = cursor.atZone(zoneId)
                val nextHour = cursorZoned
                    .truncatedTo(ChronoUnit.HOURS)
                    .plusHours(1)
                    .toInstant()
                val sliceEnd = minOf(boundedEnd, nextHour)
                val overlapMillis = Duration.between(cursor, sliceEnd)
                    .toMillis()
                    .coerceAtLeast(0L)
                val hour = cursorZoned.hour
                hourlySteps[hour] += record.count * overlapMillis.toDouble() / recordDurationMillis.toDouble()
                cursor = sliceEnd
            }
        }

        return DailyStepsTimeline(
            date = date,
            totalSteps = aggregateResult[StepsRecord.COUNT_TOTAL] ?: 0L,
            buckets = buildHourlyBuckets(LongArray(24) { index -> hourlySteps[index].roundToLong() }),
        )
    }

    private fun buildHourlyBuckets(hourlySteps: LongArray): List<StepTimelineBucket> {
        return hourlySteps.mapIndexed { hour, steps ->
            StepTimelineBucket(
                hour = hour,
                label = "%02d:00".format(hour),
                steps = steps,
            )
        }
    }
}

package com.example.wao_fe.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HealthConnectSnapshot(
    val stepsToday: Long,
    val activeCaloriesBurnedToday: Double,
    val latestHeartRateBpm: Long?,
    val latestHeartRateTime: Instant?,
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
                ?.inKilocalories
                ?: 0.0,
            latestHeartRateBpm = latestHeartRateSample?.beatsPerMinute,
            latestHeartRateTime = latestHeartRateSample?.time,
        )
    }
}

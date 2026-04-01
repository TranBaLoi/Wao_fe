package com.example.wao_fe

import com.example.wao_fe.network.ApiService
import com.example.wao_fe.network.NetworkClient
import com.example.wao_fe.network.models.WorkoutLogResponse
import retrofit2.HttpException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

private const val DEFAULT_WORKOUT_HISTORY_LOOKBACK_DAYS = 30L

data class WorkoutJournalSession(
    val logId: Long,
    val workoutType: WorkoutType,
    val date: LocalDate,
    val startedAt: LocalDateTime?,
    val endedAt: LocalDateTime?,
    val durationMin: Int,
    val caloriesBurned: Double,
    val distanceKm: Double?,
    val steps: Int?,
    val averageSpeedKmh: Double?,
    val note: String?,
) {
    val sortDateTime: LocalDateTime
        get() = endedAt ?: startedAt ?: date.atStartOfDay()
}

data class WorkoutJournalSportSummary(
    val workoutType: WorkoutType,
    val sessionCount: Int,
    val totalDistanceKm: Double,
    val totalCaloriesBurned: Double,
    val totalDurationMin: Int,
    val lastSessionAt: LocalDateTime?,
)

data class WorkoutJournalSnapshot(
    val lookbackDays: Int,
    val sessions: List<WorkoutJournalSession>,
    val sportSummaries: List<WorkoutJournalSportSummary>,
)

class WorkoutJournalRepository(
    private val apiService: ApiService = NetworkClient.apiService,
) {

    // The current backend only exposes workout logs by single date, so FE assembles a recent window until
    // BE ships a proper range endpoint for the journal and history detail screens.
    suspend fun loadRecentSnapshot(
        userId: Long,
        lookbackDays: Long = DEFAULT_WORKOUT_HISTORY_LOOKBACK_DAYS,
    ): WorkoutJournalSnapshot {
        val exerciseNameById = runCatching { apiService.getExercises() }
            .getOrDefault(emptyList())
            .associate { it.id to it.name }

        var successfulDayCount = 0
        var firstFailure: Throwable? = null
        val collectedLogs = mutableListOf<WorkoutLogResponse>()
        repeat(lookbackDays.toInt()) { dayOffset ->
            val date = LocalDate.now().minusDays(dayOffset.toLong()).toString()
            runCatching { apiService.getWorkoutLogs(userId, date) }
                .onSuccess { logs ->
                    successfulDayCount += 1
                    collectedLogs += logs
                }
                .onFailure { error ->
                    if (error is HttpException && error.code() == 404) {
                        successfulDayCount += 1
                    } else if (firstFailure == null) {
                        firstFailure = error
                    }
                }
        }

        if (successfulDayCount == 0 && firstFailure != null) {
            throw firstFailure as Throwable
        }

        val sessions = collectedLogs
            .mapNotNull { log -> log.toWorkoutJournalSession(exerciseNameById) }
            .sortedByDescending { session -> session.sortDateTime }

        val sportSummaries = sessions
            .groupBy { session -> session.workoutType }
            .map { (workoutType, groupedSessions) ->
                WorkoutJournalSportSummary(
                    workoutType = workoutType,
                    sessionCount = groupedSessions.size,
                    totalDistanceKm = groupedSessions.sumOf { it.distanceKm ?: 0.0 },
                    totalCaloriesBurned = groupedSessions.sumOf { it.caloriesBurned },
                    totalDurationMin = groupedSessions.sumOf { it.durationMin },
                    lastSessionAt = groupedSessions.maxOfOrNull { it.sortDateTime },
                )
            }
            .sortedByDescending { summary -> summary.lastSessionAt ?: LocalDateTime.MIN }

        return WorkoutJournalSnapshot(
            lookbackDays = lookbackDays.toInt(),
            sessions = sessions,
            sportSummaries = sportSummaries,
        )
    }

    suspend fun loadRecentSessionsForType(
        userId: Long,
        workoutType: WorkoutType,
        lookbackDays: Long = DEFAULT_WORKOUT_HISTORY_LOOKBACK_DAYS,
    ): List<WorkoutJournalSession> {
        return loadRecentSnapshot(userId, lookbackDays)
            .sessions
            .filter { session -> session.workoutType == workoutType }
    }

    private fun WorkoutLogResponse.toWorkoutJournalSession(
        exerciseNameById: Map<Long, String>,
    ): WorkoutJournalSession? {
        val metadata = parseWorkoutMetadata(note)
        val workoutType = resolveWorkoutType(
            explicitTypeValue = workoutType ?: metadata["typeKey"] ?: metadata["backendWorkoutType"],
            exerciseName = exerciseId?.let(exerciseNameById::get),
            noteTitle = note?.substringBefore("|")?.substringBefore(" - ")?.trim(),
        ) ?: return null

        return WorkoutJournalSession(
            logId = id,
            workoutType = workoutType,
            date = parseLogDate(logDate),
            startedAt = startedAt?.let(::parseLocalDateTimeSafely)
                ?: metadata["startedAt"]?.let(::parseLocalDateTimeSafely),
            endedAt = endedAt?.let(::parseLocalDateTimeSafely)
                ?: metadata["endedAt"]?.let(::parseLocalDateTimeSafely),
            durationMin = durationMin,
            caloriesBurned = caloriesBurned,
            distanceKm = distanceMeters?.div(1_000.0)
                ?: metadata["distanceKm"]?.toDoubleOrNull()
                ?: parseLegacyDistanceKm(note),
            steps = stepCount ?: metadata["steps"]?.toIntOrNull(),
            averageSpeedKmh = avgSpeedKmh ?: metadata["avgSpeedKmh"]?.toDoubleOrNull(),
            note = note,
        )
    }

    private fun parseWorkoutMetadata(note: String?): Map<String, String> {
        if (note.isNullOrBlank()) return emptyMap()
        return note.split("|")
            .map { token -> token.trim() }
            .mapNotNull { token ->
                val separatorIndex = token.indexOf('=')
                if (separatorIndex <= 0 || separatorIndex >= token.lastIndex) {
                    null
                } else {
                    val key = token.substring(0, separatorIndex).trim()
                    val value = token.substring(separatorIndex + 1).trim()
                    key to value
                }
            }
            .toMap()
    }

    private fun resolveWorkoutType(
        explicitTypeValue: String?,
        exerciseName: String?,
        noteTitle: String?,
    ): WorkoutType? {
        explicitTypeValue?.let { value ->
            WorkoutType.fromBackendWorkoutType(value)?.let { return it }
            WorkoutType.values().firstOrNull { type -> type.key == value }?.let { return it }
        }

        val normalizedExercise = exerciseName?.trim()
        WorkoutType.values().firstOrNull { type ->
            type.exerciseName.equals(normalizedExercise, ignoreCase = true) ||
                type.title.equals(normalizedExercise, ignoreCase = true)
        }?.let { return it }

        val normalizedTitle = noteTitle?.trim()
        return WorkoutType.values().firstOrNull { type ->
            type.title.equals(normalizedTitle, ignoreCase = true) ||
                type.exerciseName.equals(normalizedTitle, ignoreCase = true)
        }
    }

    private fun parseLegacyDistanceKm(note: String?): Double? {
        if (note.isNullOrBlank()) return null
        val distanceMatch = Regex("""([0-9]+(?:\.[0-9]+)?)\s*km""", RegexOption.IGNORE_CASE)
            .find(note)
            ?.groupValues
            ?.getOrNull(1)
        return distanceMatch?.toDoubleOrNull()
    }

    private fun parseLogDate(rawDate: String): LocalDate {
        return runCatching { LocalDate.parse(rawDate.take(10)) }
            .getOrDefault(LocalDate.now())
    }

    private fun parseLocalDateTimeSafely(rawValue: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(rawValue)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

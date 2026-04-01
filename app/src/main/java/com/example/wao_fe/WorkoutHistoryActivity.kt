package com.example.wao_fe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

class WorkoutHistoryActivity : AppCompatActivity() {

    private val journalRepository = WorkoutJournalRepository()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("vi", "VN"))
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("vi", "VN"))

    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progressHistory: ProgressBar
    private lateinit var tvTotalSessions: TextView
    private lateinit var tvTotalDistance: TextView
    private lateinit var tvTotalDuration: TextView
    private lateinit var tvTotalCalories: TextView
    private lateinit var containerHistoryEntries: LinearLayout
    private lateinit var tvEmptyState: TextView

    private var userId: Long = -1L
    private val workoutType by lazy {
        WorkoutType.fromKey(intent.getStringExtra(WorkoutType.EXTRA_WORKOUT_TYPE))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_history)

        userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getLong("USER_ID", -1L)
        bindViews()
        bindActions()
        bindHeader()

        if (userId == -1L) {
            Toast.makeText(this, "Khong tim thay thong tin dang nhap", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadHistory()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBackWorkoutHistory)
        tvTitle = findViewById(R.id.tvWorkoutHistoryTitle)
        tvSubtitle = findViewById(R.id.tvWorkoutHistorySubtitle)
        tvStatus = findViewById(R.id.tvWorkoutHistoryStatus)
        progressHistory = findViewById(R.id.progressWorkoutHistory)
        tvTotalSessions = findViewById(R.id.tvWorkoutHistoryTotalSessions)
        tvTotalDistance = findViewById(R.id.tvWorkoutHistoryTotalDistance)
        tvTotalDuration = findViewById(R.id.tvWorkoutHistoryTotalDuration)
        tvTotalCalories = findViewById(R.id.tvWorkoutHistoryTotalCalories)
        containerHistoryEntries = findViewById(R.id.containerWorkoutHistoryEntries)
        tvEmptyState = findViewById(R.id.tvWorkoutHistoryEmpty)
    }

    private fun bindActions() {
        btnBack.setOnClickListener { finish() }
    }

    private fun bindHeader() {
        tvTitle.text = workoutType.title
        tvSubtitle.text = "Chi tiet cac buoi tap gan day"
    }

    private fun loadHistory() {
        progressHistory.visibility = View.VISIBLE
        tvStatus.text = "Dang tai lich su tap luyen"

        lifecycleScope.launch {
            runCatching {
                journalRepository.loadRecentSessionsForType(userId, workoutType)
            }.onSuccess { sessions ->
                progressHistory.visibility = View.GONE
                tvStatus.text =
                    "Dang hien thi 30 ngay gan nhat. Gio tap chi co day du voi cac buoi luu metadata moi."
                renderSummary(sessions)
                renderSessions(sessions)
            }.onFailure { error ->
                progressHistory.visibility = View.GONE
                tvStatus.text = "Khong tai duoc lich su tap luyen"
                tvEmptyState.visibility = View.VISIBLE
                tvEmptyState.text = error.message ?: "Khong the ket noi den may chu"
            }
        }
    }

    private fun renderSummary(sessions: List<WorkoutJournalSession>) {
        tvTotalSessions.text = sessions.size.toString()
        tvTotalDistance.text = formatDistance(sessions.sumOf { it.distanceKm ?: 0.0 })
        tvTotalDuration.text = formatDurationMinutes(sessions.sumOf { it.durationMin })
        tvTotalCalories.text = formatCalories(sessions.sumOf { it.caloriesBurned })
    }

    private fun renderSessions(sessions: List<WorkoutJournalSession>) {
        containerHistoryEntries.removeAllViews()
        if (sessions.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            tvEmptyState.text = "Chua co buoi tap nao cho mon nay trong 30 ngay gan day."
            return
        }

        tvEmptyState.visibility = View.GONE
        val inflater = LayoutInflater.from(this)
        sessions.forEach { session ->
            val card = inflater.inflate(
                R.layout.item_nam_stat_point,
                containerHistoryEntries,
                false,
            )
            bindSessionCard(card, session)
            containerHistoryEntries.addView(card)
        }
    }

    private fun bindSessionCard(card: View, session: WorkoutJournalSession) {
        val titleView = card.findViewById<TextView>(R.id.tv_point_title)
        val subtitleView = card.findViewById<TextView>(R.id.tv_point_subtitle)
        val metricOneView = card.findViewById<TextView>(R.id.tv_metric_one)
        val metricTwoView = card.findViewById<TextView>(R.id.tv_metric_two)
        val metricThreeView = card.findViewById<TextView>(R.id.tv_metric_three)
        val metricFourView = card.findViewById<TextView>(R.id.tv_metric_four)

        titleView.text = session.date.format(dateFormatter)
        subtitleView.text = buildSessionTimeLabel(session)
        metricOneView.text = "Quang duong: ${session.distanceKm?.let(::formatDistance) ?: "--"}"
        metricTwoView.text = "Thoi gian: ${formatDurationMinutes(session.durationMin)}"
        metricThreeView.text = "Calories: ${formatCalories(session.caloriesBurned)}"
        metricFourView.text = when {
            session.steps != null -> "So buoc: ${session.steps}"
            session.averageSpeedKmh != null -> "Toc do TB: ${formatSpeed(session.averageSpeedKmh)}"
            else -> "Ghi chu: ${session.note?.substringBefore("|") ?: "Chua co them du lieu"}"
        }
    }

    private fun buildSessionTimeLabel(session: WorkoutJournalSession): String {
        val startedLabel = session.startedAt?.format(timeFormatter)
        val endedLabel = session.endedAt?.format(timeFormatter)
        return when {
            startedLabel != null && endedLabel != null -> "$startedLabel - $endedLabel"
            endedLabel != null -> "Ket thuc luc $endedLabel"
            startedLabel != null -> "Bat dau luc $startedLabel"
            else -> "Chua co gio tap tu backend"
        }
    }

    private fun formatDistance(distanceKm: Double): String {
        return String.format(Locale.US, "%.2f km", distanceKm)
    }

    private fun formatDurationMinutes(durationMin: Int): String {
        val hours = durationMin / 60
        val minutes = durationMin % 60
        return if (hours > 0) {
            "${hours}h ${minutes}p"
        } else {
            "${minutes}p"
        }
    }

    private fun formatCalories(calories: Double): String {
        return String.format(Locale.US, "%.0f kcal", calories)
    }

    private fun formatSpeed(speedKmh: Double): String {
        return String.format(Locale.US, "%.1f km/h", speedKmh)
    }
}

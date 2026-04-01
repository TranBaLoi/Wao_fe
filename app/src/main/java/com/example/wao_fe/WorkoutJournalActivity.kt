package com.example.wao_fe

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

class WorkoutJournalActivity : AppCompatActivity() {

    private val journalRepository = WorkoutJournalRepository()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("vi", "VN"))

    private lateinit var btnBack: ImageButton
    private lateinit var tvJournalStatus: TextView
    private lateinit var progressJournal: ProgressBar
    private lateinit var tvTotalSessions: TextView
    private lateinit var tvTotalDistance: TextView
    private lateinit var tvTotalDuration: TextView
    private lateinit var tvTotalCalories: TextView
    private lateinit var containerWorkoutTypes: LinearLayout
    private lateinit var tvEmptyState: TextView

    private var userId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_journal)

        userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getLong("USER_ID", -1L)
        bindViews()
        bindActions()

        if (userId == -1L) {
            Toast.makeText(this, "Khong tim thay thong tin dang nhap", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadWorkoutJournal()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBackWorkoutJournal)
        tvJournalStatus = findViewById(R.id.tvWorkoutJournalStatus)
        progressJournal = findViewById(R.id.progressWorkoutJournal)
        tvTotalSessions = findViewById(R.id.tvWorkoutJournalTotalSessions)
        tvTotalDistance = findViewById(R.id.tvWorkoutJournalTotalDistance)
        tvTotalDuration = findViewById(R.id.tvWorkoutJournalTotalDuration)
        tvTotalCalories = findViewById(R.id.tvWorkoutJournalTotalCalories)
        containerWorkoutTypes = findViewById(R.id.containerWorkoutTypes)
        tvEmptyState = findViewById(R.id.tvWorkoutJournalEmpty)
    }

    private fun bindActions() {
        btnBack.setOnClickListener { finish() }
    }

    private fun loadWorkoutJournal() {
        progressJournal.visibility = View.VISIBLE
        tvJournalStatus.text = "Dang tai nhat ki tap luyen"

        lifecycleScope.launch {
            runCatching {
                journalRepository.loadRecentSnapshot(userId)
            }.onSuccess { snapshot ->
                progressJournal.visibility = View.GONE
                tvJournalStatus.text =
                    "Dang hien thi ${snapshot.lookbackDays} ngay gan nhat do API workout hien tai moi ho tro truy van theo ngay."
                renderSummary(snapshot)
                renderWorkoutTypes(snapshot)
            }.onFailure { error ->
                progressJournal.visibility = View.GONE
                tvJournalStatus.text = "Khong tai duoc nhat ki tap luyen"
                tvEmptyState.visibility = View.VISIBLE
                tvEmptyState.text = error.message ?: "Khong the ket noi den may chu"
            }
        }
    }

    private fun renderSummary(snapshot: WorkoutJournalSnapshot) {
        tvTotalSessions.text = snapshot.sessions.size.toString()
        tvTotalDistance.text = formatDistance(snapshot.sessions.sumOf { it.distanceKm ?: 0.0 })
        tvTotalDuration.text = formatDurationMinutes(snapshot.sessions.sumOf { it.durationMin })
        tvTotalCalories.text = formatCalories(snapshot.sessions.sumOf { it.caloriesBurned })
    }

    private fun renderWorkoutTypes(snapshot: WorkoutJournalSnapshot) {
        containerWorkoutTypes.removeAllViews()
        if (snapshot.sportSummaries.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            tvEmptyState.text = "Chua co buoi tap nao trong 30 ngay gan day."
            return
        }

        tvEmptyState.visibility = View.GONE
        val inflater = LayoutInflater.from(this)
        snapshot.sportSummaries.forEach { summary ->
            val card = inflater.inflate(
                R.layout.item_workout_sport_summary,
                containerWorkoutTypes,
                false,
            )
            bindWorkoutTypeCard(card, summary)
            containerWorkoutTypes.addView(card)
        }
    }

    private fun bindWorkoutTypeCard(card: View, summary: WorkoutJournalSportSummary) {
        val iconCard = card.findViewById<MaterialCardView>(R.id.cardWorkoutTypeIcon)
        val iconView = card.findViewById<ImageView>(R.id.ivWorkoutTypeIcon)
        val titleView = card.findViewById<TextView>(R.id.tvWorkoutTypeTitle)
        val subtitleView = card.findViewById<TextView>(R.id.tvWorkoutTypeSubtitle)
        val metaOneView = card.findViewById<TextView>(R.id.tvWorkoutTypeMetaOne)
        val metaTwoView = card.findViewById<TextView>(R.id.tvWorkoutTypeMetaTwo)
        val metaThreeView = card.findViewById<TextView>(R.id.tvWorkoutTypeMetaThree)

        titleView.text = summary.workoutType.title
        subtitleView.text = buildString {
            append("${summary.sessionCount} buoi tap")
            summary.lastSessionAt?.let {
                append(" - Gan nhat ${it.toLocalDate().format(dateFormatter)}")
            }
        }
        metaOneView.text = "Quang duong: ${formatDistance(summary.totalDistanceKm)}"
        metaTwoView.text = "Thoi gian: ${formatDurationMinutes(summary.totalDurationMin)}"
        metaThreeView.text = "Calories: ${formatCalories(summary.totalCaloriesBurned)}"

        val iconColors = workoutIconColors(summary.workoutType)
        iconCard.setCardBackgroundColor(colorOf(iconColors.first))
        iconView.setImageResource(summary.workoutType.iconRes)
        iconView.imageTintList = ColorStateList.valueOf(colorOf(iconColors.second))

        card.setOnClickListener {
            startActivity(
                Intent(this, WorkoutHistoryActivity::class.java)
                    .putExtra(WorkoutType.EXTRA_WORKOUT_TYPE, summary.workoutType.key),
            )
        }
    }

    private fun workoutIconColors(workoutType: WorkoutType): Pair<Int, Int> {
        return when (workoutType) {
            WorkoutType.WALKING -> R.color.icon_bg_green to R.color.green_dark
            WorkoutType.OUTDOOR_RUNNING -> R.color.icon_bg_pink to R.color.wao_primary
            WorkoutType.INDOOR_RUNNING -> R.color.icon_bg_blue to R.color.green_dark
            WorkoutType.CYCLING -> R.color.icon_bg_yellow to R.color.green_dark
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

    private fun colorOf(colorRes: Int): Int = ContextCompat.getColor(this, colorRes)
}

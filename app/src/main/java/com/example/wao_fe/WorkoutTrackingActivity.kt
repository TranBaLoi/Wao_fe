package com.example.wao_fe

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.example.wao_fe.health.HealthConnectManager
import com.example.wao_fe.health.HealthConnectRepository
import com.example.wao_fe.network.NetworkClient
import com.example.wao_fe.network.models.CreateWorkoutLogRequest
import com.example.wao_fe.network.models.ExerciseRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

class WorkoutTrackingActivity : AppCompatActivity() {

    private enum class SessionState { IDLE, RUNNING, PAUSED, SAVING }

    private lateinit var rootView: View
    private lateinit var tvWorkoutTitle: TextView
    private lateinit var btnWorkoutMore: ImageButton
    private lateinit var tvDistanceMain: TextView
    private lateinit var tvSessionStatus: TextView
    private lateinit var tvSessionDuration: TextView
    private lateinit var tvMetricDistanceValue: TextView
    private lateinit var tvMetricSpeedValue: TextView
    private lateinit var tvMetricCaloriesValue: TextView
    private lateinit var tvMetricFourthLabel: TextView
    private lateinit var tvMetricFourthValue: TextView
    private lateinit var tvHoldHint: TextView
    private lateinit var btnPrimaryAction: MaterialButton
    private lateinit var holdToEndContainer: FrameLayout
    private lateinit var progressHoldToEnd: CircularProgressIndicator
    private lateinit var btnHoldToEnd: MaterialButton

    private val apiService = NetworkClient.apiService
    private val workoutType by lazy {
        WorkoutType.fromKey(intent.getStringExtra(WorkoutType.EXTRA_WORKOUT_TYPE))
    }

    private var userId = -1L
    private var sessionState = SessionState.IDLE
    private var sessionDurationSeconds = 0L
    private var distanceKm = 0.0
    private var estimatedCaloriesBurned = 0.0
    private var currentSpeedKmh = 0.0

    private var hasHealthConnectReadAccess = false
    private var latestHeartRateBpm: Long? = null
    private var stepsTodayAtStart: Long? = null
    private var latestStepsToday: Long? = null
    private var activeCaloriesAtStart: Double? = null
    private var latestActiveCaloriesToday: Double? = null

    private var tickJob: Job? = null
    private var healthSnapshotJob: Job? = null
    private var holdAnimator: ValueAnimator? = null
    private var holdCancelled = false

    private val requestHealthPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(HealthConnectManager.readPermissions)) {
            hasHealthConnectReadAccess = true
            toast("Đã kết nối Health Connect")
            lifecycleScope.launch {
                refreshHealthSnapshot(captureBaseline = sessionState != SessionState.IDLE)
            }
            startHealthSnapshotLoop()
        } else {
            toast("Chưa cấp quyền Health Connect")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContentView(R.layout.activity_workout_tracking)

        userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getLong("USER_ID", -1L)

        initViews()
        applyInsets()
        bindContent()
        bindActions()
        checkHealthConnectAccess()
        renderMetrics()
    }

    override fun onDestroy() {
        tickJob?.cancel()
        healthSnapshotJob?.cancel()
        holdCancelled = true
        holdAnimator?.cancel()
        super.onDestroy()
    }

    private fun initViews() {
        rootView = findViewById(R.id.rootWorkoutTracking)
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        btnWorkoutMore = findViewById(R.id.btnWorkoutMore)
        tvDistanceMain = findViewById(R.id.tvDistanceMain)
        tvSessionStatus = findViewById(R.id.tvSessionStatus)
        tvSessionDuration = findViewById(R.id.tvSessionDuration)
        tvMetricDistanceValue = findViewById(R.id.tvMetricDistanceValue)
        tvMetricSpeedValue = findViewById(R.id.tvMetricSpeedValue)
        tvMetricCaloriesValue = findViewById(R.id.tvMetricCaloriesValue)
        tvMetricFourthLabel = findViewById(R.id.tvMetricFourthLabel)
        tvMetricFourthValue = findViewById(R.id.tvMetricFourthValue)
        tvHoldHint = findViewById(R.id.tvHoldHint)
        btnPrimaryAction = findViewById(R.id.btnPrimaryAction)
        holdToEndContainer = findViewById(R.id.holdToEndContainer)
        progressHoldToEnd = findViewById(R.id.progressHoldToEnd)
        btnHoldToEnd = findViewById(R.id.btnHoldToEnd)
    }

    private fun applyInsets() {
        val start = rootView.paddingStart
        val end = rootView.paddingEnd
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(start, bars.top + dp(24), end, bars.bottom + dp(24))
            insets
        }
        ViewCompat.requestApplyInsets(rootView)
    }

    private fun bindContent() {
        tvWorkoutTitle.text = workoutType.title
        tvMetricFourthLabel.text = if (workoutType.usesStepMetric) "Số bước" else "Nhịp tim"
    }

    private fun bindActions() {
        btnWorkoutMore.setOnClickListener { showMoreMenu() }
        btnPrimaryAction.setOnClickListener {
            when (sessionState) {
                SessionState.IDLE -> startSession()
                SessionState.RUNNING -> pauseSession()
                SessionState.PAUSED -> resumeSession()
                SessionState.SAVING -> Unit
            }
        }
        btnHoldToEnd.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startHoldToFinish()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cancelHoldToFinish()
                    true
                }
                else -> true
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = handleExitRequest()
        })
    }

    private fun showMoreMenu() {
        val popupMenu = PopupMenu(this, btnWorkoutMore)
        if (!hasHealthConnectReadAccess) {
            popupMenu.menu.add(0, 1, 0, "Kết nối Health Connect")
        }
        popupMenu.menu.add(0, 2, 1, "Thoát không lưu")
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    requestHealthAccess()
                    true
                }
                2 -> {
                    showDiscardDialog()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun startSession() {
        sessionState = SessionState.RUNNING
        currentSpeedKmh = workoutType.baseSpeedKmh
        lifecycleScope.launch {
            refreshHealthSnapshot(captureBaseline = true)
        }
        startTickLoop()
        startHealthSnapshotLoop()
        renderMetrics()
    }

    private fun pauseSession() {
        sessionState = SessionState.PAUSED
        currentSpeedKmh = 0.0
        renderMetrics()
    }

    private fun resumeSession() {
        sessionState = SessionState.RUNNING
        renderMetrics()
    }

    private fun startTickLoop() {
        if (tickJob?.isActive == true) return
        tickJob = lifecycleScope.launch {
            while (isActive) {
                if (sessionState != SessionState.RUNNING) {
                    delay(250)
                    continue
                }
                delay(1000)
                if (sessionState != SessionState.RUNNING) continue
                sessionDurationSeconds += 1
                currentSpeedKmh = computeCurrentSpeed(sessionDurationSeconds)
                distanceKm += currentSpeedKmh / 3600.0
                estimatedCaloriesBurned += workoutType.caloriesPerMinute / 60.0
                renderMetrics()
            }
        }
    }

    private fun startHealthSnapshotLoop() {
        if (!hasHealthConnectReadAccess || healthSnapshotJob?.isActive == true) return
        healthSnapshotJob = lifecycleScope.launch {
            while (isActive) {
                refreshHealthSnapshot(captureBaseline = false)
                delay(20_000)
            }
        }
    }

    // Health Connect enriches the live session when available, but the tracker still works without it.
    private suspend fun refreshHealthSnapshot(captureBaseline: Boolean) {
        val snapshot = runCatching {
            val client = HealthConnectClient.getOrCreate(this@WorkoutTrackingActivity)
            HealthConnectRepository(client).readTodaySnapshot()
        }.getOrNull() ?: return

        latestHeartRateBpm = snapshot.latestHeartRateBpm
        latestStepsToday = snapshot.stepsToday
        latestActiveCaloriesToday = snapshot.activeCaloriesBurnedToday
        if (captureBaseline && stepsTodayAtStart == null) stepsTodayAtStart = snapshot.stepsToday
        if (captureBaseline && activeCaloriesAtStart == null) activeCaloriesAtStart = snapshot.activeCaloriesBurnedToday
        renderMetrics()
    }

    private fun checkHealthConnectAccess() {
        lifecycleScope.launch {
            if (HealthConnectManager.getSdkStatus(this@WorkoutTrackingActivity) != HealthConnectClient.SDK_AVAILABLE) {
                hasHealthConnectReadAccess = false
                return@launch
            }
            val client = HealthConnectClient.getOrCreate(this@WorkoutTrackingActivity)
            hasHealthConnectReadAccess = client.permissionController.getGrantedPermissions()
                .containsAll(HealthConnectManager.readPermissions)
            if (hasHealthConnectReadAccess) {
                refreshHealthSnapshot(captureBaseline = false)
                startHealthSnapshotLoop()
            }
        }
    }

    private fun requestHealthAccess() {
        if (HealthConnectManager.getSdkStatus(this) == HealthConnectClient.SDK_AVAILABLE) {
            requestHealthPermissions.launch(HealthConnectManager.readPermissions)
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Cần Health Connect")
            .setMessage("Health Connect chưa sẵn sàng trên thiết bị này. Bạn có muốn mở Play Store để cài đặt không?")
            .setNegativeButton("Để sau", null)
            .setPositiveButton("Mở Play Store") { _, _ ->
                try {
                    startActivity(HealthConnectManager.buildInstallIntent(this))
                } catch (_: ActivityNotFoundException) {
                    startActivity(HealthConnectManager.buildBrowserFallbackIntent())
                }
            }
            .show()
    }

    // A 3-second press protects the end action so a normal tap never kills the workout by accident.
    private fun startHoldToFinish() {
        if (sessionState == SessionState.IDLE || sessionState == SessionState.SAVING) return
        if (holdAnimator?.isRunning == true) {
            holdCancelled = true
            holdAnimator?.cancel()
        }
        holdCancelled = false
        tvHoldHint.text = "Thả tay để hủy"
        progressHoldToEnd.progress = 0
        holdAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = 3_000L
            interpolator = LinearInterpolator()
            addUpdateListener { progressHoldToEnd.progress = it.animatedValue as Int }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (holdCancelled) {
                        resetHoldUi()
                        return
                    }
                    progressHoldToEnd.progress = 100
                    completeHoldToFinish()
                }
            })
            start()
        }
    }

    private fun cancelHoldToFinish() {
        if (holdAnimator?.isRunning != true) return
        holdCancelled = true
        holdAnimator?.cancel()
        resetHoldUi()
    }

    private fun completeHoldToFinish() {
        pauseSession()
        resetHoldUi()
        attemptFinishSession()
    }

    private fun resetHoldUi() {
        progressHoldToEnd.progress = 0
        tvHoldHint.text = "Nhấn giữ 3 giây để kết thúc"
    }

    // The short-session rule is enforced right before exit so paused and running flows stay consistent.
    private fun attemptFinishSession() {
        if (sessionDurationSeconds < 180L) {
            AlertDialog.Builder(this)
                .setTitle("Buổi tập quá ngắn")
                .setMessage("Buổi tập dưới 3 phút sẽ không được lưu. Bạn muốn thoát hay tiếp tục tập?")
                .setNegativeButton("Thoát không lưu") { _, _ -> finish() }
                .setPositiveButton("Tiếp tục tập") { _, _ -> resumeSession() }
                .show()
            return
        }
        saveWorkout()
    }

    private fun saveWorkout() {
        if (userId == -1L) {
            toast("Không tìm thấy phiên đăng nhập")
            return
        }
        sessionState = SessionState.SAVING
        renderMetrics()
        lifecycleScope.launch {
            runCatching {
                val exerciseId = findOrCreateExerciseId()
                apiService.createWorkoutLog(
                    userId = userId,
                    request = CreateWorkoutLogRequest(
                        exerciseId = exerciseId,
                        durationMin = (sessionDurationSeconds / 60L).toInt(),
                        caloriesBurned = displayedCaloriesBurned(),
                        logDate = LocalDate.now().toString(),
                        note = "${workoutType.title} - ${formatDistance(distanceKm)} km - ${formatDuration(sessionDurationSeconds)}",
                    )
                )
            }.onSuccess {
                toast("Đã lưu buổi tập")
                finish()
            }.onFailure { error ->
                sessionState = SessionState.PAUSED
                renderMetrics()
                showSaveFailedDialog(error)
            }
        }
    }

    // Workout log requires an exerciseId, so we reuse an existing record or create one on demand.
    private suspend fun findOrCreateExerciseId(): Long {
        val existing = runCatching { apiService.getExercises(workoutType.exerciseName) }
            .getOrDefault(emptyList())
            .firstOrNull { it.name.equals(workoutType.exerciseName, ignoreCase = true) }
        if (existing != null) return existing.id

        val categoryId = runCatching { apiService.getExercises().firstOrNull()?.categoryId }.getOrNull() ?: 1L
        return apiService.createExercise(
            ExerciseRequest(
                name = workoutType.exerciseName,
                categoryId = categoryId,
                caloriesPerMin = workoutType.caloriesPerMinute,
                description = "Bài tập được tạo từ màn hình tracking của Wao",
            )
        ).id
    }

    private fun showSaveFailedDialog(error: Throwable) {
        val message = if (error is HttpException) {
            "Không thể lưu buổi tập (HTTP ${error.code()})."
        } else {
            "Không thể lưu buổi tập. Kiểm tra backend rồi thử lại."
        }
        AlertDialog.Builder(this)
            .setTitle("Lưu thất bại")
            .setMessage(message)
            .setNegativeButton("Thoát không lưu") { _, _ -> finish() }
            .setPositiveButton("Thử lại") { _, _ -> saveWorkout() }
            .show()
    }

    private fun handleExitRequest() {
        when (sessionState) {
            SessionState.IDLE -> finish()
            SessionState.SAVING -> Unit
            SessionState.RUNNING, SessionState.PAUSED -> showDiscardDialog()
        }
    }

    private fun showDiscardDialog() {
        if (sessionState == SessionState.IDLE) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Thoát buổi tập")
            .setMessage("Buổi tập hiện tại sẽ không được lưu. Bạn có chắc muốn thoát?")
            .setNegativeButton("Tiếp tục tập", null)
            .setPositiveButton("Thoát không lưu") { _, _ -> finish() }
            .show()
    }

    private fun renderMetrics() {
        val distanceText = formatDistance(distanceKm)
        tvDistanceMain.text = distanceText
        tvMetricDistanceValue.text = "$distanceText km"
        tvMetricSpeedValue.text = "${formatSpeed(if (sessionState == SessionState.RUNNING) currentSpeedKmh else 0.0)} km/h"
        tvMetricCaloriesValue.text = getString(
            R.string.format_calorie_value,
            displayedCaloriesBurned().roundToInt()
        )
        tvMetricFourthValue.text = if (workoutType.usesStepMetric) {
            "${displayedSteps()} bước"
        } else {
            displayedHeartRate()
        }
        tvSessionDuration.text = formatDuration(sessionDurationSeconds)

        when (sessionState) {
            SessionState.IDLE -> {
                tvSessionStatus.text = "Sẵn sàng"
                btnPrimaryAction.text = "Start"
                btnPrimaryAction.backgroundTintList = ColorStateList.valueOf(colorOf(R.color.workout_tracking_accent))
                btnPrimaryAction.setTextColor(colorOf(R.color.workout_tracking_bg))
                holdToEndContainer.visibility = View.GONE
                tvHoldHint.visibility = View.GONE
                btnPrimaryAction.isEnabled = true
            }
            SessionState.RUNNING -> {
                tvSessionStatus.text = "Đang tập"
                btnPrimaryAction.text = "Pause"
                btnPrimaryAction.backgroundTintList = ColorStateList.valueOf(colorOf(R.color.workout_tracking_warning))
                btnPrimaryAction.setTextColor(colorOf(android.R.color.white))
                holdToEndContainer.visibility = View.VISIBLE
                tvHoldHint.visibility = View.VISIBLE
                btnPrimaryAction.isEnabled = true
                btnHoldToEnd.isEnabled = true
            }
            SessionState.PAUSED -> {
                tvSessionStatus.text = "Đã tạm dừng"
                btnPrimaryAction.text = "Resume"
                btnPrimaryAction.backgroundTintList = ColorStateList.valueOf(colorOf(R.color.workout_tracking_accent))
                btnPrimaryAction.setTextColor(colorOf(R.color.workout_tracking_bg))
                holdToEndContainer.visibility = View.VISIBLE
                tvHoldHint.visibility = View.VISIBLE
                btnPrimaryAction.isEnabled = true
                btnHoldToEnd.isEnabled = true
            }
            SessionState.SAVING -> {
                tvSessionStatus.text = "Đang lưu..."
                btnPrimaryAction.text = "Saving"
                btnPrimaryAction.backgroundTintList = ColorStateList.valueOf(colorOf(R.color.workout_tracking_border))
                btnPrimaryAction.setTextColor(colorOf(android.R.color.white))
                holdToEndContainer.visibility = View.VISIBLE
                tvHoldHint.visibility = View.VISIBLE
                btnPrimaryAction.isEnabled = false
                btnHoldToEnd.isEnabled = false
            }
        }
    }

    private fun computeCurrentSpeed(elapsedSeconds: Long): Double {
        val wave = sin(elapsedSeconds / 18.0) * workoutType.speedVarianceKmh
        return max(0.0, workoutType.baseSpeedKmh + wave)
    }

    // Prefer Health Connect deltas when present, but never show fewer calories than the local session model.
    private fun displayedCaloriesBurned(): Double {
        val healthCalories = if (activeCaloriesAtStart != null && latestActiveCaloriesToday != null) {
            max(0.0, latestActiveCaloriesToday!! - activeCaloriesAtStart!!)
        } else {
            0.0
        }
        return max(estimatedCaloriesBurned, healthCalories)
    }

    private fun displayedSteps(): Int {
        val healthSteps = if (stepsTodayAtStart != null && latestStepsToday != null) {
            max(0L, latestStepsToday!! - stepsTodayAtStart!!).toInt()
        } else {
            0
        }
        val estimatedSteps = (distanceKm * workoutType.stepsPerKm).roundToInt()
        return max(healthSteps, estimatedSteps)
    }

    private fun displayedHeartRate(): String {
        latestHeartRateBpm?.let { return "$it bpm" }
        if (sessionState == SessionState.IDLE) return "-- bpm"
        val range = workoutType.estimatedHeartRateRange
        val drift = (sessionDurationSeconds / 60L).toInt().coerceAtMost(range.last - range.first)
        val wave = (sin(sessionDurationSeconds / 14.0) * 4).roundToInt()
        val estimated = (range.first + drift + wave).coerceIn(range.first, range.last)
        return "~$estimated bpm"
    }

    private fun formatDistance(distance: Double): String = String.format(Locale.US, "%.2f", distance)

    private fun formatSpeed(speed: Double): String = String.format(Locale.US, "%.1f", speed)

    private fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun colorOf(colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

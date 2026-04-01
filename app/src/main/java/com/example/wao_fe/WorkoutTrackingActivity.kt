package com.example.wao_fe

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.NetworkClient
import com.example.wao_fe.network.UserRepository
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
import kotlin.math.min
import kotlin.math.roundToInt

class WorkoutTrackingActivity : AppCompatActivity(), SensorEventListener {

    private enum class SessionState { IDLE, RUNNING, PAUSED, SAVING }

    private enum class PendingTrackingAction { NONE, START, RESUME }

    private enum class CaloriesDisplaySource { HEALTH_CONNECT, ESTIMATED }

    companion object {
        private const val DEFAULT_WEIGHT_KG = 70.0
        private const val DEFAULT_HEIGHT_CM = 170.0
        private const val MIN_VALID_GPS_ACCURACY_METERS = 35f
        private const val MIN_VALID_GPS_SEGMENT_METERS = 1.5
        private const val GPS_STALE_TIMEOUT_MS = 6_000L
        private const val HEALTH_REFRESH_INTERVAL_MS = 10_000L
    }

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
    private val userRepository = UserRepository()
    private val workoutType by lazy {
        WorkoutType.fromKey(intent.getStringExtra(WorkoutType.EXTRA_WORKOUT_TYPE))
    }
    private val locationManager by lazy { getSystemService(LocationManager::class.java) }
    private val sensorManager by lazy { getSystemService(SensorManager::class.java) }
    private val stepCounterSensor by lazy { sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }
    private val liveLocationListener = LocationListener { location -> handleLocationUpdate(location) }

    private var userId = -1L
    private var sessionState = SessionState.IDLE
    private var pendingTrackingAction = PendingTrackingAction.NONE
    private var sessionDurationSeconds = 0L
    private var distanceKm = 0.0
    private var estimatedCaloriesBurned = 0.0
    private var currentSpeedKmh = 0.0

    private var weightKg = DEFAULT_WEIGHT_KG
    private var heightCm = DEFAULT_HEIGHT_CM

    private var hasHealthConnectReadAccess = false
    private var latestHeartRateBpm: Long? = null
    private var latestStepsToday: Long? = null
    private var latestActiveCaloriesToday: Double? = null

    private var outdoorDistanceMeters = 0.0
    private var lastAcceptedGpsLocation: Location? = null
    private var lastOutdoorFixAtMillis: Long? = null
    private var isLocationUpdatesRegistered = false

    private var latestStepCounterValue: Float? = null
    private var sensorStepsAccumulated = 0
    private var sensorStepsSegmentBaseline: Float? = null
    private var isStepSensorRegistered = false

    private var healthStepsAccumulated = 0L
    private var healthStepsSegmentBaseline: Long? = null
    private var healthCaloriesAccumulated = 0.0
    private var healthCaloriesSegmentBaseline: Double? = null

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
                refreshHealthSnapshot(seedRunningSegment = sessionState == SessionState.RUNNING)
            }
            startHealthSnapshotLoop()
        } else {
            toast("Chưa cấp quyền Health Connect")
        }
    }

    private val requestTrackingPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        registerStepSensorIfNeeded()
        val canProceed = when {
            workoutType.usesGpsDistance -> hasLocationPermission()
            workoutType.usesStepMetric -> hasAvailableIndoorTrackingSource()
            else -> true
        }
        if (!canProceed) {
            pendingTrackingAction = PendingTrackingAction.NONE
            toast(missingTrackingPermissionMessage())
            renderMetrics()
            return@registerForActivityResult
        }

        when (pendingTrackingAction) {
            PendingTrackingAction.START -> startSessionInternal()
            PendingTrackingAction.RESUME -> resumeSessionInternal()
            PendingTrackingAction.NONE -> Unit
        }
        pendingTrackingAction = PendingTrackingAction.NONE
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
        loadTrackingProfile()
        checkHealthConnectAccess()
        registerStepSensorIfNeeded()
        renderMetrics()
    }

    override fun onDestroy() {
        tickJob?.cancel()
        healthSnapshotJob?.cancel()
        holdCancelled = true
        holdAnimator?.cancel()
        stopLocationUpdates()
        unregisterStepSensor()
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        latestStepCounterValue = event.values.firstOrNull()
        if (workoutType.usesStepMetric && sessionState == SessionState.RUNNING) {
            distanceKm = stepsToDistanceKm(displayedSteps())
            renderMetrics()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

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

    private fun loadTrackingProfile() {
        if (userId == -1L) return
        lifecycleScope.launch {
            when (val profileResult = userRepository.getLatestHealthProfile(userId)) {
                is ApiResult.Success -> {
                    heightCm = profileResult.data.heightCm
                    weightKg = profileResult.data.weightKg
                    estimatedCaloriesBurned = calculateEstimatedCaloriesBurned()
                    if (workoutType.usesStepMetric) {
                        distanceKm = stepsToDistanceKm(displayedSteps())
                    }
                    renderMetrics()
                }
                is ApiResult.Error -> Unit
            }
        }
    }

    private fun showMoreMenu() {
        val popupMenu = PopupMenu(this, btnWorkoutMore)
        var order = 0
        if (workoutType.usesGpsDistance && !hasLocationPermission()) {
            popupMenu.menu.add(0, 3, order++, "Cấp quyền vị trí")
        }
        if (workoutType.usesStepMetric && shouldRequestActivityRecognitionPermission() && !hasHealthConnectReadAccess) {
            popupMenu.menu.add(0, 4, order++, "Cấp quyền hoạt động")
        }
        if (!hasHealthConnectReadAccess) {
            popupMenu.menu.add(0, 1, order++, "Kết nối Health Connect")
        }
        popupMenu.menu.add(0, 2, order, "Thoát không lưu")
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
                3 -> {
                    requestLocationTrackingPermission()
                    true
                }
                4 -> {
                    requestIndoorTrackingPermission()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun startSession() {
        if (!ensureTrackingReady(PendingTrackingAction.START)) return
        startSessionInternal()
    }

    private fun startSessionInternal() {
        if (sessionState != SessionState.IDLE) return
        sessionState = SessionState.RUNNING
        currentSpeedKmh = 0.0
        beginLiveSegment()
        startTickLoop()
        startHealthSnapshotLoop()
        renderMetrics()
    }

    private fun pauseSession() {
        if (sessionState != SessionState.RUNNING) return
        commitLiveSegment()
        sessionState = SessionState.PAUSED
        currentSpeedKmh = 0.0
        stopLocationUpdates()
        renderMetrics()
    }

    private fun resumeSession() {
        if (!ensureTrackingReady(PendingTrackingAction.RESUME)) return
        resumeSessionInternal()
    }

    private fun resumeSessionInternal() {
        if (sessionState != SessionState.PAUSED) return
        sessionState = SessionState.RUNNING
        currentSpeedKmh = 0.0
        beginLiveSegment()
        startHealthSnapshotLoop()
        renderMetrics()
    }

    private fun ensureTrackingReady(action: PendingTrackingAction): Boolean {
        val permissionsToRequest = requiredTrackingPermissions()
        if (permissionsToRequest.isNotEmpty()) {
            pendingTrackingAction = action
            requestTrackingPermissions.launch(permissionsToRequest.toTypedArray())
            return false
        }
        registerStepSensorIfNeeded()
        if (workoutType.usesStepMetric && !hasAvailableIndoorTrackingSource()) {
            toast("Cần Health Connect hoặc step sensor để đo chạy trong nhà")
            return false
        }
        return true
    }

    private fun requiredTrackingPermissions(): List<String> {
        return when {
            workoutType.usesGpsDistance && !hasLocationPermission() -> listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
            workoutType.usesStepMetric && shouldRequestActivityRecognitionPermission() && !hasHealthConnectReadAccess ->
                listOf(Manifest.permission.ACTIVITY_RECOGNITION)
            else -> emptyList()
        }
    }

    private fun missingTrackingPermissionMessage(): String {
        return when {
            workoutType.usesGpsDistance -> "Cần quyền vị trí để lấy quãng đường và tốc độ"
            workoutType.usesStepMetric -> "Cần quyền hoạt động hoặc Health Connect để lấy số bước"
            else -> "Chưa đủ quyền để bắt đầu buổi tập"
        }
    }

    private fun requestLocationTrackingPermission() {
        requestTrackingPermissions.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    private fun requestIndoorTrackingPermission() {
        if (!workoutType.usesStepMetric || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        requestTrackingPermissions.launch(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION))
    }

    private fun beginLiveSegment() {
        if (workoutType.usesGpsDistance) {
            resetOutdoorAnchor()
            startLocationUpdatesIfPossible()
        }
        if (workoutType.usesStepMetric) {
            sensorStepsSegmentBaseline = latestStepCounterValue
        }
        seedHealthConnectSegment()
        lifecycleScope.launch {
            refreshHealthSnapshot(seedRunningSegment = true)
        }
    }

    // Session metrics use per-segment baselines so steps and calories taken while paused never leak into the workout.
    private fun commitLiveSegment() {
        if (workoutType.usesStepMetric) {
            sensorStepsAccumulated = currentSensorStepsTotal()
            sensorStepsSegmentBaseline = null
            healthStepsAccumulated = currentHealthStepsTotal()
            healthStepsSegmentBaseline = null
        }
        healthCaloriesAccumulated = currentHealthCaloriesTotal()
        healthCaloriesSegmentBaseline = null
        resetOutdoorAnchor()
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
                refreshDerivedMetricsForCurrentTick()
                estimatedCaloriesBurned = calculateEstimatedCaloriesBurned()
                renderMetrics()
            }
        }
    }

    private fun refreshDerivedMetricsForCurrentTick() {
        if (workoutType.usesStepMetric) {
            val previousDistance = distanceKm
            distanceKm = stepsToDistanceKm(displayedSteps())
            currentSpeedKmh = ((distanceKm - previousDistance).coerceAtLeast(0.0) * 3600.0)
            return
        }

        distanceKm = outdoorDistanceMeters / 1000.0
        val now = System.currentTimeMillis()
        if (lastOutdoorFixAtMillis == null || now - lastOutdoorFixAtMillis!! > GPS_STALE_TIMEOUT_MS) {
            currentSpeedKmh = 0.0
        }
    }

    private fun startHealthSnapshotLoop() {
        if (!hasHealthConnectReadAccess || healthSnapshotJob?.isActive == true) return
        healthSnapshotJob = lifecycleScope.launch {
            while (isActive) {
                refreshHealthSnapshot(seedRunningSegment = sessionState == SessionState.RUNNING)
                delay(HEALTH_REFRESH_INTERVAL_MS)
            }
        }
    }

    // Health Connect gives us real heart-rate, step, and calorie deltas without forcing the session to depend on backend latency.
    private suspend fun refreshHealthSnapshot(seedRunningSegment: Boolean) {
        val snapshot = runCatching {
            val client = HealthConnectClient.getOrCreate(this@WorkoutTrackingActivity)
            HealthConnectRepository(client).readTodaySnapshot()
        }.getOrNull() ?: return

        latestHeartRateBpm = snapshot.latestHeartRateBpm
        latestStepsToday = snapshot.stepsToday
        latestActiveCaloriesToday = snapshot.activeCaloriesBurnedToday
        if (seedRunningSegment) {
            seedHealthConnectSegment()
        }
        if (workoutType.usesStepMetric && sessionState == SessionState.RUNNING && !canReadStepSensor()) {
            distanceKm = stepsToDistanceKm(displayedSteps())
        }
        renderMetrics()
    }

    private fun seedHealthConnectSegment() {
        if (sessionState != SessionState.RUNNING || !hasHealthConnectReadAccess) return
        if (workoutType.usesStepMetric && healthStepsSegmentBaseline == null && latestStepsToday != null) {
            healthStepsSegmentBaseline = latestStepsToday
        }
        if (healthCaloriesSegmentBaseline == null && latestActiveCaloriesToday != null) {
            healthCaloriesSegmentBaseline = latestActiveCaloriesToday
        }
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
                refreshHealthSnapshot(seedRunningSegment = false)
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

    @SuppressLint("MissingPermission")
    private fun startLocationUpdatesIfPossible() {
        if (!workoutType.usesGpsDistance || isLocationUpdatesRegistered || !hasLocationPermission()) return

        val availableProviders = buildList {
            if (hasFineLocationPermission() && isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (hasLocationPermission() && isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }
        if (availableProviders.isEmpty()) {
            toast("Bật vị trí trên thiết bị để lấy quãng đường và tốc độ")
            return
        }

        availableProviders.forEach { provider ->
            locationManager?.requestLocationUpdates(
                provider,
                2_000L,
                2f,
                liveLocationListener,
                Looper.getMainLooper(),
            )
        }
        isLocationUpdatesRegistered = true
    }

    private fun stopLocationUpdates() {
        if (!isLocationUpdatesRegistered) return
        runCatching { locationManager?.removeUpdates(liveLocationListener) }
        isLocationUpdatesRegistered = false
        resetOutdoorAnchor()
    }

    private fun resetOutdoorAnchor() {
        lastAcceptedGpsLocation = null
        lastOutdoorFixAtMillis = null
    }

    // GPS noise can create fake jumps, so we only accept accurate samples and plausible speed transitions.
    private fun handleLocationUpdate(location: Location) {
        if (!workoutType.usesGpsDistance || sessionState != SessionState.RUNNING) return
        if (location.hasAccuracy() && location.accuracy > MIN_VALID_GPS_ACCURACY_METERS) return

        val previousLocation = lastAcceptedGpsLocation
        lastAcceptedGpsLocation = location
        lastOutdoorFixAtMillis = System.currentTimeMillis()

        if (previousLocation == null) {
            currentSpeedKmh = if (location.hasSpeed()) {
                min(workoutType.maxReasonableGpsSpeedKmh, location.speed.toDouble() * 3.6)
            } else {
                0.0
            }
            renderMetrics()
            return
        }

        val deltaMeters = previousLocation.distanceTo(location).toDouble()
        val deltaSeconds = ((location.time - previousLocation.time).coerceAtLeast(1_000L)).toDouble() / 1_000.0
        val computedSpeedKmh = (deltaMeters / deltaSeconds) * 3.6

        if (deltaMeters < MIN_VALID_GPS_SEGMENT_METERS) {
            currentSpeedKmh = if (location.hasSpeed()) {
                min(workoutType.maxReasonableGpsSpeedKmh, location.speed.toDouble() * 3.6)
            } else {
                0.0
            }
            renderMetrics()
            return
        }

        if (computedSpeedKmh > workoutType.maxReasonableGpsSpeedKmh * 1.35) {
            return
        }

        outdoorDistanceMeters += deltaMeters
        distanceKm = outdoorDistanceMeters / 1_000.0
        currentSpeedKmh = if (location.hasSpeed()) {
            min(workoutType.maxReasonableGpsSpeedKmh, location.speed.toDouble() * 3.6)
        } else {
            computedSpeedKmh.coerceAtMost(workoutType.maxReasonableGpsSpeedKmh)
        }
        renderMetrics()
    }

    private fun registerStepSensorIfNeeded() {
        if (!canReadStepSensor() || isStepSensorRegistered) return
        val sensor = stepCounterSensor ?: return
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        isStepSensorRegistered = true
    }

    private fun unregisterStepSensor() {
        if (!isStepSensorRegistered) return
        sensorManager?.unregisterListener(this)
        isStepSensorRegistered = false
    }

    private fun canReadStepSensor(): Boolean {
        if (!workoutType.usesStepMetric) return false
        if (stepCounterSensor == null) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    private fun hasAvailableIndoorTrackingSource(): Boolean {
        return canReadStepSensor() || hasHealthConnectReadAccess
    }

    private fun shouldRequestActivityRecognitionPermission(): Boolean {
        return workoutType.usesStepMetric &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    private fun hasLocationPermission(): Boolean {
        return hasFineLocationPermission() || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun hasFineLocationPermission(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun isProviderEnabled(provider: String): Boolean {
        return runCatching { locationManager?.isProviderEnabled(provider) == true }.getOrDefault(false)
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
                caloriesPerMin = workoutType.defaultCaloriesPerMinute,
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
        val caloriesValue = displayedCaloriesBurned().roundToInt()
        tvMetricCaloriesValue.text = when (caloriesDisplaySource()) {
            CaloriesDisplaySource.HEALTH_CONNECT -> "$caloriesValue kcal"
            CaloriesDisplaySource.ESTIMATED -> "~$caloriesValue kcal"
        }
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

    private fun caloriesDisplaySource(): CaloriesDisplaySource {
        return if (healthCaloriesValueOrNull() != null) {
            CaloriesDisplaySource.HEALTH_CONNECT
        } else {
            CaloriesDisplaySource.ESTIMATED
        }
    }

    private fun healthCaloriesValueOrNull(): Double? {
        if (!hasHealthConnectReadAccess) return null
        if (latestActiveCaloriesToday == null && healthCaloriesAccumulated == 0.0 && healthCaloriesSegmentBaseline == null) {
            return null
        }
        return currentHealthCaloriesTotal()
    }

    private fun displayedCaloriesBurned(): Double {
        return healthCaloriesValueOrNull() ?: estimatedCaloriesBurned
    }

    private fun currentHealthCaloriesTotal(): Double {
        val currentDelta = if (
            sessionState == SessionState.RUNNING &&
            healthCaloriesSegmentBaseline != null &&
            latestActiveCaloriesToday != null
        ) {
            (latestActiveCaloriesToday!! - healthCaloriesSegmentBaseline!!).coerceAtLeast(0.0)
        } else {
            0.0
        }
        return healthCaloriesAccumulated + currentDelta
    }

    // We prefer the sensor for indoor live tracking, but Health Connect remains a fallback so the session still has real step data.
    private fun displayedSteps(): Int {
        val sensorSteps = currentSensorStepsTotal()
        val healthSteps = currentHealthStepsTotal().toInt()
        return maxOf(sensorSteps, healthSteps)
    }

    private fun currentSensorStepsTotal(): Int {
        val currentDelta = if (
            sessionState == SessionState.RUNNING &&
            sensorStepsSegmentBaseline != null &&
            latestStepCounterValue != null
        ) {
            (latestStepCounterValue!! - sensorStepsSegmentBaseline!!).coerceAtLeast(0f).roundToInt()
        } else {
            0
        }
        return sensorStepsAccumulated + currentDelta
    }

    private fun currentHealthStepsTotal(): Long {
        val currentDelta = if (
            sessionState == SessionState.RUNNING &&
            healthStepsSegmentBaseline != null &&
            latestStepsToday != null
        ) {
            (latestStepsToday!! - healthStepsSegmentBaseline!!).coerceAtLeast(0L)
        } else {
            0L
        }
        return healthStepsAccumulated + currentDelta
    }

    private fun displayedHeartRate(): String {
        return latestHeartRateBpm?.let { "$it bpm" } ?: "-- bpm"
    }

    private fun calculateEstimatedCaloriesBurned(): Double {
        val durationHours = sessionDurationSeconds / 3600.0
        return workoutType.metValue * weightKg * durationHours
    }

    private fun strideLengthMeters(): Double {
        return (heightCm / 100.0) * workoutType.strideLengthFactor
    }

    private fun stepsToDistanceKm(steps: Int): Double {
        return (steps * strideLengthMeters()) / 1_000.0
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

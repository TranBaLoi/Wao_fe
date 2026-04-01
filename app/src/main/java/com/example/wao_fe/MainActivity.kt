package com.example.wao_fe

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wao_fe.namstats.StatisticsDashboardActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.wao_fe.component.FloatingAddMenu
import com.example.wao_fe.health.HealthConnectManager
import com.example.wao_fe.health.HealthConnectRepository
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.OpenFoodFactsApi
import com.example.wao_fe.network.UserRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.cardview.widget.CardView
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvCalIn: TextView
    private lateinit var tvCalOut: TextView
    private lateinit var tvCalRemaining: TextView
    private lateinit var pbCalories: ProgressBar
    private lateinit var tvWater: TextView
    private lateinit var tvSteps: TextView
    private lateinit var tvHealthConnectStatus: TextView
    private lateinit var tvHeartRate: TextView
    private lateinit var tvHeartRateMeta: TextView
    private lateinit var fabAddFood: FloatingActionButton
    private lateinit var ivAvatar: ImageView
    private lateinit var cardSteps: CardView
    private lateinit var cardHeartRate: CardView
    private lateinit var bottomNavigationView: com.google.android.material.bottomnavigation.BottomNavigationView
    private lateinit var btnUpdateWeight: android.widget.Button

    private var floatingMenuDialog: android.app.Dialog? = null
    private val userRepository = UserRepository()
    private var userId: Long = -1
    private var targetCaloriesGoal = 2000.0
    private var backendCaloriesIn = 0.0
    private var backendCaloriesOut = 0.0
    private var backendStepsToday: Long? = null
    private var healthConnectStepsToday: Long? = null
    private var healthConnectActiveCaloriesBurned: Double? = null
    private var latestHeartRateBpm: Long? = null
    private var latestHeartRateMeasuredAtText: String? = null
    private var hasHealthConnectReadAccess = false
    private val heartRateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    // Register once at Activity scope so the system can return the granted Health Connect permissions.
    private val requestHealthPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(HealthConnectManager.readPermissions)) {
            Toast.makeText(this, "Health Connect connected", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Health Connect permission was not granted", Toast.LENGTH_SHORT).show()
        }
        checkHealthConnectAccess(promptIfMissing = false, initiatedByUser = false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        userId = sharedPref.getLong("USER_ID", -1)
        val userName = sharedPref.getString("USER_NAME", "Bạn")

        initViews()
        setupHeader(userName)
        checkHealthConnectAccess(promptIfMissing = true, initiatedByUser = false)

        if (userId != -1L) {
            fetchDashboardData()
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin phiên đăng nhập", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initViews() {
        tvUserName = findViewById(R.id.tvUserName)
        tvDate = findViewById(R.id.tvDate)
        tvCalIn = findViewById(R.id.tvCalIn)
        tvCalOut = findViewById(R.id.tvCalOut)
        tvCalRemaining = findViewById(R.id.tvCalRemaining)
        pbCalories = findViewById(R.id.pbCalories)
        tvWater = findViewById(R.id.tvWater)
        tvSteps = findViewById(R.id.tvSteps)
        tvHealthConnectStatus = findViewById(R.id.tvHealthConnectStatus)
        tvHeartRate = findViewById(R.id.tvHeartRate)
        tvHeartRateMeta = findViewById(R.id.tvHeartRateMeta)
        fabAddFood = findViewById(R.id.fabAddFood)
        ivAvatar = findViewById(R.id.ivAvatar)
        cardSteps = findViewById(R.id.cardSteps)
        cardHeartRate = findViewById(R.id.cardHeartRate)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        btnUpdateWeight = findViewById(R.id.btnUpdateWeight)

        btnUpdateWeight.setOnClickListener {
            val bottomSheet = UpdateWeightBottomSheet()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }

        setupWorkoutShortcuts()

        ivAvatar.setOnClickListener {
            startActivity(android.content.Intent(this, EditProfileActivity::class.java))
        }

        cardSteps.setOnClickListener {
            checkHealthConnectAccess(promptIfMissing = true, initiatedByUser = true)
        }

        cardHeartRate.setOnClickListener {
            checkHealthConnectAccess(promptIfMissing = true, initiatedByUser = true)
        }

        renderHealthConnectMetrics()

        bottomNavigationView.selectedItemId = R.id.nav_home
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_menu -> {
                    startActivity(Intent(this, MealPlanActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, SettingsActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_diary -> {
                    startActivity(Intent(this, FoodDiaryActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_home -> true
                else -> {
                    Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }

        /*
        findViewById<TextView>(R.id.btnLogFood)?.setOnClickListener {
            Toast.makeText(this, "Tính năng Thêm món ăn đang phát triển", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.btnLogWorkout)?.setOnClickListener {
            Toast.makeText(this, "Tính năng Ghi nhận tập luyện đang phát triển", Toast.LENGTH_SHORT).show()
        }

        val btnOpenStatistics = findViewById<TextView>(R.id.btnOpenStatistics)
        if (btnOpenStatistics != null) {
            btnOpenStatistics.setOnClickListener {
                startActivity(Intent(this, StatisticsDashboardActivity::class.java))
            }
        }
        */

        fabAddFood.setOnClickListener {
            if (floatingMenuDialog?.isShowing == true) {
                floatingMenuDialog?.dismiss()
            } else {
                showFloatingMenu()
            }
        }
    }

    // Keep the home shortcuts centralized so feature 2 always exposes the same 4 sports.
    private fun setupWorkoutShortcuts() {
        findViewById<View>(R.id.cardWorkoutWalking)?.setOnClickListener {
            openWorkoutTracking(WorkoutType.WALKING)
        }
        findViewById<View>(R.id.cardWorkoutOutdoorRun)?.setOnClickListener {
            openWorkoutTracking(WorkoutType.OUTDOOR_RUNNING)
        }
        findViewById<View>(R.id.cardWorkoutIndoorRun)?.setOnClickListener {
            openWorkoutTracking(WorkoutType.INDOOR_RUNNING)
        }
        findViewById<View>(R.id.cardWorkoutCycling)?.setOnClickListener {
            openWorkoutTracking(WorkoutType.CYCLING)
        }
    }

    private fun openWorkoutTracking(workoutType: WorkoutType) {
        startActivity(
            Intent(this, WorkoutTrackingActivity::class.java)
                .putExtra(WorkoutType.EXTRA_WORKOUT_TYPE, workoutType.key)
        )
    }

    private fun showFloatingMenu() {
        if (floatingMenuDialog == null) {
            floatingMenuDialog = FloatingAddMenu.create(
                activity = this,
                onScanBarcode = { startBarcodeScanner() }
            )
            floatingMenuDialog?.setOnDismissListener {
                floatingMenuDialog = null
            }
        }
        floatingMenuDialog?.show()
    }

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Log.w("BarcodeScan", "Người dùng đã hủy quá trình quét")
            Toast.makeText(this, "Đã hủy quét", Toast.LENGTH_LONG).show()
        } else {
            val barcode = result.contents
            Log.i("BarcodeScan", "Quét thành công mã vạch: $barcode")
            Toast.makeText(this, "Đang xử lý mã vạch...", Toast.LENGTH_SHORT).show()
            fetchProductInfo(barcode)
        }
    }

    private fun startBarcodeScanner() {
        Log.d("BarcodeScan", "Bắt đầu gọi Activity quét mã vạch")
        val options = ScanOptions()
        options.setPrompt("Đặt mã vạch sản phẩm vào giữa khung hình")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        options.setCaptureActivity(CustomScannerActivity::class.java)
        barcodeLauncher.launch(options)
    }

    private fun fetchProductInfo(barcode: String) {
        Log.d("BarcodeScan", "Bắt đầu gọi API OpenFoodFacts cho barcode: $barcode")
        lifecycleScope.launch {
            try {
                val api = OpenFoodFactsApi.create()
                val response = api.getProductInfo(barcode)
                if (response.status == 1 && response.product != null) {
                    val p = response.product
                    val name = p.product_name ?: p.generic_name ?: "Không rõ tên"
                    val energy = p.nutriments?.energy_kcal_100g ?: p.nutriments?.energy_kcal ?: 0.0

                    Log.i("BarcodeScan", "Tìm thấy sản phẩm: $name - $energy kcal")

                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Thông tin sản phẩm")
                        .setMessage("Tên: $name\nNăng lượng: $energy kcal/100g")
                        .setPositiveButton("Đóng", null)
                        .show()
                } else {
                    Log.w("BarcodeScan", "Không tìm thấy thông tin trên OpenFoodFacts cho $barcode")
                    Toast.makeText(this@MainActivity, "Không tìm thấy thông tin sản phẩm", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("BarcodeScan", "Lỗi kết nối API lấy sản phẩm: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload user info when returning to app (e.g. from EditProfileActivity)
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val userName = sharedPref.getString("USER_NAME", "Bạn")
        val currentAvatar = sharedPref.getString("USER_AVATAR", null)

        setupHeader(userName)

        if (currentAvatar != null) {
            Glide.with(this)
                .load(Uri.parse(currentAvatar))
                .apply(com.bumptech.glide.request.RequestOptions.circleCropTransform())
                .into(ivAvatar)
            ivAvatar.setPadding(0, 0, 0, 0)
        }

        checkHealthConnectAccess(promptIfMissing = false, initiatedByUser = false)
    }

    private fun setupHeader(userName: String?) {
        tvUserName.text = "Chào ${userName ?: "bạn"},"
        val sdf = SimpleDateFormat("EEEE, dd MMM", Locale("vi", "VN"))
        tvDate.text = sdf.format(Date())
    }

    private fun fetchDashboardData() {
        lifecycleScope.launch {
            // First fetch latest profile to get targetCalories
            var targetCalories = 2000.0
            val profileResult = userRepository.getLatestHealthProfile(userId)
            if (profileResult is ApiResult.Success) {
                targetCalories = profileResult.data.targetCalories
            }
            targetCaloriesGoal = targetCalories

            // Fetch logs for macro tracking
            var consumedProtein = 0.0
            var consumedCarbs = 0.0
            var consumedFat = 0.0
            try {
                val api = com.example.wao_fe.network.NetworkClient.apiService
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                val logs = api.getFoodLogs(userId, todayStr)
                val foods = api.getFoods().associateBy { it.id }
                logs.forEach { log ->
                    val food = foods[log.foodId]
                    if (food != null) {
                        consumedProtein += food.protein * log.servingQty
                        consumedCarbs += food.carbs * log.servingQty
                        consumedFat += food.fat * log.servingQty
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to fetch logs for macros", e)
            }

            val tvProteinMain = findViewById<TextView>(R.id.tvProteinMain)
            val pbProteinMain = findViewById<ProgressBar>(R.id.pbProteinMain)
            val tvCarbsMain = findViewById<TextView>(R.id.tvCarbsMain)
            val pbCarbsMain = findViewById<ProgressBar>(R.id.pbCarbsMain)
            val tvFatMain = findViewById<TextView>(R.id.tvFatMain)
            val pbFatMain = findViewById<ProgressBar>(R.id.pbFatMain)

            tvProteinMain.text = "${consumedProtein.toInt()}g"
            val proteinTarget = (targetCalories * 0.3 / 4.0).coerceAtLeast(1.0)
            pbProteinMain.progress = ((consumedProtein / proteinTarget) * 100).toInt().coerceIn(0, 100)

            tvCarbsMain.text = "${consumedCarbs.toInt()}g"
            val carbsTarget = (targetCalories * 0.4 / 4.0).coerceAtLeast(1.0)
            pbCarbsMain.progress = ((consumedCarbs / carbsTarget) * 100).toInt().coerceIn(0, 100)

            tvFatMain.text = "${consumedFat.toInt()}g"
            val fatTarget = (targetCalories * 0.3 / 9.0).coerceAtLeast(1.0)
            pbFatMain.progress = ((consumedFat / fatTarget) * 100).toInt().coerceIn(0, 100)

            // Then fetch daily summary
            val summaryResult = userRepository.getTodaySummary(userId)
            if (summaryResult is ApiResult.Success) {
                val summary = summaryResult.data
                backendCaloriesIn = summary.totalCalIn
                backendCaloriesOut = summary.totalCalOut
                renderCaloriesSummary()

                tvWater.text = "${summary.totalWater} ml"
                backendStepsToday = summary.totalSteps.toLong()
                renderStepsCard()
            } else {
                // If no summary exist for today yet, just show 0
                backendCaloriesIn = 0.0
                backendCaloriesOut = 0.0
                renderCaloriesSummary()
                backendStepsToday = 0L
                renderStepsCard()
                if (summaryResult is ApiResult.Error && summaryResult.status != 404) {
                    // Ignore 404 since it just means no logs today yet
                    Toast.makeText(this@MainActivity, "Không thể tải dữ liệu hôm nay", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // This is the single place that decides whether the device supports Health Connect,
    // whether the provider must be installed, and whether our read permissions are already granted.
    private fun checkHealthConnectAccess(promptIfMissing: Boolean, initiatedByUser: Boolean) {
        lifecycleScope.launch {
            when (HealthConnectManager.getSdkStatus(this@MainActivity)) {
                HealthConnectClient.SDK_UNAVAILABLE -> {
                    hasHealthConnectReadAccess = false
                    clearHealthConnectSnapshot()
                    updateHealthConnectStatus("Health Connect is not supported on this device")
                    if (initiatedByUser) {
                        Toast.makeText(
                            this@MainActivity,
                            "Health Connect is not supported on this device",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }

                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    hasHealthConnectReadAccess = false
                    clearHealthConnectSnapshot()
                    updateHealthConnectStatus("Install or update Health Connect")
                    if (promptIfMissing) {
                        showHealthConnectInstallDialog()
                    }
                }

                HealthConnectClient.SDK_AVAILABLE -> {
                    val healthConnectClient = HealthConnectClient.getOrCreate(this@MainActivity)
                    val grantedPermissions =
                        healthConnectClient.permissionController.getGrantedPermissions()

                    if (grantedPermissions.containsAll(HealthConnectManager.readPermissions)) {
                        hasHealthConnectReadAccess = true
                        updateHealthConnectStatus("")
                        showHealthConnectLoadingState()
                        loadHealthConnectMetrics(healthConnectClient)
                        if (initiatedByUser) {
                            Toast.makeText(
                                this@MainActivity,
                                "Health Connect is ready",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    } else {
                        hasHealthConnectReadAccess = false
                        clearHealthConnectSnapshot()
                        updateHealthConnectStatus("Tap to grant Health Connect access")
                        if (promptIfMissing) {
                            showHealthConnectPermissionDialog()
                        }
                    }
                }
            }
        }
    }

    private fun updateHealthConnectStatus(statusText: String) {
        tvHealthConnectStatus.text = statusText
        tvHealthConnectStatus.visibility = if (statusText.isBlank()) View.GONE else View.VISIBLE
    }

    private fun loadHealthConnectMetrics(healthConnectClient: HealthConnectClient) {
        lifecycleScope.launch {
            runCatching {
                HealthConnectRepository(healthConnectClient).readTodaySnapshot()
            }.onSuccess { snapshot ->
                healthConnectStepsToday = snapshot.stepsToday
                healthConnectActiveCaloriesBurned = snapshot.activeCaloriesBurnedToday
                latestHeartRateBpm = snapshot.latestHeartRateBpm
                latestHeartRateMeasuredAtText = snapshot.latestHeartRateTime?.atZone(ZoneId.systemDefault())
                    ?.format(heartRateTimeFormatter)
                renderHealthConnectMetrics()
            }.onFailure { error ->
                Log.e("MainActivity", "Failed to read Health Connect data", error)
                healthConnectStepsToday = null
                healthConnectActiveCaloriesBurned = null
                latestHeartRateBpm = null
                latestHeartRateMeasuredAtText = null
                renderHealthConnectMetrics()
                updateHealthConnectStatus("Connected, but couldn't read today's data")
            }
        }
    }

    private fun clearHealthConnectSnapshot() {
        healthConnectStepsToday = null
        healthConnectActiveCaloriesBurned = null
        latestHeartRateBpm = null
        latestHeartRateMeasuredAtText = null
        renderHealthConnectMetrics()
    }

    private fun showHealthConnectLoadingState() {
        tvHeartRate.text = "-- bpm"
        tvHeartRateMeta.text = "Reading data from Health Connect"
    }

    private fun renderHealthConnectMetrics() {
        renderCaloriesSummary()
        renderStepsCard()
        renderHeartRateCard()
    }

    private fun renderCaloriesSummary() {
        val caloriesOut = healthConnectActiveCaloriesBurned ?: backendCaloriesOut
        val netCalories = backendCaloriesIn - caloriesOut
        val remaining = targetCaloriesGoal - netCalories
        val progress = if (targetCaloriesGoal > 0) {
            ((netCalories / targetCaloriesGoal) * 100).toInt()
        } else {
            0
        }

        tvCalIn.text = backendCaloriesIn.toInt().toString()
        tvCalOut.text = caloriesOut.toInt().toString()
        tvCalRemaining.text = remaining.toInt().coerceAtLeast(0).toString()
        pbCalories.progress = progress.coerceIn(0, 100)
    }

    private fun renderStepsCard() {
        val stepsToday = healthConnectStepsToday ?: backendStepsToday ?: 0L
        tvSteps.text = "$stepsToday/10000"
    }

    private fun renderHeartRateCard() {
        if (latestHeartRateBpm != null) {
            tvHeartRate.text = "${latestHeartRateBpm} bpm"
            tvHeartRateMeta.text = "Updated at ${latestHeartRateMeasuredAtText ?: "--:--"}"
            return
        }

        tvHeartRate.text = "-- bpm"
        tvHeartRateMeta.text = if (hasHealthConnectReadAccess) {
            "No heart rate data today in Health Connect"
        } else {
            "Connect Health Connect to view data"
        }
    }

    private fun showHealthConnectPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Connect Health Connect")
            .setMessage(
                "Wao needs read access to your step count, active calories burned, and heart rate so we can connect the app to Health Connect.",
            )
            .setNegativeButton("Later", null)
            .setPositiveButton("Grant access") { _, _ ->
                requestHealthPermissions.launch(HealthConnectManager.readPermissions)
            }
            .show()
    }

    private fun showHealthConnectInstallDialog() {
        AlertDialog.Builder(this)
            .setTitle("Install Health Connect")
            .setMessage(
                "Health Connect is required before Wao can ask for step count and heart rate permissions.",
            )
            .setNegativeButton("Later", null)
            .setPositiveButton("Open Play Store") { _, _ ->
                openHealthConnectStorePage()
            }
            .show()
    }

    private fun openHealthConnectStorePage() {
        try {
            startActivity(HealthConnectManager.buildInstallIntent(this))
        } catch (_: ActivityNotFoundException) {
            startActivity(HealthConnectManager.buildBrowserFallbackIntent())
        }
    }

    override fun onDestroy() {
        floatingMenuDialog?.setOnDismissListener(null)
        runCatching { floatingMenuDialog?.dismiss() }
        floatingMenuDialog = null
        super.onDestroy()
    }
}

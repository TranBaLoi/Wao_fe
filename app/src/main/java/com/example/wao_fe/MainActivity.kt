package com.example.wao_fe

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.wao_fe.component.FloatingAddMenu
import com.example.wao_fe.health.HealthConnectManager
import com.example.wao_fe.health.HealthConnectRepository
import com.example.wao_fe.namstats.models.StatisticsGroupBy
import com.example.wao_fe.namstats.views.NamTrendChartView
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.OpenFoodFactsApi
import com.example.wao_fe.network.UserRepository
import com.example.wao_fe.network.models.CreateFoodLogRequest
import com.example.wao_fe.network.models.MealType
import com.example.wao_fe.utils.NotificationHelper
import com.example.wao_fe.utils.ReminderManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.cardview.widget.CardView
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class MainActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvCalIn: TextView
    private lateinit var tvCalOut: TextView
    private lateinit var tvCalRemaining: TextView
    private lateinit var tvCalRemainingLabel: TextView
    private lateinit var pbCalories: ProgressBar
    private lateinit var tvWater: TextView
    private lateinit var tvSteps: TextView
    private lateinit var tvHealthConnectStatus: TextView
    private lateinit var tvHeartRate: TextView
    private lateinit var tvHeartRateMeta: TextView
    private lateinit var fabAddFood: FloatingActionButton
    private lateinit var fabChatbot: ImageView
    private lateinit var ivAvatar: ImageView
    private lateinit var cardSteps: CardView
    private lateinit var cardHeartRate: CardView
    private lateinit var bottomNavigationView: com.google.android.material.bottomnavigation.BottomNavigationView
//    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var btnUpdateWeight: android.widget.Button
    private lateinit var chartWeightTrend: NamTrendChartView
    private lateinit var tvWeightStatus: TextView
    private lateinit var tvWeightChangeStatus: TextView
    private lateinit var btnPlusWater: ImageView
    private lateinit var btnMinusWater: ImageView

    private var floatingMenuDialog: android.app.Dialog? = null
    private val userRepository = UserRepository()
    private var userId: Long = -1
    private var currentWaterMl = 0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Permission", "Quyền thông báo đã được cấp")
            ReminderManager.setupAllReminders(this)
        } else {
            Toast.makeText(this, "Bạn cần cấp quyền để nhận thông báo nhắc nhở", Toast.LENGTH_SHORT).show()
        }
    }
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

        NotificationHelper.createNotificationChannels(this)

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        userId = sharedPref.getLong("USER_ID", -1)
        val userName = sharedPref.getString("USER_NAME", "Ban")

        initViews()
        setupHeader(userName)
        checkHealthConnectAccess(promptIfMissing = true, initiatedByUser = false)

        if (userId != -1L) {
            fetchDashboardData()
            askNotificationPermission()
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin phiên đăng nhập", Toast.LENGTH_SHORT).show()
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // Đã cấp quyền
                ReminderManager.setupAllReminders(this)
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                AlertDialog.Builder(this)
                    .setTitle("Quyền thông báo")
                    .setMessage("Ứng dụng cần quyền gửi thông báo để nhắc nhở bạn uống nước và ghi nhật ký bữa ăn.")
                    .setPositiveButton("Đồng ý") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Android thấp hơn 13 mặc định có quyền
            ReminderManager.setupAllReminders(this)
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
        fabChatbot = findViewById(R.id.fabChatbot)
        ivAvatar = findViewById(R.id.ivAvatar)
        cardSteps = findViewById(R.id.cardSteps)
        cardHeartRate = findViewById(R.id.cardHeartRate)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        btnUpdateWeight = findViewById(R.id.btnUpdateWeight)
        chartWeightTrend = findViewById(R.id.chartWeightTrend)
        tvWeightStatus = findViewById(R.id.tvWeightStatus)
        tvWeightChangeStatus = findViewById(R.id.tvWeightChangeStatus)
        btnPlusWater = findViewById(R.id.btnPlusWater)
        btnMinusWater = findViewById(R.id.btnMinusWater)
        chartWeightTrend.setAxisUnits("(kg)", "Ngày")
        tvCalRemainingLabel = findViewById(R.id.tvCalRemainingLabel)
        btnPlusWater.setOnClickListener {
            currentWaterMl += 200
            tvWater.text = "$currentWaterMl ml"
            addWaterLog(200)
        }

        btnMinusWater.setOnClickListener {
            if (currentWaterMl > 0) {
                currentWaterMl = (currentWaterMl - 200).coerceAtLeast(0)
                tvWater.text = "$currentWaterMl ml"
                removeLastWaterLog()
            }
        }

        btnUpdateWeight.setOnClickListener {
            val bottomSheet = UpdateWeightBottomSheet.newInstance(userId).apply {
                onWeightUpdated = { fetchWeightTrendData() }
            }
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }

        setupWorkoutShortcuts()

        ivAvatar.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
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
            Toast.makeText(this, "Tinh nang Them mon an dang phat trien", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.btnLogWorkout)?.setOnClickListener {
            Toast.makeText(this, "Tinh nang Ghi nhan tap luyen dang phat trien", Toast.LENGTH_SHORT).show()
        }
        */

        fabAddFood.setOnClickListener {
            if (floatingMenuDialog?.isShowing == true) {
                floatingMenuDialog?.dismiss()
            } else {
                showFloatingMenu()
            }
        }

        fabChatbot.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
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
                onScanBarcode = { startBarcodeScanner() },
                onUploadImageScan = { pickImageLauncher.launch("image/*") }
            )
            floatingMenuDialog?.setOnDismissListener {
                floatingMenuDialog = null
            }
        }
        floatingMenuDialog?.show()
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val barcode = decodeBarcodeFromImage(uri)
            if (barcode != null) {
                Log.i("BarcodeScan", "Đã đọc mã từ ảnh: $barcode")
                Toast.makeText(this, "Đang xử lý mã vạch...", Toast.LENGTH_SHORT).show()
                fetchProductInfo(barcode)
            } else {
                Toast.makeText(this, "Không tìm thấy mã vạch trong ảnh", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun decodeBarcodeFromImage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)

            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))

            val reader = com.google.zxing.MultiFormatReader()
            // optionally configure barcode formats here
            val result = reader.decode(binaryBitmap)
            result.text
        } catch (e: Exception) {
            Log.e("BarcodeScan", "Lỗi đọc mã vạch từ ảnh", e)
            null
        }
    }

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Log.w("BarcodeScan", "Người dùng đã hủy quá trình quét")
            Toast.makeText(this, "Da huy quet", Toast.LENGTH_LONG).show()
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
                    val name = p.product_name ?: p.generic_name ?: "Sản phẩm không rõ tên"
                    // Năng lượng lấy từ energy-kcal_100g, nếu null dò tìm các key khác, nếu vẫn 0.0 hoặc null thì gán mặc định 1.0
                    val rawEnergyKcal = p.nutriments?.energy_kcal_100g ?: p.nutriments?.energy_kcal
                    val rawEnergyKj = p.nutriments?.energy_100g ?: p.nutriments?.energy

                    var energy = 1.0
                    if (rawEnergyKcal != null && rawEnergyKcal > 0) {
                        energy = rawEnergyKcal
                    } else if (rawEnergyKj != null && rawEnergyKj > 0) {
                        // kj to kcal approx conversion
                        energy = rawEnergyKj / 4.184
                    }

                    val protein = p.nutriments?.proteins_100g ?: 0.0
                    val carbs = p.nutriments?.carbohydrates_100g ?: 0.0
                    val fat = p.nutriments?.fat_100g ?: 0.0
                    val ingredients = p.ingredients_text ?: "Chưa có thông tin thành phần"
                    val imageUrl = p.image_url

                    Log.i("BarcodeScan", "Tìm thấy sản phẩm: $name - $energy kcal")

                    checkAndShowFoodDialog(name, energy, protein, carbs, fat, ingredients, imageUrl)
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

    private suspend fun checkAndShowFoodDialog(
        name: String, calories: Double, protein: Double, carbs: Double, fat: Double, ingredients: String, imageUrl: String?
    ) {
        try {
            val backendApi = com.example.wao_fe.network.NetworkClient.apiService
            val searchResult = backendApi.getFoods(name)

            // Tìm sản phẩm trùng tên nếu có
            val existingFood = searchResult.firstOrNull { it.name.equals(name, ignoreCase = true) }

            showFoodBottomSheet(
                name = name,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                ingredients = ingredients,
                imageUrl = imageUrl,
                existingFoodId = existingFood?.id
            )
        } catch (e: Exception) {
            Log.e("BarcodeScan", "Lỗi kiểm tra CSDL API: ${e.message}", e)
            Toast.makeText(this, "Không thể kiểm tra CSDL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFoodBottomSheet(
        name: String, calories: Double, protein: Double, carbs: Double, fat: Double, ingredients: String, imageUrl: String?, existingFoodId: Long?
    ) {
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_dialog_scanned_food, null)
        bottomSheet.setContentView(view)

        val ivFoodImage = view.findViewById<ImageView>(R.id.ivFoodImage)
        val tvFoodName = view.findViewById<TextView>(R.id.tvFoodName)
        val tvFoodDbStatus = view.findViewById<TextView>(R.id.tvFoodDbStatus)
        val tvFoodIngredients = view.findViewById<TextView>(R.id.tvFoodIngredients)
        val tvFoodServingSize = view.findViewById<TextView>(R.id.tvFoodServingSize)
        val tvFoodCalories = view.findViewById<TextView>(R.id.tvFoodCalories)
        val tvFoodProtein = view.findViewById<TextView>(R.id.tvFoodProtein)
        val tvFoodCarbs = view.findViewById<TextView>(R.id.tvFoodCarbs)
        val tvFoodFat = view.findViewById<TextView>(R.id.tvFoodFat)
        val btnAddFood = view.findViewById<android.widget.Button>(R.id.btnAddFood)
        val btnAction = view.findViewById<android.widget.Button>(R.id.btnAction)
        val btnCancel = view.findViewById<android.widget.Button>(R.id.btnCancel)

        tvFoodName.text = name
        tvFoodIngredients.text = "Thành phần: $ingredients"
        tvFoodServingSize.text = "Khẩu phần: 100g"

        tvFoodCalories.text = String.format("%.1f", calories)
        tvFoodProtein.text = String.format("%.1fg", protein)
        tvFoodCarbs.text = String.format("%.1fg", carbs)
        tvFoodFat.text = String.format("%.1fg", fat)

        if (imageUrl != null) {
            Glide.with(this).load(imageUrl).into(ivFoodImage)
        }

        if (existingFoodId != null) {
            tvFoodDbStatus.text = "Sản phẩm ĐÃ CÓ trong hệ thống"
            tvFoodDbStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            btnAddFood.visibility = android.view.View.GONE
            btnAction.text = "Lưu vào Nhật ký ăn uống"
            btnAction.visibility = android.view.View.VISIBLE
        } else {
            tvFoodDbStatus.text = "Sản phẩm CHƯA CÓ trong hệ thống"
            tvFoodDbStatus.setTextColor(android.graphics.Color.parseColor("#E53935"))
            btnAddFood.visibility = android.view.View.VISIBLE
            btnAction.text = "Thêm mới & Lưu nhật ký"
            btnAction.visibility = android.view.View.VISIBLE
        }

        btnCancel.setOnClickListener { bottomSheet.dismiss() }

        btnAddFood.setOnClickListener {
            bottomSheet.dismiss()
            addFoodToDatabaseOnly(name, calories, protein, carbs, fat)
        }

        btnAction.setOnClickListener {
            bottomSheet.dismiss()
            handleFoodAction(name, calories, protein, carbs, fat, existingFoodId)
        }

        bottomSheet.show()
    }

    private fun addFoodToDatabaseOnly(name: String, calories: Double, protein: Double, carbs: Double, fat: Double) {
        lifecycleScope.launch {
            try {
                // Ensure calories are > 0 for validation
                val validCalories = if (calories > 0) calories else 1.0

                val backendApi = com.example.wao_fe.network.NetworkClient.apiService
                Toast.makeText(this@MainActivity, "Đang thêm món ăn vào CSDL...", Toast.LENGTH_SHORT).show()
                val foodJson = JSONObject().apply {
                    put("name", name)
                    put("servingSize", "100g")
                    put("calories", validCalories)
                    put("protein", protein)
                    put("carbs", carbs)
                    put("fat", fat)
                }
                val requestBody = foodJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
                backendApi.createFood(food = requestBody, images = null)
                Toast.makeText(this@MainActivity, "Thêm món ăn thành công!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("BarcodeScan", "Lỗi thêm đồ ăn: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Gặp lỗi khi thêm món ăn!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleFoodAction(name: String, calories: Double, protein: Double, carbs: Double, fat: Double, existingFoodId: Long?) {
        lifecycleScope.launch {
            try {
                val backendApi = com.example.wao_fe.network.NetworkClient.apiService
                var foodIdToLog = existingFoodId

                if (foodIdToLog == null) {
                    Toast.makeText(this@MainActivity, "Đang xử lý...", Toast.LENGTH_SHORT).show()
                    // Ensure calories are > 0 for validation
                    val validCalories = if (calories > 0) calories else 1.0

                    val foodJson = JSONObject().apply {
                        put("name", name)
                        put("servingSize", "100g")
                        put("calories", validCalories)
                        put("protein", protein)
                        put("carbs", carbs)
                        put("fat", fat)
                    }
                    val requestBody = foodJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
                    val newFood = backendApi.createFood(food = requestBody, images = null)
                    foodIdToLog = newFood.id
                }

                if (userId != -1L) {
                    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val mealType = when (currentHour) {
                        in 5..10 -> MealType.BREAKFAST
                        in 11..15 -> MealType.LUNCH
                        in 16..21 -> MealType.DINNER
                        else -> MealType.SNACK
                    }
                    val logDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                    backendApi.createFoodLog(
                        userId = userId,
                        request = CreateFoodLogRequest(
                            foodId = foodIdToLog,
                            mealType = mealType,
                            servingQty = 1.0,
                            logDate = logDateStr
                        )
                    )
                    Toast.makeText(this@MainActivity, "Đã lưu vào nhật ký bữa ${mealType.name.lowercase()} thành công!", Toast.LENGTH_LONG).show()
                    fetchDashboardData()
                } else {
                    Toast.makeText(this@MainActivity, "Không tìm thấy user để lưu nhật ký", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("BarcodeScan", "Lỗi lưu đồ ăn: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Gặp lỗi khi xử lý món ăn!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val userName = sharedPref.getString("USER_NAME", "Ban")
        val currentAvatar = sharedPref.getString("USER_AVATAR", null)

        setupHeader(userName)

        if (currentAvatar != null) {
            Glide.with(this)
                .load(Uri.parse(currentAvatar))
                .into(ivAvatar)
            ivAvatar.setPadding(0, 0, 0, 0)
        }

        if (userId != -1L) {
            fetchDashboardData()
        }

        fetchWeightTrendData()

        checkHealthConnectAccess(promptIfMissing = false, initiatedByUser = false)
    }

    private fun setupHeader(userName: String?) {
        tvUserName.text = "Chào ${userName ?: "ban"},"
        val sdf = SimpleDateFormat("EEEE, dd MMM", Locale("vi", "VN"))
        tvDate.text = sdf.format(Date())
    }

    private fun fetchDashboardData() {
        lifecycleScope.launch {
            var targetCalories = 2000.0
            val profileResult = userRepository.getLatestHealthProfile(userId)
            if (profileResult is ApiResult.Success) {
                targetCalories = profileResult.data.targetCalories
            }
            targetCaloriesGoal = targetCalories

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

            val summaryResult = userRepository.getTodaySummary(userId)
            if (summaryResult is ApiResult.Success) {
                val summary = summaryResult.data
                // Keep backend state in sync, but render the summary card using the teammate's UI formula.
                backendCaloriesIn = summary.totalCalIn
                backendCaloriesOut = summary.totalCalOut
                val remaining = targetCalories - summary.netCalories

                tvCalIn.text = summary.totalCalIn.toInt().toString()
                tvCalOut.text = summary.totalCalOut.toInt().toString()

                val overCalories = summary.totalCalIn - targetCalories
                if (summary.totalCalIn > targetCalories) {
                    tvCalRemainingLabel.text = "Vượt quá"
                    tvCalRemaining.text = overCalories.toInt().toString()
                    pbCalories.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)

                    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    val lastAlertDate = sharedPref.getString("LAST_CALORIE_ALERT_DATE", "")

                    if (lastAlertDate != todayStr) {
                        // Gửi thông báo khi vượt quá calo
                        NotificationHelper.showAlertNotification(
                            context = this@MainActivity,
                            title = "Cảnh báo vượt mức năng lượng",
                            message = "Bạn đã vượt quá mục tiêu calo hôm nay ${overCalories.toInt()} calo (cal). Hãy cẩn thận!",
                            notificationId = 1001
                        )
                        sharedPref.edit().putString("LAST_CALORIE_ALERT_DATE", todayStr).apply()
                    }
                } else {
                    tvCalRemainingLabel.text = "Còn lại"
                    tvCalRemaining.text = remaining.toInt().coerceAtLeast(0).toString()
                    pbCalories.progressTintList = null
                }

                val progress = if (targetCalories > 0) {
                    ((summary.netCalories / targetCalories) * 100).toInt()
                } else 0
                pbCalories.progress = progress.coerceIn(0, 100)

                tvWater.text = "${summary.totalWater} ml"
                backendStepsToday = summary.totalSteps.toLong()
                renderStepsCard()
                currentWaterMl = summary.totalWater
                tvWater.text = "$currentWaterMl ml"
                tvSteps.text = "${summary.totalSteps}/10000"
            } else {
                // Reset to a clean empty state so the summary card never keeps stale values after a failed fetch.
                backendCaloriesIn = 0.0
                backendCaloriesOut = 0.0
                tvCalIn.text = "0"
                tvCalOut.text = "0"
                tvCalRemaining.text = targetCalories.toInt().toString()
                pbCalories.progress = 0
                backendStepsToday = 0L
                renderStepsCard()
                if (summaryResult is ApiResult.Error && summaryResult.status != 404) {
                    Toast.makeText(this@MainActivity, "Khong the tai du lieu hom nay", Toast.LENGTH_SHORT).show()
                }
            }

            fetchWeightTrendData()
        }
    }

    private fun fetchWeightTrendData() {
        if (userId == -1L) return

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val endDate = Date()
        val startDate = Calendar.getInstance().apply {
            time = endDate
            add(Calendar.DAY_OF_YEAR, -13)
        }.time

        lifecycleScope.launch {
            val latestResult = userRepository.getLatestWeightInfo(userId)
            val seriesResult = userRepository.getWeightSeries(
                userId = userId,
                from = dateFormat.format(startDate),
                to = dateFormat.format(endDate),
                groupBy = StatisticsGroupBy.DAY.name
            )

            val chartPoints = if (seriesResult is ApiResult.Success) {
                seriesResult.data.points
                    .mapNotNull { point ->
                        val value = point.endWeight ?: return@mapNotNull null
                        val dateKey = point.bucketDate.take(10)
                        dateKey to value
                    }
                    .sortedBy { it.first }
            } else {
                emptyList()
            }

            val labels = chartPoints.map { formatWeightChartDate(it.first) }
            chartWeightTrend.submitData(chartPoints.map { it.second.toFloat() }, labels)

            tvWeightStatus.text = when {
                latestResult is ApiResult.Success && latestResult.data.latestKnownWeight != null -> {
                    val dateText = latestResult.data.latestKnownDate?.let(::formatWeightChartDate) ?: "không rõ ngày"
                    "Gần nhất: ${formatWeightValue(latestResult.data.latestKnownWeight)} kg ($dateText)"
                }
                chartPoints.isNotEmpty() -> {
                    val latestPoint = chartPoints.last()
                    "Gần nhất: ${formatWeightValue(latestPoint.second)} kg (${formatWeightChartDate(latestPoint.first)})"
                }
                else -> "Chưa có dữ liệu cân nặng"
            }

            tvWeightChangeStatus.text = when {
                chartPoints.size >= 2 -> {
                    val latestWeight = chartPoints.last().second
                    val previousWeight = chartPoints[chartPoints.lastIndex - 1].second
                    val change = latestWeight - previousWeight
                    val prefix = if (change > 0) "+" else ""
                    "So với lần trước: $prefix${formatWeightValue(change)} kg"
                }
                chartPoints.size == 1 -> "So với lần trước: chưa đủ dữ liệu"
                else -> "So với lần trước: chưa có dữ liệu"
            }
        }
    }

    private fun addWaterLog(amount: Int) {
        if (userId == -1L) return
        lifecycleScope.launch {
            try {
                val api = com.example.wao_fe.network.NetworkClient.apiService
                val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
                val request = com.example.wao_fe.network.models.CreateWaterLogRequest(
                    amountMl = amount,
                    logTime = nowStr
                )
                api.createWaterLog(userId, request)
                // Refresh summary in background if necessary, but UI updated immediately
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to add water log", e)
            }
        }
    }

    private fun removeLastWaterLog() {
        if (userId == -1L) return
        lifecycleScope.launch {
            try {
                val api = com.example.wao_fe.network.NetworkClient.apiService
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val logs = api.getWaterLogs(userId, todayStr)
                if (logs.isNotEmpty()) {
                    // Try to find the most recent 200ml log, or just the last log
                    val lastLog = logs.maxByOrNull { it.id }
                    if (lastLog != null) {
                        api.deleteWaterLog(userId, lastLog.id)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to remove water log", e)
            }
        }
    }

    private fun formatWeightChartDate(raw: String): String {
        return runCatching {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw)
            SimpleDateFormat("dd/MM", Locale.getDefault()).format(parsed ?: Date())
        }.getOrDefault(raw)
    }

    private fun formatWeightValue(value: Double): String {
        return String.format(Locale.getDefault(), "%.1f", value)
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

    // Match the teammate's calories card display so Health Connect refreshes do not overwrite the merged UI.
    private fun renderCaloriesSummary() {
        val caloriesOut = backendCaloriesOut
        val netCalories = backendCaloriesIn - backendCaloriesOut
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

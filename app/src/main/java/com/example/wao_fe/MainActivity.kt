package com.example.wao_fe

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.OpenFoodFactsApi
import com.example.wao_fe.network.UserRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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
    private lateinit var fabAddFood: FloatingActionButton
    private lateinit var ivAvatar: ImageView
    private lateinit var bottomNavigationView: com.google.android.material.bottomnavigation.BottomNavigationView

    private val userRepository = UserRepository()
    private var userId: Long = -1

    private var addFoodBottomSheetDialog: com.google.android.material.bottomsheet.BottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        userId = sharedPref.getLong("USER_ID", -1)
        val userName = sharedPref.getString("USER_NAME", "Bạn")

        initViews()
        setupHeader(userName)

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
        fabAddFood = findViewById(R.id.fabAddFood)
        ivAvatar = findViewById(R.id.ivAvatar)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        ivAvatar.setOnClickListener {
            startActivity(android.content.Intent(this, EditProfileActivity::class.java))
        }

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

        findViewById<TextView>(R.id.btnLogFood).setOnClickListener {
            Toast.makeText(this, "Tính năng Thêm món ăn đang phát triển", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.btnLogWorkout).setOnClickListener {
            Toast.makeText(this, "Tính năng Ghi nhận tập luyện đang phát triển", Toast.LENGTH_SHORT).show()
        }

        val btnOpenStatistics = findViewById<TextView>(R.id.btnOpenStatistics)
        if (btnOpenStatistics != null) {
            btnOpenStatistics.setOnClickListener {
                startActivity(Intent(this, StatisticsDashboardActivity::class.java))
            }
        }

        fabAddFood.setOnClickListener { view ->
            if (addFoodBottomSheetDialog?.isShowing == true) {
                addFoodBottomSheetDialog?.dismiss()
                addFoodBottomSheetDialog = null
            } else {
                showAddFoodBottomSheet()
            }
        }
    }

    private fun showAddFoodBottomSheet() {
        if (addFoodBottomSheetDialog == null) {
            addFoodBottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        }
        val bottomSheetDialog = addFoodBottomSheetDialog!!
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_add, null)
        bottomSheetDialog.setContentView(view)

        // Make sure it clears the reference on dismiss
        bottomSheetDialog.setOnDismissListener {
            addFoodBottomSheetDialog = null
        }

        // Find buttons in bottom sheet
        view.findViewById<android.view.View>(R.id.btnSearchFood).setOnClickListener {
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "Tính năng Ghi lại bữa ăn", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<android.view.View>(R.id.btnScanBarcode).setOnClickListener {
            bottomSheetDialog.dismiss()
            startBarcodeScanner()
        }

        view.findViewById<android.view.View>(R.id.btnVoiceNote).setOnClickListener {
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "Tính năng Ghi bằng giọng nói đang phát triển", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<android.view.View>(R.id.btnLogWater).setOnClickListener {
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "Tính năng Uống nước đang phát triển", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<android.view.View>(R.id.btnLogActivity).setOnClickListener {
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "Tính năng Ghi lại hoạt động đang phát triển", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<android.view.View>(R.id.btnLogWeight).setOnClickListener {
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "Tính năng Cân nặng đang phát triển", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<android.view.View>(R.id.btnCreateRecipe).setOnClickListener {
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "Tính năng Tạo công thức đang phát triển", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<android.view.View>(R.id.btnCreateFood).setOnClickListener {
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "Tính năng Tạo thực phẩm đang phát triển", Toast.LENGTH_SHORT).show()
        }

        bottomSheetDialog.show()
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
                val remaining = targetCalories - summary.netCalories

                tvCalIn.text = summary.totalCalIn.toInt().toString()
                tvCalOut.text = summary.totalCalOut.toInt().toString()
                tvCalRemaining.text = remaining.toInt().coerceAtLeast(0).toString()

                val progress = if (targetCalories > 0) {
                    ((summary.netCalories / targetCalories) * 100).toInt()
                } else {
                    0
                }
                pbCalories.progress = progress.coerceIn(0, 100)

                tvWater.text = "${summary.totalWater} ml"
                tvSteps.text = "${summary.totalSteps}/10000"
            } else {
                // If no summary exist for today yet, just show 0
                tvCalRemaining.text = targetCalories.toInt().toString()
                if (summaryResult is ApiResult.Error && summaryResult.status != 404) {
                    // Ignore 404 since it just means no logs today yet
                    Toast.makeText(this@MainActivity, "Không thể tải dữ liệu hôm nay", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

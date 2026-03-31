package com.example.wao_fe

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.wao_fe.component.FloatingAddMenu
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
    private lateinit var fabChatbot: ImageView
    private lateinit var ivAvatar: ImageView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var btnUpdateWeight: android.widget.Button

    private var floatingMenuDialog: android.app.Dialog? = null
    private val userRepository = UserRepository()
    private var userId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        userId = sharedPref.getLong("USER_ID", -1)
        val userName = sharedPref.getString("USER_NAME", "Ban")

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
        fabChatbot = findViewById(R.id.fabChatbot)
        ivAvatar = findViewById(R.id.ivAvatar)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        btnUpdateWeight = findViewById(R.id.btnUpdateWeight)

        btnUpdateWeight.setOnClickListener {
            val bottomSheet = UpdateWeightBottomSheet()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }

        ivAvatar.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
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
                    val name = p.product_name ?: p.generic_name ?: "Khong ro ten"
                    val energy = p.nutriments?.energy_kcal_100g ?: p.nutriments?.energy_kcal ?: 0.0

                    Log.i("BarcodeScan", "Tìm thấy sản phẩm: $name - $energy kcal")

                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Thông tin sản phẩm")
                        .setMessage("Ten: $name\nNang luong: $energy kcal/100g")
                        .setPositiveButton("Dong", null)
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
                val remaining = targetCalories - summary.netCalories

                tvCalIn.text = summary.totalCalIn.toInt().toString()
                tvCalOut.text = summary.totalCalOut.toInt().toString()
                tvCalRemaining.text = remaining.toInt().coerceAtLeast(0).toString()

                val progress = if (targetCalories > 0) {
                    ((summary.netCalories / targetCalories) * 100).toInt()
                } else 0
                pbCalories.progress = progress.coerceIn(0, 100)

                tvWater.text = "${summary.totalWater} ml"
                tvSteps.text = "${summary.totalSteps}/10000"
            } else {
                tvCalRemaining.text = targetCalories.toInt().toString()
                if (summaryResult is ApiResult.Error && summaryResult.status != 404) {
                    Toast.makeText(this@MainActivity, "Khong the tai du lieu hom nay", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        floatingMenuDialog?.setOnDismissListener(null)
        runCatching { floatingMenuDialog?.dismiss() }
        floatingMenuDialog = null
        super.onDestroy()
    }
}

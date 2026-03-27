package com.example.wao_fe

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wao_fe.namstats.StatisticsDashboardActivity
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

    private val userRepository = UserRepository()
    private var userId: Long = -1

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

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_home
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_diary -> {
                    startActivity(Intent(this, FoodDiaryActivity::class.java))
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
            Toast.makeText(this, "Tính năng Thêm bữa ăn đang phát triển", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.btnLogWorkout).setOnClickListener {
            Toast.makeText(this, "Tính năng Ghi nhận tập luyện đang phát triển", Toast.LENGTH_SHORT).show()
        }
        //nam them
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_diary -> {
                    startActivity(Intent(this, StatisticsDashboardActivity::class.java))
                    true
                }
                else -> true
            }
        }

        fabAddFood.setOnClickListener { view ->
            val popupText = PopupMenu(this, view)
            popupText.menu.add("Quét mã vạch món ăn")
            popupText.menu.add("Thêm món ăn tự nhập")
            popupText.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Quét mã vạch món ăn" -> {
                        startBarcodeScanner()
                        true
                    }

                    "Thêm món ăn tự nhập" -> {
                        Toast.makeText(this, "Đang phát triển", Toast.LENGTH_SHORT).show()
                        true
                    }

                    else -> false
                }
            }
            popupText.show()
        }
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

    private fun setupHeader(userName: String?) {
        tvUserName.text = "Chào ${userName ?: "bạn"},"
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
                tvCalRemaining.text = targetCalories.toInt().toString()
                if (summaryResult is ApiResult.Error && summaryResult.status != 404) {
                    Toast.makeText(this@MainActivity, "Không thể tải dữ liệu hôm nay", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

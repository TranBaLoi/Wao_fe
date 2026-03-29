package com.example.wao_fe

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wao_fe.network.NetworkClient
import com.example.wao_fe.network.models.FoodLogResponse
import com.example.wao_fe.network.models.FoodResponse
import com.example.wao_fe.network.models.MealType
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

class FoodDiaryActivity : AppCompatActivity() {

    private val apiService = NetworkClient.apiService

    private lateinit var tvDiaryDate: TextView
    private lateinit var tvConsumedCalories: TextView
    private lateinit var tvTargetCalories: TextView
    private lateinit var pbCalories: ProgressBar

    private lateinit var tvProtein: TextView
    private lateinit var tvCarbs: TextView
    private lateinit var tvFat: TextView
    private lateinit var pbProtein: ProgressBar
    private lateinit var pbCarbs: ProgressBar
    private lateinit var pbFat: ProgressBar

    private lateinit var mealSections: Map<MealType, MealSectionViews>

    private var userId: Long = -1
    private var targetCalories: Double = 2000.0
    private var allFoods: List<FoodResponse> = emptyList()
    private var foodById: Map<Long, FoodResponse> = emptyMap()

    private val addFoodLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadDiaryData()
        }
    }

    private val searchFoodLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadDiaryData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_dairy)

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        userId = sharedPref.getLong("USER_ID", -1)

        initViews()
        bindMealSections()
        setupActions()
        setupDate()

        if (userId == -1L) {
            toast("Không tìm thấy thông tin đăng nhập")
            return
        }

        loadDiaryData()
    }

    private fun initViews() {
        tvDiaryDate = findViewById(R.id.tvDiaryDate)
        tvConsumedCalories = findViewById(R.id.tvConsumedCalories)
        tvTargetCalories = findViewById(R.id.tvTargetCalories)
        pbCalories = findViewById(R.id.pbCalories)

        tvProtein = findViewById(R.id.tvProtein)
        tvCarbs = findViewById(R.id.tvCarbs)
        tvFat = findViewById(R.id.tvFat)
        pbProtein = findViewById(R.id.pbProtein)
        pbCarbs = findViewById(R.id.pbCarbs)
        pbFat = findViewById(R.id.pbFat)
    }

    private fun bindMealSections() {
        mealSections = mapOf(
            MealType.BREAKFAST to MealSectionViews(
                calories = findViewById(R.id.tvBreakfastCalories),
                logContainer = findViewById(R.id.layoutBreakfastLogs),
                addButton = findViewById(R.id.btnAddBreakfast)
            ),
            MealType.LUNCH to MealSectionViews(
                calories = findViewById(R.id.tvLunchCalories),
                logContainer = findViewById(R.id.layoutLunchLogs),
                addButton = findViewById(R.id.btnAddLunch)
            ),
            MealType.DINNER to MealSectionViews(
                calories = findViewById(R.id.tvDinnerCalories),
                logContainer = findViewById(R.id.layoutDinnerLogs),
                addButton = findViewById(R.id.btnAddDinner)
            ),
            MealType.SNACK to MealSectionViews(
                calories = findViewById(R.id.tvSnackCalories),
                logContainer = findViewById(R.id.layoutSnackLogs),
                addButton = findViewById(R.id.btnAddSnack)
            )
        )
    }

    private fun setupActions() {
        mealSections.forEach { (mealType, section) ->
            section.addButton.setOnClickListener {
                openFoodPicker(mealType)
            }
        }

        findViewById<FloatingActionButton>(R.id.fabAddFood).setOnClickListener {
            addFoodLauncher.launch(Intent(this, AddFoodActivity::class.java))
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_diary
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.nav_diary -> true

                R.id.nav_menu -> {
                    startActivity(Intent(this, MealPlanActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.nav_profile -> {
                    startActivity(Intent(this, SettingsActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                else -> false
            }
        }
    }

    private fun setupDate() {
        val formatter = SimpleDateFormat("EEEE, dd/MM", Locale("vi", "VN"))
        tvDiaryDate.text = formatter.format(java.util.Date())
    }

    private fun loadDiaryData() {
        lifecycleScope.launch {
            fetchTargetCalories()
            fetchFoods()
            val logs = fetchLogsByDate(todayDate())
            val consumedCalories = fetchConsumedCalories(logs)

            updateTopCard(consumedCalories)
            renderMeals(logs)
            updateMacros(logs)
        }
    }

    private suspend fun fetchTargetCalories() {
        val result = runCatching { apiService.getLatestHealthProfile(userId) }
        targetCalories = result.getOrNull()?.targetCalories ?: 2000.0
    }

    private suspend fun fetchFoods() {
        val result = runCatching { apiService.getFoods() }
        allFoods = result.getOrElse { emptyList() }
        foodById = allFoods.associateBy { it.id }
    }

    private suspend fun fetchLogsByDate(date: String): List<FoodLogResponse> {
        return try {
            apiService.getFoodLogs(userId, date)
        } catch (ex: HttpException) {
            if (ex.code() == 404) emptyList() else {
                toast("Không tải được nhật ký ăn")
                emptyList()
            }
        } catch (_: Exception) {
            toast("Không tải được nhật ký ăn")
            emptyList()
        }
    }

    private suspend fun fetchConsumedCalories(logs: List<FoodLogResponse>): Double {
        return runCatching { apiService.getTodaySummary(userId).totalCalIn }
            .getOrElse { logs.sumOf { it.totalCalories } }
    }

    private fun updateTopCard(consumedCalories: Double) {
        tvConsumedCalories.text = "${consumedCalories.roundToInt()} kcal"
        tvTargetCalories.text = "${targetCalories.roundToInt()} kcal"

        val progress = if (targetCalories > 0) {
            ((consumedCalories / targetCalories) * 100).roundToInt().coerceIn(0, 100)
        } else {
            0
        }
        pbCalories.progress = progress
    }

    private fun renderMeals(logs: List<FoodLogResponse>) {
        val grouped = logs.groupBy { it.mealType }

        mealSections.forEach { (mealType, section) ->
            val mealLogs = grouped[mealType].orEmpty()
            val mealCalories = mealLogs.sumOf { it.totalCalories }.roundToInt()
            section.calories.text = "$mealCalories kcal"
            renderMealItems(section.logContainer, mealLogs)
        }
    }

    private fun renderMealItems(container: LinearLayout, logs: List<FoodLogResponse>) {
        container.removeAllViews()

        if (logs.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "Chưa có món ăn"
                textSize = 14f
                setTextColor(0xFF66736BL.toInt())
                setPadding(4, 8, 4, 0)
            }
            container.addView(emptyText)
            return
        }

        logs.forEach { log ->
            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_food_diary_log, container, false)

            val tvFoodName = itemView.findViewById<TextView>(R.id.tvFoodName)
            val tvFoodInfo = itemView.findViewById<TextView>(R.id.tvFoodInfo)
            val ivFood = itemView.findViewById<ImageView>(R.id.ivFood)
            val btnDelete = itemView.findViewById<ImageButton>(R.id.btnDelete)

            tvFoodName.text = log.foodName
            tvFoodInfo.text = "${formatServing(log.servingQty)} phần • ${log.totalCalories.roundToInt()} kcal"
            bindFoodImage(ivFood, foodById[log.foodId]?.imageUrls)

            btnDelete.setOnClickListener {
                confirmDelete(log)
            }

            itemView.setOnClickListener {
                openFoodDetail(log.foodId, log.foodName)
            }

            container.addView(itemView)
        }
    }

    private fun bindFoodImage(imageView: ImageView, imageUrls: List<String>?) {
        val imageUrl = imageUrls?.firstOrNull { it.isNotBlank() }
        if (imageUrl.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.ic_wao_leaf)
            imageView.imageTintList = ColorStateList.valueOf(getColor(R.color.green_dark))
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            val padding = (16 * resources.displayMetrics.density).toInt()
            imageView.setPadding(padding, padding, padding, padding)
            return
        }

        imageView.imageTintList = null
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setPadding(0, 0, 0, 0)
        Glide.with(this)
            .load(resolveImageUrl(imageUrl))
            .placeholder(R.drawable.ic_wao_leaf)
            .error(R.drawable.ic_wao_leaf)
            .into(imageView)
    }

    private fun resolveImageUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        val normalizedPath = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return "http://10.0.2.2:8080$normalizedPath"
    }

    private fun updateMacros(logs: List<FoodLogResponse>) {
        var protein = 0.0
        var carbs = 0.0
        var fat = 0.0

        logs.forEach { log ->
            val food = foodById[log.foodId]
            if (food != null) {
                protein += food.protein * log.servingQty
                carbs += food.carbs * log.servingQty
                fat += food.fat * log.servingQty
            }
        }

        tvProtein.text = "${protein.roundToInt()}g"
        tvCarbs.text = "${carbs.roundToInt()}g"
        tvFat.text = "${fat.roundToInt()}g"

        val proteinTarget = (targetCalories * 0.30 / 4.0).coerceAtLeast(1.0)
        val carbsTarget = (targetCalories * 0.40 / 4.0).coerceAtLeast(1.0)
        val fatTarget = (targetCalories * 0.30 / 9.0).coerceAtLeast(1.0)

        pbProtein.progress = ((protein / proteinTarget) * 100).roundToInt().coerceIn(0, 100)
        pbCarbs.progress = ((carbs / carbsTarget) * 100).roundToInt().coerceIn(0, 100)
        pbFat.progress = ((fat / fatTarget) * 100).roundToInt().coerceIn(0, 100)
    }

    private fun openFoodPicker(mealType: MealType) {
        val intent = Intent(this, FoodSearchActivity::class.java).apply {
            putExtra(FoodSearchActivity.EXTRA_USER_ID, userId)
            putExtra(FoodSearchActivity.EXTRA_MEAL_TYPE, mealType.name)
        }
        searchFoodLauncher.launch(intent)
    }

    private fun openFoodDetail(foodId: Long, foodName: String) {
        val intent = Intent(this, FoodDetailActivity::class.java).apply {
            putExtra(FoodDetailActivity.EXTRA_FOOD_ID, foodId)
            putExtra(FoodDetailActivity.EXTRA_FOOD_NAME, foodName)
        }
        startActivity(intent)
    }

    private fun confirmDelete(log: FoodLogResponse) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xóa món ăn")
            .setMessage("Bạn chắc chắn muốn xóa ${log.foodName}?")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ ->
                deleteFoodLog(log.id)
            }
            .show()
    }

    private fun deleteFoodLog(logId: Long) {
        lifecycleScope.launch {
            val result = runCatching { apiService.deleteFoodLog(userId, logId) }
            if (result.isSuccess) {
                toast("Đã xóa món ăn")
                loadDiaryData()
            } else {
                toast("Không thể xóa món ăn")
            }
        }
    }

    private fun todayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
    }

    private fun formatServing(serving: Double): String {
        return if (serving % 1.0 == 0.0) {
            serving.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", serving)
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private data class MealSectionViews(
        val calories: TextView,
        val logContainer: LinearLayout,
        val addButton: LinearLayout
    )
}

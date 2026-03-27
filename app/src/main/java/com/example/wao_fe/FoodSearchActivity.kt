package com.example.wao_fe

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.wao_fe.network.NetworkClient
import com.example.wao_fe.network.models.CreateFoodLogRequest
import com.example.wao_fe.network.models.FoodResponse
import com.example.wao_fe.network.models.MealType
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

class FoodSearchActivity : AppCompatActivity() {

    private val apiService = NetworkClient.apiService

    private lateinit var tvMealHint: TextView
    private lateinit var etSearchFood: EditText
    private lateinit var chipAll: TextView
    private lateinit var chipPopular: TextView
    private lateinit var chipHealthy: TextView
    private lateinit var layoutSearchResults: LinearLayout

    private var userId: Long = -1
    private var mealType: MealType = MealType.BREAKFAST
    private var allFoods: List<FoodResponse> = emptyList()
    private var currentFilter: FilterType = FilterType.ALL

    private val addFoodLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadFoods()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_search)

        userId = intent.getLongExtra(EXTRA_USER_ID, -1)
        mealType = runCatching {
            MealType.valueOf(intent.getStringExtra(EXTRA_MEAL_TYPE).orEmpty())
        }.getOrElse { MealType.BREAKFAST }

        if (userId == -1L) {
            val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            userId = sharedPref.getLong("USER_ID", -1)
        }

        initViews()
        setupActions()
        setupMealHint()

        if (userId == -1L) {
            toast("Không tìm thấy user để thêm món")
            finish()
            return
        }

        loadFoods()
    }

    private fun initViews() {
        tvMealHint = findViewById(R.id.tvMealHint)
        etSearchFood = findViewById(R.id.etSearchFood)
        chipAll = findViewById(R.id.chipAll)
        chipPopular = findViewById(R.id.chipPopular)
        chipHealthy = findViewById(R.id.chipHealthy)
        layoutSearchResults = findViewById(R.id.layoutSearchResults)
    }

    private fun setupActions() {
        findViewById<ImageButton>(R.id.btnBackDiary).setOnClickListener {
            finish()
        }

        findViewById<FloatingActionButton>(R.id.fabAddFood).setOnClickListener {
            addFoodLauncher.launch(Intent(this, AddFoodActivity::class.java))
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_menu
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_diary -> {
                    startActivity(Intent(this, FoodDiaryActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_menu -> true

                R.id.nav_profile -> {
                    toast("Tab Tài khoản")
                    false
                }

                else -> false
            }
        }

        etSearchFood.doAfterTextChanged {
            renderResults()
        }

        chipAll.setOnClickListener {
            currentFilter = FilterType.ALL
            updateChipState()
            renderResults()
        }

        chipPopular.setOnClickListener {
            currentFilter = FilterType.POPULAR
            updateChipState()
            renderResults()
        }

        chipHealthy.setOnClickListener {
            currentFilter = FilterType.HEALTHY
            updateChipState()
            renderResults()
        }
    }

    private fun setupMealHint() {
        tvMealHint.text = "Thêm cho ${mealLabel(mealType)}"
    }

    private fun loadFoods() {
        lifecycleScope.launch {
            val result = runCatching { apiService.getFoods() }
            allFoods = result.getOrElse { emptyList() }

            if (result.isFailure) {
                val error = result.exceptionOrNull()
                val msg = if (error is HttpException) {
                    "Không tải được món ăn (HTTP ${error.code()})"
                } else {
                    "Không tải được món ăn. Kiểm tra BE"
                }
                toast(msg)
            } else if (allFoods.isEmpty()) {
                toast("Không có món ăn trong hệ thống")
            }

            renderResults()
        }
    }

    private fun renderResults() {
        layoutSearchResults.removeAllViews()

        val query = etSearchFood.text.toString().trim().lowercase(Locale.getDefault())
        var foods = allFoods

        foods = foods.filter { food ->
            food.name.lowercase(Locale.getDefault()).contains(query)
        }

        foods = when (currentFilter) {
            FilterType.ALL -> foods
            FilterType.POPULAR -> foods.filter { it.isVerified }
            FilterType.HEALTHY -> foods.filter { it.calories <= 250.0 }
        }

        if (foods.isEmpty()) {
            val tvEmpty = TextView(this).apply {
                text = "Không tìm thấy món phù hợp"
                textSize = 16f
                setTextColor(0xFF5F6B64.toInt())
                setPadding(8, 20, 8, 8)
            }
            layoutSearchResults.addView(tvEmpty)
            return
        }

        foods.forEach { food ->
            val item = LayoutInflater.from(this)
                .inflate(R.layout.item_food_search_result, layoutSearchResults, false)

            val tvName = item.findViewById<TextView>(R.id.tvFoodName)
            val tvCalories = item.findViewById<TextView>(R.id.tvCalories)
            val tvServing = item.findViewById<TextView>(R.id.tvServing)
            val tvMeta = item.findViewById<TextView>(R.id.tvMeta)
            val btnAdd = item.findViewById<ImageButton>(R.id.btnAddFood)
            val ivFoodImage = item.findViewById<ImageView>(R.id.ivFoodImage)

            tvName.text = food.name
            tvCalories.text = "${food.calories.roundToInt()} kcal"
            tvServing.text = food.servingSize
            tvMeta.text = buildMeta(food)
            ivFoodImage.setImageResource(R.drawable.ic_wao_leaf)

            btnAdd.setOnClickListener {
                addFoodToMeal(food)
            }

            layoutSearchResults.addView(item)
        }
    }

    private fun addFoodToMeal(food: FoodResponse) {
        lifecycleScope.launch {
            val request = CreateFoodLogRequest(
                foodId = food.id,
                mealType = mealType,
                servingQty = 1.0,
                logDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
            )

            val result = runCatching { apiService.createFoodLog(userId, request) }
            if (result.isSuccess) {
                toast("Đã thêm ${food.name} vào ${mealLabel(mealType)}")
                setResult(RESULT_OK)
                finish()
            } else {
                toast("Không thể thêm món ăn")
            }
        }
    }

    private fun buildMeta(food: FoodResponse): String {
        return when {
            food.protein >= 20.0 -> "Protein cao"
            food.calories <= 180.0 -> "Nhẹ calories"
            else -> "Món ăn phổ biến"
        }
    }

    private fun updateChipState() {
        applyChipStyle(chipAll, currentFilter == FilterType.ALL)
        applyChipStyle(chipPopular, currentFilter == FilterType.POPULAR)
        applyChipStyle(chipHealthy, currentFilter == FilterType.HEALTHY)
    }

    private fun applyChipStyle(chip: TextView, selected: Boolean) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.fs_bg_chip_selected)
            chip.setTextColor(getColor(android.R.color.white))
            chip.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            chip.setBackgroundResource(R.drawable.fs_bg_chip_normal)
            chip.setTextColor(0xFF435046.toInt())
            chip.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    private fun mealLabel(type: MealType): String {
        return when (type) {
            MealType.BREAKFAST -> "bữa sáng"
            MealType.LUNCH -> "bữa trưa"
            MealType.DINNER -> "bữa tối"
            MealType.SNACK -> "ăn nhẹ"
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private enum class FilterType { ALL, POPULAR, HEALTHY }

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_MEAL_TYPE = "extra_meal_type"
    }
}

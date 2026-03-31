package com.example.wao_fe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.NetworkClient
import com.example.wao_fe.network.UserRepository
import com.example.wao_fe.network.models.FoodResponse
import com.example.wao_fe.network.models.MealPlanFoodResponse
import com.example.wao_fe.network.models.MealPlanResponse
import com.example.wao_fe.network.models.MealType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class MealPlanDetailActivity : AppCompatActivity() {

    private val userRepository = UserRepository()

    private lateinit var btnBack: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var contentContainer: LinearLayout
    private lateinit var emptyState: LinearLayout

    private lateinit var tvPlanName: TextView
    private lateinit var tvPlanDescription: TextView

    private lateinit var tvBreakfastLabel: TextView
    private lateinit var tvLunchLabel: TextView
    private lateinit var tvDinnerLabel: TextView
    private lateinit var tvSnackLabel: TextView

    private lateinit var listBreakfast: LinearLayout
    private lateinit var listLunch: LinearLayout
    private lateinit var listDinner: LinearLayout
    private lateinit var listSnack: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_plan_detail)

        val mealPlanId = intent.getLongExtra(SavedMealPlansActivity.EXTRA_MEAL_PLAN_ID, -1)
        if (mealPlanId == -1L) {
            Toast.makeText(this, "Meal Plan không hợp lệ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        btnBack.setOnClickListener { finish() }
        loadMealPlanDetail(mealPlanId)
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBackHeader)
        progressBar = findViewById(R.id.progressBar)
        contentContainer = findViewById(R.id.contentContainer)
        emptyState = findViewById(R.id.emptyState)

        tvPlanName = findViewById(R.id.tvPlanName)
        tvPlanDescription = findViewById(R.id.tvPlanDescription)

        tvBreakfastLabel = findViewById(R.id.tvBreakfastLabel)
        tvLunchLabel = findViewById(R.id.tvLunchLabel)
        tvDinnerLabel = findViewById(R.id.tvDinnerLabel)
        tvSnackLabel = findViewById(R.id.tvSnackLabel)

        listBreakfast = findViewById(R.id.listBreakfast)
        listLunch = findViewById(R.id.listLunch)
        listDinner = findViewById(R.id.listDinner)
        listSnack = findViewById(R.id.listSnack)
    }

    private fun loadMealPlanDetail(mealPlanId: Long) {
        progressBar.visibility = View.VISIBLE
        contentContainer.visibility = View.GONE
        emptyState.visibility = View.GONE

        lifecycleScope.launch {
            when (val result = userRepository.getMealPlanById(mealPlanId)) {
                is ApiResult.Success -> {
                    val foodDetails = fetchFoodDetails(result.data.foods.map { it.foodId }.distinct())
                    renderMealPlan(result.data, foodDetails)
                }

                is ApiResult.Error -> {
                    progressBar.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                    Toast.makeText(this@MealPlanDetailActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun fetchFoodDetails(foodIds: List<Long>): Map<Long, FoodResponse> {
        return runCatching {
            foodIds.map { id ->
                lifecycleScope.async {
                    runCatching { NetworkClient.apiService.getFoodById(id) }.getOrNull()
                }
            }.awaitAll().filterNotNull().associateBy { it.id }
        }.getOrDefault(emptyMap())
    }

    private fun renderMealPlan(mealPlan: MealPlanResponse, foodDetails: Map<Long, FoodResponse>) {
        progressBar.visibility = View.GONE
        contentContainer.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        tvPlanName.text = mealPlan.name
        tvPlanDescription.text = mealPlan.description ?: "Meal Plan cá nhân"

        listBreakfast.removeAllViews()
        listLunch.removeAllViews()
        listDinner.removeAllViews()
        listSnack.removeAllViews()

        renderSection(
            foods = mealPlan.foods.filter { it.mealType == MealType.BREAKFAST },
            sectionLabel = tvBreakfastLabel,
            sectionContainer = listBreakfast,
            foodDetails = foodDetails
        )
        renderSection(
            foods = mealPlan.foods.filter { it.mealType == MealType.LUNCH },
            sectionLabel = tvLunchLabel,
            sectionContainer = listLunch,
            foodDetails = foodDetails
        )
        renderSection(
            foods = mealPlan.foods.filter { it.mealType == MealType.DINNER },
            sectionLabel = tvDinnerLabel,
            sectionContainer = listDinner,
            foodDetails = foodDetails
        )
        renderSection(
            foods = mealPlan.foods.filter { it.mealType == MealType.SNACK },
            sectionLabel = tvSnackLabel,
            sectionContainer = listSnack,
            foodDetails = foodDetails
        )
    }

    private fun renderSection(
        foods: List<MealPlanFoodResponse>,
        sectionLabel: TextView,
        sectionContainer: LinearLayout,
        foodDetails: Map<Long, FoodResponse>
    ) {
        if (foods.isEmpty()) {
            sectionLabel.visibility = View.GONE
            sectionContainer.visibility = View.GONE
            return
        }

        sectionLabel.visibility = View.VISIBLE
        sectionContainer.visibility = View.VISIBLE

        foods.forEach { food ->
            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_meal_plan_detail_food, sectionContainer, false)

            val tvFoodName = itemView.findViewById<TextView>(R.id.tvFoodName)
            val tvServing = itemView.findViewById<TextView>(R.id.tvServing)
            val tvNutrition = itemView.findViewById<TextView>(R.id.tvNutrition)
            val tvIngredients = itemView.findViewById<TextView>(R.id.tvIngredients)
            val tvGuide = itemView.findViewById<TextView>(R.id.tvGuide)

            val detail = foodDetails[food.foodId]
            val calories = food.calories?.toInt() ?: detail?.calories?.toInt() ?: 0
            val protein = food.protein?.toInt() ?: detail?.protein?.toInt() ?: 0
            val carbs = food.carbs?.toInt() ?: detail?.carbs?.toInt() ?: 0
            val fat = food.fat?.toInt() ?: detail?.fat?.toInt() ?: 0

            tvFoodName.text = food.foodName ?: detail?.name ?: "Món ăn"
            tvServing.text = "Khẩu phần: ${food.servingQty} • ${detail?.servingSize ?: "N/A"}"
            tvNutrition.text = "Dinh dưỡng: ${calories} kcal | P ${protein}g | C ${carbs}g | F ${fat}g"
            tvIngredients.text = "Nguyên liệu: ${detail?.ingredients ?: "Chưa có dữ liệu"}"
            tvGuide.text = "Cách làm: Xem hướng dẫn chi tiết trong mục công thức."

            sectionContainer.addView(itemView)
        }
    }
}


package com.example.wao_fe

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.wao_fe.network.models.MealPlanFoodResponse
import com.example.wao_fe.network.models.MealType
import com.example.wao_fe.viewmodel.MealPlanState
import com.example.wao_fe.viewmodel.MealPlanViewModel
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MealPlanActivity : AppCompatActivity() {

    private lateinit var viewModel: MealPlanViewModel
    private var userId: Long = -1

    private lateinit var tvDate: TextView
    private lateinit var btnBackHeader: ImageView
    private lateinit var shimmerViewContainer: ShimmerFrameLayout
    private lateinit var contentContainer: LinearLayout
    private lateinit var emptyState: LinearLayout

    private lateinit var listBreakfast: LinearLayout
    private lateinit var listLunch: LinearLayout
    private lateinit var listDinner: LinearLayout

    private lateinit var tvBreakfastLabel: TextView
    private lateinit var tvLunchLabel: TextView
    private lateinit var tvDinnerLabel: TextView

    private lateinit var btnApply: Button
    private lateinit var bottomBar: LinearLayout
    private lateinit var fabGenerateAI: ExtendedFloatingActionButton

    private lateinit var lottieOverlay: View
    private lateinit var lottieAnimation: LottieAnimationView

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fabAddFood: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_plan)

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        userId = sharedPref.getLong("USER_ID", -1)

        viewModel = ViewModelProvider(this).get(MealPlanViewModel::class.java)

        initViews()
        setupListeners()
        observeViewModel()
    }

    private fun initViews() {
        tvDate = findViewById(R.id.tvDate)
        btnBackHeader = findViewById(R.id.btnBackHeader)
        shimmerViewContainer = findViewById(R.id.shimmerViewContainer)
        contentContainer = findViewById(R.id.contentContainer)
        emptyState = findViewById(R.id.emptyState)

        listBreakfast = findViewById(R.id.listBreakfast)
        listLunch = findViewById(R.id.listLunch)
        listDinner = findViewById(R.id.listDinner)

        tvBreakfastLabel = findViewById(R.id.tvBreakfastLabel)
        tvLunchLabel = findViewById(R.id.tvLunchLabel)
        tvDinnerLabel = findViewById(R.id.tvDinnerLabel)

        btnApply = findViewById(R.id.btnApply)
        bottomBar = findViewById(R.id.bottomBar)
        fabGenerateAI = findViewById(R.id.fabGenerateAI)

        lottieOverlay = findViewById(R.id.lottieOverlay)
        lottieAnimation = findViewById(R.id.lottieAnimation)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        fabAddFood = findViewById(R.id.fabAddFood)

        val sdf = SimpleDateFormat("EEEE, dd MMM", Locale("vi", "VN"))
        tvDate.text = sdf.format(Date())
    }

    private fun setupListeners() {
        btnBackHeader.setOnClickListener { finish() }

        fabGenerateAI.setOnClickListener {
            if (userId != -1L) {
                viewModel.generateMealPlan(userId)
            } else {
                Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show()
            }
        }

        btnApply.setOnClickListener {
            if (userId != -1L) {
                viewModel.applyMealPlan(userId)
            }
        }

        bottomNavigationView.selectedItemId = R.id.nav_menu
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_diary -> {
                    startActivity(Intent(this, FoodDiaryActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
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
                R.id.nav_menu -> true
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is MealPlanState.Idle -> {
                    shimmerViewContainer.visibility = View.GONE
                    shimmerViewContainer.stopShimmer()
                    contentContainer.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                    bottomBar.visibility = View.GONE
                    fabGenerateAI.visibility = View.VISIBLE
                }
                is MealPlanState.Loading -> {
                    emptyState.visibility = View.GONE
                    contentContainer.visibility = View.GONE
                    bottomBar.visibility = View.GONE
                    fabGenerateAI.visibility = View.GONE

                    shimmerViewContainer.visibility = View.VISIBLE
                    shimmerViewContainer.startShimmer()
                }
                is MealPlanState.Success -> {
                    shimmerViewContainer.stopShimmer()
                    shimmerViewContainer.visibility = View.GONE
                    emptyState.visibility = View.GONE
                    contentContainer.visibility = View.VISIBLE
                    bottomBar.visibility = View.VISIBLE
                    fabGenerateAI.visibility = View.VISIBLE
                    fabGenerateAI.text = "Gợi ý lại"

                    renderMealPlan(state.data.foods)
                }
                is MealPlanState.Error -> {
                    shimmerViewContainer.stopShimmer()
                    shimmerViewContainer.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                    fabGenerateAI.visibility = View.VISIBLE
                    Toast.makeText(this, "Lỗi: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is MealPlanState.Applying -> {
                    btnApply.isEnabled = false
                    btnApply.text = "Đang áp dụng..."
                }
                is MealPlanState.AppliedSuccess -> {
                    lottieOverlay.visibility = View.VISIBLE
                    lottieAnimation.playAnimation()

                    lottieAnimation.addAnimatorListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}
                        override fun onAnimationEnd(animation: Animator) {
                            Toast.makeText(this@MealPlanActivity, "Đã áp dụng thực đơn hôm nay!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        override fun onAnimationCancel(animation: Animator) {}
                        override fun onAnimationRepeat(animation: Animator) {}
                    })
                }
                is MealPlanState.AppliedError -> {
                    btnApply.isEnabled = true
                    btnApply.text = "✅ Áp dụng Thực đơn này"
                    Toast.makeText(this, "Lỗi áp dụng: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun renderMealPlan(foods: List<MealPlanFoodResponse>) {
        listBreakfast.removeAllViews()
        listLunch.removeAllViews()
        listDinner.removeAllViews()

        val breakfastFoods = foods.filter { it.mealType == MealType.BREAKFAST }
        val lunchFoods = foods.filter { it.mealType == MealType.LUNCH }
        val dinnerFoods = foods.filter { it.mealType == MealType.DINNER }

        // Cập nhật hiển thị label và danh sách
        toggleSection(breakfastFoods, tvBreakfastLabel, listBreakfast)
        toggleSection(lunchFoods, tvLunchLabel, listLunch)
        toggleSection(dinnerFoods, tvDinnerLabel, listDinner)
    }

    private fun toggleSection(foods: List<MealPlanFoodResponse>, label: TextView, container: LinearLayout) {
        if (foods.isEmpty()) {
            label.visibility = View.GONE
            container.visibility = View.GONE
        } else {
            label.visibility = View.VISIBLE
            container.visibility = View.VISIBLE
            for (food in foods) {
                container.addView(createFoodView(food))
            }
        }
    }

    private fun createFoodView(food: MealPlanFoodResponse): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_meal_food, null, false)

        val tvFoodName = view.findViewById<TextView>(R.id.tvFoodName)
        val tvServing = view.findViewById<TextView>(R.id.tvServing)
        val tvCalories = view.findViewById<TextView>(R.id.tvCalories)
        val tvMacros = view.findViewById<TextView>(R.id.tvMacros)
        val ivAllergenWarning = view.findViewById<ImageView>(R.id.ivAllergenWarning)

        tvFoodName.text = food.foodName ?: "Món ăn"
        tvServing.text = "${food.servingQty} phần"

        food.calories?.let {
            tvCalories.text = "${it.toInt()} kcal"
        }

        val p = food.protein?.toInt() ?: 0
        val c = food.carbs?.toInt() ?: 0
        val f = food.fat?.toInt() ?: 0
        tvMacros.text = "P: ${p}g • C: ${c}g • F: ${f}g"

        if (!food.containsAllergens.isNullOrEmpty()) {
            ivAllergenWarning.visibility = View.VISIBLE
        }

        return view
    }
}


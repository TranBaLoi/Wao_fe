package com.example.wao_fe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wao_fe.component.MealPlanItemMode
import com.example.wao_fe.component.MealPlanItemRenderer
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.UserRepository
import com.example.wao_fe.network.models.MealPlanResponse
import kotlinx.coroutines.launch

class SavedMealPlansActivity : AppCompatActivity() {

    private val userRepository = UserRepository()

    private lateinit var btnBack: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout
    private lateinit var listContainer: LinearLayout

    private var userId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_meal_plans)

        userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getLong("USER_ID", -1)

        btnBack = findViewById(R.id.btnBackHeader)
        progressBar = findViewById(R.id.progressBar)
        emptyState = findViewById(R.id.emptyState)
        listContainer = findViewById(R.id.listContainer)

        btnBack.setOnClickListener { finish() }

        if (userId == -1L) {
            Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadSavedMealPlans()
    }

    private fun loadSavedMealPlans() {
        progressBar.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
        listContainer.visibility = View.GONE

        lifecycleScope.launch {
            when (val result = userRepository.getUserMealPlans(userId)) {
                is ApiResult.Success -> renderMealPlans(result.data)
                is ApiResult.Error -> {
                    progressBar.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                    Toast.makeText(this@SavedMealPlansActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun renderMealPlans(mealPlans: List<MealPlanResponse>) {
        progressBar.visibility = View.GONE

        if (mealPlans.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            listContainer.visibility = View.GONE
            return
        }

        emptyState.visibility = View.GONE
        listContainer.visibility = View.VISIBLE

        MealPlanItemRenderer.render(
            container = listContainer,
            mealPlans = mealPlans,
            mode = MealPlanItemMode.VIEW_ONLY,
            onViewDetail = { mealPlan ->
                startActivity(
                    Intent(this, MealPlanDetailActivity::class.java)
                        .putExtra(EXTRA_MEAL_PLAN_ID, mealPlan.id)
                )
            }
        )
    }

    companion object {
        const val EXTRA_MEAL_PLAN_ID = "extra_meal_plan_id"
    }
}



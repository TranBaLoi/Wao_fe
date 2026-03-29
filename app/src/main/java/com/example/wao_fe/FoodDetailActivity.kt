package com.example.wao_fe

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.wao_fe.network.NetworkClient
import com.example.wao_fe.network.models.FoodResponse
import com.example.wao_fe.network.models.MealType
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class FoodDetailActivity : AppCompatActivity() {

    private val apiService = NetworkClient.apiService

    private lateinit var tvHeroName: TextView
    private lateinit var ivHeroFoodImage: ImageView
    private lateinit var tvEnergy: TextView
    private lateinit var tvProtein: TextView
    private lateinit var tvCarbs: TextView
    private lateinit var tvFat: TextView
    private lateinit var tvServing: TextView

    private var foodId: Long = -1L
    private var fallbackName: String = "Mon an"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_food_detail)

        foodId = intent.getLongExtra(EXTRA_FOOD_ID, -1L)
        fallbackName = intent.getStringExtra(EXTRA_FOOD_NAME).orEmpty().ifBlank { "Mon an" }

        initViews()
        setupActions()
        bindFallbackUi()

        if (foodId != -1L) {
            loadFoodDetail()
        }
    }

    private fun initViews() {
        tvHeroName = findViewById(R.id.tvHeroFoodName)
        ivHeroFoodImage = findViewById(R.id.ivHeroFoodImage)
        tvEnergy = findViewById(R.id.tvTotalEnergyValue)
        tvProtein = findViewById(R.id.tvProteinValue)
        tvCarbs = findViewById(R.id.tvCarbsValue)
        tvFat = findViewById(R.id.tvFatValue)
        tvServing = findViewById(R.id.tvServingValue)
    }

    private fun setupActions() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            navigateBackToDiary()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackToDiary()
            }
        })

        findViewById<FloatingActionButton>(R.id.fabAddFood).setOnClickListener {
            startActivity(Intent(this, AddFoodActivity::class.java))
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_diary
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_diary -> {
                    navigateBackToDiary()
                    true
                }

                R.id.nav_menu -> {
                    val intent = Intent(this, FoodSearchActivity::class.java).apply {
                        val userId = currentUserId()
                        if (userId != -1L) {
                            putExtra(FoodSearchActivity.EXTRA_USER_ID, userId)
                        }
                        putExtra(FoodSearchActivity.EXTRA_MEAL_TYPE, MealType.BREAKFAST.name)
                    }
                    startActivity(intent)
                    finish()
                    true
                }

                R.id.nav_profile -> {
                    toast("Tab Tai khoan")
                    false
                }

                else -> false
            }
        }
    }

    private fun bindFallbackUi() {
        tvHeroName.text = fallbackName
        bindFoodImage(null)
        tvEnergy.text = "0"
        tvProtein.text = "0g"
        tvCarbs.text = "0g"
        tvFat.text = "0g"
        tvServing.text = "Chua cap nhat khau phan"
    }

    private fun loadFoodDetail() {
        lifecycleScope.launch {
            val result = runCatching { apiService.getFoodById(foodId) }
            if (result.isSuccess) {
                bindFood(result.getOrThrow())
            } else {
                toast("Khong tai duoc chi tiet mon an")
            }
        }
    }

    private fun bindFood(food: FoodResponse) {
        tvHeroName.text = food.name.ifBlank { fallbackName }
        bindFoodImage(food.imageUrls)
        tvEnergy.text = food.calories.roundToInt().toString()
        tvProtein.text = "${food.protein.roundToInt()}g"
        tvCarbs.text = "${food.carbs.roundToInt()}g"
        tvFat.text = "${food.fat.roundToInt()}g"
        tvServing.text = food.servingSize.ifBlank { "Chua cap nhat khau phan" }
    }

    private fun bindFoodImage(imageUrls: List<String>?) {
        val imageUrl = imageUrls?.firstOrNull { it.isNotBlank() }
        if (imageUrl.isNullOrBlank()) {
            ivHeroFoodImage.setImageResource(R.drawable.ic_wao_leaf)
            ivHeroFoodImage.imageTintList = ColorStateList.valueOf(getColor(R.color.green_dark))
            ivHeroFoodImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
            return
        }

        ivHeroFoodImage.imageTintList = null
        ivHeroFoodImage.scaleType = ImageView.ScaleType.CENTER_CROP
        Glide.with(this)
            .load(resolveImageUrl(imageUrl))
            .placeholder(R.drawable.ic_wao_leaf)
            .error(R.drawable.ic_wao_leaf)
            .into(ivHeroFoodImage)
    }

    private fun resolveImageUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        val normalizedPath = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return "http://10.0.2.2:8080$normalizedPath"
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun currentUserId(): Long {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        return sharedPref.getLong("USER_ID", -1)
    }

    private fun navigateBackToDiary() {
        val intent = Intent(this, FoodDiaryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_FOOD_ID = "extra_food_id"
        const val EXTRA_FOOD_NAME = "extra_food_name"
    }
}

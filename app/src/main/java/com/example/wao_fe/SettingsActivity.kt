package com.example.wao_fe

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.UserRepository
import com.example.wao_fe.network.models.GoalType
import com.example.wao_fe.viewmodel.SettingsViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.wao_fe.network.NetworkClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.abs

class SettingsActivity : AppCompatActivity() {

    private lateinit var viewModel: SettingsViewModel
    private var userId: Long = -1

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var ivAvatar: ImageView

    private lateinit var cardWeightGoal: com.google.android.material.card.MaterialCardView
    private lateinit var tvGoalType: TextView
    private lateinit var tvCurrentWeightGoal: TextView
    private lateinit var tvTargetWeight: TextView
    private lateinit var tvWeeklyGoal: TextView
    private lateinit var tvActivityLevel: TextView
    private lateinit var tvTargetCalories: TextView
    private lateinit var tvCompletionDate: TextView

    private lateinit var btnLogout: Button

    private lateinit var bottomNavigationView: BottomNavigationView

    private val userRepository = UserRepository()
    private val apiService = NetworkClient.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        userId = sharedPref.getLong("USER_ID", -1)

        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        initViews()
        setupListeners()
        observeViewModel()

        if (userId != -1L) {
            viewModel.loadData(userId)
        } else {
            Toast.makeText(this, "Không tìm thấy user id", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initViews() {
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        ivAvatar = findViewById(R.id.ivAvatar)

        cardWeightGoal = findViewById(R.id.cardWeightGoal)
        tvGoalType = findViewById(R.id.tvGoalType)
        tvCurrentWeightGoal = findViewById(R.id.tvCurrentWeightGoal)
        tvTargetWeight = findViewById(R.id.tvTargetWeight)
        tvWeeklyGoal = findViewById(R.id.tvWeeklyGoal)
        tvActivityLevel = findViewById(R.id.tvActivityLevel)
        tvTargetCalories = findViewById(R.id.tvTargetCalories)
        tvCompletionDate = findViewById(R.id.tvCompletionDate)

        btnLogout = findViewById(R.id.btnLogout)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_profile
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.btnSettingsIcon).setOnClickListener {
            Toast.makeText(this, "Tính năng cài đặt đang phát triển", Toast.LENGTH_SHORT).show()
        }

        ivAvatar.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

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

                R.id.nav_menu -> {
                    startActivity(Intent(this, MealPlanActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.nav_profile -> {
                    true
                }

                else -> false
            }
        }

        findViewById<LinearLayout>(R.id.btnStatNutrition).setOnClickListener {
            startActivity(Intent(this, com.example.wao_fe.namstats.StatisticsDashboardActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnStatSteps).setOnClickListener {
            startActivity(Intent(this, com.example.wao_fe.namstats.StatisticsDashboardActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnStatWeight).setOnClickListener {
            startActivity(Intent(this, com.example.wao_fe.namstats.StatisticsDashboardActivity::class.java))
        }

        btnLogout.setOnClickListener {
            performLogout()
        }
    }

    override fun onResume() {
        super.onResume()
        if (userId != -1L) {
            viewModel.loadData(userId)
        }

        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val currentAvatar = sharedPref.getString("USER_AVATAR", null)
        val currentName = sharedPref.getString("USER_NAME", "")

        if (currentName?.isNotEmpty() == true) {
            tvUserName.text = currentName
        }

        if (currentAvatar != null) {
            Glide.with(this)
                .load(Uri.parse(currentAvatar))
                .apply(RequestOptions.circleCropTransform())
                .into(ivAvatar)
            ivAvatar.setPadding(0, 0, 0, 0)
        }
    }

    private fun observeViewModel() {
        viewModel.user.observe(this) { user ->
            tvUserName.text = user?.fullName ?: "Người dùng"
            tvUserEmail.text = user?.email ?: ""
        }

        viewModel.healthProfile.observe(this) { profile ->
            if (profile != null) {
                cardWeightGoal.visibility = android.view.View.VISIBLE

                // Goal type
                tvGoalType.text = when (profile.goalType) {
                    GoalType.LOSE_WEIGHT -> "Giảm cân"
                    GoalType.GAIN_WEIGHT -> "Tăng cân"
                    GoalType.MAINTAIN -> "Giữ cân"
                }

                // Current & Target weight
                tvCurrentWeightGoal.text = "${profile.weightKg.toInt()} kg"
                tvTargetWeight.text = "${profile.desiredWeightKg.toInt()} kg"

                // Weekly goal
                val diff = profile.desiredWeightKg - profile.weightKg
                if (profile.targetDays > 0) {
                    val weeklyRaw = (diff / profile.targetDays) * 7
                    val weeklyAbs = String.format(Locale.US, "%.1f", abs(weeklyRaw))
                    tvWeeklyGoal.text = if (weeklyRaw < 0) {
                        "Giảm $weeklyAbs kg/tuần"
                    } else if (weeklyRaw > 0) {
                        "Tăng $weeklyAbs kg/tuần"
                    } else {
                        "Duy trì cân nặng"
                    }
                } else {
                    tvWeeklyGoal.text = "Không xác định"
                }

                // Activity level
                tvActivityLevel.text = when (profile.activityLevel) {
                    com.example.wao_fe.network.models.ActivityLevel.SEDENTARY -> "Không tập luyện"
                    com.example.wao_fe.network.models.ActivityLevel.LIGHTLY_ACTIVE -> "Vận động nhẹ"
                    com.example.wao_fe.network.models.ActivityLevel.MODERATELY_ACTIVE -> "Vận động vừa"
                    com.example.wao_fe.network.models.ActivityLevel.VERY_ACTIVE -> "Vận động nhiều"
                    com.example.wao_fe.network.models.ActivityLevel.EXTRA_ACTIVE -> "Vận động cường độ cao"
                }

                // Target calories
                tvTargetCalories.text = "${profile.targetCalories.toInt()} calo"

                updateCompletionDate()
            } else {
                cardWeightGoal.visibility = android.view.View.GONE
            }
        }

        viewModel.oldestHealthProfile.observe(this) { oldestProfile ->
            updateCompletionDate()
        }

        viewModel.error.observe(this) { msg ->
            if (msg.isNotEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCompletionDate() {
        val profile = viewModel.healthProfile.value
        val oldestProfile = viewModel.oldestHealthProfile.value

        if (profile != null && profile.targetDays > 0) {
            var baseDate = LocalDate.now()
            if (oldestProfile?.createdAt != null) {
                try {
                    baseDate = LocalDate.parse(oldestProfile.createdAt.substring(0, 10))
                } catch (e: DateTimeParseException) {
                    // ignore and use fallback
                }
            } else if (oldestProfile == null && profile.createdAt != null) {
                // If oldest profile isn't loaded yet but current profile has a date
                try {
                    baseDate = LocalDate.parse(profile.createdAt.substring(0, 10))
                } catch (e: DateTimeParseException) {
                    // ignore
                }
            }

            val completionDate = baseDate.plusDays(profile.targetDays.toLong())
            val formatter = DateTimeFormatter.ofPattern("dd 'thg' MM, yyyy", Locale("vi", "VN"))
            tvCompletionDate.text = completionDate.format(formatter)
        } else {
            tvCompletionDate.text = "Không xác định"
        }
    }

    private fun performLogout() {
        // Clear prefs
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        // Navigate to Login and clear backstack
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

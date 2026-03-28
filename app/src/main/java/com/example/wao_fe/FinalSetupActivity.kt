package com.example.wao_fe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import android.animation.Animator
import android.view.View
import com.airbnb.lottie.LottieAnimationView
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.UserRepository
import com.example.wao_fe.network.models.ActivityLevel
import com.example.wao_fe.network.models.CreateHealthProfileRequest
import com.example.wao_fe.network.models.Gender
import com.example.wao_fe.network.models.GoalType
import com.example.wao_fe.network.models.HealthProfileResponse
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class FinalSetupActivity : AppCompatActivity() {

    private lateinit var cardSedentary: LinearLayout
    private lateinit var cardActive: LinearLayout
    private lateinit var cardVeryActive: LinearLayout
    private lateinit var radioSedentary: ImageView
    private lateinit var radioActive: ImageView
    private lateinit var radioVeryActive: ImageView

    private lateinit var tvDuration: TextView
    private lateinit var tvCaloriesStatus: TextView
    private lateinit var tvTargetCalories: TextView
    private lateinit var tvDailyCalories: TextView
    private lateinit var tvDifficultyLevel: TextView
    private lateinit var tvDifficultyNote: TextView
    private lateinit var btnContinue: Button

    private lateinit var loadingOverlay: View
    private lateinit var lottieAnimation: LottieAnimationView

    private var userId: Long = -1
    private var genderId: Int = 2
    private var age: Int = 24
    private var height: Double = 172.0
    private var weight: Double = 68.5
    private var desiredWeight: Double = 68.5
    private var goalId: Int = 1
    private var allergies: String? = null

    // 0: Sedentary, 1: Active, 2: Very Active
    private var selectedActivityIndex = 1
    private var selectedDurationWeeks = 12

    private var latestProfile: HealthProfileResponse? = null
    private var canNavigateMain = false

    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_setup)

        userId = intent.getLongExtra("USER_ID", -1)
        genderId = intent.getIntExtra("GENDER_ID", 2)
        age = intent.getIntExtra("AGE", 24)
        height = intent.getDoubleExtra("HEIGHT", 172.0)
        weight = intent.getDoubleExtra("WEIGHT", 68.5)
        desiredWeight = intent.getDoubleExtra("DESIRED_WEIGHT", weight)
        goalId = intent.getIntExtra("GOAL_ID", 1)
        allergies = intent.getStringExtra("ALLERGIES")

        if (userId == -1L) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        updateActivitySelection()
        updateButtonState()
    }

    private fun initViews() {
        cardSedentary = findViewById(R.id.cardSedentary)
        cardActive = findViewById(R.id.cardActive)
        cardVeryActive = findViewById(R.id.cardVeryActive)

        radioSedentary = findViewById(R.id.radioSedentary)
        radioActive = findViewById(R.id.radioActive)
        radioVeryActive = findViewById(R.id.radioVeryActive)

        tvDuration = findViewById(R.id.tvDuration)
        tvCaloriesStatus = findViewById(R.id.tvCaloriesStatus)
        tvTargetCalories = findViewById(R.id.tvTargetCalories)
        tvDailyCalories = findViewById(R.id.tvDailyCalories)
        tvDifficultyLevel = findViewById(R.id.tvDifficultyLevel)
        tvDifficultyNote = findViewById(R.id.tvDifficultyNote)
        btnContinue = findViewById(R.id.btnContinue)

        loadingOverlay = findViewById(R.id.loadingOverlay)
        lottieAnimation = findViewById(R.id.lottieAnimation)
    }

    private fun setupListeners() {
        findViewById<TextView>(R.id.tvBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnBackHeader).setOnClickListener { finish() }

        cardSedentary.setOnClickListener {
            selectedActivityIndex = 0
            updateActivitySelection()
            markInputChanged()
        }

        cardActive.setOnClickListener {
            selectedActivityIndex = 1
            updateActivitySelection()
            markInputChanged()
        }

        cardVeryActive.setOnClickListener {
            selectedActivityIndex = 2
            updateActivitySelection()
            markInputChanged()
        }

        (tvDuration.parent as android.view.View).setOnClickListener {
            showDurationPickerDialog(tvDuration)
        }

        btnContinue.setOnClickListener {
            if (canNavigateMain) {
                navigateToMain()
            } else {
                createHealthProfile()
            }
        }
    }

    private fun showDurationPickerDialog(tvDisplay: TextView) {
        val numberPicker = android.widget.NumberPicker(this)
        numberPicker.minValue = 4
        numberPicker.maxValue = 52
        numberPicker.value = selectedDurationWeeks

        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.gravity = android.view.Gravity.CENTER
        numberPicker.layoutParams = params
        container.addView(numberPicker)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chọn thời gian (Tuần)")
            .setView(container)
            .setPositiveButton("OK") { _, _ ->
                selectedDurationWeeks = numberPicker.value
                tvDisplay.text = selectedDurationWeeks.toString()
                markInputChanged()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun updateActivitySelection() {
        resetCard(cardSedentary, radioSedentary)
        resetCard(cardActive, radioActive)
        resetCard(cardVeryActive, radioVeryActive)

        when (selectedActivityIndex) {
            0 -> highlightCard(cardSedentary, radioSedentary)
            1 -> highlightCard(cardActive, radioActive)
            2 -> highlightCard(cardVeryActive, radioVeryActive)
        }
    }

    private fun resetCard(card: LinearLayout, radio: ImageView) {
        card.background = ContextCompat.getDrawable(this, R.drawable.bg_goal_unselected)
        radio.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_circle_outline))
        radio.clearColorFilter()
    }

    private fun highlightCard(card: LinearLayout, radio: ImageView) {
        card.background = ContextCompat.getDrawable(this, R.drawable.bg_goal_selected)
        radio.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_check_circle_filled))
        radio.setColorFilter(ContextCompat.getColor(this, R.color.green_dark))
    }

    private fun markInputChanged() {
        canNavigateMain = false
        latestProfile = null
        tvCaloriesStatus.text = "Nhấn Tiếp tục để tính target calories theo dữ liệu mới."
        tvTargetCalories.text = "-- kcal"
        tvDailyCalories.text = "-- kcal/ngày"
        tvDifficultyLevel.text = "--"
        tvDifficultyNote.text = "--"
        updateButtonState()
    }

    private fun updateButtonState() {
        btnContinue.isEnabled = true
        btnContinue.text = if (canNavigateMain) "Vào ứng dụng" else "Tính target calo"
    }

    private fun navigateToMain() {
        btnContinue.isEnabled = false
        loadingOverlay.visibility = View.VISIBLE
        lottieAnimation.playAnimation()

        lottieAnimation.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                goDirectlyToMain()
            }
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        loadingOverlay.postDelayed({
            goDirectlyToMain()
        }, 2500)
    }

    private fun goDirectlyToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun createHealthProfile() {
        btnContinue.isEnabled = false
        btnContinue.text = "Đang tính..."

        val genderEnum = when (genderId) {
            1 -> Gender.MALE
            2 -> Gender.FEMALE
            else -> Gender.OTHER
        }

        val goalEnum = when (goalId) {
            0 -> GoalType.LOSE_WEIGHT
            1 -> GoalType.MAINTAIN
            2 -> GoalType.GAIN_WEIGHT
            else -> GoalType.MAINTAIN
        }

        val activityEnum = when (selectedActivityIndex) {
            0 -> ActivityLevel.SEDENTARY
            1 -> ActivityLevel.MODERATELY_ACTIVE
            2 -> ActivityLevel.VERY_ACTIVE
            else -> ActivityLevel.MODERATELY_ACTIVE
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val birthYear = currentYear - age
        val dob = "$birthYear-01-01"

        val request = CreateHealthProfileRequest(
            gender = genderEnum,
            dob = dob,
            heightCm = height,
            weightKg = weight,
            activityLevel = activityEnum,
            goalType = goalEnum,
            desiredWeightKg = desiredWeight,
            targetDays = selectedDurationWeeks * 7,
            allergies = allergies
        )

        lifecycleScope.launch {
            when (val result = userRepository.createHealthProfile(userId, request)) {
                is ApiResult.Success -> {
                    latestProfile = result.data
                    canNavigateMain = true
                    showCalories(result.data)
                    updateButtonState()
                    Toast.makeText(this@FinalSetupActivity, "Đã tính toán xong và lưu hồ sơ tạm thời", Toast.LENGTH_SHORT).show()
                }
                is ApiResult.Error -> {
                    btnContinue.isEnabled = true
                    btnContinue.text = "Tính target calo"
                    Toast.makeText(this@FinalSetupActivity, "Lỗi: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showCalories(profile: HealthProfileResponse) {
        tvCaloriesStatus.text = "Mục tiêu ${selectedDurationWeeks} tuần (${profile.targetDays} ngày)"
        tvTargetCalories.text = "${formatCalories(profile.targetCalories)} kcal"
        tvDailyCalories.text = "${formatCalories(profile.dailyCalories)} kcal/ngày"

        tvDifficultyLevel.text = profile.dailyCalorieBreakdown.difficultyLevel
        val levelColor = when (profile.dailyCalorieBreakdown.difficultyLevel.uppercase(Locale.ROOT)) {
            "EASY" -> ContextCompat.getColor(this, R.color.green_dark)
            "HARD" -> ContextCompat.getColor(this, android.R.color.holo_red_dark)
            else -> ContextCompat.getColor(this, R.color.wao_slate_900)
        }
        tvDifficultyLevel.setTextColor(levelColor)

        tvDifficultyNote.text = profile.dailyCalorieBreakdown.note
    }

    private fun formatCalories(value: Double): String {
        return String.format(Locale.US, "%.0f", value)
    }
}

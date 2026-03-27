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
import java.util.Locale
import kotlin.math.abs

class GoalSelectionActivity : AppCompatActivity() {

    private lateinit var cardLoseWeight: LinearLayout
    private lateinit var cardMaintain: LinearLayout
    private lateinit var cardGainMuscle: LinearLayout
    private lateinit var radioLoseWeight: ImageView
    private lateinit var radioMaintain: ImageView
    private lateinit var radioGainMuscle: ImageView
    private lateinit var btnContinue: Button

    private lateinit var tvCurrentWeightNote: TextView
    private lateinit var tvDesiredWeightVal: TextView
    private lateinit var tvGoalValidationHint: TextView
    private lateinit var btnDecreaseDesiredWeight: ImageView
    private lateinit var btnIncreaseDesiredWeight: ImageView

    private var userId: Long = -1
    private var genderId: Int = 2
    private var age: Int = 24
    private var height: Double = 172.0
    private var weight: Double = 68.5
    private var desiredWeight: Double = 68.5

    // 0: Lose Weight, 1: Maintain, 2: Gain Muscle
    private var selectedGoalIndex = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_selection)

        // Retrieve data from Step 2
        userId = intent.getLongExtra("USER_ID", -1)
        genderId = intent.getIntExtra("GENDER_ID", 2)
        age = intent.getIntExtra("AGE", 24)
        height = intent.getDoubleExtra("HEIGHT", 172.0)
        weight = intent.getDoubleExtra("WEIGHT", 68.5)
        desiredWeight = weight

        if (userId == -1L) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        updateGoalSelection()
        updateDesiredWeightViews()
        updateGoalHint(isError = false)
    }

    private fun initViews() {
        cardLoseWeight = findViewById(R.id.cardLoseWeight)
        cardMaintain = findViewById(R.id.cardMaintain)
        cardGainMuscle = findViewById(R.id.cardGainMuscle)

        radioLoseWeight = findViewById(R.id.radioLoseWeight)
        radioMaintain = findViewById(R.id.radioMaintain)
        radioGainMuscle = findViewById(R.id.radioGainMuscle)

        tvCurrentWeightNote = findViewById(R.id.tvCurrentWeightNote)
        tvDesiredWeightVal = findViewById(R.id.tvDesiredWeightVal)
        tvGoalValidationHint = findViewById(R.id.tvGoalValidationHint)
        btnDecreaseDesiredWeight = findViewById(R.id.btnDecreaseDesiredWeight)
        btnIncreaseDesiredWeight = findViewById(R.id.btnIncreaseDesiredWeight)

        btnContinue = findViewById(R.id.btnContinue)
        tvCurrentWeightNote.text = "Cân nặng hiện tại: ${formatWeight(weight)} kg"
    }

    private fun setupListeners() {
        // Back buttons
        findViewById<TextView>(R.id.tvBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnBackHeader).setOnClickListener { finish() }

        // Selection
        cardLoseWeight.setOnClickListener {
            selectedGoalIndex = 0
            updateGoalSelection()
            updateGoalHint(isError = false)
        }

        cardMaintain.setOnClickListener {
            selectedGoalIndex = 1
            updateGoalSelection()
            updateGoalHint(isError = false)
        }

        cardGainMuscle.setOnClickListener {
            selectedGoalIndex = 2
            updateGoalSelection()
            updateGoalHint(isError = false)
        }

        btnDecreaseDesiredWeight.setOnClickListener {
            if (desiredWeight > 30.0) {
                desiredWeight -= 0.5
                updateDesiredWeightViews()
                updateGoalHint(isError = false)
            }
        }

        btnIncreaseDesiredWeight.setOnClickListener {
            if (desiredWeight < 250.0) {
                desiredWeight += 0.5
                updateDesiredWeightViews()
                updateGoalHint(isError = false)
            }
        }

        // Continue Button
        btnContinue.setOnClickListener {
            val validationError = validateDesiredWeightForGoal()
            if (validationError != null) {
                updateGoalHint(isError = true)
                Toast.makeText(this, validationError, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            // Go to Final Step
            val intent = Intent(this, FinalSetupActivity::class.java)
            intent.putExtra("USER_ID", userId)
            intent.putExtra("GENDER_ID", genderId)
            intent.putExtra("AGE", age)
            intent.putExtra("HEIGHT", height)
            intent.putExtra("WEIGHT", weight)
            intent.putExtra("GOAL_ID", selectedGoalIndex)
            intent.putExtra("DESIRED_WEIGHT", desiredWeight)
            startActivity(intent)
        }
    }

    private fun updateDesiredWeightViews() {
        tvDesiredWeightVal.text = "${formatWeight(desiredWeight)} kg"
    }

    private fun updateGoalSelection() {
        // Reset all
        resetCard(cardLoseWeight, radioLoseWeight)
        resetCard(cardMaintain, radioMaintain)
        resetCard(cardGainMuscle, radioGainMuscle)

        // Highlight selected
        when (selectedGoalIndex) {
            0 -> highlightCard(cardLoseWeight, radioLoseWeight)
            1 -> highlightCard(cardMaintain, radioMaintain)
            2 -> highlightCard(cardGainMuscle, radioGainMuscle)
        }
    }

    private fun updateGoalHint(isError: Boolean) {
        val hint = when (selectedGoalIndex) {
            0 -> "Mục tiêu giảm cân: cân nặng mong muốn phải nhỏ hơn cân nặng hiện tại."
            1 -> {
                val range = weight * 0.02
                "Mục tiêu giữ dáng: cân nặng mong muốn nên nằm trong ${formatWeight(weight - range)} - ${formatWeight(weight + range)} kg."
            }
            else -> "Mục tiêu tăng cân/tăng cơ: cân nặng mong muốn phải lớn hơn cân nặng hiện tại."
        }
        tvGoalValidationHint.text = hint
        val colorRes = if (isError) android.R.color.holo_red_dark else R.color.text_secondary
        tvGoalValidationHint.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    private fun validateDesiredWeightForGoal(): String? {
        return when (selectedGoalIndex) {
            0 -> if (desiredWeight < weight) null else "Giảm cân yêu cầu cân nặng mong muốn nhỏ hơn cân nặng hiện tại"
            1 -> {
                val maxDiff = weight * 0.02
                if (abs(desiredWeight - weight) <= maxDiff) null
                else "Giữ dáng yêu cầu cân nặng mong muốn trong phạm vi ±2%"
            }
            2 -> if (desiredWeight > weight) null else "Tăng cân yêu cầu cân nặng mong muốn lớn hơn cân nặng hiện tại"
            else -> "Mục tiêu chưa hợp lệ"
        }
    }

    private fun formatWeight(value: Double): String {
        return String.format(Locale.US, "%.1f", value)
    }

    private fun resetCard(card: LinearLayout, radio: ImageView) {
        card.background = ContextCompat.getDrawable(this, R.drawable.bg_goal_unselected)
        radio.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_circle_outline))
        radio.clearColorFilter()
    }

    private fun highlightCard(card: LinearLayout, radio: ImageView) {
        card.background = ContextCompat.getDrawable(this, R.drawable.bg_goal_selected)
        radio.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_check_circle_filled))
        // Tint check mark green
        radio.setColorFilter(ContextCompat.getColor(this, R.color.green_dark))
    }
}

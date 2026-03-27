package com.example.wao_fe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.UserRepository
import com.example.wao_fe.network.models.ActivityLevel
import com.example.wao_fe.network.models.CreateHealthProfileRequest
import com.example.wao_fe.network.models.Gender
import com.example.wao_fe.network.models.GoalType
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch
import java.util.Calendar

class BodyIndicesActivity : AppCompatActivity() {

    private lateinit var tvHeightVal: TextView
    private lateinit var sliderHeight: Slider
    private lateinit var tvWeightVal: TextView
    private lateinit var btnDecreaseWeight: ImageView
    private lateinit var btnIncreaseWeight: ImageView
    private lateinit var btnContinue: Button
    private lateinit var tvBMIResult: TextView

    private var userId: Long = -1
    private var genderId: Int = 2
    private var age: Int = 24

    private var currentHeight: Double = 172.0
    private var currentWeight: Double = 68.5

    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_body_indices)

        // Retrieve data from Step 1
        userId = intent.getLongExtra("USER_ID", -1)
        genderId = intent.getIntExtra("GENDER_ID", 2)
        age = intent.getIntExtra("AGE", 24)

        if (userId == -1L) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        tvHeightVal = findViewById(R.id.tvHeightVal)
        sliderHeight = findViewById(R.id.sliderHeight)
        tvWeightVal = findViewById(R.id.tvWeightVal)
        btnDecreaseWeight = findViewById(R.id.btnDecreaseWeight)
        btnIncreaseWeight = findViewById(R.id.btnIncreaseWeight)
        btnContinue = findViewById(R.id.btnContinue)
        tvBMIResult = findViewById(R.id.tvBMIResult)

        sliderHeight.value = currentHeight.toFloat()
        updateDisplays()

        // Correct the rotation of button 2 to be a plus sign roughly, or just use click listeners
        // In real app we'd swap the drawable
    }

    private fun updateDisplays() {
        tvHeightVal.text = currentHeight.toInt().toString()
        tvWeightVal.text = String.format("%.1f", currentWeight)
        calculateAndDisplayBMI()
    }

    private fun calculateAndDisplayBMI() {
        if (currentHeight <= 0) return

        val heightM = currentHeight / 100.0
        val bmi = currentWeight / (heightM * heightM)

        val category = when {
            bmi < 18.5 -> "Nhẹ cân"
            bmi < 24.9 -> "Bình thường"
            bmi < 29.9 -> "Thừa cân"
            else -> "Béo phì"
        }

        tvBMIResult.text = "BMI: %.1f ($category)".format(bmi)
    }

    private fun setupListeners() {
        // Back buttons
        findViewById<TextView>(R.id.tvBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnBackHeader).setOnClickListener { finish() }

        // Height Slider
        sliderHeight.addOnChangeListener { _, value, _ ->
            currentHeight = value.toDouble()
            updateDisplays()
        }

        // Weight Buttons
        btnDecreaseWeight.setOnClickListener {
            if (currentWeight > 30) {
                currentWeight -= 0.5
                updateDisplays()
            }
        }

        btnIncreaseWeight.setOnClickListener {
            if (currentWeight < 200) {
                currentWeight += 0.5
                updateDisplays()
            }
        }

        // Continue Button - Create Profile
        btnContinue.setOnClickListener {
            createHealthProfile()
        }
    }

    private fun createHealthProfile() {
        // Now Step 3 will handle creation
        val intent = Intent(this, GoalSelectionActivity::class.java)
        intent.putExtra("USER_ID", userId)
        intent.putExtra("GENDER_ID", genderId)
        intent.putExtra("AGE", age)
        intent.putExtra("HEIGHT", currentHeight)
        intent.putExtra("WEIGHT", currentWeight)
        startActivity(intent)
    }
}

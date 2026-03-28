package com.example.wao_fe

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.models.Gender
import com.example.wao_fe.network.models.GoalType
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.util.Calendar

class AllergiesActivity : AppCompatActivity() {

    private lateinit var chipGroupAllergies: ChipGroup
    private lateinit var btnComplete: Button
    private lateinit var tvBack: TextView
    private lateinit var btnBackHeader: ImageView

    private var userId: Long = -1
    private var genderId: Int = 2
    private var age: Int = 24
    private var height: Double = 172.0
    private var weight: Double = 68.5
    private var desiredWeight: Double = 68.5
    private var goalId: Int = 1

    // Danh sách dị ứng phổ biến
    private val allergyOptions = listOf(
        Pair("SEAFOOD", "🦐 Hải sản (Seafood)"),
        Pair("PEANUT", "🥜 Đậu phộng (Peanut)"),
        Pair("DAIRY", "🥛 Sữa (Dairy)"),
        Pair("GLUTEN", "🥖 Gluten"),
        Pair("EGG", "🥚 Trứng (Egg)"),
        Pair("SOY", "🫘 Đậu nành (Soy)"),
        Pair("TREE_NUT", "🌰 Hạt cây (Tree Nut)"),
        Pair("FISH", "🐠 Cá (Fish)")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_allergies)

        userId = intent.getLongExtra("USER_ID", -1)
        genderId = intent.getIntExtra("GENDER_ID", 2)
        age = intent.getIntExtra("AGE", 24)
        height = intent.getDoubleExtra("HEIGHT", 172.0)
        weight = intent.getDoubleExtra("WEIGHT", 68.5)
        desiredWeight = intent.getDoubleExtra("DESIRED_WEIGHT", weight)
        goalId = intent.getIntExtra("GOAL_ID", 1)

        if (userId == -1L) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        populateAllergyChips()
    }

    private fun initViews() {
        chipGroupAllergies = findViewById(R.id.chipGroupAllergies)
        btnComplete = findViewById(R.id.btnComplete)
        tvBack = findViewById(R.id.tvBack)
        btnBackHeader = findViewById(R.id.btnBackHeader)

        btnComplete.text = "Tiếp tục"
    }

    private fun setupListeners() {
        tvBack.setOnClickListener { finish() }
        btnBackHeader.setOnClickListener { finish() }

        btnComplete.setOnClickListener {
            goToFinalSetup()
        }
    }

    private fun populateAllergyChips() {
        for ((code, display) in allergyOptions) {
            val chip = Chip(this).apply {
                text = display
                tag = code
                isCheckable = true
                isClickable = true

                // Style cho FilterChip bo góc
                setChipBackgroundColorResource(R.color.wao_slate_50)
                setTextColor(ContextCompat.getColor(context, R.color.wao_slate_800))
                chipStrokeWidth = 2f
                chipStrokeColor = ContextCompat.getColorStateList(context, R.color.wao_slate_300)
                chipCornerRadius = 24f // Bo góc đẹp mắt

                setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        setChipBackgroundColorResource(R.color.green_light)
                        chipStrokeColor = ContextCompat.getColorStateList(context, R.color.green_dark)
                    } else {
                        setChipBackgroundColorResource(R.color.wao_slate_50)
                        chipStrokeColor = ContextCompat.getColorStateList(context, R.color.wao_slate_300)
                    }
                }
            }
            chipGroupAllergies.addView(chip)
        }
    }

    private fun goToFinalSetup() {
        val selectedAllergies = chipGroupAllergies.checkedChipIds.mapNotNull { id ->
            chipGroupAllergies.findViewById<Chip>(id)?.tag?.toString()
        }.joinToString(", ")

        val intent = Intent(this, FinalSetupActivity::class.java).apply {
            putExtra("USER_ID", userId)
            putExtra("GENDER_ID", genderId)
            putExtra("AGE", age)
            putExtra("HEIGHT", height)
            putExtra("WEIGHT", weight)
            putExtra("DESIRED_WEIGHT", desiredWeight)
            putExtra("GOAL_ID", goalId)
            putExtra("ALLERGIES", selectedAllergies)
        }
        startActivity(intent)
    }
}

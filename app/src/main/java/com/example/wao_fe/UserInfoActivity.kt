package com.example.wao_fe

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class UserInfoActivity : AppCompatActivity() {

    private lateinit var cardMale: LinearLayout
    private lateinit var cardFemale: LinearLayout
    private lateinit var cardOther: LinearLayout

    private lateinit var ivMale: ImageView
    private lateinit var ivFemale: ImageView
    private lateinit var ivOther: ImageView

    private var selectedGender = 2 // Default Female
    private var userId: Long = -1

    private var selectedAge = 24
    private val minAge = 10
    private val maxAge = 100

    private lateinit var tvSelectedAge: TextView
    private lateinit var tvAgeDisplay: TextView
    private lateinit var tvAgePrev2: TextView
    private lateinit var tvAgePrev1: TextView
    private lateinit var tvAgeNext1: TextView
    private lateinit var tvAgeNext2: TextView

    private var ageDragAnchorX = 0f
    private var ageDragAccumulatedX = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)

        userId = intent.getLongExtra("USER_ID", -1)
        if (userId == -1L) {
             // Fallback to shared prefs if intent is missing (e.g. process death)
             userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getLong("USER_ID", -1)
        }

        initViews()
        setupListeners()
        updateGenderUI()
        updateAgeUi()
    }

    private fun initViews() {
        cardMale = findViewById(R.id.cardMale)
        cardFemale = findViewById(R.id.cardFemale)
        cardOther = findViewById(R.id.cardOther)

        ivMale = findViewById(R.id.ivMale)
        ivFemale = findViewById(R.id.ivFemale)
        ivOther = findViewById(R.id.ivOther)

        tvSelectedAge = findViewById(R.id.tvSelectedAge)
        tvAgeDisplay = findViewById(R.id.tvAgeDisplay)
        tvAgePrev2 = findViewById(R.id.tvAgePrev2)
        tvAgePrev1 = findViewById(R.id.tvAgePrev1)
        tvAgeNext1 = findViewById(R.id.tvAgeNext1)
        tvAgeNext2 = findViewById(R.id.tvAgeNext2)
    }

    private fun setupListeners() {
        // Gender Selection
        val genderListener = View.OnClickListener { v ->
            selectedGender = when (v.id) {
                R.id.cardMale -> 1
                R.id.cardFemale -> 2
                else -> 3
            }
            updateGenderUI()
        }

        cardMale.setOnClickListener(genderListener)
        cardFemale.setOnClickListener(genderListener)
        cardOther.setOnClickListener(genderListener)

        val agePickerContainer = findViewById<FrameLayout>(R.id.layoutAgePicker)
        val ageRow = findViewById<LinearLayout>(R.id.layoutAgeRow)
        setupAgeSwipe(agePickerContainer)
        setupAgeSwipe(ageRow)

        // Navigation - Back button hidden in layout but logic can remain or be removed
        // findViewById<View>(R.id.btnBack).setOnClickListener { onBackPressed() }

        findViewById<View>(R.id.btnContinue).setOnClickListener {
            val intent = Intent(this, BodyIndicesActivity::class.java)
            intent.putExtra("USER_ID", userId)
            intent.putExtra("GENDER_ID", selectedGender)
            intent.putExtra("AGE", selectedAge)
            startActivity(intent)
        }
    }

    private fun updateGenderUI() {
        resetCard(cardMale, ivMale)
        resetCard(cardFemale, ivFemale)
        resetCard(cardOther, ivOther)

        when (selectedGender) {
            1 -> highlightCard(cardMale, ivMale)
            2 -> highlightCard(cardFemale, ivFemale)
            3 -> highlightCard(cardOther, ivOther)
        }
    }

    private fun resetCard(card: LinearLayout, iconView: ImageView) {
        card.background = ContextCompat.getDrawable(this, R.drawable.bg_gender_unselected)
        // Assuming circle_primary_alpha is a solid color drawable or shape
        iconView.background = ContextCompat.getDrawable(this, R.drawable.circle_primary_alpha)
        iconView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_dark))
    }

    private fun highlightCard(card: LinearLayout, iconView: ImageView) {
        card.background = ContextCompat.getDrawable(this, R.drawable.bg_gender_selected)
        iconView.background = ContextCompat.getDrawable(this, R.drawable.bg_logo_circle)
        iconView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_dark))
        iconView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.white))
    }

    private fun setupAgeSwipe(target: View) {
        target.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    ageDragAnchorX = event.x
                    ageDragAccumulatedX = 0f
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.x - ageDragAnchorX
                    ageDragAnchorX = event.x
                    ageDragAccumulatedX += deltaX

                    val stepPx = 26f
                    while (ageDragAccumulatedX >= stepPx) {
                        // Drag right: increase age.
                        changeAgeBy(1)
                        ageDragAccumulatedX -= stepPx
                    }
                    while (ageDragAccumulatedX <= -stepPx) {
                        // Drag left: decrease age.
                        changeAgeBy(-1)
                        ageDragAccumulatedX += stepPx
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.performClick()
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                    true
                }

                else -> false
            }
        }
    }

    private fun changeAgeBy(delta: Int) {
        val nextAge = (selectedAge + delta).coerceIn(minAge, maxAge)
        if (nextAge != selectedAge) {
            selectedAge = nextAge
            updateAgeUi()
        }
    }

    private fun updateAgeUi() {
        tvSelectedAge.text = selectedAge.toString()
        tvAgeDisplay.text = "$selectedAge tuổi"
        tvAgePrev2.text = displayNeighborAge(selectedAge - 2)
        tvAgePrev1.text = displayNeighborAge(selectedAge - 1)
        tvAgeNext1.text = displayNeighborAge(selectedAge + 1)
        tvAgeNext2.text = displayNeighborAge(selectedAge + 2)
    }

    private fun displayNeighborAge(age: Int): String {
        return if (age in minAge..maxAge) age.toString() else ""
    }
}

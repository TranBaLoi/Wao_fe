package com.example.wao_fe

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.UserRepository
import com.example.wao_fe.network.models.ActivityLevel
import com.example.wao_fe.network.models.CreateHealthProfileRequest
import com.example.wao_fe.network.models.Gender
import com.example.wao_fe.network.models.GoalType
import com.example.wao_fe.network.models.UpdateUserRequest
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var btnBackHeader: ImageView
    private lateinit var ivAvatarEdit: ImageView
    private lateinit var etFullName: EditText

    private lateinit var spinnerGender: Spinner
    private lateinit var etDob: EditText
    private lateinit var etHeight: EditText
    private lateinit var etWeight: EditText
    private lateinit var etDesiredWeight: EditText
    private lateinit var etTargetDays: EditText
    private lateinit var spinnerActivityLevel: Spinner
    private lateinit var spinnerGoalType: Spinner
    private lateinit var etAllergies: EditText

    private lateinit var btnSave: Button

    private var userId: Long = -1
    private val userRepository = UserRepository()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            sharedPref.edit().putString("USER_AVATAR", uri.toString()).apply()

            Glide.with(this)
                .load(uri)
                .apply(RequestOptions.circleCropTransform())
                .into(ivAvatarEdit)
            ivAvatarEdit.setPadding(0, 0, 0, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        userId = sharedPref.getLong("USER_ID", -1)
        val currentName = sharedPref.getString("USER_NAME", "")
        val currentAvatar = sharedPref.getString("USER_AVATAR", null)

        initViews()
        setupSpinners()
        setupListeners()

        etFullName.setText(currentName)
        if (currentAvatar != null) {
            Glide.with(this)
                .load(Uri.parse(currentAvatar))
                .apply(RequestOptions.circleCropTransform())
                .into(ivAvatarEdit)
            ivAvatarEdit.setPadding(0, 0, 0, 0)
        }

        loadCurrentProfile()
    }

    private fun initViews() {
        btnBackHeader = findViewById(R.id.btnBackHeader)
        ivAvatarEdit = findViewById(R.id.ivAvatarEdit)
        etFullName = findViewById(R.id.etFullName)

        spinnerGender = findViewById(R.id.spinnerGender)
        etDob = findViewById(R.id.etDob)
        etHeight = findViewById(R.id.etHeight)
        etWeight = findViewById(R.id.etWeight)
        etDesiredWeight = findViewById(R.id.etDesiredWeight)
        etTargetDays = findViewById(R.id.etTargetDays)
        spinnerActivityLevel = findViewById(R.id.spinnerActivityLevel)
        spinnerGoalType = findViewById(R.id.spinnerGoalType)
        etAllergies = findViewById(R.id.etAllergies)

        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupSpinners() {
        val genders = arrayOf("MALE", "FEMALE", "OTHER")
        spinnerGender.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genders)

        val activities = arrayOf("SEDENTARY", "LIGHTLY_ACTIVE", "MODERATELY_ACTIVE", "VERY_ACTIVE", "EXTRA_ACTIVE")
        spinnerActivityLevel.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, activities)

        val goals = arrayOf("LOSE_WEIGHT", "GAIN_WEIGHT", "MAINTAIN")
        spinnerGoalType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, goals)
    }

    private fun loadCurrentProfile() {
        if (userId == -1L) return

        lifecycleScope.launch {
            when (val userResult = userRepository.getUserById(userId)) {
                is ApiResult.Success -> {
                    etFullName.setText(userResult.data.fullName)
                }
                else -> {} // ignore error handling for simplification
            }

            when (val result = userRepository.getLatestHealthProfile(userId)) {
                is ApiResult.Success -> {
                    val profile = result.data

                    val genders = arrayOf("MALE", "FEMALE", "OTHER")
                    spinnerGender.setSelection(genders.indexOf(profile.gender.name).takeIf { it >= 0 } ?: 0)

                    etDob.setText(profile.dob)
                    etHeight.setText(profile.heightCm.toString())
                    etWeight.setText(profile.weightKg.toString())
                    etDesiredWeight.setText(profile.desiredWeightKg.toString())
                    etTargetDays.setText(profile.targetDays.toString())

                    val activities = arrayOf("SEDENTARY", "LIGHTLY_ACTIVE", "MODERATELY_ACTIVE", "VERY_ACTIVE", "EXTRA_ACTIVE")
                    spinnerActivityLevel.setSelection(activities.indexOf(profile.activityLevel.name).takeIf { it >= 0 } ?: 0)

                    val goals = arrayOf("LOSE_WEIGHT", "GAIN_WEIGHT", "MAINTAIN")
                    spinnerGoalType.setSelection(goals.indexOf(profile.goalType.name).takeIf { it >= 0 } ?: 0)
                }
                else -> {}
            }
        }
    }

    private fun setupListeners() {
        btnBackHeader.setOnClickListener { finish() }

        findViewById<android.view.View>(R.id.layoutAvatar).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSave.setOnClickListener {
            saveChanges()
        }
    }

    private fun saveChanges() {
        val newName = etFullName.text.toString().trim()
        val dob = etDob.text.toString().trim()
        val heightStr = etHeight.text.toString()
        val weightStr = etWeight.text.toString()
        val desiredWeightStr = etDesiredWeight.text.toString()
        val targetDaysStr = etTargetDays.text.toString()
        val allergies = etAllergies.text.toString().trim().takeIf { it.isNotEmpty() }

        if (newName.isEmpty() || dob.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty() || desiredWeightStr.isEmpty() || targetDaysStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId == -1L) {
            Toast.makeText(this, "Lỗi không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show()
            return
        }

        btnSave.isEnabled = false
        btnSave.text = "Đang lưu..."

        lifecycleScope.launch {
            // Update User Name
            val userRequest = UpdateUserRequest(fullName = newName)
            val userSuccess = when (val res = userRepository.updateUser(userId, userRequest)) {
                is ApiResult.Success -> {
                    val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    sharedPref.edit().putString("USER_NAME", newName).apply()
                    true
                }
                is ApiResult.Error -> {
                    Toast.makeText(this@EditProfileActivity, "Lỗi User: ${res.message}", Toast.LENGTH_LONG).show()
                    false
                }
            }

            // Create New Health Profile
            val healthRequest = CreateHealthProfileRequest(
                gender = Gender.valueOf(spinnerGender.selectedItem.toString()),
                dob = dob,
                heightCm = heightStr.toDoubleOrNull() ?: 170.0,
                weightKg = weightStr.toDoubleOrNull() ?: 60.0,
                activityLevel = ActivityLevel.valueOf(spinnerActivityLevel.selectedItem.toString()),
                goalType = GoalType.valueOf(spinnerGoalType.selectedItem.toString()),
                desiredWeightKg = desiredWeightStr.toDoubleOrNull() ?: 60.0,
                targetDays = targetDaysStr.toIntOrNull() ?: 30,
                allergies = allergies
            )

            val healthSuccess = when (val res = userRepository.createHealthProfile(userId, healthRequest)) {
                is ApiResult.Success -> true
                is ApiResult.Error -> {
                    Toast.makeText(this@EditProfileActivity, "Lỗi Health Profile: ${res.message}", Toast.LENGTH_LONG).show()
                    false
                }
            }

            btnSave.isEnabled = true
            btnSave.text = "Lưu thay đổi"

            if (userSuccess && healthSuccess) {
                Toast.makeText(this@EditProfileActivity, "Đã lưu toàn bộ thông tin!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}

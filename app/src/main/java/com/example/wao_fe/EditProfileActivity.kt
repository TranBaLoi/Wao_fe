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
import com.example.wao_fe.network.models.HealthProfileResponse
import com.example.wao_fe.network.models.UpdateUserRequest
import java.util.Locale
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var btnBackHeader: ImageView
    private lateinit var ivAvatarEdit: ImageView
    private lateinit var etFullName: EditText

    private lateinit var spinnerGender: Spinner
    private lateinit var etDob: EditText
    private lateinit var etHeight: EditText
    private lateinit var etDesiredWeight: EditText
    private lateinit var etTargetDays: EditText
    private lateinit var spinnerActivityLevel: Spinner
    private lateinit var spinnerGoalType: Spinner
    private lateinit var etAllergies: EditText

    private lateinit var btnSave: Button

    private var userId: Long = -1
    private val userRepository = UserRepository()
    private var currentHealthProfile: HealthProfileResponse? = null

    private data class SpinnerOption<T>(val label: String, val value: T)

    private lateinit var genderOptions: List<SpinnerOption<Gender>>
    private lateinit var activityOptions: List<SpinnerOption<ActivityLevel>>
    private lateinit var goalOptions: List<SpinnerOption<GoalType>>

    // Display Vietnamese labels in UI while keeping API-safe tokens when saving.
    private val allergyViMap = mapOf(
        "SEAFOOD" to "🦐 Hải sản (Seafood)",
        "PEANUT" to "🥜 Đậu phộng (Peanut)",
        "DAIRY" to "🥛 Sữa (Dairy)",
        "GLUTEN" to "🥖 Gluten",
        "EGG" to "🥚 Trứng (Egg)",
        "SOY" to "🫘 Đậu nành (Soy)",
        "TREE_NUT" to "🌰 Hạt cây (Tree Nut)",
        "FISH" to "🐠 Cá (Fish)",
        "MILK" to "🥛 Sữa (Dairy)"
    )

    private val allergyApiMap = allergyViMap.entries.associate { (api, vi) -> vi.lowercase(Locale.ROOT) to api }

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
        etDesiredWeight = findViewById(R.id.etDesiredWeight)
        etTargetDays = findViewById(R.id.etTargetDays)
        spinnerActivityLevel = findViewById(R.id.spinnerActivityLevel)
        spinnerGoalType = findViewById(R.id.spinnerGoalType)
        etAllergies = findViewById(R.id.etAllergies)

        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupSpinners() {
        genderOptions = listOf(
            SpinnerOption(getString(R.string.gender_male), Gender.MALE),
            SpinnerOption(getString(R.string.gender_female), Gender.FEMALE),
            SpinnerOption(getString(R.string.gender_other), Gender.OTHER)
        )
        spinnerGender.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            genderOptions.map { it.label }
        )

        activityOptions = listOf(
            SpinnerOption(getString(R.string.activity_sedentary), ActivityLevel.SEDENTARY),
            SpinnerOption(getString(R.string.activity_lightly_active), ActivityLevel.LIGHTLY_ACTIVE),
            SpinnerOption(getString(R.string.activity_moderately_active), ActivityLevel.MODERATELY_ACTIVE),
            SpinnerOption(getString(R.string.activity_very_active), ActivityLevel.VERY_ACTIVE),
            SpinnerOption(getString(R.string.activity_extra_active), ActivityLevel.EXTRA_ACTIVE)
        )
        spinnerActivityLevel.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            activityOptions.map { it.label }
        )

        goalOptions = listOf(
            SpinnerOption(getString(R.string.goal_lose_weight), GoalType.LOSE_WEIGHT),
            SpinnerOption(getString(R.string.goal_gain_weight), GoalType.GAIN_WEIGHT),
            SpinnerOption(getString(R.string.goal_maintain), GoalType.MAINTAIN)
        )
        spinnerGoalType.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            goalOptions.map { it.label }
        )
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
                    currentHealthProfile = profile

                    spinnerGender.setSelection(
                        genderOptions.indexOfFirst { it.value == profile.gender }.takeIf { it >= 0 } ?: 0
                    )

                    etDob.setText(profile.dob)
                    etHeight.setText(profile.heightCm.toString())
                    etDesiredWeight.setText(profile.desiredWeightKg.toString())
                    etTargetDays.setText(profile.targetDays.toString())
                    etAllergies.setText(localizeAllergiesForDisplay(profile.allergies))

                    spinnerActivityLevel.setSelection(
                        activityOptions.indexOfFirst { it.value == profile.activityLevel }.takeIf { it >= 0 } ?: 0
                    )

                    spinnerGoalType.setSelection(
                        goalOptions.indexOfFirst { it.value == profile.goalType }.takeIf { it >= 0 } ?: 0
                    )
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
        val desiredWeightStr = etDesiredWeight.text.toString()
        val targetDaysStr = etTargetDays.text.toString()
        val allergies = normalizeAllergiesForApi(etAllergies.text.toString())

        if (newName.isEmpty() || dob.isEmpty() || heightStr.isEmpty() || targetDaysStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.edit_profile_fill_required), Toast.LENGTH_SHORT).show()
            return
        }

        if (userId == -1L) {
            Toast.makeText(this, getString(R.string.edit_profile_missing_user_id), Toast.LENGTH_SHORT).show()
            return
        }

        val existingProfile = currentHealthProfile
        if (existingProfile == null) {
            Toast.makeText(this, getString(R.string.edit_profile_missing_existing_profile), Toast.LENGTH_SHORT).show()
            return
        }

        btnSave.isEnabled = false
        btnSave.text = getString(R.string.edit_profile_saving)

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
                    Toast.makeText(
                        this@EditProfileActivity,
                        getString(R.string.edit_profile_user_error, res.message),
                        Toast.LENGTH_LONG
                    ).show()
                    false
                }
            }

            // Keep hidden fields unchanged by reusing values from the existing profile.
            val healthRequest = CreateHealthProfileRequest(
                gender = genderOptions.getOrNull(spinnerGender.selectedItemPosition)?.value ?: existingProfile.gender,
                dob = dob,
                heightCm = heightStr.toDoubleOrNull() ?: 170.0,
                weightKg = existingProfile.weightKg,
                activityLevel = activityOptions.getOrNull(spinnerActivityLevel.selectedItemPosition)?.value
                    ?: existingProfile.activityLevel,
                goalType = goalOptions.getOrNull(spinnerGoalType.selectedItemPosition)?.value ?: existingProfile.goalType,
                desiredWeightKg = desiredWeightStr.toDoubleOrNull() ?: existingProfile.desiredWeightKg,
                targetDays = targetDaysStr.toIntOrNull() ?: 30,
                allergies = allergies,
                preferenceVector = existingProfile.preferenceVector
            )

            val healthSuccess = when (val res = userRepository.createHealthProfile(userId, healthRequest)) {
                is ApiResult.Success -> true
                is ApiResult.Error -> {
                    Toast.makeText(
                        this@EditProfileActivity,
                        getString(R.string.edit_profile_health_error, res.message),
                        Toast.LENGTH_LONG
                    ).show()
                    false
                }
            }

            btnSave.isEnabled = true
            btnSave.text = getString(R.string.edit_profile_save)

            if (userSuccess && healthSuccess) {
                Toast.makeText(this@EditProfileActivity, getString(R.string.edit_profile_save_success), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun localizeAllergiesForDisplay(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ") { token ->
                allergyViMap[token.uppercase(Locale.ROOT)] ?: token
            }
    }

    private fun normalizeAllergiesForApi(input: String?): String? {
        if (input.isNullOrBlank()) return null
        val normalized = input.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ") { token ->
                val key = token.lowercase(Locale.ROOT)
                allergyApiMap[key] ?: token.uppercase(Locale.ROOT)
            }
        return normalized.takeIf { it.isNotBlank() }
    }
}

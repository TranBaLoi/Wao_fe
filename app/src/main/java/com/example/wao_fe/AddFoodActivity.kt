package com.example.wao_fe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.wao_fe.network.NetworkClient
import com.example.wao_fe.network.models.FoodRequest
import com.example.wao_fe.network.models.MealType
import com.google.gson.Gson
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class AddFoodActivity : AppCompatActivity() {

    private val apiService = NetworkClient.apiService

    private lateinit var cardUpload: LinearLayout
    private lateinit var ivUploaded: ImageView
    private lateinit var layoutUploadPlaceholder: LinearLayout

    private lateinit var etFoodName: EditText
    private lateinit var etServingSize: EditText
    private lateinit var etCalories: EditText
    private lateinit var etCarbs: EditText
    private lateinit var etProtein: EditText
    private lateinit var etFat: EditText

    private lateinit var btnSaveFood: TextView

    private var selectedImageUri: Uri? = null
    private var isSaving = false

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            ivUploaded.visibility = View.VISIBLE
            layoutUploadPlaceholder.visibility = View.GONE
            ivUploaded.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_add_food)

        initViews()
        setupActions()
    }

    private fun initViews() {
        cardUpload = findViewById(R.id.cardUpload)
        ivUploaded = findViewById(R.id.ivUploaded)
        layoutUploadPlaceholder = findViewById(R.id.layoutUploadPlaceholder)

        etFoodName = findViewById(R.id.etFoodName)
        etServingSize = findViewById(R.id.etServingSize)
        etCalories = findViewById(R.id.etCalories)
        etCarbs = findViewById(R.id.etCarbs)
        etProtein = findViewById(R.id.etProtein)
        etFat = findViewById(R.id.etFat)

        btnSaveFood = findViewById(R.id.btnSaveFood)

        etCalories.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        etCarbs.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        etProtein.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        etFat.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

    private fun setupActions() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            navigateBackToHome()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackToHome()
            }
        })

        cardUpload.setOnClickListener {
            imagePicker.launch("image/*")
        }

        findViewById<FloatingActionButton>(R.id.fabAddFood).setOnClickListener {
            // Already on add screen
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
                    startActivity(Intent(this, FoodDiaryActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_menu -> {
                    startActivity(Intent(this, MealPlanActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_profile -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }

                else -> false
            }
        }

        btnSaveFood.setOnClickListener {
            if (!isSaving) {
                submitCreateFood()
            }
        }
    }

    private fun submitCreateFood() {
        val name = etFoodName.text.toString().trim()
        val servingSize = etServingSize.text.toString().trim()
        val calories = etCalories.text.toString().toDoubleOrNull()
        val carbs = etCarbs.text.toString().toDoubleOrNull() ?: 0.0
        val protein = etProtein.text.toString().toDoubleOrNull() ?: 0.0
        val fat = etFat.text.toString().toDoubleOrNull() ?: 0.0

        if (name.isBlank()) {
            toast("Vui lòng nhập tên món ăn")
            return
        }

        if (servingSize.isBlank()) {
            toast("Vui lòng nhập khẩu phần")
            return
        }

        if (calories == null || calories <= 0.0) {
            toast("Calories phải lớn hơn 0")
            return
        }

        val request = FoodRequest(
            name = name,
            servingSize = servingSize,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat
        )

        lifecycleScope.launch {
            isSaving = true
            btnSaveFood.alpha = 0.7f
            btnSaveFood.text = "ĐANG LƯU..."

            val foodPart = Gson().toJson(request)
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            val imageParts = buildImageParts(selectedImageUri)

            val result = runCatching { apiService.createFood(foodPart, imageParts) }

            isSaving = false
            btnSaveFood.alpha = 1f
            btnSaveFood.text = "LƯU"

            if (result.isSuccess) {
                toast("Đã lưu món ăn")
                setResult(RESULT_OK)
                finish()
            } else {
                val err = result.exceptionOrNull()
                val msg = if (err is HttpException) {
                    "Không thể lưu món ăn (HTTP ${err.code()})"
                } else {
                    "Không thể lưu món ăn. Kiểm tra BE và thử lại"
                }
                toast(msg)
            }
        }
    }

    private fun currentUserId(): Long {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        return sharedPref.getLong("USER_ID", -1)
    }

    private fun buildImageParts(imageUri: Uri?): List<MultipartBody.Part>? {
        if (imageUri == null) return null

        val mimeType = contentResolver.getType(imageUri) ?: "image/*"
        val bytes = contentResolver.openInputStream(imageUri)?.use { it.readBytes() } ?: return null
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val fileName = resolveDisplayName(imageUri) ?: "food_image.jpg"

        return listOf(
            MultipartBody.Part.createFormData("images", fileName, requestBody)
        )
    }

    private fun resolveDisplayName(uri: Uri): String? {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateBackToHome() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
        finish()
    }
}

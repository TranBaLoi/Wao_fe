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

class SettingsActivity : AppCompatActivity() {

    private lateinit var viewModel: SettingsViewModel
    private var userId: Long = -1

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var ivAvatar: ImageView

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
            // Deprecated view logic removed for new UI
        }

        viewModel.error.observe(this) { msg ->
            if (msg.isNotEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
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

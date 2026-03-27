package com.example.wao_fe

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView

    private lateinit var googleSignInClient: GoogleSignInClient
    private val userRepository = UserRepository()

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                performGoogleLogin(idToken)
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google login failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        setupGoogleSignIn()
        initViews()
        setupClickListeners()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun initViews() {
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        btnGoogleLogin = findViewById(R.id.btn_google_login)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
        tvRegister = findViewById(R.id.tv_register)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }

        btnGoogleLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (TextUtils.isEmpty(email)) {
            tilEmail.error = "Vui lòng nhập email"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Email không hợp lệ"
            isValid = false
        } else {
            tilEmail.error = null
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.error = "Vui lòng nhập mật khẩu"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Mật khẩu phải có ít nhất 6 ký tự"
            isValid = false
        } else {
            tilPassword.error = null
        }

        return isValid
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()

        btnLogin.text = getString(R.string.loading_login)
        btnLogin.isEnabled = false

        lifecycleScope.launch {
            when (val result = userRepository.loginByEmail(email)) {
                is ApiResult.Success -> {
                    checkHealthProfileAndNavigate(result.data.id, result.data.fullName)
                }

                is ApiResult.Error -> {
                    btnLogin.text = getString(R.string.btn_login)
                    btnLogin.isEnabled = true
                    tilEmail.error = result.fieldErrors["email"]
                    Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun performGoogleLogin(idToken: String) {
        lifecycleScope.launch {
            when (val result = userRepository.googleLogin(idToken)) {
                is ApiResult.Success -> {
                   checkHealthProfileAndNavigate(result.data.id, result.data.fullName)
                }
                is ApiResult.Error -> {
                    Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkHealthProfileAndNavigate(userId: Long, fullName: String) {
        // Save user info
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putLong("USER_ID", userId)
            putString("USER_NAME", fullName)
            putBoolean("IS_LOGGED_IN", true)
            apply()
        }

        lifecycleScope.launch {
            when (userRepository.getLatestHealthProfile(userId)) {
                is ApiResult.Success -> {
                    // Profile exists, go to Main
                    Toast.makeText(
                        this@LoginActivity,
                        "Chào mừng trở lại, $fullName",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
                is ApiResult.Error -> {
                    // Profile likely not found (or error), go to User Info collection
                     Toast.makeText(
                        this@LoginActivity,
                        "Chào mừng $fullName, hãy cập nhật thông tin sức khỏe",
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(this@LoginActivity, UserInfoActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}

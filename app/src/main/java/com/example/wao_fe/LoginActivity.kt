package com.example.wao_fe

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
        tvRegister = findViewById(R.id.tv_register)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
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
        btnLogin.text = getString(R.string.loading_login)
        btnLogin.isEnabled = false

        // TODO: Gọi API đăng nhập
        btnLogin.postDelayed({
            btnLogin.text = getString(R.string.btn_login)
            btnLogin.isEnabled = true
            Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
            // TODO: Navigate to MainActivity
        }, 1500)
    }
}

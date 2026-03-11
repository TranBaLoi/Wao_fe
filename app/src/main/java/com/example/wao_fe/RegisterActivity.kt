package com.example.wao_fe

import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    private lateinit var tilFullname: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etFullname: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        tilFullname = findViewById(R.id.til_fullname)
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password)
        etFullname = findViewById(R.id.et_fullname)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnRegister = findViewById(R.id.btn_register)
        tvLogin = findViewById(R.id.tv_login)
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            if (validateInputs()) {
                performRegister()
            }
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val fullname = etFullname.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (TextUtils.isEmpty(fullname)) {
            tilFullname.error = "Vui lòng nhập họ và tên"
            isValid = false
        } else if (fullname.length < 2) {
            tilFullname.error = "Tên quá ngắn"
            isValid = false
        } else {
            tilFullname.error = null
        }

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

        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.error = "Vui lòng xác nhận mật khẩu"
            isValid = false
        } else if (confirmPassword != password) {
            tilConfirmPassword.error = "Mật khẩu không khớp"
            isValid = false
        } else {
            tilConfirmPassword.error = null
        }

        return isValid
    }

    private fun performRegister() {
        btnRegister.text = getString(R.string.loading_register)
        btnRegister.isEnabled = false

        // TODO: Gọi API đăng ký
        btnRegister.postDelayed({
            btnRegister.text = getString(R.string.btn_register)
            btnRegister.isEnabled = true
            Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_LONG).show()
            finish()
        }, 1500)
    }
}

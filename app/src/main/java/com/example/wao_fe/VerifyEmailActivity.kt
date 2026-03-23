package com.example.wao_fe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.UserRepository
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class VerifyEmailActivity : AppCompatActivity() {

    private lateinit var etCode: TextInputEditText
    private lateinit var btnVerify: Button
    private lateinit var tvResend: TextView
    private var email: String = ""

    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_email)

        email = intent.getStringExtra("email") ?: ""
        if (email.isEmpty()) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etCode = findViewById(R.id.et_code)
        btnVerify = findViewById(R.id.btn_verify)
        tvResend = findViewById(R.id.tv_resend)
    }

    private fun setupClickListeners() {
        btnVerify.setOnClickListener {
            val code = etCode.text.toString().trim()
            if (code.length == 6) {
                performVerification(code)
            } else {
                Toast.makeText(this, "Vui lòng nhập mã 6 số", Toast.LENGTH_SHORT).show()
            }
        }

        tvResend.setOnClickListener {
            // In a real app, call API to resend code
            Toast.makeText(this, "Đã gửi lại mã", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performVerification(code: String) {
        btnVerify.isEnabled = false
        btnVerify.text = getString(R.string.verifying)

        lifecycleScope.launch {
            when (val result = userRepository.verifyEmail(email, code)) {
                is ApiResult.Success -> {
                    Toast.makeText(this@VerifyEmailActivity, getString(R.string.verification_success), Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@VerifyEmailActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                is ApiResult.Error -> {
                    btnVerify.isEnabled = true
                    btnVerify.text = getString(R.string.btn_verify)
                    Toast.makeText(this@VerifyEmailActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}



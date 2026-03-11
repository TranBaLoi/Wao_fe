package com.example.wao_fe

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Edge-to-edge display (modern API)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        animateDots()

        // Navigate to Login after 2.5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 2500)
    }

    private fun animateDots() {
        val dot1 = findViewById<View>(R.id.dot1)
        val dot2 = findViewById<View>(R.id.dot2)
        val dot3 = findViewById<View>(R.id.dot3)

        val anim1 = ObjectAnimator.ofFloat(dot1, "alpha", 0.3f, 1f, 0.3f).apply {
            duration = 900
            repeatCount = ObjectAnimator.INFINITE
            startDelay = 0
            interpolator = AccelerateDecelerateInterpolator()
        }
        val anim2 = ObjectAnimator.ofFloat(dot2, "alpha", 0.3f, 1f, 0.3f).apply {
            duration = 900
            repeatCount = ObjectAnimator.INFINITE
            startDelay = 300
            interpolator = AccelerateDecelerateInterpolator()
        }
        val anim3 = ObjectAnimator.ofFloat(dot3, "alpha", 0.3f, 1f, 0.3f).apply {
            duration = 900
            repeatCount = ObjectAnimator.INFINITE
            startDelay = 600
            interpolator = AccelerateDecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(anim1, anim2, anim3)
            start()
        }
    }
}

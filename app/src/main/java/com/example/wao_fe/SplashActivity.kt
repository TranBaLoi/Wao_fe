package com.example.wao_fe

import android.animation.Animator

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Edge-to-edge display (modern API)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        animateProgress()
    }

    private fun animateProgress() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvProgress = findViewById<TextView>(R.id.tvProgress)

        val anim = ValueAnimator.ofInt(0, 100)
        anim.duration = 2500
        anim.interpolator = DecelerateInterpolator()
        anim.addUpdateListener { animation ->
            val progress = animation.animatedValue as Int
            progressBar.progress = progress
            tvProgress.text = "$progress%"
        }
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                 routeUser()
            }
        })
        anim.start()
    }

    private fun routeUser() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val userId = sharedPref.getLong("USER_ID", -1)

        val destinationClass = if (userId != -1L) {
            MainActivity::class.java
        } else {
            LoginActivity::class.java
        }

        startActivity(Intent(this@SplashActivity, destinationClass))
        finish()
    }
}

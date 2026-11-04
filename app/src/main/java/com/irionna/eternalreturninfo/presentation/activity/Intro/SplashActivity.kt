package com.irionna.eternalreturninfo.presentation.activity.Intro

import android.content.Intent
import com.irionna.eternalreturninfo.util.setUpDarkSystemBars
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.presentation.activity.login.LoginActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()


        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setUpDarkSystemBars()
        setContentView(R.layout.splash_activity)

        /**
         * 안드로이드 12 버전 이상부터 SPLASH API 강제화로
         * SPLASH 화면이 두 번 나오는 현상을 해결하기 위해
         * 버전별 실행코드 달리함.
         */

//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
//            Handler().postDelayed({
//                val intent = Intent(this, LoginActivity::class.java)
//                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
//                startActivity(intent)
//                finish()
//            },2000)
//        } else {
//            val intent = Intent(this, LoginActivity::class.java)
//            startActivity(intent)
//            finish()

            Handler().postDelayed({
                val intent = Intent(this, LoginActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intent)
                finish()
            },1500)
//        }
    }
}
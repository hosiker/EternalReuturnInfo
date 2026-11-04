package com.irionna.eternalreturninfo.util

import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * targetSdk 35 부터는 화면이 상태바·내비게이션 바 뒤까지 그려지는 edge-to-edge 가 강제된다.
 * 앱의 모든 화면은 검은 배경이므로, 시스템 바는 투명하게 두고 아이콘은 밝은 색으로 고정한다.
 */
private fun ComponentActivity.enableDarkEdgeToEdge() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
    )
}

/**
 * setContentView(root) 대신 쓴다.
 * 시스템 바(상태바, 내비게이션 바), 화면 컷아웃, 키보드가 차지하는 만큼 root 에 여백(padding)을 줘서
 * 내용이 가려지지 않게 한다. root 의 배경이 여백 영역에도 그려지므로 모양은 이전과 같다.
 */
fun ComponentActivity.setContentViewEdgeToEdge(root: View) {
    enableDarkEdgeToEdge()
    setContentView(root)
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val types = WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout() or
                WindowInsetsCompat.Type.ime()
        val bars = insets.getInsets(types)
        view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
        WindowInsetsCompat.CONSUMED
    }
}

/** 가운데 로고만 있는 화면(대기화면)처럼 여백이 필요 없는 화면용: 시스템 바 설정만 맞춘다. */
fun ComponentActivity.setUpDarkSystemBars() = enableDarkEdgeToEdge()

package com.irionna.eternalreturninfo.presentation.activity.main

import androidx.appcompat.app.AppCompatActivity
import com.irionna.eternalreturninfo.util.setContentViewEdgeToEdge
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import com.irionna.eternalreturninfo.databinding.WebVewActivityBinding

class WebView : AppCompatActivity() {

    private lateinit var binding: WebVewActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WebVewActivityBinding.inflate(layoutInflater)
        setContentViewEdgeToEdge(binding.root)

        // 자바 스크립트 허용
        binding.webView.settings.javaScriptEnabled = true

        /* 웹뷰에서 새 창이 뜨지 않도록 방지하는 구문 */
        binding.webView.webViewClient = WebViewClient()
        binding.webView.webChromeClient = WebChromeClient()

        val url = intent.getStringExtra("url")

        /* 링크 주소를 로드 */
        if (url != null) {
            binding.webView.loadUrl(url)
        }

    }
}
package com.irionna.eternalreturninfo.presentation.activity.main

import androidx.appcompat.app.AppCompatActivity
import com.irionna.eternalreturninfo.util.setContentViewEdgeToEdge
import com.irionna.eternalreturninfo.data.repository.BlockedUsers
import android.content.Intent
import android.os.Bundle
import com.irionna.eternalreturninfo.presentation.activity.login.LoginActivity
import com.irionna.eternalreturninfo.util.AutoLogin
import com.irionna.eternalreturninfo.util.TermsConsent
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.MainActivityBinding
import com.irionna.eternalreturninfo.presentation.adapter.main.MainViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    lateinit var binding: MainActivityBinding
    private var auth: FirebaseAuth? = null


    private val viewPagerAdapter by lazy {
        MainViewPagerAdapter(this)
    }

    private val tabIcon = arrayListOf(
        R.drawable.ic_tab_selected_home,
        R.drawable.ic_tab_duo,
        R.drawable.ic_tab_pencil,
        R.drawable.ic_tab_chat,
        R.drawable.ic_tab_user
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        binding = MainActivityBinding.inflate(layoutInflater)
        setContentViewEdgeToEdge(binding.root)
        BlockedUsers.start()
        initView()

        // 약관 시행 전에 가입한 계정은 한 번 동의해야 계속 쓸 수 있다
        val myUid = auth?.uid
        if (!TermsConsent.isAccepted(this, myUid)) {
            TermsConsent.showGate(
                this,
                onAccepted = { myUid?.let { TermsConsent.markAccepted(this, it) } },
                onDeclined = {
                    BlockedUsers.stop()
                    FirebaseAuth.getInstance().signOut()
                    AutoLogin.clear(this)
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            )
        }
    }

    private fun initView() = with(binding) {
        viewPager.adapter = viewPagerAdapter

        // 탭을 누르면 중간 화면들을 지나가며 미끄러지지 않고 바로 그 화면으로 이동한다 (smoothScroll = false)
        TabLayoutMediator(tabLayout, viewPager, true, false) { tab, position ->
            tab.setText(viewPagerAdapter.getTitle(position))
            tab.setIcon(tabIcon[position])
        }.attach()

        viewPager.run {
            isUserInputEnabled = false
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateTabIconsAndText(tab, true)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                updateTabIconsAndText(tab, false)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

    }

    private fun updateTabIconsAndText(tab: TabLayout.Tab?, isSelected: Boolean) {
        val position = tab?.position ?: return

        // 선택된 탭의 아이콘 및 텍스트 업데이트
        when (position) {
            0 -> {
                tab.setIcon(if (isSelected) R.drawable.ic_tab_selected_home else R.drawable.ic_tab_home)
            }

            1 -> {
                tab.setIcon(if (isSelected) R.drawable.ic_tab_selected_duo else R.drawable.ic_tab_duo)
            }

            2 -> {
                tab.setIcon(if (isSelected) R.drawable.ic_tab_selected_pencil else R.drawable.ic_tab_pencil)
            }

            3 -> {
                tab.setIcon(if (isSelected) R.drawable.ic_tab_selected_chat else R.drawable.ic_tab_chat)
            }

            4 -> {
                tab.setIcon(if (isSelected) R.drawable.ic_tab_selected_user else R.drawable.ic_tab_user)
            }
        }
    }
}
package com.irionna.eternalreturninfo.presentation.adapter.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.model.MainTabs
import com.irionna.eternalreturninfo.presentation.fragment.board.BoardFragment
import com.irionna.eternalreturninfo.presentation.fragment.chat.ChatListFragment
import com.irionna.eternalreturninfo.presentation.fragment.myprofile.MyProfileFragment
import com.irionna.eternalreturninfo.presentation.fragment.findduo.FindDuoFragment
import com.irionna.eternalreturninfo.presentation.fragment.main.MainFragment

class MainViewPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(
    fragmentActivity
) {

    private val fragments = listOf(
        MainTabs({ MainFragment.newInstance() }, R.string.main_tab_main_title),
        MainTabs({ FindDuoFragment.newInstance() }, R.string.main_tab_search_duo),
        MainTabs({ BoardFragment.newInstance() }, R.string.main_tab_board_title),
        MainTabs({ ChatListFragment.newInstance() }, R.string.main_tab_chat_title),
        MainTabs({ MyProfileFragment.newInstance() }, R.string.main_tab_my_profile)
    )


    fun getTitle(position: Int): Int {
        return fragments[position].titleRes
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position].create()
    }
    override fun getItemCount(): Int {
        return fragments.size
    }

}
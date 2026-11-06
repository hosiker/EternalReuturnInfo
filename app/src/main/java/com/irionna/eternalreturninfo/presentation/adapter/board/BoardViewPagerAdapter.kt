package com.irionna.eternalreturninfo.presentation.adapter.board

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.model.MainTabs
import com.irionna.eternalreturninfo.presentation.fragment.board.BoardAskFragment
import com.irionna.eternalreturninfo.presentation.fragment.board.BoardDailyFragment
import com.irionna.eternalreturninfo.presentation.fragment.board.BoardNoticeFragment
import com.irionna.eternalreturninfo.presentation.fragment.board.BoardTipsFragment
import com.irionna.eternalreturninfo.presentation.fragment.board.BoardTotalFragment

class BoardViewPagerAdapter (fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

        private val fragments = listOf(
            MainTabs({ BoardTotalFragment.newInstance() }, R.string.board_total),
            MainTabs({ BoardNoticeFragment.newInstance() }, R.string.board_notice),
            MainTabs({ BoardAskFragment.newInstance() }, R.string.board_ask),
            MainTabs({ BoardTipsFragment.newInstance() }, R.string.board_tips),
            MainTabs({ BoardDailyFragment.newInstance() }, R.string.board_daily)
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
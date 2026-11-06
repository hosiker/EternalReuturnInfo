package com.irionna.eternalreturninfo.presentation.adapter.myprofile

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.model.MainTabs
import com.irionna.eternalreturninfo.presentation.fragment.myprofile.MyProfileBoardFragment
import com.irionna.eternalreturninfo.presentation.fragment.myprofile.MyprofileRecordFragment

class MyProfileViewPagerAdapter (fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

        private val fragments = listOf(
            MainTabs({ MyprofileRecordFragment.newInstance() }, R.string.myprofile_record),
            MainTabs({ MyProfileBoardFragment.newInstance() }, R.string.myprofile_board),
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
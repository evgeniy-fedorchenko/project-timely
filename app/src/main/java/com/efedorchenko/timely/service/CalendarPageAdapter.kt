package com.efedorchenko.timely.service

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.efedorchenko.timely.fragment.CalendarFragmentNew

class CalendarPageAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    companion object {
        const val CALENDAR_SCROLL_BORDERS = 100
        const val INITIAL_MONTH_OFFSET = 0

        fun calculateMonthOffset(rawPosition: Int): Int {
            return rawPosition - CALENDAR_SCROLL_BORDERS / 2
        }
    }

    override fun getItemCount(): Int = CALENDAR_SCROLL_BORDERS

    override fun createFragment(position: Int): Fragment {
        return CalendarFragmentNew.newInstance(calculateMonthOffset(position))

    }
}
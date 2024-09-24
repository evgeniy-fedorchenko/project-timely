package com.efedorchenko.timely.fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class CalendarPageAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    companion object {
        const val CALENDAR_SCROLL_BORDERS = 1000
    }

    override fun getItemCount(): Int = CALENDAR_SCROLL_BORDERS

    override fun createFragment(position: Int): Fragment {
        return MonthFragment.newInstance(position - CALENDAR_SCROLL_BORDERS / 2)

    }
}
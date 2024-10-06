package com.efedorchenko.timely.fragment

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.viewpager2.widget.ViewPager2
import com.efedorchenko.timely.R
import com.google.android.material.navigation.NavigationView
import com.jakewharton.threetenabp.AndroidThreeTen

class MainFragment : Fragment() {


    private lateinit var summaryFragment: SummaryFragment
    private lateinit var viewPager: ViewPager2
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.main_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.view_pager)

        setupViewPager()
        setupSummaryCard()
        setupSideMenu(view)
    }

    private fun setupViewPager() {
        AndroidThreeTen.init(requireContext())

        viewPager.adapter = this.activity?.let { CalendarPageAdapter(it) }
        viewPager.setCurrentItem(CalendarPageAdapter.CALENDAR_SCROLL_BORDERS / 2, false)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                summaryFragment.updateSummaryData()
            }
        })
    }

    private fun setupSummaryCard() {
        childFragmentManager.commit {
            setReorderingAllowed(true)
            summaryFragment = SummaryFragment()
            replace(R.id.summaryCard, summaryFragment)
        }
    }

    private fun setupSideMenu(view: View) {
        drawerLayout = view.findViewById(R.id.main_content)

        val menuButton: ImageButton = view.findViewById(R.id.menu_button)
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val navigationView: NavigationView = view.findViewById(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)

        headerView.findViewById<TextView>(R.id.user_name).text = "Федорченко Евгений Викторович"

        val positionTextView = headerView.findViewById<TextView>(R.id.position)
        val spannablePositionText = SpannableString("Должность: колотильщик паллетов")
        spannablePositionText.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            9,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        positionTextView.text = spannablePositionText

        val rateTextView = headerView.findViewById<TextView>(R.id.rate)
        val spannableRateText = SpannableString("Ставка: 200 руб./ч.")
        spannableRateText.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            6,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        rateTextView.text = spannableRateText

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.fill_period -> {
                }

                R.id.my_team -> {
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }
}

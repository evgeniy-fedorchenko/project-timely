package com.efedorchenko.timely.fragment

import android.app.Application
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
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.efedorchenko.timely.R
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.repository.FineRepository
import com.efedorchenko.timely.service.CalendarPageAdapter
import com.efedorchenko.timely.service.MainViewModel
import com.google.android.material.navigation.NavigationView
import org.threeten.bp.LocalDate

class MainFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var calendarFragment: CalendarFragment
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

        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        summaryFragment = SummaryFragment()
        calendarFragment = CalendarFragment()

        setupViewPager(view)
        setupSummaryCard()
        setupSideMenu(view)
        initFines()
    }

    private fun initFines() {
        val fineRepository = FineRepository(requireActivity().applicationContext as Application)

        fineRepository.save(
            Fine(LocalDate.parse("2024-08-01"), "some desc", 100),
            Fine(LocalDate.parse("2024-08-02"), "some desc", 200),
            Fine(LocalDate.parse("2024-08-03"), "some desc", 300),
            Fine(LocalDate.parse("2024-08-04"), "some desc", 400),
            Fine(LocalDate.parse("2024-08-05"), "some desc", 500),
            Fine(LocalDate.parse("2024-08-06"), "some desc", 600),
            Fine(LocalDate.parse("2024-09-07"), "some desc", 700),
            Fine(LocalDate.parse("2024-09-08"), "some desc", 800),
            Fine(LocalDate.parse("2024-09-09"), "some desc", 900),
            Fine(LocalDate.parse("2024-09-10"), "some desc", 1000),
            Fine(LocalDate.parse("2024-09-11"), "some desc", 1100),
            Fine(LocalDate.parse("2024-09-12"), "some desc", 1200),
            Fine(LocalDate.parse("2024-10-13"), "some desc", 1300),
            Fine(LocalDate.parse("2024-10-14"), "some desc", 1400),
            Fine(LocalDate.parse("2024-10-15"), "some desc", 1500),
            Fine(LocalDate.parse("2024-10-16"), "some desc", 1600),
            Fine(LocalDate.parse("2024-10-17"), "some desc", 1700),
            Fine(LocalDate.parse("2024-10-18"), "some desc", 1800),
            Fine(LocalDate.parse("2024-11-19"), "some desc", 1900),
            Fine(LocalDate.parse("2024-11-20"), "some desc", 2000),
            Fine(LocalDate.parse("2024-11-21"), "some desc", 2100),
            Fine(LocalDate.parse("2024-11-22"), "some desc", 2200),
            Fine(LocalDate.parse("2024-11-23"), "some desc", 2300),
            Fine(LocalDate.parse("2024-11-24"), "some desc", 2400),
            Fine(LocalDate.parse("2024-12-25"), "some desc", 2500),
            Fine(LocalDate.parse("2024-12-26"), "some desc", 2600),
            Fine(LocalDate.parse("2024-12-27"), "some desc", 2700),
            Fine(LocalDate.parse("2024-12-28"), "some desc", 2800),
            Fine(LocalDate.parse("2024-12-29"), "some desc", 2900),
            Fine(LocalDate.parse("2024-12-30"), "some desc", 3000)
        )
    }

    private fun setupViewPager(view: View) {

        viewPager = view.findViewById(R.id.view_pager)
        viewPager.adapter = this.activity?.let { CalendarPageAdapter(it) }
        viewPager.setCurrentItem(CalendarPageAdapter.CALENDAR_SCROLL_BORDERS / 2, false)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.updateSummaryData(position)
                viewModel.updateMonthOffset(position)
            }
        })
    }

    private fun setupSummaryCard() {
        childFragmentManager.commit {
            setReorderingAllowed(true)
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

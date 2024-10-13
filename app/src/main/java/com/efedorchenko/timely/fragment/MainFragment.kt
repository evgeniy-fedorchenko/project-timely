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
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.efedorchenko.timely.R
import com.efedorchenko.timely.databinding.FragmentMainBinding
import com.efedorchenko.timely.security.SecurityService
import com.efedorchenko.timely.service.CalendarPageAdapter
import com.efedorchenko.timely.service.MainViewModel
import com.google.android.material.navigation.NavigationView

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel
    private lateinit var securityService: SecurityService
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val baseContext = requireActivity()

        viewModel = ViewModelProvider(baseContext).get(MainViewModel::class.java)
        securityService = SecurityService(baseContext)

        setupViewPager()
        setupSummaryCard()
        setupSideMenu(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewPager.adapter = null
    }

    private fun setupViewPager() {
        viewPager = binding.viewPager
        viewPager.adapter = CalendarPageAdapter(requireActivity())
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
            replace(R.id.summaryCard, SummaryFragment())
        }
    }

    private fun setupSideMenu(view: View) {
        val drawerLayout = binding.mainContent
        val menuButton: ImageButton = binding.headerLayout.menuButton
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val navigationView: NavigationView = binding.navView
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
                R.id.exit -> {
                    securityService.removeToken()
                    findNavController().navigate(R.id.authFragment)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }
}

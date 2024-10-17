package com.efedorchenko.timely.fragment

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
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
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.efedorchenko.timely.R
import com.efedorchenko.timely.databinding.DialogAccessKeysBinding
import com.efedorchenko.timely.databinding.FragmentMainBinding
import com.efedorchenko.timely.security.SecurityService
import com.efedorchenko.timely.security.SecurityServiceImpl
import com.efedorchenko.timely.service.CalendarAdapter
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
        securityService = SecurityServiceImpl.getInstance(baseContext)

        setupViewPager()
        setupSummaryCard()
        setupSideMenu()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewPager.adapter = null
    }

    private fun setupViewPager() {
        viewPager = binding.viewPager
        viewPager.adapter = CalendarAdapter(requireActivity())
        viewPager.setCurrentItem(CalendarAdapter.CALENDAR_SCROLL_BORDERS / 2, false)

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

    private fun setupSideMenu() {
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

        if (securityService.isPrivileged()) {
            val accessKeysMenuItem = navigationView.menu.findItem(R.id.access_keys)
            accessKeysMenuItem.isVisible = true
        }

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

                R.id.access_keys -> {
                    if (menuItem.isVisible) {
                        showAccessKeysDialog(navigationView.context)
                    }
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    fun showAccessKeysDialog(context: Context) {
        val binding = DialogAccessKeysBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context).setView(binding.root).create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        val keys = securityService.requireAccessKeys()
        binding.workerKey.text = keys.first
        binding.adminKey.text = keys.second

        setupButtonAnimationAndClick(binding.key1CopyButton, context) {
            copyToClipboard(context, "worker_key", binding.workerKey.text.toString())
        }

        setupButtonAnimationAndClick(binding.key2CopyButton, context) {
            copyToClipboard(context, "admin_key", binding.adminKey.text.toString())
        }

        dialog.show()
    }

    private fun setupButtonAnimationAndClick(
        button: ImageButton, context: Context, onClick: () -> Unit
    ) {
        val whiteColor =  ContextCompat.getColor(context, R.color.weekend_gray)
        val blackColor =  ContextCompat.getColor(context, R.color.dark_gray)

        button.setOnClickListener {
            button.animate().scaleX(0.9f).scaleY(0.9f).setDuration(150).withEndAction {
                button.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()

            ValueAnimator.ofArgb(whiteColor, blackColor, whiteColor).apply {
                duration = 300
                addUpdateListener { animator ->
                    button.imageTintList = ColorStateList.valueOf(animator.animatedValue as Int)
                }
                doOnEnd { onClick() }
                start()
            }
        }
    }


    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}

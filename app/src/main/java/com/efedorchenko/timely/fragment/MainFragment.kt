package com.efedorchenko.timely.fragment

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.databinding.FragmentMainBinding
import com.efedorchenko.timely.fragment.support.CalendarAdapter
import com.efedorchenko.timely.fragment.support.NavigationMenuListener
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.service.ToastHelper
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment() {

    companion object {
        const val USER_UUID_ARG = "user_uuid"
    }

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModel: DataViewModel

    @Inject
    lateinit var spaceViewModel: SpaceViewModel

    @Inject
    lateinit var encProfileStorage: EncProfileStorage

    @Inject
    lateinit var profileStorage: ProfileStorage

    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        setupViewPager(null)   // Calendar scroller
        setupSummaryCard(null)   // Summary card at the bottom of screen
        setupSideMenu()   // Side navigation menu

        lifecycleScope.launch {
            spaceViewModel.selectedMember.collect {
                it?.let {
                    setupSummaryCard(it.userUuid)
                    setupViewPager(it.userUuid)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.alert.collect { ToastHelper.message(it, context) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewPager.adapter = null
    }

    private fun setupViewPager(userUuid: String?) {
        viewPager = binding.viewPager
        viewPager.adapter = CalendarAdapter(requireActivity(), userUuid)
        viewPager.setCurrentItem(CalendarAdapter.CALENDAR_SCROLL_BORDERS / 2, false)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.updateLiveData(position, userUuid)
                viewModel.updateMonthOffset(position)
            }
        })
    }

    private fun setupSummaryCard(userUuid: String?) {
        childFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.summary_card, SummaryFragment())
            if (userUuid != null) {
                runOnCommit {
                    viewModel.updateLiveData(DataType.EVENT, LocalDate.now(), userUuid)
                    viewModel.updateLiveData(DataType.FINE, LocalDate.now(), userUuid)

                }
            }
        }
    }

    private fun setupSideMenu() {
        val drawerLayout = binding.mainContent
        binding.headerLayout.menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val navigationView: NavigationView = binding.navView
        val headerView = navigationView.getHeaderView(0)

        val userData = profileStorage.getUserData()
        headerView.findViewById<TextView>(R.id.user_name).text = userData?.name

        userData?.position?.let {
            val positionRawText = getString(R.string.nav_menu_header_position, it)
            val preparedHeaderLine = prepareHeaderLine(positionRawText, 9)
            headerView.findViewById<TextView>(R.id.position).text = preparedHeaderLine
        }

        val mySpaceItem = navigationView.menu.findItem(R.id.my_space)
        val connectToSpaceItem = navigationView.menu.findItem(R.id.connect_to_space)
        val leaveToSpaceItem = navigationView.menu.findItem(R.id.leave_space)
        val accessKeysItem = navigationView.menu.findItem(R.id.access_keys)

        userData?.spaceName?.let {
            val spaceRawText = getString(R.string.nav_menu_header_space, it)
            val preparedHeaderLine = prepareHeaderLine(spaceRawText, 8)
            headerView.findViewById<TextView>(R.id.space).text = preparedHeaderLine

            if (encProfileStorage.isPrivileged()) {
                accessKeysItem.isVisible = true
            }
            mySpaceItem.isVisible = true
            leaveToSpaceItem.isVisible = true
        } ?: run { connectToSpaceItem.isVisible = true }

        userData?.rate?.let {
            val rateRawText = getString(R.string.nav_menu_header_rate, it)
            val preparedHeaderLine = prepareHeaderLine(rateRawText, 6)
            headerView.findViewById<TextView>(R.id.rate).text = preparedHeaderLine
        }

        lifecycleScope.launch {
            spaceViewModel.needSwitchSpaceItemsInSideMenu.collect { needsSwitch ->
                if (needsSwitch) {
                    if (mySpaceItem.isVisible && !connectToSpaceItem.isVisible) {
                        mySpaceItem.isVisible = false
                        leaveToSpaceItem.isVisible = false
                        connectToSpaceItem.isVisible = true
                        if (encProfileStorage.isPrivileged()) {
                            accessKeysItem.isVisible = true
                        }
                    } else if (!mySpaceItem.isVisible && connectToSpaceItem.isVisible) {
                        mySpaceItem.isVisible = true
                        leaveToSpaceItem.isVisible = true
                        connectToSpaceItem.isVisible = false
                        accessKeysItem.isVisible = false
                    }
                }
            }
        }
        // TODO: добавить кнопку "покуинуть пространство"
        navigationView.setNavigationItemSelectedListener(
            NavigationMenuListener(drawerLayout, viewModel, this)
        )
    }

    private fun prepareHeaderLine(rawText: String, boldEndPosition: Int): SpannableString {
        val spannablePositionText = SpannableString(rawText)
        spannablePositionText.setSpan(
            StyleSpan(Typeface.BOLD), 0,
            boldEndPosition, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannablePositionText
    }
}

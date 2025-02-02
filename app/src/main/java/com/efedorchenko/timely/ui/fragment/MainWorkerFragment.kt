package com.efedorchenko.timely.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.databinding.FragmentMainWorkerBinding
import com.efedorchenko.timely.model.SpaceMember
import com.efedorchenko.timely.service.ToastHelper
import com.efedorchenko.timely.ui.support.CalendarAdapter
import com.efedorchenko.timely.ui.support.FragmentUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainWorkerFragment : AbstractMainFragment() {

    private var _binding: FragmentMainWorkerBinding? = null
    private val binding get() = _binding!!

    @Inject
    override lateinit var viewModel: DataViewModel

    @Inject
    override lateinit var spaceViewModel: SpaceViewModel

    @Inject
    override lateinit var encProfileStorage: EncProfileStorage

    @Inject
    override lateinit var profileStorage: ProfileStorage

    private lateinit var viewPager: ViewPager2
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainWorkerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        if (spaceViewModel.selectedMember.value != null) {
            setupViewPager(null)   // Calendar scroller
            setupSummaryCard()   // Summary card at the bottom of screen
            super.setupSideMenu(context)   // Side navigation menu
        }

        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                spaceViewModel.selectedMember.collect {
                    it?.let {
                        setupMemberCard(it, context)
                        setupSummaryCard()
                        setupViewPager(it.userUuid)
                    } ?: run {
//                    При сбросе юзера админов отправляем обратно на свой экран
                        if (encProfileStorage.isPrivileged()) {
                            findNavController().navigate(R.id.mainBossFragment)
                            return@collect
                        }
                        binding.selectedUserInfo.visibility = View.GONE
                        setupSummaryCard()
                        setupViewPager(null)
                    }
                }
//            }
        }
        lifecycleScope.launch {
            viewModel.alert.collect { ToastHelper.message(it, context) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewPager.adapter = null
        pageChangeCallback?.let { viewPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
    }

    override fun getDrawerLayout() = binding.mainContent

    override fun getHeaderLayout() = binding.headerLayout

    override fun getNavigationView() = binding.navView

    private fun setupViewPager(userUuid: String?) {
        viewPager = binding.viewPager
        viewPager.adapter = CalendarAdapter(requireActivity(), userUuid)

        pageChangeCallback?.let { viewPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.updateLiveData(position, userUuid)
                viewModel.updateMonthOffset(position)
            }
        }.also { viewPager.registerOnPageChangeCallback(it) }

        // При установке вызывается onPageSelected, поэтому сначала обновляем колбек, потом ставим setCurrentItem
        viewPager.setCurrentItem(CalendarAdapter.CALENDAR_SCROLL_BORDERS / 2, false)
    }

    private fun setupSummaryCard() {
        childFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.summary_card, SummaryFragment())
        }
    }

    private fun setupMemberCard(member: SpaceMember, context: Context) {
        with(binding) {
            selectedUserName.text = member.name
            selectedUserPosition.text = getString(R.string.selected_user_position, member.position)
            selectedUserInfo.visibility = View.VISIBLE
            if (encProfileStorage.isPrivileged()) {
                FragmentUtils.setupButtonAnimationAndClick(selectedUserSettingsButton, context, { showUserSettings() })
                selectedUserSettingsButton.visibility = View.VISIBLE
            }
        }
    }

    private fun showUserSettings() {
        // Что происходит при нажатии на кнопку настроек просматриваемого юзера
        // binding.selectedUserSettingsButton
    }
}

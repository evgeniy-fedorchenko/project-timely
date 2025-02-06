package com.efedorchenko.timely.ui.fragment

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.databinding.HeaderLayoutBinding
import com.efedorchenko.timely.ui.support.FragmentUtils
import com.efedorchenko.timely.ui.support.NavigationMenuListener
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

abstract class AbstractMainFragment : Fragment() {

    companion object {
        const val USER_UUID_ARG = "user_uuid"
    }

    protected abstract fun getDrawerLayout(): DrawerLayout
    protected abstract fun getHeaderLayout(): HeaderLayoutBinding
    protected abstract fun getNavigationView(): NavigationView

    protected abstract val viewModel: DataViewModel
    protected abstract val spaceViewModel: SpaceViewModel
    protected abstract val encProfileStorage: EncProfileStorage
    protected abstract val profileStorage: ProfileStorage

    fun setupSideMenu(context: Context) {
        getHeaderLayout().menuButton.setOnClickListener {
            getDrawerLayout().openDrawer(GravityCompat.START)
        }
        FragmentUtils.setupButtonAnimationAndClick(getHeaderLayout().homeButton, context) {
            spaceViewModel.resetSelectedMember()
            getHeaderLayout().homeButton.visibility = View.GONE
        }

        val headerView = getNavigationView().getHeaderView(0)

        val userData = profileStorage.getUserData()
        headerView.findViewById<TextView>(R.id.user_name).text = userData?.name

        userData?.position?.let {
            val positionRawText = getString(R.string.nav_menu_header_position, it)
            val preparedHeaderLine = prepareHeaderLine(positionRawText, 9)
            headerView.findViewById<TextView>(R.id.position).text = preparedHeaderLine
        }

        val fillPeriodItem = getNavigationView().menu.findItem(R.id.fill_period)
        val mySpaceItem = getNavigationView().menu.findItem(R.id.my_space)
        val leaveSpaceItem = getNavigationView().menu.findItem(R.id.leave_space)
        val connectToSpaceItem = getNavigationView().menu.findItem(R.id.connect_to_space)
        val doSyncItem = getNavigationView().menu.findItem(R.id.do_sync)

        userData?.spaceName?.let {
            val spaceRawText = getString(R.string.nav_menu_header_space, it)
            val preparedHeaderLine = prepareHeaderLine(spaceRawText, 8)
            headerView.findViewById<TextView>(R.id.space).text = preparedHeaderLine
        }
        val onHomePage = spaceViewModel.selectedMember.value == null
        setupButtons(onHomePage, doSyncItem, fillPeriodItem, mySpaceItem, leaveSpaceItem, connectToSpaceItem)

        userData?.rate?.let {
            val rateRawText = getString(R.string.nav_menu_header_rate, it)
            val preparedHeaderLine = prepareHeaderLine(rateRawText, 6)
            headerView.findViewById<TextView>(R.id.rate).text = preparedHeaderLine
        }

        viewLifecycleOwner.lifecycleScope.launch {
            spaceViewModel.selectedMember.collect {
                getHeaderLayout().homeButton.visibility = if (it == null) View.GONE else View.VISIBLE
                val onHomeUpdated = spaceViewModel.selectedMember.value == null
                setupButtons(onHomeUpdated, doSyncItem, fillPeriodItem, mySpaceItem, leaveSpaceItem, connectToSpaceItem)

            }
        }
        lifecycleScope.launch {
            spaceViewModel.needSwitchSpaceItemsInSideMenu.collect { needsSwitch ->
                if (needsSwitch) {
                    val onHomeUpdated = spaceViewModel.selectedMember.value == null
                    setupButtons(
                        onHomeUpdated,
                        doSyncItem,
                        fillPeriodItem,
                        mySpaceItem,
                        leaveSpaceItem,
                        connectToSpaceItem
                    )
                }
            }
        }
        getNavigationView().setNavigationItemSelectedListener(
            NavigationMenuListener(getDrawerLayout(), viewModel, spaceViewModel, this)
        )
    }

    private fun setupButtons(
        onHomePage: Boolean,
        doSyncItem: MenuItem,
        fillPeriodItem: MenuItem,
        mySpaceItem: MenuItem,
        leaveSpaceItem: MenuItem,
        connectToSpaceItem: MenuItem
    ) {
//        Надо чтобы с чужого экрана нельзя было понять, что я как участник был удален
        if (!profileStorage.spaceExists()) {
            fillPeriodItem.isVisible = true
            mySpaceItem.isVisible = false
            leaveSpaceItem.isVisible = false
            connectToSpaceItem.isVisible = true
            doSyncItem.isVisible = true
            return
        }
        connectToSpaceItem.isVisible = false
        leaveSpaceItem.isVisible = true
        if (encProfileStorage.isPrivileged()) {
            if (onHomePage) {
                doSyncItem.title = "Обновить участников"
            } else {
                doSyncItem.title = "Синхронизировать данные"
                fillPeriodItem.isVisible = true
            }
            doSyncItem.isVisible = true
        } else {
            mySpaceItem.isVisible = true
            doSyncItem.isVisible = true
            fillPeriodItem.isVisible = onHomePage
        }
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
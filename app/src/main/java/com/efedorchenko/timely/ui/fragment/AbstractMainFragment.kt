package com.efedorchenko.timely.ui.fragment

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
import com.efedorchenko.timely.data.EncUserProfile
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.data.UserProfile
import com.efedorchenko.timely.databinding.HeaderLayoutBinding
import com.efedorchenko.timely.model.auth.UserData
import com.efedorchenko.timely.model.member.SpaceStatus
import com.efedorchenko.timely.ui.fragment.AbstractMainFragment.Companion.Button.CONNECT_TO_SPACE
import com.efedorchenko.timely.ui.fragment.AbstractMainFragment.Companion.Button.DO_SYNC
import com.efedorchenko.timely.ui.fragment.AbstractMainFragment.Companion.Button.FILL_PERIOD
import com.efedorchenko.timely.ui.fragment.AbstractMainFragment.Companion.Button.LEAVE_SPACE
import com.efedorchenko.timely.ui.fragment.AbstractMainFragment.Companion.Button.MY_SPACE
import com.efedorchenko.timely.ui.support.NavigationMenuListener
import com.efedorchenko.timely.ui.support.animClickListener
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

abstract class AbstractMainFragment : Fragment() {

    companion object {
        const val USER_UUID_ARG = "user_uuid"

        private enum class Button {
            FILL_PERIOD, MY_SPACE, LEAVE_SPACE, CONNECT_TO_SPACE, DO_SYNC
        }
    }

    protected abstract fun getDrawerLayout(): DrawerLayout
    protected abstract fun getHeaderLayout(): HeaderLayoutBinding
    protected abstract fun getNavigationView(): NavigationView

    protected abstract val viewModel: DataViewModel
    protected abstract val spaceViewModel: SpaceViewModel
    protected abstract val encUserProfile: EncUserProfile
    protected abstract val userProfile: UserProfile

    fun setupSideMenu() {
        getHeaderLayout().menuButton.setOnClickListener {
            getDrawerLayout().openDrawer(GravityCompat.START)
        }
        getHeaderLayout().homeButton.animClickListener {
            spaceViewModel.resetSelectedMember()
            getHeaderLayout().homeButton.visibility = View.GONE
        }
        val headerView = getNavigationView().getHeaderView(0)

        val userData = userProfile.getUserData()
        setupSideMenuHeader(headerView, userData)

        val buttons = hashMapOf(
            FILL_PERIOD to getNavigationView().menu.findItem(R.id.fill_period),
            MY_SPACE to getNavigationView().menu.findItem(R.id.my_space),
            LEAVE_SPACE to getNavigationView().menu.findItem(R.id.leave_space),
            CONNECT_TO_SPACE to getNavigationView().menu.findItem(R.id.connect_to_space),
            DO_SYNC to getNavigationView().menu.findItem(R.id.do_sync)
        )

//        setupButtons(isOnHomePage(), buttons)

        viewLifecycleOwner.lifecycleScope.launch {
            spaceViewModel.selectedMember.collect {
                getHeaderLayout().homeButton.visibility = if (it == null) View.GONE else View.VISIBLE
                setupButtons(isOnHomePage(), buttons)
            }
        }
        lifecycleScope.launch {
            spaceViewModel.needSwitchSideMenuItems.collect { needsSwitch ->
                if (needsSwitch) setupButtons(isOnHomePage(), buttons)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            spaceViewModel.statusChangedNty.collect { newStatus ->
                setSpaceStatusOnSideMenu(newStatus, headerView, userProfile.getSpaceName())
//                val spaceRawText = getString(R.string.nav_menu_header_space, "На рассмотрении async")
//                headerView.findViewById<TextView>(R.id.space).text = prepareHeaderLine(spaceRawText, 8)
            }
        }
        getNavigationView().setNavigationItemSelectedListener(
            NavigationMenuListener(getDrawerLayout(), viewModel, spaceViewModel, this)
        )
    }

    private fun setupSideMenuHeader(headerView: View, userData: UserData?) {

        val userNameView = headerView.findViewById<TextView>(R.id.user_name)
        userNameView.text = userData?.name
        if (userData == null) return

        val userPositionView = headerView.findViewById<TextView>(R.id.user_position)
        val userRateView = headerView.findViewById<TextView>(R.id.rate)

        val positionRawText = getString(R.string.nav_menu_header_position, userData.position)
        val preparedHeaderLine = prepareHeaderLine(positionRawText, 9)
        userPositionView.text = preparedHeaderLine

        setSpaceStatusOnSideMenu(userData.spaceStatus, headerView, userData.spaceName)

        val userRate = userData.rate?.let { "$it руб./ч." } ?: "Не указана"
        val rateRawText = getString(R.string.nav_menu_header_rate, userRate)
        userRateView.text = prepareHeaderLine(rateRawText, 6)
    }

    private fun setSpaceStatusOnSideMenu(spaceStatus: SpaceStatus, headerView: View, spaceName: String?) {
        val spaceNameView = headerView.findViewById<TextView>(R.id.space)
        if (spaceStatus == SpaceStatus.NONE) {
            spaceNameView.visibility = View.GONE
        } else {
            spaceNameView.visibility = View.VISIBLE
            val companyName = (if (spaceStatus.isPending()) "На рассмотрении" else spaceName)
            val spaceRawText = getString(R.string.nav_menu_header_space, companyName)
            spaceNameView.text = prepareHeaderLine(spaceRawText, 8)
        }
    }

    private fun setupButtons(onHomePage: Boolean, buttons: Map<Button, MenuItem>) {
//        Надо чтобы с чужого экрана нельзя было понять, что я как участник был удален
        if (!userProfile.spaceExists()) {
            buttons[FILL_PERIOD]?.isVisible = true
            buttons[MY_SPACE]?.isVisible = false
            buttons[LEAVE_SPACE]?.isVisible = false
            buttons[CONNECT_TO_SPACE]?.isVisible = true
            buttons[DO_SYNC]?.isVisible = true
            return
        }
        buttons[CONNECT_TO_SPACE]?.isVisible = false
        buttons[LEAVE_SPACE]?.isVisible = true
        if (encUserProfile.isPrivileged()) {
            if (onHomePage) {
                buttons[DO_SYNC]?.title = "Обновить участников"
            } else {
                buttons[DO_SYNC]?.title = "Синхронизировать данные"
                buttons[FILL_PERIOD]?.isVisible = true
            }
            buttons[DO_SYNC]?.isVisible = true
        } else {
            buttons[MY_SPACE]?.isVisible = true
            buttons[DO_SYNC]?.isVisible = true
            buttons[FILL_PERIOD]?.isVisible = onHomePage
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

    private fun isOnHomePage() = spaceViewModel.selectedMember.value == null
}
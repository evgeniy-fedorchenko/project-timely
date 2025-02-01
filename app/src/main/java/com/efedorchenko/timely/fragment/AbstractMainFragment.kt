package com.efedorchenko.timely.fragment

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.databinding.HeaderLayoutBinding
import com.efedorchenko.timely.fragment.support.FragmentUtils
import com.efedorchenko.timely.fragment.support.NavigationMenuListener
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
        FragmentUtils.setupButtonAnimationAndClick(getHeaderLayout().homeButton, context, {
            spaceViewModel.resetSelectedMember()
            getHeaderLayout().homeButton.visibility = View.GONE
        }, ContextCompat.getColor(context, R.color.orange))

        val headerView = getNavigationView().getHeaderView(0)

        val userData = profileStorage.getUserData()
        headerView.findViewById<TextView>(R.id.user_name).text = userData?.name

        userData?.position?.let {
            val positionRawText = getString(R.string.nav_menu_header_position, it)
            val preparedHeaderLine = prepareHeaderLine(positionRawText, 9)
            headerView.findViewById<TextView>(R.id.position).text = preparedHeaderLine
        }

        val mySpaceItem = getNavigationView().menu.findItem(R.id.my_space)
        val connectToSpaceItem = getNavigationView().menu.findItem(R.id.connect_to_space)
        val leaveToSpaceItem = getNavigationView().menu.findItem(R.id.leave_space)

        userData?.spaceName?.let {
            val spaceRawText = getString(R.string.nav_menu_header_space, it)
            val preparedHeaderLine = prepareHeaderLine(spaceRawText, 8)
            headerView.findViewById<TextView>(R.id.space).text = preparedHeaderLine

            mySpaceItem.isVisible = true
            leaveToSpaceItem.isVisible = true
        } ?: run { connectToSpaceItem.isVisible = true }

        userData?.rate?.let {
            val rateRawText = getString(R.string.nav_menu_header_rate, it)
            val preparedHeaderLine = prepareHeaderLine(rateRawText, 6)
            headerView.findViewById<TextView>(R.id.rate).text = preparedHeaderLine
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                spaceViewModel.selectedMember.collect {
                    getHeaderLayout().homeButton.visibility = if (it == null) View.GONE else View.VISIBLE
                }
            }
        }
        lifecycleScope.launch {
            spaceViewModel.needSwitchSpaceItemsInSideMenu.collect { needsSwitch ->
                if (needsSwitch) {
                    if (mySpaceItem.isVisible && !connectToSpaceItem.isVisible) {
                        mySpaceItem.isVisible = false
                        leaveToSpaceItem.isVisible = false
                        connectToSpaceItem.isVisible = true
                    } else if (!mySpaceItem.isVisible && connectToSpaceItem.isVisible) {
                        mySpaceItem.isVisible = true
                        leaveToSpaceItem.isVisible = true
                        connectToSpaceItem.isVisible = false
                    }
                }
            }
        }
        getNavigationView().setNavigationItemSelectedListener(
            NavigationMenuListener(getDrawerLayout(), viewModel, spaceViewModel, this)
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
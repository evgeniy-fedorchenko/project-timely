package com.efedorchenko.timely.ui.support

import android.view.MenuItem
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncUserProfile
import com.efedorchenko.timely.data.EncUserProfileImpl
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.data.UserProfile
import com.efedorchenko.timely.data.UserProfileImpl
import com.efedorchenko.timely.ui.dialog.ConnectSpaceDialogFragment
import com.efedorchenko.timely.ui.dialog.LeaveSpaceDialogFragment
import com.efedorchenko.timely.ui.dialog.SpaceDialogFragment
import com.efedorchenko.timely.ui.dialog.SyncDialogFragment
import com.google.android.material.navigation.NavigationView

// FIXME: подумать, как отдать сборку на di
class NavigationMenuListener(
    private val drawerLayout: DrawerLayout,
    private val viewModel: DataViewModel,
    private val spaceViewModel: SpaceViewModel,
    private val parentFragment: Fragment
) : NavigationView.OnNavigationItemSelectedListener {

    companion object {
        private const val SYNC_DIALOG_TAG = "sync_dialog"
        private const val SPACE_DIALOG_TAG = "space_dialog"
        private const val CONNECT_SPACE_DIALOG_TAG = "connect_space_dialog"
        private const val LEAVE_SPACE_DIALOG_TAG = "leave_space_dialog"
    }

    private val userProfile: UserProfile = UserProfileImpl(parentFragment.requireContext())
    private val encUserProfile: EncUserProfile = EncUserProfileImpl(parentFragment.requireContext())

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.fill_period -> {}
            R.id.do_sync -> SyncDialogFragment().show(parentFragment.childFragmentManager, SYNC_DIALOG_TAG)
            R.id.my_space -> SpaceDialogFragment.newInstance(SpaceDialogFragment.Companion.State.MEMBERS)
                .show(parentFragment.childFragmentManager, SPACE_DIALOG_TAG)

            R.id.leave_space -> {
                if (item.isVisible) {
                    LeaveSpaceDialogFragment().show(parentFragment.childFragmentManager, LEAVE_SPACE_DIALOG_TAG)
                }
            }

            R.id.connect_to_space -> {
                if (item.isVisible) {
                    ConnectSpaceDialogFragment().show(parentFragment.childFragmentManager, CONNECT_SPACE_DIALOG_TAG)
                }
            }

            R.id.exit -> {
                userProfile.deleteUserData()
                encUserProfile.deleteAuthData()
                viewModel.cleanAll()
                spaceViewModel.cleanAll()
                val navController = parentFragment.findNavController()
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(navController.graph.id, true)
                    .build()
                navController.navigate(R.id.authFragment, null, navOptions)
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
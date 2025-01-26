package com.efedorchenko.timely.fragment.support

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater.from
import android.view.MenuItem
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.EncProfileStorageImpl
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.data.ProfileStorageImpl
import com.efedorchenko.timely.databinding.DialogAccessKeysBinding
import com.efedorchenko.timely.fragment.dialog.ConnectSpaceDialogFragment
import com.efedorchenko.timely.fragment.dialog.LeaveSpaceDialogFragment
import com.efedorchenko.timely.fragment.dialog.SpaceDialogFragment
import com.efedorchenko.timely.fragment.dialog.SyncDialogFragment
import com.efedorchenko.timely.service.ToastHelper
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

    private val profileStorage: ProfileStorage = ProfileStorageImpl(parentFragment.requireContext())
    private val encProfileStorage: EncProfileStorage = EncProfileStorageImpl(parentFragment.requireContext())

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val context = parentFragment.requireContext()
        when (item.itemId) {
            R.id.fill_period -> {}
            R.id.do_sync -> SyncDialogFragment().show(parentFragment.childFragmentManager, SYNC_DIALOG_TAG)
            R.id.my_space -> SpaceDialogFragment().show(parentFragment.childFragmentManager, SPACE_DIALOG_TAG)
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
                profileStorage.deleteUserData()
                encProfileStorage.deleteAuthData()
                viewModel.cleanAll()
                spaceViewModel.cleanAll()
                parentFragment.findNavController().navigate(R.id.authFragment)
            }
            R.id.access_keys -> {
                if (item.isVisible) {
                    showAccessKeysDialog(context)
                }
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showAccessKeysDialog(context: Context) {
        val binding = DialogAccessKeysBinding.inflate(from(context))
        val dialog = AlertDialog.Builder(context).setView(binding.root).create()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val keys = encProfileStorage.getSpaceKeys()
        binding.workerKey.text = keys?.workerKey
        binding.bossKey.text = keys?.bossKey

        FragmentUtils.setupButtonAnimationAndClick(binding.workerKeyCopyButton, context) {
            copyToClipboard(context, "worker_key", binding.workerKey.text.toString())
        }
        FragmentUtils.setupButtonAnimationAndClick(binding.bossKeyCopyButton, context) {
            copyToClipboard(context, "boss_key", binding.bossKey.text.toString())
        }
        dialog.show()
    }

    private fun copyToClipboard(context: Context, label: String, text: String) {
        ToastHelper.keyCopied(context)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}
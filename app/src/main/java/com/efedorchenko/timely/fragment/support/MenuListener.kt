package com.efedorchenko.timely.fragment.support

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater.from
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.EncProfileStorageImpl
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.data.ProfileStorageImpl
import com.efedorchenko.timely.databinding.DialogAccessKeysBinding
import com.efedorchenko.timely.databinding.DialogSyncingDataBinding
import com.efedorchenko.timely.fragment.dialog.SpaceDialogFragment
import com.efedorchenko.timely.service.ToastHelper
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MenuListener(
    private val drawerLayout: DrawerLayout,
    private val viewModel: DataViewModel,
    private val parentFragment: Fragment
) : NavigationView.OnNavigationItemSelectedListener {

    private val profileStorage: ProfileStorage = ProfileStorageImpl(parentFragment.requireContext())
    private val encProfileStorage: EncProfileStorage = EncProfileStorageImpl(parentFragment.requireContext())

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val context = parentFragment.requireContext()
        when (item.itemId) {
            R.id.fill_period -> {}

            R.id.do_sync -> doSync(context)

            // TODO: переименовать team в space
            R.id.my_team -> SpaceDialogFragment().show(parentFragment.childFragmentManager, "SpaceDialog")

            R.id.exit -> {
                // TODO: очищать DataRepository
                profileStorage.deleteUserData()
                encProfileStorage.deleteAuthData()
                viewModel.cleanAll()
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

    private fun doSync(context: Context) {
        var eventsOutOfSync = viewModel.getNotSyncedEvents()
        var finesOutOfSync = viewModel.getNotSyncedFine()

        if ((eventsOutOfSync.size + finesOutOfSync.size) < 1) {
            ToastHelper.message(ToastHelper.ALL_SYNCED, context)
            return
        }
        // TODO: когда работает начальник и ставит смены работникам - если он переключается на другого работника и при этом есть неотправленные смены говорить ему чтобы отправил иначе они потеряются, потому что бд очистится

        val dialogBinding = DialogSyncingDataBinding.inflate(from(context))
        if (eventsOutOfSync.isNotEmpty()) {
            dialogBinding.eventsCount.text =
                parentFragment.getString(R.string.found_not_synced_events, eventsOutOfSync.size)
        }
        if (finesOutOfSync.isNotEmpty()) {
            dialogBinding.finesCount.text =
                parentFragment.getString(R.string.found_not_synced_fines, finesOutOfSync.size)
        }
        val syncingDialog = showSyncingDialog(dialogBinding, context)

        var syncJob: Job? = null
        dialogBinding.doSyncButton.setOnClickListener {

            if (syncJob?.isActive == true) {
                syncJob?.cancel()
                dialogBinding.doSyncButton.text = "Синхронизировать"
                dialogBinding.loadingProgressBar.visibility = View.INVISIBLE
                return@setOnClickListener
            }
            dialogBinding.doSyncButton.text = "Остановить"
            dialogBinding.loadingProgressBar.visibility = View.VISIBLE

            eventsOutOfSync = viewModel.getNotSyncedEvents()
            finesOutOfSync = viewModel.getNotSyncedFine()
            var eventsNotSyncSize = eventsOutOfSync.size
            var finesNotSyncSize = finesOutOfSync.size
            if ((eventsNotSyncSize + finesNotSyncSize) < 1) {
                ToastHelper.message(ToastHelper.ALL_SYNCED, context)
                dialogBinding.doSyncButton.text = "Синхронизировать"
                dialogBinding.loadingProgressBar.visibility = View.INVISIBLE
                syncingDialog?.cancel()
                return@setOnClickListener
            }

            syncJob = parentFragment.lifecycleScope.launch {
                try {
                    eventsOutOfSync.forEach {
                        if (!isActive) return@launch
                        if (viewModel.sendData(it)) {
                            dialogBinding.eventsCount.text =
                                parentFragment.getString(R.string.found_not_synced_events, --eventsNotSyncSize)
                        }
                    }
                    finesOutOfSync.forEach {
                        if (!isActive) return@launch
                        if (viewModel.sendData(it)) {
                            dialogBinding.finesCount.text =
                                parentFragment.getString(R.string.found_not_synced_fines, --finesNotSyncSize)
                        }
                    }
                } finally {
                    dialogBinding.loadingProgressBar.visibility = View.INVISIBLE
                    dialogBinding.doSyncButton.text = "Синхронизировать"
                    if (eventsNotSyncSize == 0 && finesNotSyncSize == 0) {
                        ToastHelper.message(ToastHelper.ALL_SYNCED, context)
                        syncingDialog?.cancel()
                    } else {
                        if (isActive) {
                            ToastHelper.syncFiled(eventsNotSyncSize, finesNotSyncSize, context)
                        }
                    }
                }
            }
        }
    }

    private fun showAccessKeysDialog(context: Context) {
        val binding = DialogAccessKeysBinding.inflate(from(context))
        val dialog = AlertDialog.Builder(context).setView(binding.root).create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        val keys = encProfileStorage.getSpaceKeys()
        binding.workerKey.text = keys?.workerKey
        binding.bossKey.text = keys?.bossKey

        setupButtonAnimationAndClick(binding.key1CopyButton, context) {
            copyToClipboard(context, "worker_key", binding.workerKey.text.toString())
        }
        setupButtonAnimationAndClick(binding.key2CopyButton, context) {
            copyToClipboard(context, "boss_key", binding.bossKey.text.toString())
        }

        dialog.show()
    }

    private fun setupButtonAnimationAndClick(
        button: ImageButton, context: Context, onClick: () -> Unit
    ) {
        val whiteColor = ContextCompat.getColor(context, R.color.weekend_gray)
        val blackColor = ContextCompat.getColor(context, R.color.dark_gray)

        button.setOnClickListener {
            ToastHelper.keyCopied(context)
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

    private fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    private fun showSyncingDialog(viewBinding: ViewBinding, context: Context): AlertDialog? {
        val dialog = AlertDialog.Builder(context).setView(viewBinding.root).create()
        val window = dialog.window
        window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
        return dialog
    }
}
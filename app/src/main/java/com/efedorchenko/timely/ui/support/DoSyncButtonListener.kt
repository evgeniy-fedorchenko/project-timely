package com.efedorchenko.timely.ui.support

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.databinding.DialogDetachedFromSpaceBinding
import com.efedorchenko.timely.databinding.DialogSyncingDataBinding
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.model.SaveResult
import com.efedorchenko.timely.model.SyncProcess
import com.efedorchenko.timely.model.SyncProcess.UpdateResult
import com.efedorchenko.timely.service.DataService
import com.efedorchenko.timely.service.SpaceService
import com.efedorchenko.timely.service.ToastHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DoSyncButtonListener(
    private val parent: DialogFragment,
    private val spaceService: SpaceService,
    private val dataService: DataService,
    private val parentBinding: DialogSyncingDataBinding,
    private val profileStorage: ProfileStorage,
    private val viewModel: DataViewModel,
    private val spaceViewModel: SpaceViewModel
) : View.OnClickListener {

    private var syncJob: Job? = null

    override fun onClick(v: View?) {
        if (cancelIfActive()) return

        parentBinding.doSyncButton.text = parent.getString(R.string.sync_button_stop)
        parentBinding.loadingProgressBar.visibility = View.VISIBLE

        val syncProcess = SyncProcess(viewModel.getNotSynced(DataType.EVENT), viewModel.getNotSynced(DataType.FINE))

        syncJob = parent.lifecycleScope.launch {
            try {

                doSync(syncProcess, this)

            } catch (ex: Exception) {
                Log.e("SyncError", "Sync failed", ex)
                parent.context?.let { ToastHelper.message(ToastHelper.NOT_SYNCED, it) }

            } finally {
                parentBinding.loadingProgressBar.visibility = View.INVISIBLE
                parentBinding.doSyncButton.text = parent.getString(R.string.sync_button_sync)
                val context = parent.context
                if (syncProcess.isSuccess()) {
                    context?.let { ToastHelper.message(ToastHelper.ALL_SYNCED, it) }
                    parent.dismiss()
                } else {
                    if (isActive) {
                        val result = syncProcess.getResult()
                        context?.let { ToastHelper.syncFiled(result, it) }
                        if (result.isRemoteMembersAccepted == UpdateResult.NOT_CONSIST_IN_SPACE) {
                            parent.dismiss()
                            context?.let { showDialogDetachedFromSpace(it) }
                            profileStorage.deleteSpace()
                            spaceViewModel.cleanAll()
                            spaceViewModel.needSwitchSpaceItemsInSideMenu()
                            // Удалить всех участников из таблиц events и fines для участников
                        }
                    }
                }
            }
        }
    }

    private suspend fun doSync(syncProcess: SyncProcess, coroutineScope: CoroutineScope) {
        val notSyncedEventsPattern = parent.getString(R.string.found_not_synced_events)
        syncProcess.eventsOutOfSync?.forEach {
            if (!coroutineScope.isActive) return@doSync
            if (dataService.sendData(it) == SaveResult.Success) {
                parentBinding.eventsCount.text = String.format(notSyncedEventsPattern, syncProcess.eventsDec())
            }
        }
        val notSyncedFinesPattern = parent.getString(R.string.found_not_synced_fines)
        syncProcess.finesOutOfSync?.forEach {
            if (!coroutineScope.isActive) return@doSync
            if (dataService.sendData(it) == SaveResult.Success) {
                parentBinding.finesCount.text = String.format(notSyncedFinesPattern, syncProcess.finesDec())
            }
        }
        syncProcess.isRemoteDataAccepted = dataService.updateData(null)
        if (profileStorage.spaceExists()) {
            syncProcess.isRemoteMembersAccepted = spaceService.updateMembers()
        }
    }

    private fun cancelIfActive(): Boolean {
        if (syncJob?.isActive == true) {
            syncJob?.cancel()
            parentBinding.doSyncButton.text = parent.getString(R.string.sync_button_sync)
            parentBinding.loadingProgressBar.visibility = View.INVISIBLE
            return true
        }
        return false
    }

    private fun showDialogDetachedFromSpace(context: Context) {
        val dBinding = DialogDetachedFromSpaceBinding.inflate(LayoutInflater.from(context))
        val text = dBinding.youDetachedHeader.text.toString()
        dBinding.youDetachedHeader.text = String.format(text, profileStorage.getSpaceName())
        val dialog = AlertDialog.Builder(context).setView(dBinding.root).create()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (parent.resources.displayMetrics.widthPixels * 0.85).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog.show()
    }
}

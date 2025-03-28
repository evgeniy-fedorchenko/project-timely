package com.efedorchenko.timely.ui.support

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncUserProfile
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.data.UserProfile
import com.efedorchenko.timely.databinding.DialogDetachedFromSpaceBinding
import com.efedorchenko.timely.databinding.DialogJoinRequestAcceptedBinding
import com.efedorchenko.timely.databinding.DialogJoinRequestRejectedBinding
import com.efedorchenko.timely.databinding.DialogSyncingDataBinding
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.model.SaveResult
import com.efedorchenko.timely.model.SyncOperator
import com.efedorchenko.timely.model.SyncOperator.UpdateResult
import com.efedorchenko.timely.model.member.SpaceStatus
import com.efedorchenko.timely.service.DataService
import com.efedorchenko.timely.service.SpaceService
import com.efedorchenko.timely.service.ToastHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DataSynchronizer(
    private val parent: DialogFragment? = null,
    private val spaceService: SpaceService,
    private val dataService: DataService,
    private val parentBinding: DialogSyncingDataBinding? = null,
    private val userProfile: UserProfile,
    private val encUserProfile: EncUserProfile,
    private val viewModel: DataViewModel,
    private val spaceViewModel: SpaceViewModel
) : View.OnClickListener {

    private var syncJob: Job? = null

//    Синхронизация в фоне
    suspend fun syncBackground(scope: CoroutineScope, context: Context) {
        performSync(context, scope, isUiMode = false)
    }

//    Синхронизация на UI
    override fun onClick(v: View?) {
        if (cancelIfActive()) return
        parentBinding?.apply {
            doSyncButton.text = parent?.getString(R.string.sync_button_stop)
            loadingProgressBar.visibility = View.VISIBLE
        }
        val context = parent?.context ?: return
        syncJob = parent.lifecycleScope.launch {
            performSync(context, this, isUiMode = true)
        }
    }

    private suspend fun performSync(context: Context, scope: CoroutineScope, isUiMode: Boolean) {
        val srcSpaceStatus = userProfile.getSpaceStatus()
        val isAdmin = encUserProfile.isPrivileged()

        val syncOperator = SyncOperator(
            eventsOutOfSync = viewModel.getNotSynced(DataType.EVENT, isAdmin),
            finesOutOfSync = viewModel.getNotSynced(DataType.FINE, isAdmin),
            executor = { process, currentScope -> doSync(process, currentScope, isUiMode) },
            failureHandler = { result: SyncOperator.Result -> processFailure(result, context, isUiMode) },
            successHandler = { processSuccess(srcSpaceStatus, context, isUiMode) }
        )

        try {

            syncOperator.start(scope)

        } catch (ex: Exception) {
            Log.e("SyncError", "Sync failed", ex)
            ToastHelper.message(ToastHelper.NOT_SYNCED, context)

        } finally {
            if (isUiMode) {
                parentBinding?.apply {
                    loadingProgressBar.visibility = View.INVISIBLE
                    doSyncButton.text = parent?.getString(R.string.sync_button_sync)
                }
            }
            syncOperator.handleResult(scope.isActive)
        }
    }

    private fun processFailure(result: SyncOperator.Result, context: Context, isUiMode: Boolean) {
        ToastHelper.syncFiled(result, context)
        if (result.getRemoteMembersResult == UpdateResult.NOT_CONSIST_IN_SPACE) {
            if (isUiMode) parent?.dismiss()
            showDialogSpaceStatusChanged(context, SpaceStatusChangingType.DETACHED)
            userProfile.detachFromSpace()
            spaceViewModel.detachFromSpace()
            // Удалить всех участников из таблиц events и fines для участников
        }
    }

    private fun processSuccess(srcSpaceStatus: SpaceStatus, context: Context, isUiMode: Boolean) {
        ToastHelper.message(ToastHelper.ALL_SYNCED, context)
        if (isUiMode) parent?.dismiss()

        if (srcSpaceStatus.isPending()) {
            when (userProfile.getSpaceStatus()) {
                SpaceStatus.NONE -> {
                    showDialogSpaceStatusChanged(context, SpaceStatusChangingType.REJECTED)
                    spaceViewModel.emitStatusChanged(SpaceStatus.NONE)
                }

                SpaceStatus.MEMBER -> {
                    showDialogSpaceStatusChanged(context, SpaceStatusChangingType.ACCEPTED)
                    spaceViewModel.emitStatusChanged(SpaceStatus.MEMBER)
                }

                else -> {}
            }
        }
    }

    private suspend fun doSync(syncOperator: SyncOperator, coroutineScope: CoroutineScope, isUiMode: Boolean) {
        val notSyncedEventsPattern = if (isUiMode) parent?.getString(R.string.found_not_synced_events) else null
        syncOperator.eventsOutOfSync?.forEach {
            if (!coroutineScope.isActive) return@doSync
            if (dataService.sendData(it) == SaveResult.Success) {
                if (isUiMode) {
                    notSyncedEventsPattern?.let { pattern ->
                        parentBinding?.eventsCount?.text = String.format(pattern, syncOperator.eventsDec())
                    }
                } else {
                    syncOperator.eventsDec()
                }
            }
        }
        val notSyncedFinesPattern = if (isUiMode) parent?.getString(R.string.found_not_synced_fines) else null
        syncOperator.finesOutOfSync?.forEach {
            if (!coroutineScope.isActive) return@doSync
            if (dataService.sendData(it) == SaveResult.Success) {
                if (isUiMode) {
                    notSyncedFinesPattern?.let { pattern ->
                        parentBinding?.finesCount?.text = String.format(pattern, syncOperator.finesDec())
                    }
                } else {
                    syncOperator.finesDec()
                }
            }
        }
        syncOperator.isRemoteDataAccepted = dataService.updateData(null)
        val withJoinRequests = encUserProfile.isPrivileged()
        syncOperator.getRemoteMembersResult = spaceService.updateMembers(withJoinRequests)
    }

    private fun cancelIfActive(): Boolean {
        if (syncJob?.isActive == true) {
            syncJob?.cancel()
            parentBinding?.apply {
                doSyncButton.text = parent?.getString(R.string.sync_button_sync)
                loadingProgressBar.visibility = View.INVISIBLE
            }
            return true
        }
        return false
    }

    private fun showDialogSpaceStatusChanged(context: Context, newStatus: SpaceStatusChangingType) {
        val (companyNameView, rootView) = when (newStatus) {
            SpaceStatusChangingType.DETACHED -> {
                val binding = DialogDetachedFromSpaceBinding.inflate(LayoutInflater.from(context))
                Pair(binding.companyName, binding.root)
            }
            SpaceStatusChangingType.REJECTED -> {
                val binding = DialogJoinRequestRejectedBinding.inflate(LayoutInflater.from(context))
                Pair(binding.companyName, binding.root)
            }
            SpaceStatusChangingType.ACCEPTED -> {
                val binding = DialogJoinRequestAcceptedBinding.inflate(LayoutInflater.from(context))
                Pair(binding.companyName, binding.root)
            }
        }

        companyNameView.text = String.format(companyNameView.text.toString(), userProfile.getSpaceName())

        Handler(Looper.getMainLooper()).post {
            if (context is Activity && !context.isFinishing && !context.isDestroyed) {
                val dialog = AlertDialog.Builder(context).setView(rootView).create()
                dialog.window?.apply {
                    setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    val resources = parent?.resources ?: context.resources
                    setLayout(
                        (resources.displayMetrics.widthPixels * 0.85).toInt(),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                dialog.show()
            } else {
                Log.d("DataSynchronizer",
                    "Cannot show dialog of new status [$newStatus] for user [${encUserProfile.getUserUuid()}]:" +
                            " invalid context state")
            }
        }
    }

    private enum class SpaceStatusChangingType {
       DETACHED, REJECTED, ACCEPTED
    }
}

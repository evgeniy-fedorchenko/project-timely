package com.efedorchenko.timely.fragment.support

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.databinding.DialogDetachedFromSpaceBinding
import com.efedorchenko.timely.databinding.DialogSyncingDataBinding
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.service.SpaceService
import com.efedorchenko.timely.service.SpaceServiceImpl.UpdateResult
import com.efedorchenko.timely.service.ToastHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DoSyncButtonListener(
    private val parent: DialogFragment,
    private val spaceService: SpaceService,
    private val parentBinding: DialogSyncingDataBinding,
    private val viewModel: DataViewModel,
    private val profileStorage: ProfileStorage
) : View.OnClickListener {

    private var syncJob: Job? = null

    override fun onClick(v: View?) {
        if (cancelIfActive()) return

        parentBinding.doSyncButton.text = parent.getString(R.string.sync_button_stop)
        parentBinding.loadingProgressBar.visibility = View.VISIBLE

        val eventsOutOfSync = viewModel.getNotSyncedEvents()
        val finesOutOfSync = viewModel.getNotSyncedFine()
        var eventsNotSyncSize = eventsOutOfSync.size
        var finesNotSyncSize = finesOutOfSync.size
        var downloadResult: UpdateResult? = null

        syncJob = parent.lifecycleScope.launch {
            try {

                doSyncLocalData(eventsOutOfSync, finesOutOfSync, this)?.let {
                    eventsNotSyncSize = it.first
                    finesNotSyncSize = it.second
                }

                downloadResult = spaceService.updateData(null, profileStorage.spaceExists())

            } finally {
                parentBinding.loadingProgressBar.visibility = View.INVISIBLE
                parentBinding.doSyncButton.text = parent.getString(R.string.sync_button_sync)
                val context = parent.requireContext()
                if (eventsNotSyncSize == 0 && finesNotSyncSize == 0 && downloadResult == UpdateResult.SUCCESS) {
                    ToastHelper.message(ToastHelper.ALL_SYNCED, context)
                    parent.dismiss()
                } else {
                    if (isActive) {
                        ToastHelper.syncFiled(eventsNotSyncSize, finesNotSyncSize, downloadResult, context)
                        if (downloadResult == UpdateResult.NOT_CONSIST_IN_SPACE) {
                            showDialogDetachedFromSpace(context)
                            profileStorage.deleteSpace()
                            viewModel.deleteMembers()
                            // Удалить всех участников из таблиц events и fines для участников
                        }
                    }
                }
            }
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

    /**
     * Отправить данные, которые еще не были отправлены на бек
     *
     * После получения успеха данные будут помечены как отправленные (устнаовиться `backend_id` в БД).
     * Если отправляемые данные неконсистентны с БД сервера - за истину принимаются данные сервера
     */
    private suspend fun doSyncLocalData(
        events: List<Event>, fines: List<Fine>, coroutineScope: CoroutineScope
    ): Pair<Int, Int>? {

        var eventsCount = events.size
        var finesCount = fines.size
        val notSyncedEventsPattern = parent.getString(R.string.found_not_synced_events)
        events.forEach {
            if (!coroutineScope.isActive) return@doSyncLocalData null
            if (viewModel.sendData(it)) {
                parentBinding.eventsCount.text = String.format(notSyncedEventsPattern, --eventsCount)
            }
        }
        val notSyncedFinesPattern = parent.getString(R.string.found_not_synced_fines)
        fines.forEach {
            if (!coroutineScope.isActive) return@doSyncLocalData null
            if (viewModel.sendData(it)) {
                parentBinding.finesCount.text = String.format(notSyncedFinesPattern, --finesCount)
            }
        }
        return Pair(eventsCount, finesCount)
    }

    private fun showDialogDetachedFromSpace(context: Context) {
        val dBinding = DialogDetachedFromSpaceBinding.inflate(LayoutInflater.from(context))
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

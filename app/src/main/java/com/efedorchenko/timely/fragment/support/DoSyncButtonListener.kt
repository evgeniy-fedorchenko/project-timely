package com.efedorchenko.timely.fragment.support

import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.databinding.DialogSyncingDataBinding
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.service.ToastHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DoSyncButtonListener(
    private val parent: DialogFragment,
    private val parentBinding: DialogSyncingDataBinding,
    private val viewModel: DataViewModel
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

        syncJob = parent.lifecycleScope.launch {
            try {

                doSyncLocalData(eventsOutOfSync, finesOutOfSync, this)?.let {
                    eventsNotSyncSize = it.first
                    finesNotSyncSize = it.second
                }
                val downloadResult = downloadNewData()

            } finally {
                parentBinding.loadingProgressBar.visibility = View.INVISIBLE
                parentBinding.doSyncButton.text = parent.getString(R.string.sync_button_sync)
                val context = parent.requireContext()
                if (eventsNotSyncSize == 0 && finesNotSyncSize == 0) {  // Так же чекнуть ошибки получения данных
                    ToastHelper.message(ToastHelper.ALL_SYNCED, context)
                    parent.dismiss()
                } else {
                    if (isActive) {
                        ToastHelper.syncFiled(eventsNotSyncSize, finesNotSyncSize, context)
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

    /**
     * Запросить новые данные с сервера
     *
     * Отправляется наивысший `backend_id`, в ответе приходят все события после него - все они новые.
     * Полученные данные сохраняются и обновляются их `LiveData`.
     * По очереди для каждого типа данных: `Event`, `Fine`, `SpaceMember`
     */
    private suspend fun downloadNewData(): Boolean {
        return true   // stub
    }
}

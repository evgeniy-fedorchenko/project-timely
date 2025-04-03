package com.efedorchenko.timely.ui.support

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
import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.model.SaveResult
import com.efedorchenko.timely.model.SyncOperator
import com.efedorchenko.timely.model.SyncOperator.UpdateResult
import com.efedorchenko.timely.model.member.SpaceStatus
import com.efedorchenko.timely.model.member.SpaceStatus.MEMBER
import com.efedorchenko.timely.model.member.SpaceStatus.NONE
import com.efedorchenko.timely.model.member.SpaceStatus.PENDING_BOSS
import com.efedorchenko.timely.model.member.SpaceStatus.PENDING_WORKER
import com.efedorchenko.timely.service.DataService
import com.efedorchenko.timely.service.SpaceService
import com.efedorchenko.timely.service.ToastHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Класс для выполнения синхронизации данных с сервером. Шаги синхронизаци:
 * - Отправка локально сохраненных смен и штрафов, если такие есть
 * - Обновление профиля юзера: данные профиля, факт нахождения в команде
 * - Обновление участников команды (если юзер в команде)
 * - Загрузка новых смен и штрафов, если такие есть на удаленном севере (применимо только к работникам)
 *
 * Работа выполняется в предоставленной корутине (при фоновой синхронизации) или запускается собственная.
 * Все изменения фиксируются в своих хранилищах
 *
 */
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

    /**
     * Фоновая синхронизация в запущенной корутине. НЕ включает движения по фрагментам, так как нет
     * переданного фрагмента. Клиенты сами заботятся об этом движении, если у них есть живой фрагмент
     */
    suspend fun syncBackground(scope: CoroutineScope, context: Context) {
        performSync(context, scope, isUiMode = false)
    }

    /**
     * Синхронизация на UI, запускается в коруте от `parent: DialogFragment`, которая может быть отменена
     * повторным вызовом (нажатием `onClick()`); включает движения по фрагментам на основе этого же парента.
     * Изменения на UI отображают статус прогресса синхронизации - отправки/загрузки данных
     */
    override fun onClick(v: View?) {
        if (cancelIfActive()) return
        val oldStatus = userProfile.getSpaceStatus()
        val wasPrivileged = encUserProfile.isPrivileged()

        parentBinding?.apply {
            doSyncButton.text = parent?.getString(R.string.sync_button_stop)
            loadingProgressBar.visibility = View.VISIBLE
        }
        val context = parent?.context ?: return
        syncJob = parent.lifecycleScope.launch {
            performSync(context, this, isUiMode = true)

            val newStatus = userProfile.getSpaceStatus()
            if (oldStatus == newStatus) return@launch
            if (oldStatus == PENDING_BOSS && newStatus == MEMBER) {
                parent.navigateForgetting(R.id.mainBossFragment)
            }
            if (oldStatus == PENDING_WORKER && newStatus == MEMBER) {
                spaceViewModel.needSwitchSideMenuItems()
            }
//                Если oldStatus == PENDING_WORKER && newStatus == NONE, то надо убрать "Компания: на рассмотрении"
            if (oldStatus == MEMBER && newStatus == NONE && wasPrivileged) {
                parent.navigateForgetting(R.id.mainWorkerFragment)
            }
        }
    }

    private suspend fun performSync(context: Context, scope: CoroutineScope, isUiMode: Boolean) {
        val srcSpaceStatus = userProfile.getSpaceStatus()
        val isAdmin = encUserProfile.isPrivileged()

        val syncOperator = SyncOperator(
            eventsOutOfSync = viewModel.getNotSynced(DataType.EVENT, isAdmin),
            finesOutOfSync = viewModel.getNotSynced(DataType.FINE, isAdmin),
            executor = { process, currentScope -> doSync(srcSpaceStatus, process, currentScope, isUiMode) },
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

    private suspend fun doSync(
        srcSpaceStatus: SpaceStatus,
        syncOperator: SyncOperator,
        coroutineScope: CoroutineScope,
        isUiMode: Boolean
    ) {
        val notSyncedEventsPattern = if (isUiMode) parent?.getString(R.string.found_not_synced_events) else null
        syncOperator.eventsOutOfSync?.forEach {
            if (!coroutineScope.isActive) return@doSync
            dec(it, isUiMode, notSyncedEventsPattern) { syncOperator.eventsDec() }
        }
        val notSyncedFinesPattern = if (isUiMode) parent?.getString(R.string.found_not_synced_fines) else null
        syncOperator.finesOutOfSync?.forEach {
            if (!coroutineScope.isActive) return@doSync
            dec(it, isUiMode, notSyncedFinesPattern) { syncOperator.finesDec() }
        }

//        Обновлять мемберов надо всегда, так как если статус сменился с isPending() на другой - там это будет отражено и изменено
        val srcRole = encUserProfile.getRole()
        syncOperator.getRemoteMembersResult = spaceService.updateMembers(srcRole, srcSpaceStatus)
        if (!srcRole.isPrivileged()) {
            syncOperator.isRemoteDataAccepted = dataService.updateData(null)
        }
    }

    private suspend fun dec(data: AbstractData, isUiMode: Boolean, notSyncedPattern: String?, decFunc: () -> Unit) {
        if (dataService.sendData(data) == SaveResult.Success) {
            if (!isUiMode) {
                decFunc.invoke()
                return
            }
            notSyncedPattern?.let { pattern ->
                when (data.getType()) {
                    DataType.EVENT -> parentBinding?.eventsCount?.text = String.format(pattern, decFunc.invoke())
                    DataType.FINE -> parentBinding?.finesCount?.text = String.format(pattern, decFunc.invoke())
                }
            }
        }
    }

    private fun processSuccess(srcSpaceStatus: SpaceStatus, context: Context, isUiMode: Boolean) {
        ToastHelper.message(ToastHelper.ALL_SYNCED, context) // TODO 29.03.2025 19:20: if (!isUiMode) не показывать тост
        if (isUiMode) parent?.dismiss()

        if (srcSpaceStatus.isPending()) {
            when (userProfile.getSpaceStatus()) {
                NONE -> {
                    showDialogSpaceStatusChanged(context, SpaceStatusChangingType.REJECTED, srcSpaceStatus)
                    spaceViewModel.emitStatusChanged(NONE)
                }

                MEMBER -> {
                    showDialogSpaceStatusChanged(context, SpaceStatusChangingType.ACCEPTED, srcSpaceStatus)
                    spaceViewModel.emitStatusChanged(MEMBER)
                }

                else -> {}
            }
        }
    }

    private fun processFailure(result: SyncOperator.Result, context: Context, isUiMode: Boolean) {
        ToastHelper.syncFiled(result, context)
        if (result.getRemoteMembersResult == UpdateResult.NOT_CONSIST_IN_SPACE) {
            if (isUiMode) parent?.dismiss()

            showDialogSpaceStatusChanged(context, SpaceStatusChangingType.DETACHED)
            userProfile.detachFromSpace()
            encUserProfile.detachFromSpace()
            spaceViewModel.detachFromSpace()
            // TODO 30.03.2025 20:19: Удалить всех участников из таблиц events и fines для участников
        }
    }

    private fun showDialogSpaceStatusChanged(
        context: Context,
        newStatus: SpaceStatusChangingType,
        srcSpaceStatus: SpaceStatus? = null
    ) {

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
                when (srcSpaceStatus) {
                    PENDING_WORKER -> binding.workerPart.visibility = View.VISIBLE
                    PENDING_BOSS -> binding.bossPart.visibility = View.VISIBLE
                    else -> {}
                }
                Pair(binding.companyName, binding.root)
            }
        }

        companyNameView.text = String.format(companyNameView.text.toString(), userProfile.getSpaceName())

        Handler(Looper.getMainLooper()).post {
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
        }
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

    private enum class SpaceStatusChangingType {
        DETACHED, REJECTED, ACCEPTED
    }
}

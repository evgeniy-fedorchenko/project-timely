package com.efedorchenko.timely.fragment.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.databinding.DialogLeaveSpaceBinding
import com.efedorchenko.timely.model.auth.RoleType
import com.efedorchenko.timely.service.SpaceService
import com.efedorchenko.timely.service.ToastHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LeaveSpaceDialogFragment : DialogFragment() {

    @Inject
    lateinit var spaceService: SpaceService

    @Inject
    lateinit var profileStorage: ProfileStorage

    @Inject
    lateinit var encProfileStorage: EncProfileStorage

    @Inject
    lateinit var spaceViewModel: SpaceViewModel

    private var _binding: DialogLeaveSpaceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogLeaveSpaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        with(binding) {
            youDetachedHeader.text = youDetachedHeader.text.toString().format(profileStorage.getSpaceName())
            configureTextAsRole(this, encProfileStorage.getRole())

            doLeaveButton.setOnClickListener {
                doLeaveButton.isEnabled = false
                cancelLeaveButton.isEnabled = false
                loadingContainer.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        if (spaceService.leaveSpace()) {
                            spaceViewModel.cleanAll()
                            profileStorage.deleteSpace()
                            spaceViewModel.needSwitchSpaceItemsInSideMenu()
                            dismiss()
                            ToastHelper.leaveSpaceSuccess(context)
                        } else {
                            ToastHelper.leaveSpaceFiled(context)
                        }
                    } finally {
                        doLeaveButton.isEnabled = true
                        cancelLeaveButton.isEnabled = true
                        loadingContainer.visibility = View.GONE
                    }
                }
            }

            cancelLeaveButton.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun configureTextAsRole(binding: DialogLeaveSpaceBinding, role: RoleType?) {
        with(binding) {
            when (role) {
                RoleType.WORKER -> {
                    workerCenterFirst.visibility = View.VISIBLE
                    workerCenterSecond.visibility = View.VISIBLE
                    secondHeader.visibility = View.VISIBLE
                    commonLowerBlock.visibility = View.VISIBLE
                    doLeaveButton.isEnabled = true
                }
                RoleType.BOSS -> {
                    bossCenterFirst.visibility = View.VISIBLE
                    secondHeader.visibility = View.VISIBLE
                    commonLowerBlock.visibility = View.VISIBLE
                    bossLowerBlock.visibility = View.VISIBLE
                    doLeaveButton.isEnabled = true
                }
                RoleType.CREATOR -> {
                    creatorCenterFirst.visibility = View.VISIBLE
                    creatorCenterSecond.visibility = View.VISIBLE
                    doLeaveButton.isEnabled = false
                }
                null -> TODO()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

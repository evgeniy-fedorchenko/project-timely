package com.efedorchenko.timely.fragment.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.databinding.DialogLeaveSpaceBinding
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

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

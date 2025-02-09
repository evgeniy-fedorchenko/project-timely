package com.efedorchenko.timely.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.databinding.DialogConnectToSpaceBinding
import com.efedorchenko.timely.input.AuthInputWatcher
import com.efedorchenko.timely.model.Model
import com.efedorchenko.timely.model.api.Resource
import com.efedorchenko.timely.service.AuthService
import com.efedorchenko.timely.service.SpaceService
import com.efedorchenko.timely.service.ToastHelper
import com.efedorchenko.timely.ui.support.applicationScope
import com.efedorchenko.timely.ui.support.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConnectSpaceDialogFragment : DialogFragment() {

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var spaceService: SpaceService

    @Inject
    lateinit var spaceViewModel: SpaceViewModel

    private var _binding: DialogConnectToSpaceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogConnectToSpaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            keyEditText.addTextChangedListener(AuthInputWatcher(keyEditText, Model::isSpaceKeyValid))
            connectButton.setOnClickListener {
                activity?.hideKeyboard()
                val context = requireContext()

                val key = keyEditText.text.toString()
                if (!Model.isSpaceKeyValid(key)) {
                    ToastHelper.invalidSpaceKey(context)
                    return@setOnClickListener
                }
                connectButton.isEnabled = false
                loadingContainer.visibility = View.VISIBLE

                context.applicationScope().launch { //  Чтобы корутина не умерла без фрагмента
                    try {
                        when (val result = authService.connectToSpace(key)) {
                            is Resource.Success -> {
                                dismiss()
                                spaceViewModel.needSwitchSpaceItemsInSideMenu()
                                ToastHelper.connectToSpaceSuccess(context)
                                if (!spaceService.initMembers()) {
                                    ToastHelper.failDownloadMembers(context)
                                }
                            }

                            is Resource.Error -> ToastHelper.message(result.message, context)
                        }

                    } finally {
                        activity?.hideKeyboard()
                        connectButton.isEnabled = true
                        loadingContainer.visibility = View.GONE
                    }
                }
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

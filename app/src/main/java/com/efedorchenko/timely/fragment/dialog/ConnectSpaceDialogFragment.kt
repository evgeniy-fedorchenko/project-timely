package com.efedorchenko.timely.fragment.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.efedorchenko.timely.databinding.DialogConnectToSpaceBinding
import com.efedorchenko.timely.fragment.support.FragmentUtils
import com.efedorchenko.timely.input.AuthInputWatcher
import com.efedorchenko.timely.model.Model
import com.efedorchenko.timely.model.api.Resource
import com.efedorchenko.timely.service.AuthService
import com.efedorchenko.timely.service.SpaceService
import com.efedorchenko.timely.service.ToastHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConnectSpaceDialogFragment : DialogFragment() {

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var spaceService: SpaceService

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
                FragmentUtils.hideKeyboard(activity)
                val context = requireContext()

                val key = keyEditText.text.toString()
                if (!Model.isSpaceKeyValid(key)) {
                    ToastHelper.invalidSpaceKey(context)
                    return@setOnClickListener
                }
                connectButton.isEnabled = false
                loadingContainer.visibility = View.VISIBLE
                requireActivity().lifecycleScope.launch { //  Скоуп активити, чтобы корутина не умерла без фрагмента
                    try {
                        when (val result = authService.connectToSpace(key)) {
                            is Resource.Success -> {
                                dismiss()
                                ToastHelper.connectToSpaceSuccess(context)
                                if (!spaceService.initMembers()) {
                                    ToastHelper.failDownloadMembers(context)
                                }
                            }

                            is Resource.Error -> ToastHelper.message(result.message, context)
                        }

                    } finally {
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

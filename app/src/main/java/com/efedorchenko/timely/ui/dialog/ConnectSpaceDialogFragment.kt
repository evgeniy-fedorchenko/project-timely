package com.efedorchenko.timely.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.data.UserProfile
import com.efedorchenko.timely.databinding.DialogConnectToSpaceBinding
import com.efedorchenko.timely.input.AuthInputWatcher
import com.efedorchenko.timely.model.Model
import com.efedorchenko.timely.model.member.SpaceConnectResultType
import com.efedorchenko.timely.model.member.SpaceStatus
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
    lateinit var userProfile: UserProfile

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
        val joinRequestAlreadyExisting = userProfile.getSpaceStatus().isPending()
        if (joinRequestAlreadyExisting) setupAboutExisting() else setupNew()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupAboutExisting() {
        with(binding) {
            find1Existing.visibility = View.VISIBLE
            find2Existing.visibility = View.VISIBLE
            cancelButtons.visibility = View.VISIBLE

            doCancel.setOnClickListener {
                val context = context
                context?.applicationScope()?.launch {
                    val leftResult = spaceService.leaveSpace()
                    if (leftResult) {
//                    Не сбрасываем spaceName и encProfile, тк это просто отмена отправленной (еще не принятой) заявки
                        userProfile.setSpaceStatus(SpaceStatus.NONE)
                        spaceViewModel.emitStatusChanged(SpaceStatus.NONE)
                    }
                    ToastHelper.joinRequestCancel(context, leftResult)
                }
                this@ConnectSpaceDialogFragment.dismiss()
            }
            doNotCancel.setOnClickListener { this@ConnectSpaceDialogFragment.dismiss() }
        }
    }

    private fun setupNew() {
        with(binding) {

            text1connect.visibility = View.VISIBLE
            text2connect.visibility = View.VISIBLE
            keyEditText.visibility = View.VISIBLE
            connectButton.visibility = View.VISIBLE

            keyEditText.addTextChangedListener(AuthInputWatcher(keyEditText, Model::isSpaceKeyValid))
            connectButton.setOnClickListener {
                activity?.hideKeyboard()
                val context = context

                val key = keyEditText.text.toString()
                if (!Model.isSpaceKeyValid(key)) {
                    ToastHelper.invalidSpaceKey(context)
                    return@setOnClickListener
                }
                connectButton.isEnabled = false
                loadingContainer.visibility = View.VISIBLE

                context?.applicationScope()?.launch { //  Чтобы корутина не умерла без фрагмента
                    try {
                        when (authService.requestConnectToSpace(key)) {
                            SpaceConnectResultType.SUCCESS -> {
                                ToastHelper.connectToSpaceSuccess(context)
                                this@ConnectSpaceDialogFragment.dismiss()
                            }
                            SpaceConnectResultType.KEY_INVALID -> ToastHelper.connectToSpaceFiledKeyInvalid(context)
                            SpaceConnectResultType.UNKNOWN_ERROR -> ToastHelper.connectToSpaceFiledUnknown(context)
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
}

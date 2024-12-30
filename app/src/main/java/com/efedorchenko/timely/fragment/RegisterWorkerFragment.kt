package com.efedorchenko.timely.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.LayoutInflater.from
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.databinding.DialogRegisterWorkerEmailHelpBinding
import com.efedorchenko.timely.databinding.DialogRegisterWorkerNameHelpBinding
import com.efedorchenko.timely.databinding.DialogRegisterWorkerPasswordHelpBinding
import com.efedorchenko.timely.databinding.DialogRegisterWorkerPositionHelpBinding
import com.efedorchenko.timely.databinding.DialogRegisterWorkerRepeatPasswordHelpBinding
import com.efedorchenko.timely.databinding.DialogRegisterWorkerSpaceKeyHelpBinding
import com.efedorchenko.timely.databinding.FragmentRegisterWorkerBinding
import com.efedorchenko.timely.input.AuthInputWatcher
import com.efedorchenko.timely.model.Model
import com.efedorchenko.timely.model.api.ApiErrorCode.AUTH
import com.efedorchenko.timely.model.api.ApiErrorCode.CLIENT
import com.efedorchenko.timely.model.api.ApiErrorCode.SERVER
import com.efedorchenko.timely.model.api.ApiErrorCode.VALIDATION
import com.efedorchenko.timely.model.api.onError
import com.efedorchenko.timely.model.api.onSuccess
import com.efedorchenko.timely.model.auth.RegisterRequest
import com.efedorchenko.timely.model.auth.RoleType
import com.efedorchenko.timely.service.ApiService
import com.efedorchenko.timely.service.OnTryRegisterListener
import com.efedorchenko.timely.service.ToastHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RegisterWorkerFragment : Fragment(), OnTryRegisterListener {

    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var encProfileStorage: EncProfileStorage

    private var _binding: FragmentRegisterWorkerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterWorkerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        setupImeInsets()
        setTextChangedListeners()
        setHelpButtonListeners(context)

        view.setOnClickListener {
            hideKeyboard()
        }

        binding.containerLayout.setOnClickListener {
            hideKeyboard()
        }
        binding.backToLoginTextView.setOnClickListener {
            findNavController().navigate(R.id.authFragment)
        }

        binding.registerButton.setOnClickListener {
            hideKeyboard()
            val registerRequest = validateAndCreateDto(context)
            registerRequest?.let { dto -> this.tryRegister(dto) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun tryRegister(registerRequest: RegisterRequest) {
        val context = requireContext()
        lifecycleScope.launch {
            showLoading()
            try {
                apiService.register(registerRequest)
                    .onSuccess { data ->
                        data?.let {
                            encProfileStorage.saveApiToken(it.jwtToken.toString())
                            encProfileStorage.saveUserUuid(it.userUuid!!)
                            encProfileStorage.saveRole(it.role!!)
                            findNavController().navigate(R.id.mainFragment)
                        }
                    }
                    .onError { apiErrorCode, _, errorData ->
                        when (apiErrorCode) {
                            VALIDATION -> ToastHelper.invalidDataOnReg(context)
                            SERVER -> ToastHelper.networkError(context)
                            AUTH -> ToastHelper.message("Client error", context)
                            CLIENT -> ToastHelper.message(
                                errorData?.errorCode?.description ?: "Client error", context
                            )
                        }
                    }
            } finally {
                hideLoading()
            }
        }
    }

    override fun showLoading() {
        binding.loadingProgressBar.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }

    override fun hideLoading() {
        binding.loadingProgressBar.visibility = View.GONE
    }

    private fun setupImeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val imeHeight = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.updatePadding(bottom = imeHeight)
            windowInsets
        }
    }

    private fun setTextChangedListeners() {
        with(binding) {

            nameEditText.addTextChangedListener(
                AuthInputWatcher(nameEditText, Model::isNameValid)
            )
            positionEditText.addTextChangedListener(
                AuthInputWatcher(positionEditText, Model::isPositionValid)
            )
            emailEditText.addTextChangedListener(
                AuthInputWatcher(emailEditText, Model::isLoginValid)
            )
            spaceKeyEditText.addTextChangedListener(
                AuthInputWatcher(spaceKeyEditText, Model::isSpaceKeyValid)
            )
            passwordEditText.addTextChangedListener(
                AuthInputWatcher(passwordEditText, Model::isPasswordValid)
            )
            repeatPasswordEditText.addTextChangedListener(
                AuthInputWatcher(repeatPasswordEditText) { repeatPassword ->
                    Model.isRepeatPasswordValid(repeatPassword, passwordEditText.text.toString())
                }
            )
        }
    }

    private fun setHelpButtonListeners(c: Context) {
        with(binding) {
            nameHelpButton.setOnClickListener {
                showHint(DialogRegisterWorkerNameHelpBinding.inflate(from(c)), c)
            }
            positionHelpButton.setOnClickListener {
                showHint(DialogRegisterWorkerPositionHelpBinding.inflate(from(c)), c)
            }
            emailHelpButton.setOnClickListener {
                showHint(DialogRegisterWorkerEmailHelpBinding.inflate(from(c)), c)
            }
            passwordHelpButton.setOnClickListener {
                showHint(DialogRegisterWorkerPasswordHelpBinding.inflate(from(c)), c)
            }
            repeatPasswordHelpButton.setOnClickListener {
                showHint(DialogRegisterWorkerRepeatPasswordHelpBinding.inflate(from(c)), c)
            }
            spaceKeyHelpButton.setOnClickListener {
                showHint(DialogRegisterWorkerSpaceKeyHelpBinding.inflate(from(c)), c)
            }
        }
    }

    private fun validateAndCreateDto(context: Context): RegisterRequest? {
        val name = binding.nameEditText.text.toString()
        if (!Model.isNameValid(name)) {
            ToastHelper.invalidNameOnReg(context)
            return null
        }
        val position = binding.positionEditText.text.toString()
        if (!Model.isPositionValid(position)) {
            ToastHelper.invalidPositionOnReg(context)
            return null
        }
        val login = binding.emailEditText.text.toString()
        if (!Model.isLoginValid(login)) {
            ToastHelper.invalidEmailOnReg(context)
            return null
        }
        val password = binding.passwordEditText.text.toString()
        if (!Model.isPasswordValid(password)) {
            ToastHelper.invalidPasswordOnReg(context)
            return null
        }
        val repeatPassword = binding.repeatPasswordEditText.text.toString()
        if (!Model.isRepeatPasswordValid(repeatPassword, password)) {
            ToastHelper.passwordsAreDifferentOnReg(context)
            return null
        }
        val spaceKey = binding.spaceKeyEditText.text.toString()
        if (!Model.isSpaceKeyValid(spaceKey)) {
            ToastHelper.invalidSpaceKeyOnReg(context)
            return null
        }

        val registerRequest = RegisterRequest.build {
            username = login
            this.password = password
            this.position = position
            role = RoleType.WORKER
            this.name = name
            this.spaceKey = spaceKey
        }
        return registerRequest
    }

    private fun showHint(viewBinding: ViewBinding, context: Context) {
        val dialog = AlertDialog.Builder(context).setView(viewBinding.root).create()
        val window = dialog.window
        window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()

        window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.70).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun hideKeyboard() {
        activity?.let { activity ->
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
        }
    }
}

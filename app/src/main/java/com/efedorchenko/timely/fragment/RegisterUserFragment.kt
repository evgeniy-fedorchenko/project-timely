package com.efedorchenko.timely.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.LayoutInflater.from
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.efedorchenko.timely.databinding.DialogRegisterEmailHelpBinding
import com.efedorchenko.timely.databinding.DialogRegisterNameHelpBinding
import com.efedorchenko.timely.databinding.DialogRegisterPasswordHelpBinding
import com.efedorchenko.timely.databinding.DialogRegisterPositionHelpBinding
import com.efedorchenko.timely.databinding.DialogRegisterRepeatPasswordHelpBinding
import com.efedorchenko.timely.databinding.DialogRegisterSpaceKeyHelpBinding
import com.efedorchenko.timely.databinding.FragmentRegisterUserBinding
import com.efedorchenko.timely.input.AuthInputWatcher
import com.efedorchenko.timely.model.Model
import com.efedorchenko.timely.model.auth.RegisterRequest
import com.efedorchenko.timely.model.auth.RoleType
import com.efedorchenko.timely.service.ToastHelper

/**
 * Регистрация для `RoleType.WORKER` и `RoleType.BOSS`
 */
class RegisterUserFragment : AbstractRegisterFragment() {

    private val args: RegisterUserFragmentArgs by navArgs()
    private var _binding: FragmentRegisterUserBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        this.setupImeInsets(binding)
        setTextChangedListeners()
        setHelpButtonListeners(context)

        this.setHideKeyboardListener(binding.containerLayout)
        this.setBackButtonListener(binding.backToLoginTextView)
        this.setRegisterButtonListener(binding.registerButton, context, binding.loadingProgressBar)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun setTextChangedListeners() {
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

    override fun setHelpButtonListeners(c: Context) {
        with(binding) {
            nameHelpButton.setOnClickListener {
                showHint(DialogRegisterNameHelpBinding.inflate(from(c)), c)
            }
            positionHelpButton.setOnClickListener {
                showHint(DialogRegisterPositionHelpBinding.inflate(from(c)), c)
            }
            emailHelpButton.setOnClickListener {
                showHint(DialogRegisterEmailHelpBinding.inflate(from(c)), c)
            }
            passwordHelpButton.setOnClickListener {
                showHint(DialogRegisterPasswordHelpBinding.inflate(from(c)), c)
            }
            repeatPasswordHelpButton.setOnClickListener {
                showHint(DialogRegisterRepeatPasswordHelpBinding.inflate(from(c)), c)
            }
            spaceKeyHelpButton.setOnClickListener {
                showHint(DialogRegisterSpaceKeyHelpBinding.inflate(from(c)), c)
            }
        }
    }

    override fun validateAndCreateDto(context: Context): RegisterRequest? {
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
            ToastHelper.invalidSpaceKey(context)
            return null
        }

        return RegisterRequest.build {
            username = login
            this.password = password
            this.position = position
            role = RoleType.valueOf(args.userType)
            this.name = name
            this.spaceKey = spaceKey
        }
    }
}

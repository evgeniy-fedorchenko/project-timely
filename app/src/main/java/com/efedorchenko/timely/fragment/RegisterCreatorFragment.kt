package com.efedorchenko.timely.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.LayoutInflater.from
import android.view.View
import android.view.ViewGroup
import com.efedorchenko.timely.R
import com.efedorchenko.timely.databinding.DialogHelpCreatorBinding
import com.efedorchenko.timely.databinding.DialogRegisterEmailHelpBinding
import com.efedorchenko.timely.databinding.DialogRegisterNameHelpBinding
import com.efedorchenko.timely.databinding.DialogRegisterPasswordHelpBinding
import com.efedorchenko.timely.databinding.DialogRegisterPositionHelpBinding
import com.efedorchenko.timely.databinding.DialogRegisterRepeatPasswordHelpBinding
import com.efedorchenko.timely.databinding.DialogRegisterSpaceNameHelpBinding
import com.efedorchenko.timely.databinding.FragmentRegisterCreatorBinding
import com.efedorchenko.timely.input.AuthInputWatcher
import com.efedorchenko.timely.model.Model
import com.efedorchenko.timely.model.auth.RegisterRequest
import com.efedorchenko.timely.model.auth.RoleType
import com.efedorchenko.timely.model.auth.SpaceCreateDto
import com.efedorchenko.timely.service.ToastHelper

/**
 * Регистрация для `RoleType.CREATOR`
 */
class RegisterCreatorFragment : AbstractRegisterFragment() {

    private var _binding: FragmentRegisterCreatorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentRegisterCreatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        this.setupImeInsets(binding)
        setTextChangedListeners()
        setHelpButtonListeners(context)

        val windowToken = binding.root.windowToken
        this.setHideKeyboardListener(binding.containerLayout, windowToken)
        this.setBackButtonListener(binding.backToLoginTextView)
        this.setRegisterButtonListener(binding.registerButton, context, binding.loadingProgressBar, windowToken)

        binding.helpButton.setOnClickListener {
            showHelpDialog(context)
        }
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
            spaceNameEditText.addTextChangedListener(
                AuthInputWatcher(spaceNameEditText, Model::isSpaceNameValid)
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
            spaceNameHelpButton.setOnClickListener {
                showHint(DialogRegisterSpaceNameHelpBinding.inflate(from(c)), c)
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
        val spaceName = binding.spaceNameEditText.text.toString()
        if (!Model.isSpaceNameValid(spaceName)) {
            ToastHelper.invalidSpaceNameOnReg(context)
            return null
        }

        return RegisterRequest.build {
            username = login
            this.password = password
            this.position = position
            role = RoleType.CREATOR
            this.name = name
            creatingSpace = SpaceCreateDto(spaceName)
        }
    }

    private fun showHelpDialog(context: Context) {
        val binding = DialogHelpCreatorBinding.inflate(from(context))
        val dialog = AlertDialog.Builder(context).setView(binding.root).create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        binding.closeButton.setOnClickListener {
            dialog.cancel()
        }
    }
}

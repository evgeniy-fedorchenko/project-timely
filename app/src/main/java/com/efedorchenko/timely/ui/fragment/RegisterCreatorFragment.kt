package com.efedorchenko.timely.ui.fragment

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.LayoutInflater.from
import android.view.View
import android.view.ViewGroup
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
import com.efedorchenko.timely.model.auth.SpaceDto
import com.efedorchenko.timely.service.ToastHelper
import com.efedorchenko.timely.ui.support.animClickListener

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
        setHelpButtonListeners(context)   // Подсказки у полей ввода

        this.setHideKeyboardListener(binding.containerLayout)
        this.setBackButtonListener(binding.backToLoginTextView)
        this.setRegisterButtonListener(binding.registerButton, context, binding.loadingProgressBar)

        binding.helpButton.setOnClickListener {
            showHelpDialog(context)   // Кнопка помощи внизу экрана
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
            nameHelpButton.animClickListener {
                    showHint(DialogRegisterNameHelpBinding.inflate(from(c)), c)
            }
            positionHelpButton.animClickListener {
                    showHint(DialogRegisterPositionHelpBinding.inflate(from(c)), c)
            }
            emailHelpButton.animClickListener {
                    showHint(DialogRegisterEmailHelpBinding.inflate(from(c)), c)
            }
            passwordHelpButton.animClickListener {
                    showHint(DialogRegisterPasswordHelpBinding.inflate(from(c)), c)
            }
            repeatPasswordHelpButton.animClickListener {
                    showHint(DialogRegisterRepeatPasswordHelpBinding.inflate(from(c)), c)
            }
            spaceNameHelpButton.animClickListener {
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
            creatingSpace = SpaceDto(spaceName)
        }
    }

    private fun showHelpDialog(context: Context) {
        val binding = DialogHelpCreatorBinding.inflate(from(context))
        val dialog = AlertDialog.Builder(context).setView(binding.root).create()

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (resources.displayMetrics.widthPixels * HELP_DIALOG_WIDTH_RATIO).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog.show()

        binding.closeButton.setOnClickListener {
            dialog.cancel()
        }
    }
}

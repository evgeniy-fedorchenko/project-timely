package com.efedorchenko.timely.ui.fragment

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.efedorchenko.timely.R
import com.efedorchenko.timely.ui.support.FragmentUtils
import com.efedorchenko.timely.model.api.Resource
import com.efedorchenko.timely.model.auth.RegisterRequest
import com.efedorchenko.timely.service.AuthService
import com.efedorchenko.timely.service.SpaceService
import com.efedorchenko.timely.service.ToastHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
abstract class AbstractRegisterFragment : Fragment() {

    companion object {
        const val INPUT_HINT_WIDTH_RATIO = 0.8   // Маленькая подсказка справа от поля ввода
        const val HELP_DIALOG_WIDTH_RATIO = 0.85   // Кнопка помощи внизу экрана
    }

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var spaceService: SpaceService

    abstract fun setTextChangedListeners()

    abstract fun setHelpButtonListeners(c: Context)

    abstract fun validateAndCreateDto(context: Context): RegisterRequest?

    protected fun setupImeInsets(binding: ViewBinding) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val imeHeight = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.updatePadding(bottom = imeHeight)
            windowInsets
        }
    }

    protected fun setHideKeyboardListener(targetLayout: LinearLayout) {
        targetLayout.setOnClickListener {
           FragmentUtils.hideKeyboard(activity)
        }
    }

    protected fun setBackButtonListener(backToLoginTextView: TextView) {
        backToLoginTextView.setOnClickListener {
            findNavController().navigate(R.id.authFragment)
        }
    }

    protected fun setRegisterButtonListener(
        registerButton: Button, context: Context, loadingProgressBar: ProgressBar
    ) {
        registerButton.setOnClickListener {
            doRegister(registerButton, context, loadingProgressBar)
        }
    }

    protected fun showHint(viewBinding: ViewBinding, context: Context) {
        val dialog = AlertDialog.Builder(context).setView(viewBinding.root).create()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (resources.displayMetrics.widthPixels * INPUT_HINT_WIDTH_RATIO).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog.show()
    }

    private fun doRegister(
        registerButton: Button, context: Context, loadingProgressBar: ProgressBar
    ) {
        registerButton.isEnabled = false
        FragmentUtils.hideKeyboard(activity)

        val registerRequest = validateAndCreateDto(context)
        if (registerRequest == null) {
            registerButton.isEnabled = true
            return
        }

        lifecycleScope.launch {
            FragmentUtils.showLoading(loadingProgressBar)
            try {
                when (val result = authService.tryRegister(registerRequest)) {
                    is Resource.Success -> {
                        // TODO: сначала переводить юзеров просто на экран одиночки и ждать решения по заявке от руководителей
                        findNavController().navigate(R.id.mainWorkerFragment)
                        if (result.spacePresent && !spaceService.initMembers()) {   // Все равно пытаемся, хотя бы чтобы показать тост
                            ToastHelper.failDownloadMembers(context)
                        }
                    }

                    is Resource.Error -> ToastHelper.message(result.message, context)
                }

            } finally {
                registerButton.isEnabled = true
                FragmentUtils.hideLoading(loadingProgressBar)
            }
        }
    }
}
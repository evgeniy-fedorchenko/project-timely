package com.efedorchenko.timely.ui.fragment

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater.from
import android.view.View
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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.efedorchenko.timely.R
import com.efedorchenko.timely.databinding.DialogSpaceAccessPendingBossBinding
import com.efedorchenko.timely.databinding.DialogSpaceAccessPendingWorkerBinding
import com.efedorchenko.timely.model.api.Resource
import com.efedorchenko.timely.model.auth.RegisterRequest
import com.efedorchenko.timely.model.auth.RoleType
import com.efedorchenko.timely.model.member.SpaceStatus
import com.efedorchenko.timely.service.AuthService
import com.efedorchenko.timely.service.SpaceService
import com.efedorchenko.timely.service.ToastHelper
import com.efedorchenko.timely.ui.support.hide
import com.efedorchenko.timely.ui.support.hideKeyboard
import com.efedorchenko.timely.ui.support.show
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
            activity?.hideKeyboard()
        }
    }

    protected fun setBackButtonListener(backToLoginTextView: TextView) {
        backToLoginTextView.setOnClickListener {
            findNavController().navigate(R.id.authFragment)
        }
    }

    protected fun setRegisterButtonListener(registerButton: Button, context: Context, progressBar: ProgressBar) {
        registerButton.setOnClickListener {
            doRegister(registerButton, context, progressBar)
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

    private fun doRegister(registerButton: Button, context: Context, progressBar: ProgressBar) {
        registerButton.isEnabled = false
        activity?.hideKeyboard()

        val registerRequest = validateAndCreateDto(context)
        if (registerRequest == null) {
            registerButton.isEnabled = true
            return
        }

        lifecycleScope.launch {
            progressBar.show()
            try {
                when (val result = authService.tryRegister(registerRequest)) {
                    is Resource.Success -> handleSuccess(result, context)
                    is Resource.Error -> ToastHelper.message(result.message, context)
                }

            } finally {
                registerButton.isEnabled = true
                progressBar.hide()
            }
        }
    }

    private fun handleSuccess(result: Resource.Success<Unit>, context: Context) {
        val doNavigate = { fragmentId: Int ->
            findNavController().navigate(
                fragmentId, null,
                NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
            )
        }

        val spaceName = result.userData.spaceName
        val spaceStatus = result.userData.spaceStatus

        when {
            result.authData.role.isHigherThan(RoleType.BOSS) -> {
                doNavigate.invoke(R.id.mainBossFragment)
            }
            spaceStatus == SpaceStatus.PENDING_BOSS -> {
                doNavigate(R.id.mainWorkerFragment)
                showRequestToSpaceAccessDialog(context, true, spaceName)
            }
            spaceStatus == SpaceStatus.PENDING_WORKER -> {
                doNavigate(R.id.mainWorkerFragment)
                showRequestToSpaceAccessDialog(context, false, spaceName)
            }

            else -> {} // TODO: показать ошибку
        }
//        Не грузим участников и данные, так как при регистрации их еще не существует
    }

    private fun showRequestToSpaceAccessDialog(context: Context, isBoss: Boolean, spaceName: String?) {
        spaceName?.let {
            val dialog = if (isBoss) {
                val inflate = DialogSpaceAccessPendingBossBinding.inflate(from(context))
                setupDialog(context, inflate.root, inflate.header, inflate.closeButton, spaceName)
            } else {
                val inflate = DialogSpaceAccessPendingWorkerBinding.inflate(from(context))
                setupDialog(context, inflate.root, inflate.header, inflate.closeButton, spaceName)
            }
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    (resources.displayMetrics.widthPixels * HELP_DIALOG_WIDTH_RATIO).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            dialog.show()
        }
    }

    private fun setupDialog(
        context: Context, root: View, header: TextView, closeButton: View, spaceName: String
    ): AlertDialog {
        header.text = String.format(header.text.toString(), spaceName)
        val dialog = AlertDialog.Builder(context).setView(root).create()
        closeButton.setOnClickListener { dialog.cancel() }
        return dialog
    }
}
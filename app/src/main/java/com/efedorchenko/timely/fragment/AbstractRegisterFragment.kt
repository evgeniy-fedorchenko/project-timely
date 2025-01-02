package com.efedorchenko.timely.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.IBinder
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import com.efedorchenko.timely.model.api.Resource
import com.efedorchenko.timely.model.auth.RegisterRequest
import com.efedorchenko.timely.service.AuthService
import com.efedorchenko.timely.service.ToastHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
abstract class AbstractRegisterFragment : Fragment() {

    @Inject
    lateinit var authService: AuthService

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

    protected fun setHideKeyboardListener(targetLayout: LinearLayout, windowToken: IBinder) {
        targetLayout.setOnClickListener {
            hideKeyboard(windowToken)
        }
    }

    protected fun setBackButtonListener(backToLoginTextView: TextView) {
        backToLoginTextView.setOnClickListener {
            findNavController().navigate(R.id.authFragment)
        }
    }

    protected fun setRegisterButtonListener(
        registerButton: Button, context: Context, loadingProgressBar: ProgressBar, windowToken: IBinder
    ) {
        registerButton.setOnClickListener {
            doRegister(registerButton, context, loadingProgressBar, windowToken)
        }
    }

    protected fun showHint(viewBinding: ViewBinding, context: Context) {
        val dialog = AlertDialog.Builder(context).setView(viewBinding.root).create()
        val window = dialog.window
        window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()

        window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.70).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun hideKeyboard(windowToken: IBinder) {
        activity?.let { activity ->
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    private fun doRegister(
        registerButton: Button, context: Context, loadingProgressBar: ProgressBar, windowToken: IBinder
    ) {
        registerButton.isEnabled = false
        hideKeyboard(windowToken)

        val registerRequest = validateAndCreateDto(context)
        if (registerRequest == null) {
            registerButton.isEnabled = true
            return
        }

        lifecycleScope.launch {
            showLoading(loadingProgressBar)
            try {
                when (val result = authService.tryRegister(registerRequest)) {
                    is Resource.Success -> findNavController().navigate(R.id.mainFragment)
                    is Resource.Error -> ToastHelper.message(result.message, context)
                }

            } finally {
                registerButton.isEnabled = true
                hideLoading(loadingProgressBar)
            }
        }
    }

    private fun showLoading(loadingProgressBar: ProgressBar) {
        loadingProgressBar.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }

    private fun hideLoading(loadingProgressBar: ProgressBar) {
        loadingProgressBar.visibility = View.GONE
    }
}
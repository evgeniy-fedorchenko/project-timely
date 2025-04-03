package com.efedorchenko.timely.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.efedorchenko.timely.R
import com.efedorchenko.timely.databinding.FragmentLoginBinding
import com.efedorchenko.timely.input.AuthInputWatcher
import com.efedorchenko.timely.model.Model
import com.efedorchenko.timely.model.api.Resource
import com.efedorchenko.timely.model.auth.Credentials
import com.efedorchenko.timely.service.AuthService
import com.efedorchenko.timely.service.DataService
import com.efedorchenko.timely.service.SpaceService
import com.efedorchenko.timely.service.ToastHelper
import com.efedorchenko.timely.ui.support.applicationScope
import com.efedorchenko.timely.ui.support.hide
import com.efedorchenko.timely.ui.support.hideKeyboard
import com.efedorchenko.timely.ui.support.show
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AuthFragment : Fragment() {

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var spaceService: SpaceService

    @Inject
    lateinit var dataService: DataService

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val activity = activity
        setupImeInsets()

        view.setOnClickListener {
            activity?.hideKeyboard()
        }

        binding.loginEditText.addTextChangedListener(
            AuthInputWatcher(binding.loginEditText, Model::isLoginValid)
        )
        binding.passwordEditText.addTextChangedListener(
            AuthInputWatcher(binding.passwordEditText, Model::isPasswordValid)
        )
        binding.noAccountTextView.setOnClickListener {
            findNavController().navigate(R.id.registerDispatcherFragment)
        }

        binding.loginButton.setOnClickListener {
            doLogin(context, activity)
        }
    }

    private fun doLogin(context: Context, activity: FragmentActivity?) {
        with(binding) {
            loginButton.isEnabled = false
            activity?.hideKeyboard()
            val credentials = Credentials(loginEditText.text.toString(), passwordEditText.text.toString())
            if (!Model.isCredentialsValid(credentials)) {
                ToastHelper.invalidCredentials(context)
                loginButton.isEnabled = true
                return
            }

            lifecycleScope.launch {
                loadingProgressBar.show()
                try {
                    when (val result = authService.tryLogin(credentials)) {
                        is Resource.Success -> handleSuccess(context, result)
                        is Resource.Error -> ToastHelper.message(result.message, context)
                    }

                } finally {
                    loginButton.isEnabled = true
                    loadingProgressBar.hide()
                }
            }
        }
    }

    private fun handleSuccess(context: Context, result: Resource.Success<Unit>) {
        if (result.authData.role.isPrivileged()) navigateForgetting(R.id.mainBossFragment)
        else navigateForgetting(R.id.mainWorkerFragment)

        context.applicationScope().launch {
            if (!isPrivileged) {
                if (!dataService.loadData()) {
                    ToastHelper.failDownloadData(context)
                }
            }
            if (oldStatus == SpaceStatus.MEMBER && newStatus == SpaceStatus.NONE && wasPrivileged) {
                navigateForgetting(R.id.mainWorkerFragment)
            }
        }
    }

    private fun setupImeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val imeHeight = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.updatePadding(bottom = imeHeight)
            windowInsets
        }
    }
}

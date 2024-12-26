package com.efedorchenko.timely.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.efedorchenko.timely.R
import com.efedorchenko.timely.databinding.FragmentLoginBinding
import com.efedorchenko.timely.input.AuthInputWatcher
import com.efedorchenko.timely.model.AuthRequest
import com.efedorchenko.timely.model.Model
import com.efedorchenko.timely.model.auth.AuthResponse
import com.efedorchenko.timely.security.SecurityService
import com.efedorchenko.timely.service.ApiService
import com.efedorchenko.timely.service.OnTryLoginListener
import com.efedorchenko.timely.service.ToastHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AuthFragment : Fragment(), OnTryLoginListener {

    @Inject
    lateinit var securityService: SecurityService

    @Inject
    lateinit var apiService: ApiService

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        val view = binding.root

        view.setOnClickListener {
            hideKeyboard()
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

        val context = requireContext()
        binding.loginButton.setOnClickListener {
            val login = binding.loginEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val loginPair = Pair(login, password)
            if (!Model.isLoginPairValid(loginPair)) {
                ToastHelper.incorrectLoginData(context)
            } else {
                this.tryLogin(loginPair)
            }
        }

        return view
    }

    override fun tryLogin(loginData: Pair<String, String>) {
        val context = requireContext()

        lifecycleScope.launch {
            val response = apiService.login(AuthRequest(loginData))
            val authResponse: AuthResponse? = response.data.also { r ->
                when {
                    r == null && response.isAuthError -> ToastHelper.incorrectLoginData(context)
                    r == null -> ToastHelper.networkError(context)
                }
            }
            if (authResponse == null) {
                return@launch
            }

//            'jwtToken', 'userId' and 'role' are null only if isRegister == false
            // TODO: объединить
            securityService.saveApiToken(authResponse.jwtToken.toString())
            securityService.saveUserId(authResponse.userId!!)
            securityService.saveRole(authResponse.role!!)
            findNavController().navigate(R.id.mainFragment)
        }
    }

    private fun hideKeyboard() {
        val activity = activity
        if (activity != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val view = activity.currentFocus ?: View(activity)
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

}

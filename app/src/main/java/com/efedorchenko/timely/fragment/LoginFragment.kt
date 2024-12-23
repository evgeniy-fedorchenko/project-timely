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
import com.efedorchenko.timely.databinding.AuthBinding
import com.efedorchenko.timely.model.AuthRequest
import com.efedorchenko.timely.security.SecurityService
import com.efedorchenko.timely.service.ApiService
import com.efedorchenko.timely.service.OnTryLoginListener
import com.efedorchenko.timely.service.ToastHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment(), OnTryLoginListener {

    @Inject
    lateinit var securityService: SecurityService

    @Inject
    lateinit var apiService: ApiService

    private var _binding: AuthBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = AuthBinding.inflate(inflater, container, false)
        val view = binding.root

        view.setOnClickListener {
            hideKeyboard()
        }

        binding.loginEditText.addTextChangedListener(
            createTextWatcher(binding.loginEditText, Model::isLoginValid)
        )
        binding.passwordEditText.addTextChangedListener(
            createTextWatcher(binding.passwordEditText, Model::isPasswordValid)
        )

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

        binding.noAccountTextView.setOnClickListener {
            ToastHelper.noAccount(context)
        }

        return view
    }

    private fun createTextWatcher(editText: EditText, validator: (String) -> Boolean) =
        object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                if (input.isEmpty()) {
                    editText.setBackgroundResource(R.drawable.login_form_background)
                } else {
                    val isValid = validator(input)
                    editText.setBackgroundResource(
                        if (isValid) R.drawable.login_form_background
                        else R.drawable.login_form_background_error
                    )
                }
            }
        }

    override fun tryLogin(loginData: Pair<String, String>) {
        val context = requireContext()

        lifecycleScope.launch {
            val loginResult = apiService.login(AuthRequest(loginData))
            loginResult.onFailure {
                ToastHelper.networkError(context)
                return@launch
            }
            val loginResponse = loginResult.getOrNull()
            if (loginResponse == null) {
                ToastHelper.networkError(context)
                return@launch
            }

            if (!loginResponse.isRegister) {
                ToastHelper.message(loginResponse.errorCode?.description.toString(), context)
                return@launch
            }

//            'jwtToken', 'userId' and 'role' are null only if isRegister == false
            // TODO: объединить
            securityService.saveApiToken(loginResponse.jwtToken.toString())
            securityService.saveUserId(loginResponse.userId!!)
            securityService.saveRole(loginResponse.role!!)
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

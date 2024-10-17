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
import com.efedorchenko.timely.model.AuthStatus
import com.efedorchenko.timely.security.SecurityService
import com.efedorchenko.timely.security.SecurityServiceImpl
import com.efedorchenko.timely.service.ApiService
import com.efedorchenko.timely.service.ApiServiceImpl
import com.efedorchenko.timely.service.OnTryLoginListener
import com.efedorchenko.timely.service.ToastHelper
import kotlinx.coroutines.launch

class LoginFragment : Fragment(), OnTryLoginListener {

    private var _binding: AuthBinding? = null
    private val binding get() = _binding!!

    private lateinit var securityService: SecurityService
    private val apiService: ApiService by lazy { ApiServiceImpl() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = AuthBinding.inflate(inflater, container, false)
        val view = binding.root

        view.setOnClickListener { hideKeyboard() }

        binding.loginButton.setOnClickListener {
            val login = binding.loginEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            if (!login.isNullOrBlank() && !password.isNullOrBlank()) {
                this.tryLogin(Pair(login, password))
            }
        }

        binding.noAccountTextView.setOnClickListener {
            ToastHelper.noAccount(requireContext())
        }

        return view
    }

    override fun tryLogin(loginData: Pair<String, String>) {
        val context = requireContext()
        securityService = SecurityServiceImpl.getInstance(context)

        lifecycleScope.launch {
            val loginResalt = apiService.login(AuthRequest(loginData))
            if (loginResalt == null) {
                ToastHelper.networkError(context)
                return@launch
            }
            if (loginResalt.status == AuthStatus.FAIL) {
                ToastHelper.incorrectLoginData(context)
                return@launch
            }

//            'uuid' and 'role' are null only if status = fail
            securityService.saveToken(loginResalt.uuid!!, loginResalt.user!!.role)
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

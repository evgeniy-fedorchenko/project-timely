package com.efedorchenko.timely.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.efedorchenko.timely.R
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

    private lateinit var navController: NavController
    private lateinit var securityService: SecurityService
    private val apiService: ApiService by lazy { ApiServiceImpl() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.auth, container, false)

        view.setOnTouchListener { v, event ->
            v.performClick()
            if (event.action == MotionEvent.ACTION_DOWN) {
                hideKeyboard()
            }
            true
        }

        val loginField = view.findViewById<EditText>(R.id.loginEditText)
        val passwordField = view.findViewById<EditText>(R.id.passwordEditText)
        val button = view.findViewById<Button>(R.id.loginButton)

        button.setOnClickListener {
            val login = loginField.text.toString()
            val password = passwordField.text.toString()
            if (!login.isNullOrBlank() && !password.isNullOrBlank()) {
                this.tryLogin(Pair(login, password))
            }
        }

        val noAccountLink = view.findViewById<TextView>(R.id.noAccountTextView)
        noAccountLink.setOnClickListener {
            ToastHelper.showToast("Ну и пошел нахуй тогда", requireContext())
        }

        return view
    }

    override fun tryLogin(loginData: Pair<String, String>) {
        val context = requireContext()
            securityService = SecurityServiceImpl.getInstance(context)

        lifecycleScope.launch {
            val loginResalt = apiService.login(AuthRequest(loginData))
            if (loginResalt == null) {
                ToastHelper.showToast(
                    "Проблемы с подключением, проверте работу сети Интернет",
                    context
                )
                return@launch
            }
            if (loginResalt.status == AuthStatus.FAIL) {
                ToastHelper.showToast("Неверный логин или пароль", context)
                return@launch
            }

//            'uuid' and 'role' are null only if status = fail
            securityService.saveToken(loginResalt.uuid!!, loginResalt.role!!)
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

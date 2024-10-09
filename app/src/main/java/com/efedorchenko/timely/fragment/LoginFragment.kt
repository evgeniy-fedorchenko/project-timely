package com.efedorchenko.timely.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.efedorchenko.timely.R
import com.efedorchenko.timely.service.CalendarHelper
import com.efedorchenko.timely.service.OnTryLoginListener

class LoginFragment: Fragment(), OnTryLoginListener {

    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.auth, container, false)
        val loginField = view.findViewById<EditText>(R.id.loginEditText)
        val passwordField = view.findViewById<EditText>(R.id.passwordEditText)
        val button = view.findViewById<Button>(R.id.loginButton)

        button.setOnClickListener {
            val login = loginField.text.toString()
            val password = passwordField.text.toString()
            this.tryLogin(Pair(login, password))
        }

        val noAccountLink = view.findViewById<TextView>(R.id.noAccountTextView)
        noAccountLink.setOnClickListener {
            CalendarHelper.showToast("Ну и пошел нахуй тогда", requireContext())
        }

        return view
    }

    override fun tryLogin(loginData: Pair<String, String>) {
        if (loginData.first.equals("user") && loginData.second.equals("12345")) {
            findNavController().navigate(R.id.mainFragment)
        } else {
            CalendarHelper.showToast("#37 filed load SecurtyContext. AuthException: <Unuthorized>", requireContext())
        }
    }
}
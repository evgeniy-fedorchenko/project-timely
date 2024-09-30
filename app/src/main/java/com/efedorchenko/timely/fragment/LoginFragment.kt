package com.efedorchenko.timely.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.efedorchenko.timely.R

class LoginFragment: Fragment() {

    private lateinit var calendarHelper: CalendarHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.auth, container, false)
        calendarHelper = CalendarHelper(requireContext())
        val button = view.findViewById<Button>(R.id.loginButton)
        button.setOnClickListener {
            calendarHelper.oldDateSelected().show()
        }

        val noAccountLink = view.findViewById<TextView>(R.id.noAccountTextView)
        noAccountLink.setOnClickListener {
            calendarHelper.showToast("Ну и пошел нахуй тогда")
        }

        return view
    }

}
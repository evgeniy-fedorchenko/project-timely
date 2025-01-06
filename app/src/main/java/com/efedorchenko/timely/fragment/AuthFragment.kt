package com.efedorchenko.timely.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.efedorchenko.timely.R
import com.efedorchenko.timely.databinding.FragmentLoginBinding
import com.efedorchenko.timely.input.AuthInputWatcher
import com.efedorchenko.timely.model.Model
import com.efedorchenko.timely.model.api.Resource
import com.efedorchenko.timely.model.auth.Credentials
import com.efedorchenko.timely.service.AuthService
import com.efedorchenko.timely.service.SpaceService
import com.efedorchenko.timely.service.SpaceServiceImpl.InitResult.SUCCESS
import com.efedorchenko.timely.service.ToastHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AuthFragment : Fragment() {

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var spaceService: SpaceService

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
        setupImeInsets()

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

        binding.loginButton.setOnClickListener {
            doLogin(context)
        }
    }

    private fun doLogin(context: Context) {
        binding.loginButton.isEnabled = false
        hideKeyboard()
        val login = binding.loginEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        val loginPair = Pair(login, password)

        if (!Model.isLoginPairValid(loginPair)) {
            ToastHelper.incorrectLoginData(context)
            binding.loginButton.isEnabled = true
            return
        }

        lifecycleScope.launch {
            showLoading()
            try {
                val credentials = Credentials(login, password)
                when (val result = authService.tryLogin(credentials)) {
                    is Resource.Success -> {
                        findNavController().navigate(R.id.mainFragment)
                        val initResult = spaceService.initData()
                        if (initResult != SUCCESS) {
                            ToastHelper.message(initResult.failMess, context)
                        }
                        if (!spaceService.initMembers()) {   // Все равно пытаемся, хотя бы чтобы показать тост
                            ToastHelper.failDownloadMembers(context)
                        }
                    }

                    is Resource.Error -> ToastHelper.message(result.message, context)
                }

            } finally {
                binding.loginButton.isEnabled = true
                hideLoading()
            }
        }
    }

    private fun showLoading() {
        binding.loadingProgressBar.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }

    private fun hideLoading() {
        binding.loadingProgressBar.visibility = View.GONE
    }

    private fun setupImeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val imeHeight = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.updatePadding(bottom = imeHeight)
            windowInsets
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

package com.efedorchenko.timely

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.efedorchenko.timely.security.SecurityService
import com.efedorchenko.timely.security.UserRole
import com.efedorchenko.timely.service.MainViewModel

class MainActivity : AppCompatActivity() {

    lateinit var viewModel: MainViewModel

    private lateinit var navController: NavController
    private lateinit var securityService: SecurityService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setupInsets()

        securityService = SecurityService(this)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navController = navHost.navController

        if (isUserAuthenticated()) {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val userRole = securityService.authorize()
        when (userRole) {
            UserRole.WORKER -> navController.navigate(R.id.mainFragment)
            UserRole.BOSS -> navController.navigate(R.id.mainFragment)
            null -> TODO()
        }
    }

    private fun isUserAuthenticated(): Boolean {
        return securityService.isUserAuthenticated()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
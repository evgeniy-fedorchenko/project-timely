package com.efedorchenko.timely

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.model.auth.RoleType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    @Inject
    lateinit var encProfileStorage: EncProfileStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setupInsets()

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navController = navHost.navController

        if (isUserAuthenticated()) {
            navigateToMain()
        } else {
            navController.navigate(R.id.authFragment)
        }
    }

    private fun navigateToMain() {
        val userRole = encProfileStorage.getRole()
        when (userRole) {
            RoleType.WORKER -> navController.navigate(R.id.mainFragment)
            RoleType.BOSS -> navController.navigate(R.id.mainFragment)
            RoleType.CREATOR -> navController.navigate(R.id.mainFragment)
            null -> navController.navigate(R.id.authFragment)
        }
    }

    private fun isUserAuthenticated(): Boolean = encProfileStorage.isAuthenticated()

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
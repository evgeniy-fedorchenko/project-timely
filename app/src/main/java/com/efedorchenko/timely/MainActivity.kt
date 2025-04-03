package com.efedorchenko.timely

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.efedorchenko.timely.data.EncUserProfile
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.data.UserProfile
import com.efedorchenko.timely.model.auth.RoleType
import com.efedorchenko.timely.model.member.SpaceStatus
import com.efedorchenko.timely.ui.support.DataSynchronizerFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    @Inject
    lateinit var encUserProfile: EncUserProfile

    @Inject
    lateinit var userProfile: UserProfile

    @Inject
    lateinit var spaceViewModel: SpaceViewModel

    @Inject
    lateinit var dataSynchronizerFactory: DataSynchronizerFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setupInsets()

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navController = navHost.navController

        if (isUserAuthenticated()) navigateToMain() else navController.navigate(R.id.authFragment)
    }

    private fun navigateToMain() {
        val userRole = encUserProfile.getRole()

        syncStart()
        when (userRole) {
            RoleType.WORKER -> navController.navigate(R.id.mainWorkerFragment)
            RoleType.BOSS, RoleType.CREATOR -> navController.navigate(R.id.mainBossFragment)
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun isUserAuthenticated() = encUserProfile.isAuthenticated()

    private fun syncStart() {
        lifecycleScope.launch {
            try {
                val oldStatus = userProfile.getSpaceStatus()
                val wasPrivileged = encUserProfile.isPrivileged()

                dataSynchronizerFactory.create().syncBackground(this, this@MainActivity)

                val newStatus = userProfile.getSpaceStatus()
                if (oldStatus == newStatus) return@launch
                if (oldStatus == SpaceStatus.PENDING_BOSS && newStatus == SpaceStatus.MEMBER) {
                    navController.navigate(R.id.mainBossFragment)
                }
                if (oldStatus == SpaceStatus.PENDING_WORKER && newStatus == SpaceStatus.MEMBER) {
                    spaceViewModel.needSwitchSideMenuItems()
                }
                if (oldStatus == SpaceStatus.MEMBER && newStatus == SpaceStatus.NONE && wasPrivileged) {
                    navController.navigate(R.id.mainWorkerFragment)
                }
            } catch (ex: Exception) {
                Log.e("MainActivity", "Sync failed", ex)
            }
        }
    }
}

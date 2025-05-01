package com.atvantiq.wfms.ui.screens

import android.os.Bundle
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.core.view.GravityCompat
import androidx.navigation.ui.NavigationUI
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseBindingActivity
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.databinding.ActivityDashboardBinding
import com.atvantiq.wfms.databinding.NavHeaderDashboardBinding
import com.atvantiq.wfms.models.loginResponse.User
import com.atvantiq.wfms.ui.screens.login.LoginActivity
import com.atvantiq.wfms.utils.Utils
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DashboardActivity : BaseBindingActivity<ActivityDashboardBinding>() {

    @Inject
    lateinit var prefMain: SecurePrefMain

    private lateinit var appBarConfiguration: AppBarConfiguration

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_dashboard)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        setSupportActionBar(binding.appBarDashboard.toolbar)
        setupNavigationDrawer()

        var userData  = PrefMethods.getUserData(prefMain)
        setupDataDrawerHeader(userData)
    }

    private fun setupDataDrawerHeader(userData: User?) {
        if(userData == null)
            return
        val headerView = binding.navView.getHeaderView(0)
        val navHeaderBinding = NavHeaderDashboardBinding.bind(headerView)
        navHeaderBinding.textView.text = userData?.email
    }

    private fun setupNavigationDrawer(){
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_dashboard)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_dashboard, R.id.nav_attendance, R.id.nav_reimbursement, R.id.nav_vendor,R.id.nav_cab,R.id.nav_material_reco,R.id.nav_about, R.id.nav_feedback
            ), binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.logout -> {
                    // Implement your logout logic here
                    performLogout()
                    true
                }
                else -> {
                    // Handle other menu items with the navController
                    val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                    if (handled) {
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    handled
                }
            }
        }
    }

    private fun performLogout(){
        prefMain.deleteAll()
        Utils.jumpActivity(this,LoginActivity::class.java)
        finish()
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_dashboard)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
package com.atvantiq.wfms.ui.screens

import android.content.DialogInterface
import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

    private lateinit var appBarConfiguration: AppBarConfiguration
    private val navController: androidx.navigation.NavController
        get() = findNavController(R.id.nav_host_fragment_content_dashboard)

    // Register the permission launcher
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
    }

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_dashboard)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        setSupportActionBar(binding.appBarDashboard.toolbar)
        setupNavigationDrawer()

        var userData = PrefMethods.getUserData(prefMain)
        setupDataDrawerHeader(userData)
        requestPostNotificationsPermissionIfNeeded()
        batterOptimizationCheck()
    }

    private fun batterOptimizationCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }

    private fun requestPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupDataDrawerHeader(userData: User?) {
        if (userData == null)
            return
        val headerView = binding.navView.getHeaderView(0)
        val navHeaderBinding = NavHeaderDashboardBinding.bind(headerView)
        navHeaderBinding.textView.text = userData?.email
    }

    private fun setupNavigationDrawer() {
        val navView: NavigationView = binding.navView
        //val navController = findNavController(R.id.nav_host_fragment_content_dashboard)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_dashboard,
                R.id.nav_attendance,
                R.id.nav_reimbursement,
                R.id.nav_vendor,
                R.id.nav_cab,
                R.id.nav_material_reco,
                R.id.nav_about,
                R.id.nav_feedback
            ), binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.logout -> {
                    // Implement your logout logic here
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    alertDialogShow(this,
                        getString(R.string.logout),
                        getString(R.string.logout_confirmation),
                        getString(R.string.yes),
                        DialogInterface.OnClickListener { dialog, which ->
                            dialog.dismiss()
                            performLogout()
                        },
                        DialogInterface.OnClickListener { dialog, which ->
                           dialog.dismiss()
                        })
                    true
                }
                R.id.nav_feedback -> {
                    // Implement your logout logic here
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    alertDialogShow(this,getString(R.string.under_development))
                    true
                }

                R.id.nav_about -> {
                    // Implement your logout logic here
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    alertDialogShow(this,getString(R.string.under_development))
                    true
                }

                R.id.nav_material_reco -> {
                    // Implement your logout logic here
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    alertDialogShow(this,getString(R.string.under_development))
                    true
                }

                R.id.nav_cab -> {
                    // Implement your logout logic here
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    alertDialogShow(this,getString(R.string.under_development))
                    true
                }

                R.id.nav_vendor -> {
                    // Implement your logout logic here
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    alertDialogShow(this,getString(R.string.under_development))
                    true
                }

                R.id.nav_reimbursement -> {
                    // Implement your logout logic here
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    alertDialogShow(this,getString(R.string.under_development))
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

    private fun performLogout() {
        prefMain.deleteAll()
        Utils.jumpActivity(this, LoginActivity::class.java)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_dashboard)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}

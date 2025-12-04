package com.atvantiq.wfms.ui.screens

import android.os.Bundle
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
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
import com.atvantiq.wfms.databinding.ActivityDashboardBinding
import com.atvantiq.wfms.databinding.NavHeaderDashboardBinding
import com.atvantiq.wfms.models.loginResponse.User
import com.atvantiq.wfms.ui.screens.login.LoginActivity
import com.atvantiq.wfms.utils.Utils
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.firebase.messaging.FirebaseMessaging
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.play.core.appupdate.*
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.InstallStatus


@AndroidEntryPoint
class DashboardActivity : BaseBindingActivity<ActivityDashboardBinding>() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private val navController: androidx.navigation.NavController
        get() = findNavController(R.id.nav_host_fragment_content_dashboard)

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->

    }

    private lateinit var appUpdateManager: AppUpdateManager

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_dashboard)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        setSupportActionBar(binding.appBarDashboard.toolbar)
        setupNavigationDrawer()

        var userData = PrefMethods.getUserData(prefMain)
        setupDataDrawerHeader(userData)
        batterOptimizationCheck()
        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForUpdates()
    }

    private fun batterOptimizationCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                alertDialogShow(this,
                    getString(R.string.battery_optimization),
                    getString(R.string.battery_optimization_msg),
                    getString(R.string.ok),
                    { dialog, which ->
                        dialog.dismiss()
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    },
                    { dialog, which ->
                        dialog.dismiss()
                    })
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
        navHeaderBinding.appNameText.text = getString(R.string.app_name)
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
                        { dialog, which ->
                            dialog.dismiss()
                            performLogout()
                        },
                        { dialog, which ->
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
        FirebaseMessaging.getInstance().deleteToken()
        prefMain.deleteAll()
        Utils.jumpActivity(this, LoginActivity::class.java)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_dashboard)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun checkForUpdates() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                val options = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                appUpdateManager.startUpdateFlow(
                    info,
                    this,            // Activity
                    options          // AppUpdateOptions
                )
            } else if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                val options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                appUpdateManager.startUpdateFlow(
                    info,
                    this,
                    options
                )
                listenFlexibleUpdate()
            }
        }
    }

    private fun listenFlexibleUpdate() {
        appUpdateManager.registerListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                Snackbar.make(
                    findViewById(android.R.id.content), getString(R.string.update_downloaded),
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(getString(R.string.install)) {
                    appUpdateManager.completeUpdate()
                }.show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                val options = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                appUpdateManager.startUpdateFlow(info, this, options)
            }
        }
    }
}

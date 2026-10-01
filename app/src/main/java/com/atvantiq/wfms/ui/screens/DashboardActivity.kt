package com.atvantiq.wfms.ui.screens

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseBindingActivity
import com.atvantiq.wfms.databinding.ActivityDashboardBinding
import com.atvantiq.wfms.ui.dialogs.ThemePickerBottomSheet
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.play.core.appupdate.*
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.InstallStatus


@AndroidEntryPoint
class DashboardActivity : BaseBindingActivity<ActivityDashboardBinding>(){

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
        // Light status-bar icons over the green header, which is drawn behind the status bar.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setSupportActionBar(binding.appBarDashboard.toolbar)
        setupBottomNavigation()
        batterOptimizationCheck()
        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForUpdates()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.dashboard_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_palette -> {
                ThemePickerBottomSheet().show(supportFragmentManager, "ThemePicker")
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
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

    /**
     * Employee tabs: Dashboard · Work · Claims · More. Each tab keeps its own back stack; Back from
     * another tab returns to Dashboard, and Back on Dashboard leaves the app.
     */
    private fun setupBottomNavigation() {
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_dashboard, R.id.nav_attendance, R.id.nav_reimbursement, R.id.nav_more)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.appBarDashboard.toolbarTitle.text = destination.label
        }
        binding.bottomNav.setupWithNavController(navController)
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

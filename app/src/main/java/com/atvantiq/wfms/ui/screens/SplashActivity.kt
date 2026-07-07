package com.atvantiq.wfms.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.atvantiq.wfms.BuildConfig
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivitySimple
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.ui.screens.admin.SharedDashboardActivity
import com.atvantiq.wfms.ui.screens.login.LoginActivity
import com.atvantiq.wfms.utils.Utils
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashActivity : BaseActivitySimple() {

    companion object {
        const val ACTION_LOCATION_NOTIFICATION = "com.atvantiq.wfms.action.LOCATION_NOTIFICATION"
        const val ACTION_PUSH_NOTIFICATION = "com.atvantiq.wfms.action.PUSH_NOTIFICATION"
        private const val SPLASH_DELAY_MS = 2000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        checkAppVersion()
        if (isNotificationLaunch()) {
            routeNext()
        } else {
            splashTimer()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isNotificationLaunch()) {
            routeNext()
        }
    }

    private fun isNotificationLaunch(): Boolean {
        return intent?.action in setOf(ACTION_LOCATION_NOTIFICATION, ACTION_PUSH_NOTIFICATION)
    }

    private fun checkAppVersion() {
        findViewById<TextView>(R.id.versionText).text = "V${BuildConfig.VERSION_NAME}"
    }

    private fun splashTimer() {
        // lifecycleScope cancels automatically on destroy, so routeNext() can never
        // fire on a finished activity (avoids the leak + double-launch of the old Handler).
        lifecycleScope.launch {
            delay(SPLASH_DELAY_MS)
            routeNext()
        }
    }

    private fun routeNext() {
        val token: String? = try {
            PrefMethods.getUserToken(prefMain)
        } catch (e: Exception) {
            Log.e("SplashActivity", "Failed to read secure prefs even after recovery", e)
            null
        }

        if (token.isNullOrBlank()) {
            Utils.jumpActivity(this, LoginActivity::class.java)
            finish()
            return
        }

        val user = try {
            PrefMethods.getUserData(prefMain)
        } catch (e: Exception) {
            Log.e("SplashActivity", "Failed to read user data", e)
            null
        }

        if (user == null) {
            Utils.jumpActivity(this, LoginActivity::class.java)
            finish()
            return
        }

        val role = user.role ?: ""
        val permissions = user.permissions
        val target = if (role.equals(ValConstants.ROLE_EMPLOYEE, ignoreCase = true)) {
            DashboardActivity::class.java
        } else {
            SharedDashboardActivity::class.java
        }

        Utils.jumpActivityWithData(this, target, Bundle().apply {
            putParcelableArrayList(SharingKeys.ROLE_PERMISSIONS, ArrayList(permissions.orEmpty()))
        })
        finish()
    }
}

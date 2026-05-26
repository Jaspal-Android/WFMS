package com.atvantiq.wfms.ui.screens

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

@AndroidEntryPoint
class SplashActivity : BaseActivitySimple() {

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
        splashTimer()
    }

    private fun checkAppVersion() {
        findViewById<TextView>(R.id.versionText).text = "V${BuildConfig.VERSION_NAME}"
    }

    private fun splashTimer() {
        Handler(mainLooper).postDelayed({
            routeNext()
        }, 2000)
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
            putParcelableArrayList(SharingKeys.ROLE_PERMISSIONS, permissions as ArrayList)
        })
        finish()
    }
}
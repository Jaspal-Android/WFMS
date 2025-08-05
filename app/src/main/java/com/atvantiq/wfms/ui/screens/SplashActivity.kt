package com.atvantiq.wfms.ui.screens

import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.data.prefs.PrefMain
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.ui.screens.login.LoginActivity
import com.atvantiq.wfms.utils.Utils
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var prefMain: SecurePrefMain

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        splashTimer()
    }

    private fun splashTimer() {
        var token:String? = PrefMethods.getUserToken(prefMain)
        Handler(mainLooper).postDelayed({
            //if(token.isNullOrEmpty() || token.isNullOrBlank()){
            //    Utils.jumpActivity(this, LoginActivity::class.java)
            //}else{
                Utils.jumpActivity(this, DashboardActivity::class.java)
            //}
            finish()
        }, 2000)
    }
}
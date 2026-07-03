package com.atvantiq.wfms.app

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import com.atvantiq.wfms.BuildConfig
import com.atvantiq.wfms.utils.ThemeManager
import com.facebook.stetho.Stetho
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MApplication : Application() {
	
	override fun onCreate() {
		super.onCreate()
		//stetho only working debug
        FirebaseApp.initializeApp(this)
        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(true)

		if (BuildConfig.DEBUG) {
			Stetho.initializeWithDefaults(this)
		}
		instance = this

		provider = ViewModelProvider.AndroidViewModelFactory(this)

		ThemeManager.applyStoredDarkMode(this)
	}
	
	companion object {
		lateinit var instance: MApplication
		lateinit var provider: ViewModelProvider.NewInstanceFactory
	}
}

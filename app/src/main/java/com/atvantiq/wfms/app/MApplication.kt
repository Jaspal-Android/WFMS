package com.atvantiq.wfms.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import com.facebook.stetho.BuildConfig
import com.facebook.stetho.Stetho
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MApplication : Application() {
	
	override fun onCreate() {
		super.onCreate()
		//stetho only working debug
		if (BuildConfig.DEBUG) {
			Stetho.initializeWithDefaults(this)
		}

		instance = this

		provider = ViewModelProvider.AndroidViewModelFactory(this)

		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
	}
	
	companion object {
		lateinit var instance: MApplication
		lateinit var provider: ViewModelProvider.NewInstanceFactory
		var a = 10
	}
}
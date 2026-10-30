package com.atvantiq.wfms.app

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import com.atvantiq.wfms.BuildConfig
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.utils.ThemeManager
import com.atvantiq.wfms.debug.DebugTools
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class MApplication : Application() {

	@Inject
	lateinit var securePrefs: Lazy<SecurePrefMain>
	
	override fun onCreate() {
		super.onCreate()
		Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else CrashReportingTree())
		// Firebase is already initialised by its content provider before this runs.
		FirebaseCrashlytics.getInstance()
			.setCrashlyticsCollectionEnabled(true)

		if (BuildConfig.DEBUG) {
			DebugTools.init(this)
		}
		instance = this

		provider = ViewModelProvider.AndroidViewModelFactory(this)

		ThemeManager.applyStoredDarkMode(this)

		// Opening the encrypted preferences goes through the Keystore and can take hundreds of
		// milliseconds. Start it now, off the main thread, so the first screen that needs the
		// session does not wait for it. The singleton is built once; callers that arrive while it
		// is being built simply wait for the same instance.
		Thread({ runCatching { securePrefs.get() } }, "secure-prefs-warmup").start()
	}
	
	companion object {
		lateinit var instance: MApplication
		lateinit var provider: ViewModelProvider.NewInstanceFactory
	}
}

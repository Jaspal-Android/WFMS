package com.atvantiq.wfms.ui.screens

import android.app.Application
import timber.log.Timber
import android.os.SystemClock
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SplashVM @Inject constructor(
    application: Application,
    private val prefMain: SecurePrefMain
) : BaseViewModel(application) {

    /** Where the app opens; set once the saved session has been read. */
    val destination = MutableLiveData<SplashTarget>()

    /** Reading the secure prefs goes through the Keystore; replaced in tests. */
    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /** Clock used for the minimum splash time; replaced in tests. */
    internal var now: () -> Long = { SystemClock.elapsedRealtime() }

    private var resolving: Job? = null

    /**
     * Reads the saved session off the main thread, keeps the splash up for at least
     * [minVisibleMs], then publishes [destination]. A later call (a tapped notification) restarts
     * with its own, shorter wait.
     */
    fun resolve(minVisibleMs: Long) {
        if (destination.value != null) return
        resolving?.cancel()
        resolving = viewModelScope.launch {
            val startedAt = now()
            val resolved = withContext(ioDispatcher) { readDestination() }
            val remaining = minVisibleMs - (now() - startedAt)
            if (remaining > 0) delay(remaining)
            destination.value = resolved
        }
    }

    private fun readDestination(): SplashTarget {
        val token: String? = try {
            PrefMethods.getUserToken(prefMain)
        } catch (e: Exception) {
            Timber.e(e, "Failed to read secure prefs even after recovery")
            null
        }
        val user = if (token.isNullOrBlank()) {
            null
        } else {
            try {
                PrefMethods.getUserData(prefMain)
            } catch (e: Exception) {
                Timber.e(e, "Failed to read user data")
                null
            }
        }
        return SplashRouting.targetFor(token, user)
    }
}

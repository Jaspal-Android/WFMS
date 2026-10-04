package com.atvantiq.wfms.debug

import android.app.Application
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import okhttp3.Interceptor

/**
 * Developer tooling that exists only in debug builds. The release source set has a no-op twin of
 * this object, so no inspection code is compiled into, or shipped in, a release.
 */
object DebugTools {

    fun init(application: Application) {
        Stetho.initializeWithDefaults(application)
    }

    /** Lets Chrome DevTools inspect the app's network traffic. */
    fun networkInterceptor(): Interceptor? = StethoInterceptor()
}

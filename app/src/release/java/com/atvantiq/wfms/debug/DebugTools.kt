package com.atvantiq.wfms.debug

import android.app.Application
import okhttp3.Interceptor

/** Release twin of the debug `DebugTools`: nothing to initialise and nothing to intercept. */
@Suppress("UNUSED_PARAMETER")
object DebugTools {

    fun init(application: Application) = Unit

    fun networkInterceptor(): Interceptor? = null
}

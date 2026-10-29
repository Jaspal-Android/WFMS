package com.atvantiq.wfms.app

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * The Timber tree planted in release builds. Nothing is written to logcat; warnings and errors
 * become Crashlytics breadcrumbs, and an error that carries a throwable is recorded as a
 * non-fatal, so the crash report shows what led up to a failure. Debug and info messages are
 * dropped. Log messages must never contain personal data (phone numbers, tokens, addresses).
 */
class CrashReportingTree : Timber.Tree() {

    public override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= android.util.Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        runCatching {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.log(if (tag == null) message else "$tag: $message")
            if (priority >= android.util.Log.ERROR && t != null) crashlytics.recordException(t)
        }
    }
}

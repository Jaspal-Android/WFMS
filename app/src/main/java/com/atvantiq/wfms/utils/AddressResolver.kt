package com.atvantiq.wfms.utils

import java.util.concurrent.Executor

/**
 * Turns coordinates into an address without ever blocking the caller.
 *
 * `Geocoder.getFromLocation` is a blocking network call. On Android 12 and below it used to run
 * on whichever thread asked, which was the main thread (twice in `AttendanceDetailActivity.onCreate`),
 * so a slow connection froze the screen. [geocode] is that blocking call: it runs on [background],
 * and the result is always delivered on [mainThread], where callers touch the UI.
 *
 * @param geocode blocking lookup; returns the formatted address, or null when there is none
 */
class AddressResolver(
    private val geocode: (latitude: Double, longitude: Double) -> String?,
    private val background: Executor,
    private val mainThread: Executor,
    private val notFound: String
) {

    fun resolve(latitude: Double, longitude: Double, onResult: (String) -> Unit) {
        background.execute {
            // A failed lookup (no network, no geocoder backend) reads the same as "no address".
            val address = try {
                geocode(latitude, longitude)
            } catch (e: Exception) {
                null
            }
            mainThread.execute { onResult(address?.takeIf { it.isNotBlank() } ?: notFound) }
        }
    }
}

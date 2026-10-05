package com.atvantiq.wfms.utils

import android.Manifest
import android.content.Context
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Foreground location is requested first (with notifications) and background location separately
 * afterwards, so the result of the foreground request is judged on location alone.
 */
class LocationPermissionStagingTest {

    private val fine = Manifest.permission.ACCESS_FINE_LOCATION
    private val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
    private val notifications = "android.permission.POST_NOTIFICATIONS"

    @Test
    fun `location granted is enough even when notifications are denied`() {
        val results = mapOf(fine to true, coarse to true, notifications to false)

        assertTrue(PermissionUtils.areGranted(results, PermissionUtils.LOCATION_PERMISSIONS))
    }

    @Test
    fun `approximate-only location is not treated as granted`() {
        val results = mapOf(fine to false, coarse to true)

        assertFalse(PermissionUtils.areGranted(results, PermissionUtils.LOCATION_PERMISSIONS))
    }

    @Test
    fun `a permission missing from the result map counts as denied`() {
        assertFalse(PermissionUtils.areGranted(mapOf(fine to true), PermissionUtils.LOCATION_PERMISSIONS))
        assertFalse(PermissionUtils.areGranted(emptyMap(), PermissionUtils.LOCATION_PERMISSIONS))
    }

    @Test
    fun `below Android 13 there is no notification permission to request`() {
        // Local unit tests run with Build.VERSION.SDK_INT == 0.
        assertTrue(PermissionUtils.notificationPermissions().isEmpty())
    }

    @Test
    fun `below Android 10 background location needs no runtime grant`() {
        assertTrue(PermissionUtils.hasBackgroundLocationPermission(mockk<Context>(relaxed = true)))
    }

    @Test
    fun `granted location is the outcome whatever the notification answer`() {
        val results = mapOf(fine to true, coarse to true, notifications to false)

        assertEquals(
            PermissionUtils.LocationOutcome.GRANTED,
            PermissionUtils.locationOutcome(results) { error("not asked when granted") }
        )
    }

    @Test
    fun `a denial the system will not prompt for again sends the user to Settings`() {
        val results = mapOf(fine to false, coarse to false)

        assertEquals(
            PermissionUtils.LocationOutcome.DENIED_PERMANENTLY,
            PermissionUtils.locationOutcome(results) { false }
        )
    }

    @Test
    fun `a denial the user can still be asked about again keeps the rationale path`() {
        val results = mapOf(fine to false, coarse to false)

        assertEquals(
            PermissionUtils.LocationOutcome.DENIED_CAN_RETRY,
            PermissionUtils.locationOutcome(results) { it == fine }
        )
    }

    @Test
    fun `approximate-only location is a denial, not a grant`() {
        val results = mapOf(fine to false, coarse to true)

        assertEquals(
            PermissionUtils.LocationOutcome.DENIED_CAN_RETRY,
            PermissionUtils.locationOutcome(results) { true }
        )
    }

    @Test
    fun `an empty result is treated as denied`() {
        assertEquals(
            PermissionUtils.LocationOutcome.DENIED_PERMANENTLY,
            PermissionUtils.locationOutcome(emptyMap()) { false }
        )
    }
}

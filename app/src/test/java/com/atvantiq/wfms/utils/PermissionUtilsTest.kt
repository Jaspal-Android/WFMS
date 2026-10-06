package com.atvantiq.wfms.utils

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionUtilsTest {

    private val location = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @Test
    fun `granted location is enough even when notifications are denied`() {
        val results = mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to true,
            Manifest.permission.ACCESS_COARSE_LOCATION to true,
            Manifest.permission.POST_NOTIFICATIONS to false
        )

        assertTrue(PermissionUtils.areGranted(results, location))
    }

    @Test
    fun `approximate-only location is not treated as granted`() {
        val results = mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to false,
            Manifest.permission.ACCESS_COARSE_LOCATION to true
        )

        assertFalse(PermissionUtils.areGranted(results, location))
    }

    @Test
    fun `a permission missing from the result map counts as denied`() {
        val results = mapOf(Manifest.permission.ACCESS_FINE_LOCATION to true)

        assertFalse(PermissionUtils.areGranted(results, location))
    }

    @Test
    fun `an empty result map is denied`() {
        assertFalse(PermissionUtils.areGranted(emptyMap(), location))
    }

    @Test
    fun `allow all the time granted`() {
        assertEquals(
            PermissionUtils.LocationOutcome.GRANTED,
            PermissionUtils.backgroundLocationOutcome(granted = true) { false }
        )
    }

    @Test
    fun `allow all the time declined once can be asked again`() {
        assertEquals(
            PermissionUtils.LocationOutcome.DENIED_CAN_RETRY,
            PermissionUtils.backgroundLocationOutcome(granted = false) { it == Manifest.permission.ACCESS_BACKGROUND_LOCATION }
        )
    }

    @Test
    fun `allow all the time Android will not ask for again needs Settings`() {
        assertEquals(
            PermissionUtils.LocationOutcome.DENIED_PERMANENTLY,
            PermissionUtils.backgroundLocationOutcome(granted = false) { false }
        )
    }

    @Test
    fun `approximate only is its own outcome, not a denial`() {
        val results = mapOf(Manifest.permission.ACCESS_FINE_LOCATION to false, Manifest.permission.ACCESS_COARSE_LOCATION to true)
        assertEquals(PermissionUtils.LocationOutcome.APPROXIMATE_ONLY, PermissionUtils.locationOutcome(results) { false })
        assertEquals(PermissionUtils.LocationOutcome.APPROXIMATE_ONLY, PermissionUtils.locationOutcome(results) { true })
    }

    @Test
    fun `precise and approximate together is granted`() {
        val results = mapOf(Manifest.permission.ACCESS_FINE_LOCATION to true, Manifest.permission.ACCESS_COARSE_LOCATION to true)
        assertEquals(PermissionUtils.LocationOutcome.GRANTED, PermissionUtils.locationOutcome(results) { false })
    }
}

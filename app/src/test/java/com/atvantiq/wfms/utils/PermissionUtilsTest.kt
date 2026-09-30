package com.atvantiq.wfms.utils

import android.Manifest
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
}

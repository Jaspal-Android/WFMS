package com.atvantiq.wfms.utils

import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectivityReceiverTest {

    private fun network(hasInternet: Boolean): NetworkCapabilities = mockk {
        every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns hasInternet
        // Deliberately not stubbed: the transport must not matter, so a check that asks for
        // Wi-Fi or cellular would fail with an unmocked-call error.
    }

    @Test
    fun `a network that reaches the internet counts whatever its transport`() {
        assertTrue(ConnectivityReceiver.hasInternetCapability(network(hasInternet = true)))
    }

    @Test
    fun `a network without internet access does not count`() {
        assertFalse(ConnectivityReceiver.hasInternetCapability(network(hasInternet = false)))
    }

    @Test
    fun `no active network means offline`() {
        assertFalse(ConnectivityReceiver.hasInternetCapability(null))
    }
}

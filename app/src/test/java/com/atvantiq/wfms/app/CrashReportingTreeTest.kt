package com.atvantiq.wfms.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportingTreeTest {

    private val tree = CrashReportingTree()

    @Test
    fun `debug and info messages are dropped in release`() {
        assertFalse(tree.isLoggable("Tag", android.util.Log.DEBUG))
        assertFalse(tree.isLoggable("Tag", android.util.Log.INFO))
    }

    @Test
    fun `warnings and errors are reported`() {
        assertTrue(tree.isLoggable("Tag", android.util.Log.WARN))
        assertTrue(tree.isLoggable("Tag", android.util.Log.ERROR))
    }

    @Test
    fun `logging without Firebase initialised does not throw`() {
        tree.e(IllegalStateException("boom"), "Something failed")
    }
}

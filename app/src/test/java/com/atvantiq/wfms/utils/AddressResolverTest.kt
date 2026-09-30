package com.atvantiq.wfms.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executor

class AddressResolverTest {

    private class QueueExecutor : Executor {
        val pending = ArrayDeque<Runnable>()
        override fun execute(command: Runnable) { pending.addLast(command) }
        fun runAll() { while (pending.isNotEmpty()) pending.removeFirst().run() }
    }

    private val background = QueueExecutor()
    private val mainThread = QueueExecutor()

    private fun resolver(geocode: (Double, Double) -> String?) =
        AddressResolver(geocode, background, mainThread, notFound = "NOT_FOUND")

    @Test
    fun `lookup is deferred to the background executor, never run on the caller`() {
        var lookups = 0
        val results = mutableListOf<String>()

        resolver { _, _ -> lookups++; "Sector 5" }.resolve(1.0, 2.0) { results.add(it) }

        assertEquals("caller must not perform the blocking lookup", 0, lookups)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `result is delivered through the main-thread executor`() {
        val results = mutableListOf<String>()
        resolver { _, _ -> "Sector 5" }.resolve(1.0, 2.0) { results.add(it) }

        background.runAll()
        assertTrue("must not call back until the main thread runs it", results.isEmpty())

        mainThread.runAll()
        assertEquals(listOf("Sector 5"), results)
    }

    @Test
    fun `coordinates are passed to the lookup`() {
        var seen: Pair<Double, Double>? = null
        resolver { lat, lon -> seen = lat to lon; "x" }.resolve(28.6, 77.2) { }
        background.runAll()
        assertEquals(28.6 to 77.2, seen)
    }

    @Test
    fun `null, blank and failed lookups all report not found`() {
        val results = mutableListOf<String>()
        resolver { _, _ -> null }.resolve(0.0, 0.0) { results.add(it) }
        resolver { _, _ -> "  " }.resolve(0.0, 0.0) { results.add(it) }
        resolver { _, _ -> throw java.io.IOException("no network") }.resolve(0.0, 0.0) { results.add(it) }

        background.runAll()
        mainThread.runAll()

        assertEquals(listOf("NOT_FOUND", "NOT_FOUND", "NOT_FOUND"), results)
    }
}

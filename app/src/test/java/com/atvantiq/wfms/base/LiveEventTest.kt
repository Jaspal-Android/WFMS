package com.atvantiq.wfms.base

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LiveEventTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class TestOwner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
        fun moveTo(state: Lifecycle.State) { registry.currentState = state }
    }

    private fun startedOwner() = TestOwner().apply { moveTo(Lifecycle.State.STARTED) }

    @Test
    fun `an event is delivered once to an active observer`() {
        val event = LiveEvent<String>()
        val received = mutableListOf<String>()
        event.observe(startedOwner()) { received += it }

        event.value = "open"

        assertEquals(listOf("open"), received)
    }

    @Test
    fun `a new observer does not receive an event that was already delivered`() {
        val event = LiveEvent<String>()
        event.observe(startedOwner()) { }
        event.value = "open"

        val received = mutableListOf<String>()
        event.observe(startedOwner()) { received += it }   // e.g. the view was recreated

        assertTrue(received.isEmpty())
    }

    @Test
    fun `an event posted while the screen is inactive waits for it`() {
        val owner = TestOwner().apply { moveTo(Lifecycle.State.CREATED) }
        val event = LiveEvent<String>()
        val received = mutableListOf<String>()
        event.observe(owner) { received += it }

        event.value = "login done"
        assertTrue(received.isEmpty())

        owner.moveTo(Lifecycle.State.STARTED)
        assertEquals(listOf("login done"), received)
    }

    @Test
    fun `the same value posted twice is delivered twice`() {
        val event = LiveEvent<String>()
        val received = mutableListOf<String>()
        event.observe(startedOwner()) { received += it }

        event.value = "name is empty"
        event.value = "name is empty"

        assertEquals(listOf("name is empty", "name is empty"), received)
    }

    @Test
    fun `leaving and returning to the screen does not replay the event`() {
        val owner = startedOwner()
        val event = LiveEvent<String>()
        val received = mutableListOf<String>()
        event.observe(owner) { received += it }
        event.value = "open"

        owner.moveTo(Lifecycle.State.CREATED)
        owner.moveTo(Lifecycle.State.STARTED)

        assertEquals(listOf("open"), received)
    }

    @Test
    fun `postValue also delivers once`() {
        val event = LiveEvent<Int>()
        val received = mutableListOf<Int>()
        event.observe(startedOwner()) { received += it }

        event.postValue(7)

        assertEquals(listOf(7), received)
    }

    @Test
    fun `value still reads the last event for callers that inspect it`() {
        val event = LiveEvent<String>()

        event.value = "open"

        assertEquals("open", event.value)
    }
}

package com.atvantiq.wfms.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiStateTest {

    @Test
    fun `consumeOnce is true only for the first caller`() {
        val state = ApiState.success("ok")

        assertTrue(state.consumeOnce())
        assertFalse(state.consumeOnce())
    }

    @Test
    fun `each new state can be consumed independently`() {
        val first = ApiState.success("a")
        val second = ApiState.success("a")

        assertTrue(first.consumeOnce())
        assertTrue(second.consumeOnce())
    }

    @Test
    fun `a replayed state is not consumed again by a second observer`() {
        val replayed = ApiState.loading<String>()

        val firstObserverHandled = replayed.consumeOnce()
        val recreatedViewHandled = replayed.consumeOnce()

        assertTrue(firstObserverHandled)
        assertFalse(recreatedViewHandled)
    }
}

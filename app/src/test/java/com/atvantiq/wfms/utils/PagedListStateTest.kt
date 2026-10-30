package com.atvantiq.wfms.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PagedListStateTest {

    private val pageSize = 10
    private val state = PagedListState(pageSize)

    @Test
    fun `a new list has loaded nothing and asks for page 1`() {
        assertEquals(0, state.loadedPage)
        assertFalse(state.isLoading)

        assertEquals(1, state.startNextPage())
        assertTrue(state.isLoading)
        assertTrue(state.isLoadingFirstPage)
    }

    @Test
    fun `pages advance one at a time as each one arrives`() {
        state.startNextPage()
        assertEquals(true, state.onPageReceived(pageSize))
        assertEquals(1, state.loadedPage)

        assertEquals(2, state.startNextPage())
        assertFalse(state.isLoadingFirstPage)
        assertEquals(false, state.onPageReceived(pageSize))
        assertEquals(2, state.loadedPage)
    }

    @Test
    fun `a second page cannot be requested while one is in flight`() {
        state.startNextPage()

        assertNull(state.startNextPage())
        assertNull(state.startNextPage())
    }

    @Test
    fun `a failed page is requested again instead of being skipped`() {
        state.startNextPage()
        state.onPageReceived(pageSize)
        assertEquals(2, state.startNextPage())

        state.onRequestFailed()

        assertEquals(1, state.loadedPage)
        assertFalse(state.isLoading)
        assertEquals("the same page is retried", 2, state.startNextPage())
    }

    @Test
    fun `a rejected or empty response releases the lock so the list can load more`() {
        state.startNextPage()
        state.onRequestFailed() // e.g. a non-200 body, or a body that is null

        assertFalse(state.isLoading)
        assertEquals(1, state.startNextPage())
    }

    @Test
    fun `a short page marks the end of the list`() {
        state.startNextPage()

        state.onPageReceived(pageSize - 1)

        assertTrue(state.isLastPage)
        assertNull(state.startNextPage())
    }

    @Test
    fun `an empty page marks the end of the list`() {
        state.startNextPage()
        state.onPageReceived(pageSize)
        state.startNextPage()

        state.onPageReceived(0)

        assertTrue(state.isLastPage)
        assertNull(state.startNextPage())
    }

    @Test
    fun `a full page does not mark the end`() {
        state.startNextPage()

        state.onPageReceived(pageSize)

        assertFalse(state.isLastPage)
    }

    @Test
    fun `restart drops everything in flight and starts again from page 1`() {
        state.startNextPage()
        state.onPageReceived(pageSize)
        state.startNextPage() // page 2 still running
        state.onPageReceived(3) // ... and it turned out to be the last page for the old query
        assertTrue(state.isLastPage)

        assertEquals(1, state.restart())

        assertEquals(0, state.loadedPage)
        assertFalse(state.isLastPage)
        assertTrue(state.isLoading)
        assertTrue(state.isLoadingFirstPage)
    }

    @Test
    fun `after a restart the answer is applied as page 1 even if page 3 was being loaded`() {
        repeat(2) { state.startNextPage(); state.onPageReceived(pageSize) }
        state.startNextPage() // page 3 in flight (the old query)

        state.restart() // user changed the filter

        assertEquals("replace the list, do not append to it", true, state.onPageReceived(pageSize))
        assertEquals(1, state.loadedPage)
    }

    @Test
    fun `a result nobody asked for is ignored`() {
        // e.g. a replayed old LiveData value after the view was recreated
        assertNull(state.onPageReceived(pageSize))
        assertEquals(0, state.loadedPage)

        state.startNextPage()
        state.onPageReceived(pageSize)
        assertNull("the same result twice must not advance the page", state.onPageReceived(pageSize))
        assertEquals(1, state.loadedPage)
    }
}

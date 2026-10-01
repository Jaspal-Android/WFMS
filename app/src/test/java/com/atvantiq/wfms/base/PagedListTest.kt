package com.atvantiq.wfms.base

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.utils.Utils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@ExperimentalCoroutinesApi
class PagedListTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private data class Page(val accepted: Boolean, val items: List<Item>)
    private class Item(var name: String)

    private class ListViewModel(application: Application, fetch: suspend (Int, Int) -> Page) :
        BaseViewModel(application) {
        val list = PagedList<Page, Item>(
            pageSize = PAGE_SIZE,
            fetch = fetch,
            pageItems = { if (it.accepted) it.items else null }
        )
    }

    private val dispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val requests = mutableListOf<Int>()
    private val answers = ArrayDeque<CompletableDeferred<Page>>()
    private lateinit var viewModel: ListViewModel

    private val state get() = viewModel.list.state.value!!

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(Utils)
        every { Utils.isInternet(application) } returns true
        viewModel = ListViewModel(application) { page, _ ->
            requests += page
            CompletableDeferred<Page>().also { answers.addLast(it) }.await()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun items(vararg names: String) = names.map { Item(it) }
    private fun fullPage(prefix: String) = Page(true, items(*Array(PAGE_SIZE) { "$prefix$it" }))
    private fun names() = state.items.map { it.name }

    /** Lets the pending request start, answers it, and lets the answer be applied. */
    private fun answer(page: Page) {
        dispatcher.scheduler.advanceUntilIdle()
        answers.removeFirst().complete(page)
        dispatcher.scheduler.advanceUntilIdle()
    }

    private fun fail(error: Exception) {
        dispatcher.scheduler.advanceUntilIdle()
        answers.removeFirst().completeExceptionally(error)
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `first open shows the full-screen progress, then the first page`() {
        viewModel.list.open()
        assertTrue(state.isLoadingFirstPage)
        assertFalse(state.isRefreshing)

        answer(Page(true, items("a", "b")))

        assertEquals(listOf("a", "b"), names())
        assertFalse(state.isLoadingFirstPage)
        assertEquals(listOf(1), requests)
    }

    @Test
    fun `reopening keeps the list on screen and refreshes it in the background`() {
        viewModel.list.open()
        answer(Page(true, items("a", "b")))

        viewModel.list.open()

        assertEquals("kept while refreshing", listOf("a", "b"), names())
        assertTrue(state.isRefreshing)
        assertFalse("no full-screen progress over a kept list", state.isLoadingFirstPage)

        answer(Page(true, items("a", "c")))

        assertEquals("page 1 replaces the list, it is not appended", listOf("a", "c"), names())
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `next pages append, show the footer while loading and stop after a short page`() {
        viewModel.list.open()
        answer(fullPage("p1-"))

        viewModel.list.loadNextPage()
        assertTrue(state.isLoadingMore)
        answer(Page(true, items("last")))

        assertEquals(PAGE_SIZE + 1, state.items.size)
        assertFalse(state.isLoadingMore)

        viewModel.list.loadNextPage()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("nothing after the last page", listOf(1, 2), requests)
    }

    @Test
    fun `a second scroll-to-end while a page is loading asks for nothing more`() {
        viewModel.list.open()
        answer(fullPage("p1-"))

        viewModel.list.loadNextPage()
        viewModel.list.loadNextPage()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(1, 2), requests)
    }

    @Test
    fun `reload for a new query clears the old items straight away`() {
        viewModel.list.open()
        answer(Page(true, items("old")))

        viewModel.list.reload()

        assertTrue(state.items.isEmpty())
        assertTrue(state.isLoadingFirstPage)
    }

    @Test
    fun `a failed page is reported once, keeps the list and is requested again`() {
        viewModel.list.open()
        answer(fullPage("p1-"))
        viewModel.list.loadNextPage()

        fail(IOException("offline"))

        val failure = viewModel.list.failure.value!!
        assertEquals(Status.ERROR, failure.status)
        assertTrue(failure.consumeOnce())
        assertFalse("a replay to a new observer is not shown again", failure.consumeOnce())
        assertEquals(PAGE_SIZE, state.items.size)
        assertFalse(state.isLoadingMore)

        viewModel.list.loadNextPage()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("the same page again", listOf(1, 2, 2), requests)
    }

    @Test
    fun `a page the server rejects is reported with its body and requested again`() {
        val rejected = Page(false, emptyList())
        viewModel.list.open()

        answer(rejected)

        assertEquals(Status.SUCCESS, viewModel.list.failure.value?.status)
        assertEquals(rejected, viewModel.list.failure.value?.response)
        assertFalse(state.isEmpty)

        viewModel.list.loadNextPage()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(1, 1), requests)
    }

    @Test
    fun `an empty first page shows the empty state`() {
        viewModel.list.open()
        answer(Page(true, emptyList()))

        assertTrue(state.isEmpty)
    }

    @Test
    fun `an answer to a superseded query never reaches the list`() {
        viewModel.list.open()
        dispatcher.scheduler.advanceUntilIdle()
        val stale = answers.removeFirst()

        viewModel.list.reload()
        answer(Page(true, items("new")))
        stale.complete(Page(true, items("stale")))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("new"), names())
    }

    @Test
    fun `offline reports no internet without calling the API`() {
        every { Utils.isInternet(application) } returns false

        viewModel.list.open()

        assertEquals(Status.ERROR, viewModel.list.failure.value?.status)
        assertFalse(state.isLoadingFirstPage)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `an item changed elsewhere is redrawn`() {
        viewModel.list.open()
        answer(Page(true, items("a")))

        viewModel.list.updateItem(0) { it.name = "changed" }

        assertEquals(listOf("changed"), names())
    }

    @Test
    fun `nothing is published for a position that does not exist`() {
        viewModel.list.open()
        answer(Page(true, items("a")))
        val before = viewModel.list.state.value

        viewModel.list.updateItem(5) { it.name = "x" }

        assertEquals(before, viewModel.list.state.value)
        assertNull(viewModel.list.failure.value)
    }

    private companion object {
        const val PAGE_SIZE = 3
    }
}

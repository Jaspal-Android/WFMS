package com.atvantiq.wfms.base

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** A "restored" ViewModel is one built on the same [SavedStateHandle] as the one that was lost. */
class SavedStateExtTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private data class Pick(val id: Long, val name: String, val tags: List<String> = emptyList())

    private class Form(handle: SavedStateHandle) {
        val name = handle.savedField("name", "")
        val outstation = handle.savedField("outstation", false)
        val picked = handle.savedField<String?>("picked", null)
        var projectId: Long? by handle.savedValue("projectId", null)
        var site: Pick? by handle.savedObject("site")
        val entries = handle.savedList<Pick>("entries")
        var offered: List<Pick> by handle.savedListValue("offered")
    }

    @Test
    fun `a field starts at its initial value and writes every change through`() {
        val handle = SavedStateHandle()
        val form = Form(handle)

        assertEquals("", form.name.get())
        form.name.set("Site 12")

        assertEquals("Site 12", handle.get<String>("name"))
    }

    @Test
    fun `a recreated form starts from what was saved`() {
        val handle = SavedStateHandle()
        Form(handle).apply {
            name.set("Site 12")
            outstation.set(true)
            picked.set("Client A")
            projectId = 42L
            site = Pick(7, "Tower", listOf("a", "b"))
            entries.value = listOf(Pick(1, "x"), Pick(2, "y", listOf("z")))
        }

        val restored = Form(handle)

        assertEquals("Site 12", restored.name.get())
        assertEquals(true, restored.outstation.get())
        assertEquals("Client A", restored.picked.get())
        assertEquals(42L, restored.projectId)
        assertEquals(Pick(7, "Tower", listOf("a", "b")), restored.site)
        assertEquals(listOf(Pick(1, "x"), Pick(2, "y", listOf("z"))), restored.entries.value)
    }

    @Test
    fun `a field cleared to null stays null after restore, not back at its initial value`() {
        val handle = SavedStateHandle()
        Form(handle).apply {
            picked.set("Client A")
            picked.set(null)
        }

        assertNull(Form(handle).picked.get())
    }

    @Test
    fun `a fresh form has nothing picked and no entries`() {
        val form = Form(SavedStateHandle())

        assertNull(form.projectId)
        assertNull(form.site)
        assertEquals(emptyList<Pick>(), form.entries.value)
    }

    @Test
    fun `clearing an object or a list is saved too`() {
        val handle = SavedStateHandle()
        Form(handle).apply {
            site = Pick(7, "Tower")
            entries.value = listOf(Pick(1, "x"))
            site = null
            entries.value = emptyList()
        }

        val restored = Form(handle)

        assertNull(restored.site)
        assertEquals(emptyList<Pick>(), restored.entries.value)
    }

    @Test
    fun `a list property is saved when assigned and restored as the real row type`() {
        val handle = SavedStateHandle()
        val form = Form(handle)
        assertEquals(emptyList<Pick>(), form.offered)

        form.offered = listOf(Pick(5, "Site A"), Pick(6, "Site B", listOf("t")))

        assertEquals(listOf(Pick(5, "Site A"), Pick(6, "Site B", listOf("t"))), Form(handle).offered)
    }

    @Test
    fun `postValue is saved like setValue`() {
        val handle = SavedStateHandle()
        Form(handle).entries.postValue(listOf(Pick(3, "z")))

        assertEquals(listOf(Pick(3, "z")), Form(handle).entries.value)
    }
}

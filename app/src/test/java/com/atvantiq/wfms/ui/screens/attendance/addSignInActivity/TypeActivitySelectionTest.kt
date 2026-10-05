package com.atvantiq.wfms.ui.screens.attendance.addSignInActivity

import com.atvantiq.wfms.models.activity.ActivityData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypeActivitySelectionTest {

    private val survey = ActivityData(1L, "Survey")
    private val install = ActivityData(2L, "Install")
    private val types = listOf(10L to "A", 20L to "B")

    private fun selection() = TypeActivitySelection().apply {
        setAvailable(10L, listOf(survey))
        setAvailable(20L, listOf(install))
    }

    @Test
    fun `options are grouped by type in the given order`() {
        assertEquals(
            listOf(10L to survey, 20L to install),
            selection().options(types).map { it.typeId to it.activity }
        )
        assertEquals(listOf(20L, 10L), selection().options(types.reversed()).map { it.typeId })
    }

    @Test
    fun `same activity offered by two types stays separate`() {
        val s = TypeActivitySelection().apply {
            setAvailable(10L, listOf(survey))
            setAvailable(20L, listOf(survey))
        }
        s.select(setOf(TypeActivityOption(10L, "A", survey)))

        assertEquals(setOf(1L), s.selectedIds(10L))
        assertTrue(s.selectedIds(20L).isEmpty())
    }

    @Test
    fun `retainTypes drops removed types but keeps the picks of the others`() {
        val s = selection()
        s.select(s.options(types).toSet())

        s.retainTypes(setOf(20L))

        assertTrue(s.selectedIds(10L).isEmpty())
        assertFalse(s.hasActivities(10L))
        assertEquals(setOf(2L), s.selectedIds(20L))
    }

    @Test
    fun `refreshed activities drop picks the server no longer offers`() {
        val s = selection()
        s.select(s.options(types).toSet())

        s.setAvailable(10L, listOf(ActivityData(9L, "Other")))

        assertTrue(s.selectedIds(10L).isEmpty())
    }

    @Test
    fun `coversAll needs a pick for every type and at least one type`() {
        val s = selection()
        assertFalse(s.coversAll(emptyList()))
        s.select(setOf(TypeActivityOption(10L, "A", survey)))
        assertFalse(s.coversAll(listOf(10L, 20L)))
        assertTrue(s.coversAll(listOf(10L)))
    }

    @Test
    fun `picks are reported on every change and come back from a restore`() {
        var saved = emptyList<TypePicks>()
        val first = TypeActivitySelection(onPicksChanged = { saved = it }).apply {
            setAvailable(10L, listOf(survey, install))
            select(setOf(TypeActivityOption(10L, "A", survey), TypeActivityOption(10L, "A", install)))
        }
        assertEquals(listOf(TypePicks(10L, listOf(1L, 2L))), saved)

        val restored = TypeActivitySelection(restoredPicks = saved)

        assertEquals(setOf(1L, 2L), restored.selectedIds(10L))
        assertEquals(first.selectedIds(10L), restored.selectedIds(10L))
    }

    @Test
    fun `a restored pick the server no longer offers is dropped when the offer is fetched again`() {
        val restored = TypeActivitySelection(restoredPicks = listOf(TypePicks(10L, listOf(1L, 99L))))

        restored.setAvailable(10L, listOf(survey, install))

        assertEquals(setOf(1L), restored.selectedIds(10L))
    }
}

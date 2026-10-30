package com.atvantiq.wfms.widgets

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiffByIdTest {

    private data class Row(val id: Long?, val title: String)

    private val diff = diffById<Row> { it.id }

    @Test
    fun `rows with the same id are the same row`() {
        assertTrue(diff.areItemsTheSame(Row(1, "a"), Row(1, "b")))
    }

    @Test
    fun `rows with different ids are different rows`() {
        assertFalse(diff.areItemsTheSame(Row(1, "a"), Row(2, "a")))
    }

    @Test
    fun `a row without an id is only the same row as itself`() {
        val row = Row(null, "a")

        assertTrue(diff.areItemsTheSame(row, row))
        assertFalse(diff.areItemsTheSame(row, Row(null, "a")))
        assertFalse(diff.areItemsTheSame(Row(null, "a"), Row(1, "a")))
    }

    @Test
    fun `equal rows are unchanged and a changed field is a change`() {
        assertTrue(diff.areContentsTheSame(Row(1, "a"), Row(1, "a")))
        assertFalse(diff.areContentsTheSame(Row(1, "a"), Row(1, "b")))
    }
}

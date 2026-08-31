package com.atvantiq.wfms.models.empDetail

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `employee/me` switched `circle` from a plain code string to an array of circle objects.
 * Both shapes must parse: the array is what the API sends now, the string is what older
 * builds cached in EMP_DATA on devices that are about to be upgraded.
 */
class EmpDataCircleParsingTest {

    private val gson = Gson()

    @Test
    fun `parses circle as array of objects (current API shape)`() {
        val json = """
            {"circle":[{"id":210370816945,"code":"CHD","name":"chandigarh"}],"name":"Jaspal"}
        """.trimIndent()

        val emp = gson.fromJson(json, EmpData::class.java)

        assertNotNull(emp.circle)
        assertEquals(1, emp.circle?.size)
        assertEquals("CHD", emp.circle?.first()?.code)
        assertEquals("chandigarh", emp.circle?.first()?.name)
        assertEquals("CHD", emp.circleDisplay)
    }

    @Test
    fun `parses circle as plain string (legacy cached shape) without throwing`() {
        val json = """{"circle":"CHD","name":"Jaspal"}"""

        val emp = gson.fromJson(json, EmpData::class.java)

        assertEquals(1, emp.circle?.size)
        assertEquals("CHD", emp.circle?.first()?.code)
        assertEquals("CHD", emp.circleDisplay)
    }

    @Test
    fun `handles null and missing circle`() {
        assertEquals("", gson.fromJson("""{"circle":null}""", EmpData::class.java).circleDisplay)
        assertEquals("", gson.fromJson("""{"name":"Jaspal"}""", EmpData::class.java).circleDisplay)
    }

    @Test
    fun `joins multiple circle codes for display`() {
        val json = """
            {"circle":[{"id":1,"code":"CHD","name":"chandigarh"},{"id":2,"code":"PB","name":"punjab"}]}
        """.trimIndent()

        assertEquals("CHD, PB", gson.fromJson(json, EmpData::class.java).circleDisplay)
    }

    @Test
    fun `circleDisplay is not serialised back into the cached payload`() {
        val json = """{"circle":[{"id":1,"code":"CHD","name":"chandigarh"}]}"""
        val emp = gson.fromJson(json, EmpData::class.java)

        val roundTripped = gson.toJson(emp)

        assertTrue("circleDisplay must not be persisted", !roundTripped.contains("circleDisplay"))
        // and the round-tripped payload must still parse
        assertEquals("CHD", gson.fromJson(roundTripped, EmpData::class.java).circleDisplay)
    }
}

package com.atvantiq.wfms.utils

import org.junit.Assert.*
import org.junit.Test

class UtilsTest {

    @Test
    fun testModelToStringAndStringToModel() {
        data class TestModel(val name: String, val age: Int)
        val model = TestModel("John", 30)
        val json = Utils.modelToString(model)
        val result = Utils.stringToModel(json, TestModel::class.java) as TestModel
        assertEquals(model, result)
    }

    @Test
    fun testRoundOffDecimal() {
        Utils.roundOffDecimal(1.234)?.let { assertEquals(1.23, it, 0.0) }
        Utils.roundOffDecimal(0.0)?.let { assertEquals(0.0, it, 0.0) }
    }

    @Test
    fun testFormatToString() {
        val formatted = Utils.formatToString("%.2f", 1.234)
        assertEquals("1.23", formatted)
    }

    @Test
    fun testDateToString() {
        // Input: "2023-01-02T08:09:10"
        val result = Utils.dateToString("2023-01-02T08:09:10")
        // Output format: "dd/mm/yyyy hh:mm:ss a"
        assertTrue(result.contains("2023"))
    }

    @Test
    fun testPxToDpAndDpToPx() {
        val mockDisplayMetrics = android.util.DisplayMetrics().apply { xdpi = 160f }
        val mockContext = org.mockito.Mockito.mock(android.content.Context::class.java)
        val mockResources = org.mockito.Mockito.mock(android.content.res.Resources::class.java)
        org.mockito.Mockito.`when`(mockContext.resources).thenReturn(mockResources)
        org.mockito.Mockito.`when`(mockResources.displayMetrics).thenReturn(mockDisplayMetrics)

        assertEquals(10, Utils.pxToDp(10, mockContext))
        assertEquals(10, Utils.dpToPx(10f, mockContext))
    }

    @Test
    fun testFormatAddress() {
        val address = org.mockito.Mockito.mock(android.location.Address::class.java)
        org.mockito.Mockito.`when`(address.featureName).thenReturn("Feature")
        org.mockito.Mockito.`when`(address.thoroughfare).thenReturn("Thoroughfare")
        org.mockito.Mockito.`when`(address.subLocality).thenReturn("SubLocality")
        org.mockito.Mockito.`when`(address.locality).thenReturn("Locality")
        org.mockito.Mockito.`when`(address.adminArea).thenReturn("AdminArea")
        org.mockito.Mockito.`when`(address.postalCode).thenReturn("12345")
        org.mockito.Mockito.`when`(address.countryName).thenReturn("Country")
        val result = Utils.invokePrivateFormatAddress(address)
        assertTrue(result.contains("Feature"))
        assertTrue(result.contains("Country"))
    }

    // Helper to access private function
    private fun Utils.invokePrivateFormatAddress(address: android.location.Address?): String {
        val method = Utils::class.java.getDeclaredMethod("formatAddress", android.location.Address::class.java)
        method.isAccessible = true
        return method.invoke(Utils, address) as String
    }
}

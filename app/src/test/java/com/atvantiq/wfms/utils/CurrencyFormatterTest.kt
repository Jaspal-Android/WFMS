package com.atvantiq.wfms.utils

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CurrencyFormatterTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun tearDown() = Locale.setDefault(originalLocale)

    @Test
    fun `amounts under a thousand are whole numbers`() {
        assertEquals("$0", CurrencyFormatter.compact(0.0, "$"))
        assertEquals("$950", CurrencyFormatter.compact(950.0, "$"))
    }

    @Test
    fun `thousands use K without decimals`() {
        assertEquals("$108K", CurrencyFormatter.compact(108_000.0, "$"))
        assertEquals("$450K", CurrencyFormatter.compact(450_000.0, "$"))
    }

    @Test
    fun `millions use M with one decimal`() {
        assertEquals("$1.3M", CurrencyFormatter.compact(1_250_000.0, "$"))
    }

    @Test
    fun `negative amounts keep the sign in front of the symbol`() {
        assertEquals("-$12K", CurrencyFormatter.compact(-12_000.0, "$"))
    }

    @Test
    fun `digits stay ASCII whatever the device locale`() {
        Locale.setDefault(Locale("ar", "EG"))
        assertEquals("$108K", CurrencyFormatter.compact(108_000.0, "$"))
    }
}

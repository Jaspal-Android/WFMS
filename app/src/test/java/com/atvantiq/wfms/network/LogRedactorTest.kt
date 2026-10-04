package com.atvantiq.wfms.network

import org.junit.Assert.assertEquals
import org.junit.Test

class LogRedactorTest {

    @Test
    fun `a login body keeps its shape but loses the password`() {
        assertEquals(
            """{"email":"pm@yopmail.com","password":"***"}""",
            LogRedactor.redact("""{"email":"pm@yopmail.com","password":"S3cret!"}""")
        )
    }

    @Test
    fun `otp and tokens in a response are masked too`() {
        val response = """{"data":{"access_token":"abc.def","refresh_token":"xyz","otp": "123456"}}"""
        assertEquals(
            """{"data":{"access_token":"***","refresh_token":"***","otp": "***"}}""",
            LogRedactor.redact(response)
        )
    }

    @Test
    fun `keys are matched ignoring case, and other fields are untouched`() {
        assertEquals("""{"Password":"***","name":"Jaspal"}""", LogRedactor.redact("""{"Password":"x","name":"Jaspal"}"""))
        assertEquals("--> POST https://api/login", LogRedactor.redact("--> POST https://api/login"))
    }
}

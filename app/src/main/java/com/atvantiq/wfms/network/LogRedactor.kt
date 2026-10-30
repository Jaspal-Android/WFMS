package com.atvantiq.wfms.network

/**
 * Masks credentials in HTTP log lines so a debug build's body logging never prints a password,
 * an OTP or a token. Only the value is replaced; the key stays so the log still shows the shape
 * of the request.
 */
object LogRedactor {

    private const val MASK = "***"

    private val SENSITIVE_KEYS = listOf("password", "new_password", "confirm_password", "otp", "access_token", "refresh_token", "token")

    private val JSON_VALUE = Regex(
        "(\"(?:${SENSITIVE_KEYS.joinToString("|")})\"\\s*:\\s*\")[^\"]*(\")",
        RegexOption.IGNORE_CASE
    )

    fun redact(message: String): String = JSON_VALUE.replace(message, "$1$MASK$2")
}

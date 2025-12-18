package com.atvantiq.wfms.utils

object ValidatorUtils {

    fun isValidEmail(email: String?): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return !email.isNullOrBlank() && email.matches(emailPattern.toRegex())
    }
}
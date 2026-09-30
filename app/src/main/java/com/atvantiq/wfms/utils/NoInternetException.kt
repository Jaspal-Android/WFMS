package com.atvantiq.wfms.utils

/** [error] is also the exception message, so screens that show `throwable.message` display it. */
class NoInternetException(val error: String) : Exception(error)

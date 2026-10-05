package com.atvantiq.wfms.utils

import java.text.NumberFormat

/** A count written with the digits of the user's language. For text on screen, never for API values. */
fun Int.toLocalizedString(): String = NumberFormat.getIntegerInstance().format(this)

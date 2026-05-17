package com.aminmart.moneymanager.presentation.ui

import kotlin.math.roundToLong

/**
 * Parses app amount inputs as whole rupiah values.
 * Non-digit separators are ignored so pasted values such as "1.000" still work.
 */
fun parseWholeAmount(rawValue: CharSequence?): Double? {
    val digitsOnly = rawValue
        ?.toString()
        ?.filter(Char::isDigit)
        .orEmpty()

    if (digitsOnly.isBlank()) {
        return null
    }

    return digitsOnly.toLongOrNull()?.toDouble()
}

fun formatWholeAmount(amount: Double): String {
    if (amount <= 0) {
        return ""
    }

    return amount.roundToLong().toString()
}

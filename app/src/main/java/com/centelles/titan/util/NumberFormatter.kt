package com.centelles.titan.util

import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

object NumberFormatter {
    private val suffixes = arrayOf("", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc")

    fun format(value: Double): String {
        if (value < 1000) return floor(value).toInt().toString()
        
        var exp = (log10(value) / 3).toInt()
        var scaled = value / 10.0.pow(exp * 3.0)

        // Handle rounding up to 1000
        if (scaled >= 999.95 && exp < suffixes.size - 1) {
            scaled /= 1000.0
            exp += 1
        }
        
        val suffix = if (exp < suffixes.size) suffixes[exp] else "e${exp * 3}"
        
        return if (scaled >= 100) {
            String.format(Locale.US, "%.0f%s", scaled, suffix)
        } else if (scaled >= 10) {
            String.format(Locale.US, "%.1f%s", scaled, suffix)
        } else {
            String.format(Locale.US, "%.2f%s", scaled, suffix)
        }
    }
}

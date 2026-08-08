package com.centelles.titan.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatterTest {

    @Test
    fun testFormatSmallNumbers() {
        assertEquals("0", NumberFormatter.format(0.0))
        assertEquals("5", NumberFormatter.format(5.4))
        assertEquals("999", NumberFormatter.format(999.9))
    }

    @Test
    fun testFormatThousands() {
        assertEquals("1.00K", NumberFormatter.format(1000.0))
        assertEquals("1.23K", NumberFormatter.format(1234.0))
        assertEquals("12.3K", NumberFormatter.format(12345.0))
        assertEquals("123K", NumberFormatter.format(123456.0))
    }

    @Test
    fun testFormatMillions() {
        assertEquals("1.00M", NumberFormatter.format(1_000_000.0))
        assertEquals("1.00B", NumberFormatter.format(999_999_999.0))
    }

    @Test
    fun testFormatScientific() {
        // DC is the last one in our list (10^33)
        // 10^36 should be e36
        assertEquals("1.00e36", NumberFormatter.format(10.0.pow(36.0)))
    }
    
    private fun Double.pow(exp: Double) = Math.pow(this, exp)
}

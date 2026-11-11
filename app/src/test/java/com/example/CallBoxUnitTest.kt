package com.example

import com.example.ui.components.formatDate
import com.example.ui.components.formatDuration
import com.example.ui.components.formatFileSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CallBoxUnitTest {

    @Test
    fun testDurationFormatting() {
        assertEquals("00:00", formatDuration(0L))
        assertEquals("00:05", formatDuration(5000L))
        assertEquals("01:00", formatDuration(60000L))
        assertEquals("02:35", formatDuration(155000L))
        assertEquals("10:00", formatDuration(600000L))
    }

    @Test
    fun testFileSizeFormatting() {
        assertEquals("0 KB", formatFileSize(0L))
        assertEquals("500 KB", formatFileSize(512000L))
        assertEquals("1.0 MB", formatFileSize(1048576L))
        assertEquals("4.2 MB", formatFileSize(4404019L))
    }

    @Test
    fun testDateFormatting() {
        val timestamp = 1731320000000L
        val formatted = formatDate(timestamp)
        assertTrue(formatted.isNotEmpty())
    }
}

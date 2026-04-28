package com.wifimapper.presentation.map

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class RssiColorTest {

    @Test
    fun `rssi above minus 40 returns dark green`() {
        val color = rssiToColor(-30)
        assertEquals(Color(0xFF006400), color)
    }

    @Test
    fun `rssi minus 45 returns green`() {
        val color = rssiToColor(-45)
        assertEquals(Color(0xFF4CAF50), color)
    }

    @Test
    fun `rssi minus 55 returns light green`() {
        val color = rssiToColor(-55)
        assertEquals(Color(0xFF8BC34A), color)
    }

    @Test
    fun `rssi minus 65 returns yellow`() {
        val color = rssiToColor(-65)
        assertEquals(Color(0xFFFFEB3B), color)
    }

    @Test
    fun `rssi minus 75 returns orange`() {
        val color = rssiToColor(-75)
        assertEquals(Color(0xFFFF9800), color)
    }

    @Test
    fun `rssi below minus 85 returns red`() {
        val color = rssiToColor(-90)
        assertEquals(Color(0xFFB71C1C), color)
    }

    @Test
    fun `boundary minus 50 returns green`() {
        val color = rssiToColor(-50)
        assertEquals(Color(0xFF4CAF50), color)
    }

    @Test
    fun `boundary minus 60 returns lime`() {
        val color = rssiToColor(-60)
        assertEquals(Color(0xFFCDDC39), color)
    }

    @Test
    fun `boundary minus 70 returns amber`() {
        val color = rssiToColor(-70)
        assertEquals(Color(0xFFFFC107), color)
    }

    @Test
    fun `boundary minus 80 returns deep orange`() {
        val color = rssiToColor(-80)
        assertEquals(Color(0xFFFF5722), color)
    }
}

package io.github.jqssun.airplay.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `system mode follows dark system appearance`() {
        assertTrue(ThemeMode.SYSTEM.useDarkColors(systemIsDark = true))
        assertFalse(ThemeMode.SYSTEM.useDarkColors(systemIsDark = false))
    }

    @Test
    fun `dark mode always uses dark colors`() {
        assertTrue(ThemeMode.DARK.useDarkColors(systemIsDark = false))
    }

    @Test
    fun `light mode always uses light colors`() {
        assertFalse(ThemeMode.LIGHT.useDarkColors(systemIsDark = true))
    }

    @Test
    fun `stored names are parsed safely`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("system"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorage("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorage("dark"))
    }

    @Test
    fun `unknown stored name falls back to system`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("sepia"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(null))
    }
}

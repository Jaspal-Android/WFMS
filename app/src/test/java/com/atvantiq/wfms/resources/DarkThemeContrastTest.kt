package com.atvantiq.wfms.resources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.pow

/**
 * The brand primary is a dark color, which is unreadable as text or an icon on a dark surface
 * (1.7-2.4:1). Dark mode therefore uses a lighter accent; these tests keep it readable and keep
 * light mode unchanged.
 */
class DarkThemeContrastTest {

    private val palettes = listOf("forest", "ocean", "rose", "indigo", "amber", "slate")

    private val resDir: File =
        listOf(File("src/main/res"), File("app/src/main/res")).first { it.exists() }

    private val colors: Map<String, String> =
        Regex("""<color name="(\w+)">(#[0-9a-fA-F]{6})</color>""")
            .findAll(File(resDir, "values/colors.xml").readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    private fun luminance(hex: String): Double {
        val c = hex.removePrefix("#").chunked(2).map { it.toInt(16) / 255.0 }
            .map { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
        return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]
    }

    private fun contrast(a: String, b: String): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private val darkSurfaces = listOf("dark_bg", "dark_surface", "dark_surface_variant", "dark_surface_2")

    @Test
    fun `the dark accent is readable on every dark surface`() {
        palettes.forEach { palette ->
            val accent = colors.getValue("${palette}_accent_dark")
            darkSurfaces.forEach { surface ->
                val ratio = contrast(accent, colors.getValue(surface))
                assertTrue("$palette accent $accent on $surface is only ${"%.2f".format(ratio)}:1", ratio >= 4.5)
            }
        }
    }

    @Test
    fun `dark text on the accent stays readable for filled controls`() {
        palettes.forEach { palette ->
            val ratio = contrast(colors.getValue("dark_bg"), colors.getValue("${palette}_accent_dark"))
            assertTrue("$palette: ${"%.2f".format(ratio)}:1", ratio >= 4.5)
        }
    }

    @Test
    fun `strong body text is light in dark mode and readable`() {
        val night = Regex("""<color name="text_strong">@color/(\w+)</color>""")
            .find(File(resDir, "values-night/colors.xml").readText())!!.groupValues[1]
        val light = Regex("""<color name="text_strong">@color/(\w+)</color>""")
            .find(File(resDir, "values/colors.xml").readText())!!.groupValues[1]

        assertEquals("black", light)
        assertTrue(contrast(colors.getValue(night), colors.getValue("dark_surface")) >= 7.0)
    }

    private fun overlayBlocks(file: String): Map<String, String> =
        Regex("""<style name="(ThemeOverlay\.WFMS\.(?:Forest|Ocean|Rose|Indigo|Amber|Slate))"[^>]*>(.*?)</style>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(File(resDir, file).readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    @Test
    fun `every palette defines the accent in both light and dark`() {
        listOf("values/themes.xml", "values-night/themes.xml").forEach { file ->
            val blocks = overlayBlocks(file)
            assertEquals("$file overlays", 6, blocks.size)
            blocks.forEach { (name, body) ->
                assertTrue("$name in $file is missing wfmsColorAccent", body.contains("""name="wfmsColorAccent""""))
            }
        }
    }

    @Test
    fun `in light mode the accent is exactly the brand primary, so light mode is unchanged`() {
        overlayBlocks("values/themes.xml").forEach { (name, body) ->
            val palette = name.substringAfterLast('.').lowercase()
            assertTrue(name, body.contains("""<item name="wfmsColorAccent">@color/${palette}_primary</item>"""))
        }
    }

    @Test
    fun `in dark mode the accent is the lighter tint`() {
        overlayBlocks("values-night/themes.xml").forEach { (name, body) ->
            val palette = name.substringAfterLast('.').lowercase()
            assertTrue(name, body.contains("""<item name="wfmsColorAccent">@color/${palette}_accent_dark</item>"""))
        }
    }
}

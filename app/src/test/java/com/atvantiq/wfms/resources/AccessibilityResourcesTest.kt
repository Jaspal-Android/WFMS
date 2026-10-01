package com.atvantiq.wfms.resources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards accessibility rules that lint only reports as warnings, so a regression fails the build.
 * Reads the layouts straight from the source tree.
 */
class AccessibilityResourcesTest {

    private val android = "http://schemas.android.com/apk/res/android"

    private val resDir: File =
        listOf(File("src/main/res"), File("app/src/main/res")).first { it.exists() }

    private val imageTags = setOf(
        "ImageView", "ImageButton", "AppCompatImageView", "AppCompatImageButton",
        "ShapeableImageView", "CircleImageView"
    )

    private fun layouts(): List<File> =
        File(resDir, "layout").listFiles { f -> f.extension == "xml" }.orEmpty().sortedBy { it.name }

    private fun elements(file: File): List<Element> {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val all = factory.newDocumentBuilder().parse(file).getElementsByTagName("*")
        return (0 until all.length).map { all.item(it) as Element }
    }

    @Test
    fun `every image is either described for screen readers or marked decorative`() {
        val offenders = layouts().flatMap { file ->
            elements(file)
                .filter { it.tagName.substringAfterLast('.') in imageTags }
                .filter {
                    !it.hasAttributeNS(android, "contentDescription") &&
                        !it.hasAttributeNS(android, "importantForAccessibility")
                }
                .map { "${file.name}: ${it.getAttributeNS(android, "id").ifEmpty { it.tagName }}" }
        }

        assertTrue("Images with no contentDescription and not marked decorative:\n" + offenders.joinToString("\n"), offenders.isEmpty())
    }

    @Test
    fun `icon-only buttons have a real label, not a null description`() {
        val offenders = layouts().flatMap { file ->
            elements(file)
                .filter { it.tagName.substringAfterLast('.') == "ImageButton" }
                .filter { it.getAttributeNS(android, "contentDescription").let { d -> d.isEmpty() || d == "@null" } }
                .filter { it.getAttributeNS(android, "importantForAccessibility") != "no" }
                .map { "${file.name}: ${it.getAttributeNS(android, "id")}" }
        }

        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `the six OTP boxes are each labelled with their position`() {
        val file = File(resDir, "layout/bottom_sheet_dialog_get_otp.xml")
        val boxes = elements(file).filter { it.getAttributeNS(android, "id").startsWith("@+id/otp_box_") }

        assertEquals(6, boxes.size)
        boxes.forEachIndexed { index, box ->
            assertEquals(
                box.getAttributeNS(android, "id"),
                "@{@string/otp_digit_description(${index + 1}, @integer/otp_length)}",
                box.getAttributeNS(android, "contentDescription")
            )
        }
    }
}

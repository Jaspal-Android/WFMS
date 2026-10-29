package com.atvantiq.wfms.utils.files

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PrivateFilesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `clearUserFiles removes photos and cache but keeps other app files`() {
        val filesDir = tmp.newFolder("files")
        val cacheDir = tmp.newFolder("cache")
        val photo = File(File(filesDir, FileUtils.PHOTO_DIR).apply { mkdirs() }, "WFMS_1.jpg").apply { writeText("x") }
        val cached = File(cacheDir, "picked_1.jpg").apply { writeText("x") }
        val nested = File(File(cacheDir, "sub").apply { mkdirs() }, "a.tmp").apply { writeText("x") }
        val other = File(filesDir, "keep.txt").apply { writeText("x") }

        PrivateFiles.clearUserFiles(filesDir, cacheDir)

        assertFalse(photo.exists())
        assertFalse(File(filesDir, FileUtils.PHOTO_DIR).exists())
        assertFalse(cached.exists())
        assertFalse(nested.exists())
        assertTrue(other.exists())
        assertTrue(cacheDir.exists())
    }

    @Test
    fun `clearUserFiles tolerates missing folders`() {
        PrivateFiles.clearUserFiles(File(tmp.root, "nope"), File(tmp.root, "none"))
    }

    @Test
    fun `isInside is true for a nested file`() {
        val dir = tmp.newFolder("data")
        assertTrue(PrivateFiles.isInside(dir, File(dir, "shared_prefs/secret.xml")))
    }

    @Test
    fun `isInside is false for a file elsewhere`() {
        val dir = tmp.newFolder("data")
        assertFalse(PrivateFiles.isInside(dir, File(tmp.newFolder("sdcard"), "photo.jpg")))
    }

    @Test
    fun `isInside resolves dot-dot segments`() {
        val dir = tmp.newFolder("data")
        val outside = tmp.newFolder("outside")
        assertFalse(PrivateFiles.isInside(dir, File(dir, "../outside/${outside.name}.jpg")))
        assertTrue(PrivateFiles.isInside(dir, File(outside, "../data/secret")))
    }
}

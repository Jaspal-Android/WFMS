package com.atvantiq.wfms.utils.files

import java.io.File
import java.io.IOException
import timber.log.Timber

/** Housekeeping and guards for the files the app keeps in its private storage. */
object PrivateFiles {

    /**
     * Deletes everything the user captured or picked: the photos in `filesDir/WFMS` and the
     * temporary copies in the cache. Used on logout so the next person on a shared device
     * cannot reach the previous user's receipts and site photos.
     */
    fun clearUserFiles(filesDir: File, cacheDir: File) {
        File(filesDir, FileUtils.PHOTO_DIR).deleteRecursively()
        cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    /**
     * True when [file] lives inside [directory] once symlinks and `..` segments are resolved.
     * A file that cannot be resolved counts as inside, so the caller refuses it.
     */
    fun isInside(directory: File, file: File): Boolean = try {
        val root = directory.canonicalPath
        val path = file.canonicalPath
        path == root || path.startsWith(root + File.separator)
    } catch (e: IOException) {
        Timber.w(e, "Could not resolve a file path; treating it as private")
        true
    }
}

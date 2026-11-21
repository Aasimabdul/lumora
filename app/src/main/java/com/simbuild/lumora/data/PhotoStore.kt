package com.simbuild.lumora.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Keeps user-chosen background photos in app-private storage (never shared, never uploaded).
 * Photos are re-encoded as JPEG on import, which also drops EXIF metadata such as GPS location.
 */
class PhotoStore(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val dir = File(context.applicationContext.filesDir, "backgrounds")

    /** Copies the picked image into private storage. Returns the stored file name, or null on failure. */
    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            dir.mkdirs()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, IMPORT_MAX_EDGE)
            }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@withContext null
            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val upright = rotate(decoded, orientation)

            val name = "bg_${UUID.randomUUID()}.jpg"
            File(dir, name).outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            if (upright !== decoded) decoded.recycle()
            name
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    /** Decodes a stored photo no larger than about [maxEdge] pixels on its long side, or null. */
    suspend fun load(fileName: String, maxEdge: Int = DISPLAY_MAX_EDGE): Bitmap? = withContext(Dispatchers.IO) {
        val file = fileFor(fileName) ?: return@withContext null
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge) }
            BitmapFactory.decodeFile(file.path, opts)
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    suspend fun delete(fileName: String) = withContext(Dispatchers.IO) {
        fileFor(fileName)?.delete()
        Unit
    }

    /** Removes every stored photo whose name is not in [keep]. */
    suspend fun deleteUnused(keep: Set<String>) = withContext(Dispatchers.IO) {
        dir.listFiles()?.filter { it.isFile && it.name !in keep }?.forEach { it.delete() }
        Unit
    }

    /** Resolves a stored name to a file inside [dir] only; anything that could escape it is rejected. */
    private fun fileFor(name: String): File? {
        if (!SAFE_NAME.matches(name)) return null
        val f = File(dir, name)
        return if (f.parentFile?.canonicalPath == dir.canonicalPath && f.isFile) f else null
    }

    private fun rotate(src: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            else -> return src
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    private fun sampleSize(w: Int, h: Int, maxEdge: Int): Int {
        var s = 1
        while (maxOf(w, h) / (s * 2) >= maxEdge) s *= 2
        return s
    }

    private companion object {
        const val IMPORT_MAX_EDGE = 2048
        const val DISPLAY_MAX_EDGE = 1600
        val SAFE_NAME = Regex("^[A-Za-z0-9._-]{1,80}$")
    }
}

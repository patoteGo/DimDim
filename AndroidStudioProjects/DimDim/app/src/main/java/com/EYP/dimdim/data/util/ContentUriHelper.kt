package com.EYP.dimdim.data.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

data class ImageInfo(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val size: Long,
    val isValid: Boolean,
    val errorMessage: String? = null
)

object ContentUriHelper {
    
    private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
    private val SUPPORTED_MIME_TYPES = setOf(
        "image/jpeg",
        "image/jpg", 
        "image/png",
        "image/webp"
    )

    suspend fun processSharedImages(
        context: Context,
        uris: List<Uri>
    ): Result<List<ImageInfo>> = withContext(Dispatchers.IO) {
        try {
            val imageInfoList = mutableListOf<ImageInfo>()
            val contentResolver = context.contentResolver

            for (uri in uris) {
                val imageInfo = processUri(contentResolver, uri)
                imageInfoList.add(imageInfo)
            }

            Result.success(imageInfoList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun processUri(contentResolver: ContentResolver, uri: Uri): ImageInfo {
        return try {
            val cursor = contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.Media.SIZE
                ),
                null,
                null,
                null
            )

            var displayName = "receipt_${System.currentTimeMillis()}"
            var mimeType = "image/jpeg"
            var size = 0L

            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    val mimeIndex = it.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                    val sizeIndex = it.getColumnIndex(MediaStore.Images.Media.SIZE)

                    if (nameIndex >= 0) displayName = it.getString(nameIndex) ?: displayName
                    if (mimeIndex >= 0) mimeType = it.getString(mimeIndex) ?: mimeType
                    if (sizeIndex >= 0) size = it.getLong(sizeIndex)
                }
            }

            // Fallback to get mime type from URI if not available
            if (mimeType.isEmpty() || mimeType == "null") {
                mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            }

            // Validate the image
            val validation = validateImage(mimeType, size)
            
            ImageInfo(
                uri = uri,
                displayName = displayName,
                mimeType = mimeType,
                size = size,
                isValid = validation.first,
                errorMessage = validation.second
            )

        } catch (e: Exception) {
            ImageInfo(
                uri = uri,
                displayName = "unknown",
                mimeType = "unknown",
                size = 0,
                isValid = false,
                errorMessage = "Failed to read image: ${e.message}"
            )
        }
    }

    private fun validateImage(mimeType: String, size: Long): Pair<Boolean, String?> {
        if (!SUPPORTED_MIME_TYPES.contains(mimeType.lowercase())) {
            return false to "Unsupported image format: $mimeType. Please use JPEG, PNG, or WebP."
        }

        if (size > MAX_FILE_SIZE) {
            return false to "Image too large: ${size / (1024 * 1024)}MB. Maximum size is ${MAX_FILE_SIZE / (1024 * 1024)}MB."
        }

        if (size <= 0) {
            return false to "Invalid image file size."
        }

        return true to null
    }

    suspend fun copyImageToAppStorage(
        context: Context,
        sourceUri: Uri,
        fileName: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream = contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(Exception("Cannot open input stream for URI"))

            val mimeType = contentResolver.getType(sourceUri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            val finalFileName = fileName ?: "receipt_${UUID.randomUUID()}.${extension}"

            val receiptDir = File(context.filesDir, "receipts")
            if (!receiptDir.exists()) {
                receiptDir.mkdirs()
            }

            val destFile = File(receiptDir, finalFileName)
            val outputStream = FileOutputStream(destFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun cleanup(context: Context, olderThanMillis: Long = 24 * 60 * 60 * 1000) {
        try {
            val receiptDir = File(context.filesDir, "receipts")
            if (receiptDir.exists()) {
                val cutoffTime = System.currentTimeMillis() - olderThanMillis
                receiptDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < cutoffTime) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't crash
        }
    }
}
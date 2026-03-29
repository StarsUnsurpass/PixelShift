package com.example.pixelshift.data

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.io.File
import java.io.FileOutputStream

private const val TAG = "ExifRepository"

data class ExifTag(
    val tag: String,
    val label: String,
    val value: String?,
    val category: String
)

class ExifRepository(private val context: Context) {

    // Common EXIF tags categorized for better UI presentation
    private val tagCategories = mapOf(
        "Image Information" to listOf(
            ExifInterface.TAG_IMAGE_WIDTH to "Width",
            ExifInterface.TAG_IMAGE_LENGTH to "Height",
            ExifInterface.TAG_DATETIME to "Date/Time",
            ExifInterface.TAG_IMAGE_DESCRIPTION to "Description",
            ExifInterface.TAG_SOFTWARE to "Software",
            ExifInterface.TAG_ARTIST to "Artist",
            ExifInterface.TAG_COPYRIGHT to "Copyright",
            ExifInterface.TAG_ORIENTATION to "Orientation",
            ExifInterface.TAG_USER_COMMENT to "User Comment"
        ),
        "Camera Settings" to listOf(
            ExifInterface.TAG_MAKE to "Manufacturer",
            ExifInterface.TAG_MODEL to "Model",
            ExifInterface.TAG_EXPOSURE_TIME to "Exposure Time",
            ExifInterface.TAG_F_NUMBER to "F-Number",
            ExifInterface.TAG_ISO_SPEED_RATINGS to "ISO Speed",
            ExifInterface.TAG_FOCAL_LENGTH to "Focal Length",
            ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM to "Focal Length (35mm)",
            ExifInterface.TAG_FLASH to "Flash",
            ExifInterface.TAG_WHITE_BALANCE to "White Balance",
            ExifInterface.TAG_EXPOSURE_PROGRAM to "Exposure Program",
            ExifInterface.TAG_METERING_MODE to "Metering Mode",
            ExifInterface.TAG_LIGHT_SOURCE to "Light Source"
        ),
        "GPS Information" to listOf(
            ExifInterface.TAG_GPS_LATITUDE to "Latitude",
            ExifInterface.TAG_GPS_LONGITUDE to "Longitude",
            ExifInterface.TAG_GPS_ALTITUDE to "Altitude",
            ExifInterface.TAG_GPS_DATESTAMP to "GPS Date",
            ExifInterface.TAG_GPS_TIMESTAMP to "GPS Time",
            ExifInterface.TAG_GPS_PROCESSING_METHOD to "Processing Method"
        )
    )

    suspend fun getExifMetadata(uri: Uri): List<ExifTag> = withContext(Dispatchers.IO) {
        val tags = mutableListOf<ExifTag>()
        try {
            // Copying to temp file for reading is often more reliable than direct stream
            val tempFile = File(context.cacheDir, "temp_exif_read.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val exifInterface = ExifInterface(tempFile.absolutePath)
            
            tagCategories.forEach { (category, tagPairs) ->
                tagPairs.forEach { (tag, label) ->
                    val value = exifInterface.getAttribute(tag)
                    tags.add(ExifTag(tag, label, value, category))
                }
            }
            tempFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading EXIF from $uri", e)
        }
        tags
    }

    suspend fun updateExifMetadata(uri: Uri, updatedTags: Map<String, String>): Boolean = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "temp_exif_edit.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val exifInterface = ExifInterface(tempFile.absolutePath)
            updatedTags.forEach { (tag, value) ->
                if (value.isBlank()) {
                    exifInterface.setAttribute(tag, null)
                } else {
                    exifInterface.setAttribute(tag, value)
                }
            }
            exifInterface.saveAttributes()

            // Try to write back to the original URI
            context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Could not open output stream for URI: $uri")
            
            tempFile.delete()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating EXIF", e)
            false
        }
    }

    suspend fun clearExifMetadata(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "temp_exif_clear.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val exifInterface = ExifInterface(tempFile.absolutePath)
            
            // Comprehensive clear list
            val tagsToClear = listOf(
                ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_TIMESTAMP, ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL,
                ExifInterface.TAG_DATETIME, ExifInterface.TAG_USER_COMMENT,
                ExifInterface.TAG_ARTIST, ExifInterface.TAG_COPYRIGHT,
                ExifInterface.TAG_SOFTWARE, ExifInterface.TAG_IMAGE_DESCRIPTION
            )

            tagsToClear.forEach { tag ->
                exifInterface.setAttribute(tag, null)
            }
            
            exifInterface.saveAttributes()

            context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Could not open output stream for URI: $uri")
            
            tempFile.delete()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing EXIF", e)
            false
        }
    }
}

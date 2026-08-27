package ai.january.partner.photos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** Prepares a local meal photo for the photo-scanning endpoint. */
public object PhotoScanImage {
    /** Matches the food-photo treatment used by January's mobile applications. */
    public const val DEFAULT_MAX_DIMENSION: Int = 1_000
    public const val DEFAULT_JPEG_QUALITY: Int = 70

    /** Returns a correctly oriented, aspect-preserving JPEG suitable for upload. */
    public fun jpegData(
        imageData: ByteArray,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        jpegQuality: Int = DEFAULT_JPEG_QUALITY,
    ): ByteArray {
        require(maxDimension > 0) { "maxDimension must be greater than zero." }
        require(jpegQuality in 0..100) { "jpegQuality must be between 0 and 100." }

        val bounds = BitmapFactory.Options().also { options ->
            options.inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) {
            "imageData must contain a supported image."
        }
        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        val decoded = requireNotNull(
            BitmapFactory.decodeByteArray(
                imageData,
                0,
                imageData.size,
                BitmapFactory.Options().apply { inSampleSize = sampleSize },
            ),
        ) { "imageData must contain a supported image." }
        val exif = runCatching { ExifInterface(ByteArrayInputStream(imageData)) }.getOrNull()
        val rotation = exif?.rotationDegrees?.toFloat() ?: 0f
        val flipped = exif?.isFlipped == true
        val oriented = if (rotation == 0f && !flipped) decoded else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply {
                if (flipped) postScale(-1f, 1f)
                if (rotation != 0f) postRotate(rotation)
            }, true).also { result -> if (result !== decoded) decoded.recycle() }
        }
        return jpegData(oriented, maxDimension, jpegQuality).also { oriented.recycle() }
    }

    /** Returns an aspect-preserving JPEG from an Android bitmap. */
    public fun jpegData(
        bitmap: Bitmap,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        jpegQuality: Int = DEFAULT_JPEG_QUALITY,
    ): ByteArray {
        require(maxDimension > 0) { "maxDimension must be greater than zero." }
        require(jpegQuality in 0..100) { "jpegQuality must be between 0 and 100." }

        val longestEdge = max(bitmap.width, bitmap.height)
        val resized = if (longestEdge <= maxDimension) {
            bitmap
        } else {
            val scale = maxDimension.toDouble() / longestEdge.toDouble()
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).roundToInt(),
                (bitmap.height * scale).roundToInt(),
                true,
            )
        }
        return ByteArrayOutputStream().use { output ->
            check(resized.compress(Bitmap.CompressFormat.JPEG, jpegQuality, output)) {
                "The image could not be encoded as JPEG."
            }
            output.toByteArray()
        }.also { if (resized !== bitmap) resized.recycle() }
    }

    /** Returns a JPEG data URI ready for [ScanFoodPhotoRequest]. */
    public fun dataUri(
        imageData: ByteArray,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        jpegQuality: Int = DEFAULT_JPEG_QUALITY,
    ): String = "data:image/jpeg;base64,${
        Base64.encodeToString(jpegData(imageData, maxDimension, jpegQuality), Base64.NO_WRAP)
    }"

    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        while (max(width, height) / (sampleSize * 2) >= maxDimension) sampleSize *= 2
        return sampleSize
    }
}

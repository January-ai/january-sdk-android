package ai.january.partner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import ai.january.partner.photos.PhotoScanImage
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoScanImageInstrumentedTest {
    @Test
    fun preservesAspectRatioConstrainsLongestEdgeAndProducesJpegDataUri() {
        val bitmap = Bitmap.createBitmap(2_400, 1_200, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.YELLOW) }
        val source = ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
        bitmap.recycle()

        val jpeg = PhotoScanImage.jpegData(source)
        val decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        assertEquals(1_000, decoded.width)
        assertEquals(500, decoded.height)
        decoded.recycle()

        val uri = PhotoScanImage.dataUri(source)
        assertTrue(uri.startsWith("data:image/jpeg;base64,"))
        assertTrue(Base64.decode(uri.substringAfter(','), Base64.DEFAULT).isNotEmpty())
    }

    @Test
    fun normalizesExifRotationBeforeFinalResize() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = Bitmap.createBitmap(1_200, 600, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        val file = File.createTempFile("photo-scan-orientation-", ".jpg", context.cacheDir)
        try {
            file.outputStream().use { source.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            ExifInterface(file).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }
            val jpeg = PhotoScanImage.jpegData(file.readBytes())
            val decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
            assertEquals(500, decoded.width)
            assertEquals(1_000, decoded.height)
            decoded.recycle()
        } finally {
            source.recycle()
            file.delete()
        }
    }
}

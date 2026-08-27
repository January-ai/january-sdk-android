package ai.january.partner.scanner

import ai.january.partner.foods.FoodSearchItem
import ai.january.partner.photos.FoodScan
import ai.january.partner.photos.PhotoScanImage

public enum class JanuaryMealScannerMode { PHOTO, BARCODE }

public data class JanuaryMealScannerConfiguration(
    public val enabledModes: Set<JanuaryMealScannerMode> = JanuaryMealScannerMode.entries.toSet(),
    public val initialMode: JanuaryMealScannerMode = JanuaryMealScannerMode.PHOTO,
    public val maximumImageDimension: Int = PhotoScanImage.DEFAULT_MAX_DIMENSION,
    public val jpegQuality: Int = PhotoScanImage.DEFAULT_JPEG_QUALITY,
) {
    init {
        require(enabledModes.isNotEmpty()) { "At least one scanner mode must be enabled." }
        require(initialMode in enabledModes) { "The initial scanner mode must be enabled." }
        require(maximumImageDimension > 0) { "maximumImageDimension must be greater than zero." }
        require(jpegQuality in 0..100) { "jpegQuality must be between 0 and 100." }
    }
}

public data class JanuaryProcessedMealImage(
    public val jpegData: ByteArray,
    public val pixelWidth: Int,
    public val pixelHeight: Int,
) {
    public val dataUri: String by lazy(LazyThreadSafetyMode.NONE) {
        "data:image/jpeg;base64,${android.util.Base64.encodeToString(jpegData, android.util.Base64.NO_WRAP)}"
    }

    override fun equals(other: Any?): Boolean = other is JanuaryProcessedMealImage &&
        jpegData.contentEquals(other.jpegData) && pixelWidth == other.pixelWidth && pixelHeight == other.pixelHeight

    override fun hashCode(): Int = 31 * (31 * jpegData.contentHashCode() + pixelWidth) + pixelHeight
}

public sealed interface JanuaryMealScannerResult {
    public data class Meal(
        public val image: JanuaryProcessedMealImage,
        public val analysis: FoodScan,
    ) : JanuaryMealScannerResult

    public data class Barcode(
        public val value: String,
        public val food: FoodSearchItem,
    ) : JanuaryMealScannerResult
}

public class NoBarcodeMatchException(public val barcode: String) :
    IllegalStateException("No January food matched barcode $barcode.")

package ai.january.partner.scanner

import android.graphics.BitmapFactory
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.PartnerUserId
import ai.january.partner.foods.GetFoodRequest
import ai.january.partner.foods.LookupFoodByBarcodeRequest
import ai.january.partner.photos.PhotoScanImage
import ai.january.partner.photos.ScanFoodPhotoRequest

/** Network and image orchestration shared by CameraX and host-owned scanner UIs. */
public class JanuaryMealScannerController(
    private val client: JanuaryPartnerClient,
    private val endUserId: PartnerUserId? = null,
    private val configuration: JanuaryMealScannerConfiguration = JanuaryMealScannerConfiguration(),
) {
    public suspend fun analyzePhoto(imageData: ByteArray): JanuaryMealScannerResult.Meal {
        val jpeg = PhotoScanImage.jpegData(
            imageData,
            configuration.maximumImageDimension,
            configuration.jpegQuality,
        )
        val bounds = BitmapFactory.Options().also { options ->
            options.inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, options)
        }
        val image = JanuaryProcessedMealImage(jpeg, bounds.outWidth, bounds.outHeight)
        val analysis = client.photoScanning.scan(ScanFoodPhotoRequest(image.dataUri, endUserId))
        return JanuaryMealScannerResult.Meal(image, analysis)
    }

    public suspend fun lookupBarcode(value: String): JanuaryMealScannerResult.Barcode {
        val match = client.foods.lookupBarcode(LookupFoodByBarcodeRequest(value, endUserId)).items.firstOrNull()
            ?: throw NoBarcodeMatchException(value)
        val food = client.foods.getFood(GetFoodRequest(match.id, endUserId))
        return JanuaryMealScannerResult.Barcode(value, food)
    }
}

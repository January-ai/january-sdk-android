package ai.january.partner.scanner

import android.graphics.BitmapFactory
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.JanuaryPartnerUserClient
import ai.january.partner.PartnerUserId
import ai.january.partner.foods.GetFoodRequest
import ai.january.partner.foods.LookupFoodByBarcodeRequest
import ai.january.partner.photos.PhotoScanImage
import ai.january.partner.photos.ScanFoodPhotoRequest

/** Network and image orchestration shared by CameraX and host-owned scanner UIs. */
public class JanuaryFoodScannerController(
    private val client: JanuaryPartnerClient,
    private val endUserId: PartnerUserId? = null,
    private val configuration: JanuaryFoodScannerConfiguration = JanuaryFoodScannerConfiguration(),
) {
    /** Creates a scanner that reuses the identity configured by [userClient]. */
    public constructor(
        userClient: JanuaryPartnerUserClient,
        configuration: JanuaryFoodScannerConfiguration = JanuaryFoodScannerConfiguration(),
    ) : this(userClient.client, userClient.context.endUserId, configuration)

    public suspend fun analyzePhoto(imageData: ByteArray): JanuaryFoodScannerResult.Photo {
        val jpeg = PhotoScanImage.jpegData(
            imageData,
            configuration.maximumImageDimension,
            configuration.jpegQuality,
        )
        val bounds = BitmapFactory.Options().also { options ->
            options.inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, options)
        }
        val image = JanuaryProcessedFoodImage(jpeg, bounds.outWidth, bounds.outHeight)
        val analysis = client.foodAnalysis.analyzePhoto(ScanFoodPhotoRequest(image.dataUri, endUserId))
        return JanuaryFoodScannerResult.Photo(image, analysis)
    }

    public suspend fun lookupBarcode(value: String): JanuaryFoodScannerResult.Barcode {
        val match = client.foods.lookupBarcode(LookupFoodByBarcodeRequest(value, endUserId)).items.firstOrNull()
            ?: throw NoBarcodeMatchException(value)
        val food = client.foods.get(GetFoodRequest(match.id, endUserId))
        return JanuaryFoodScannerResult.Barcode(value, food)
    }
}

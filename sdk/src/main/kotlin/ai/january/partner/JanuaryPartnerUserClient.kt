package ai.january.partner

import ai.january.partner.foodlogs.CreateFoodLogRequest
import ai.january.partner.foodlogs.DeleteFoodLogRequest
import ai.january.partner.foodlogs.DeleteFoodLogResponse
import ai.january.partner.foodlogs.FoodLog
import ai.january.partner.foodlogs.FoodLogsResource
import ai.january.partner.foodlogs.ListFoodLogsRequest
import ai.january.partner.foodlogs.ListFoodLogsResponse
import ai.january.partner.foodlogs.UpdateFoodLogRequest
import ai.january.partner.foods.AutocompleteFoodsRequest
import ai.january.partner.foods.AutocompleteFoodsResponse
import ai.january.partner.foods.FoodSearchItem
import ai.january.partner.foods.FoodSearchResults
import ai.january.partner.foods.FoodsResource
import ai.january.partner.foods.GetFoodRequest
import ai.january.partner.foods.LookupFoodByBarcodeRequest
import ai.january.partner.foods.SearchFoodsByNaturalLanguageRequest
import ai.january.partner.foods.SearchFoodsByNaturalLanguageResponse
import ai.january.partner.foods.SearchFoodsRequest
import ai.january.partner.foods.SuggestFoodAlternativesRequest
import ai.january.partner.foods.SuggestFoodAlternativesResponse
import ai.january.partner.glucose.GlucosePrediction
import ai.january.partner.glucose.GlucoseResource
import ai.january.partner.glucose.PredictGlucoseRequest
import ai.january.partner.models.FoodSelection
import ai.january.partner.photos.CorrectPhotoScanRequest
import ai.january.partner.photos.FoodScan
import ai.january.partner.photos.FoodAnalysisResource
import ai.january.partner.photos.ScanFoodPhotoRequest
import ai.january.partner.restaurants.RestaurantsResource
import ai.january.partner.restaurants.SearchRestaurantMenuItemsResponse
import ai.january.partner.restaurants.SearchRestaurantsRequest
import ai.january.partner.restaurants.SearchRestaurantsResponse

/** A lightweight January client scoped to one partner-owned end-user identity. */
public class JanuaryPartnerUserClient internal constructor(
    internal val client: JanuaryPartnerClient,
    public val context: PartnerUserContext,
) {
    public val foods: UserFoodsResource = UserFoodsResource(client.foods, context)
    public val restaurants: UserRestaurantsResource = UserRestaurantsResource(client.restaurants, context)
    public val foodAnalysis: UserFoodAnalysisResource =
        UserFoodAnalysisResource(client.foodAnalysis, context)
    public val foodLogs: UserFoodLogsResource = UserFoodLogsResource(client.foodLogs, context)
    public val glucose: UserGlucoseResource = UserGlucoseResource(client.glucose, context)
}

/** Food operations that automatically reuse a [PartnerUserContext]. */
public class UserFoodsResource internal constructor(
    private val resource: FoodsResource,
    private val context: PartnerUserContext,
) {
    public suspend fun autocomplete(request: AutocompleteFoodsRequest): AutocompleteFoodsResponse =
        resource.autocomplete(request.copy(endUserId = context.endUserId))

    public suspend fun get(request: GetFoodRequest): FoodSearchItem =
        resource.get(request.copy(endUserId = context.endUserId))

    public suspend fun search(request: SearchFoodsRequest): FoodSearchResults =
        resource.search(request.copy(endUserId = context.endUserId))

    public suspend fun lookupBarcode(request: LookupFoodByBarcodeRequest): FoodSearchResults =
        resource.lookupBarcode(request.copy(endUserId = context.endUserId))

    public suspend fun suggestAlternatives(
        request: SuggestFoodAlternativesRequest,
    ): SuggestFoodAlternativesResponse =
        resource.suggestAlternatives(request.copy(endUserId = context.endUserId))
}

/** Restaurant operations that automatically reuse a [PartnerUserContext]. */
public class UserRestaurantsResource internal constructor(
    private val resource: RestaurantsResource,
    private val context: PartnerUserContext,
) {
    public suspend fun getMenuItems(request: ai.january.partner.restaurants.GetRestaurantMenuItemsRequest): SearchRestaurantMenuItemsResponse =
        resource.getMenuItems(request.copy(endUserId = context.endUserId))

    public suspend fun search(request: SearchRestaurantsRequest): SearchRestaurantsResponse =
        resource.search(request.copy(endUserId = context.endUserId))

    public suspend fun searchMenuItems(
        request: SearchRestaurantsRequest,
    ): SearchRestaurantMenuItemsResponse =
        resource.searchMenuItems(request.copy(endUserId = context.endUserId))
}

/** Food-analysis operations that automatically reuse a [PartnerUserContext]. */
public class UserFoodAnalysisResource internal constructor(
    private val resource: FoodAnalysisResource,
    private val context: PartnerUserContext,
) {
    public suspend fun analyzePhoto(request: ScanFoodPhotoRequest): FoodScan =
        resource.analyzePhoto(request.copy(endUserId = context.endUserId))

    public suspend fun analyzeDescription(
        request: SearchFoodsByNaturalLanguageRequest,
    ): SearchFoodsByNaturalLanguageResponse =
        resource.analyzeDescription(request.copy(endUserId = context.endUserId))

    public suspend fun correct(request: CorrectPhotoScanRequest): FoodScan =
        resource.correct(request.copy(endUserId = context.endUserId))
}

/** Food Log operations that automatically reuse a [PartnerUserContext]. */
public class UserFoodLogsResource internal constructor(
    private val resource: FoodLogsResource,
    private val context: PartnerUserContext,
) {
    public suspend fun create(
        foods: List<FoodSelection>,
        timestampUtc: String? = null,
        name: String? = null,
    ): FoodLog = resource.create(CreateFoodLogRequest(foods, timestampUtc, name, context))

    public suspend fun list(start: String, end: String): ListFoodLogsResponse =
        resource.list(ListFoodLogsRequest(start, end, context))

    public suspend fun update(
        id: String,
        foods: List<FoodSelection>? = null,
        timestampUtc: String? = null,
        name: String? = null,
    ): FoodLog = resource.update(UpdateFoodLogRequest(id, foods, timestampUtc, name, context))

    public suspend fun delete(id: String): DeleteFoodLogResponse =
        resource.delete(DeleteFoodLogRequest(id, context))
}

/** Glucose operations that replace request identity with the scoped context. */
public class UserGlucoseResource internal constructor(
    private val resource: GlucoseResource,
    private val context: PartnerUserContext,
) {
    public suspend fun predict(request: PredictGlucoseRequest): GlucosePrediction =
        resource.predict(
            request.copy(
                endUserId = context.endUserId,
                timezone = context.timezone,
            ),
        )
}

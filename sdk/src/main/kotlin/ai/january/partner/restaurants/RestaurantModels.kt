package ai.january.partner.restaurants

import ai.january.partner.PartnerUserId
import ai.january.partner.foods.ServingOption
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

public data class GetRestaurantMenuItemsRequest(
    public val restaurantId: String,
    public val limit: Int = 100,
    public val offset: Int = 0,
    public val endUserId: PartnerUserId? = null,
)

public data class SearchRestaurantsRequest(
    public val query: String, public val latitude: Double, public val longitude: Double,
    public val radius: Double = 8000.0, public val limit: Int = 10,
    public val endUserId: PartnerUserId? = null,
)

public enum class RestaurantResultType {
    @Json(name = "restaurant") RESTAURANT,
    @Json(name = "menu_item") MENU_ITEM,
}

@JsonClass(generateAdapter = false)
public data class Restaurant(
    public val type: RestaurantResultType, public val id: String, public val name: String,
    @Json(name = "is_chain") public val isChain: Boolean? = null,
    public val distance: Double? = null, public val city: String? = null,
    public val address1: String? = null, public val address2: String? = null,
)

@JsonClass(generateAdapter = false)
public data class SearchRestaurantsResponse(
    @Json(name = "total_count") public val totalCount: Int,
    public val items: List<Restaurant>,
)

@JsonClass(generateAdapter = false)
public data class RestaurantMenuItem(
    public val type: String, public val id: String, public val name: String,
    @Json(name = "restaurant_name") public val restaurantName: String,
    @Json(name = "is_chain") public val isChain: Boolean? = null,
    @Json(name = "energy") public val calories: Double? = null,
    public val protein: Double? = null,
    @Json(name = "carbs") public val carbohydrates: Double? = null,
    @Json(name = "net_carbs") public val netCarbohydrates: Double? = null,
    @Json(name = "fat") public val totalFat: Double? = null,
    public val fiber: Double? = null,
    @Json(name = "sugars") public val totalSugars: Double? = null,
    @Json(name = "added_sugars") public val addedSugars: Double? = null,
    @Json(name = "gi") public val glycemicIndex: Double? = null,
    @Json(name = "gl") public val glycemicLoad: Double? = null,
    @Json(name = "photo_url") public val photoUrl: String? = null,
    public val distance: Double? = null,
    public val servings: List<ServingOption>,
)

@JsonClass(generateAdapter = false)
public data class SearchRestaurantMenuItemsResponse(
    @Json(name = "total_count") public val totalCount: Int,
    public val items: List<RestaurantMenuItem>,
)


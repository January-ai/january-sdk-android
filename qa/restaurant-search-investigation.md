# Restaurant menu lookup — August 31, 2026

## Update: ID-based implementation added; deployment pending

See [the implementation and verification report](../../partner-api-contract/docs/restaurant-menu-by-id.md). The API and all six SDKs now have local restaurant-ID support, and the three demos passed fixture checks. Backend verification is blocked by private npm authentication; no deployment has occurred.

## Earlier correction: the name-search change was not the intended fix

The earlier name-query change below does **not** implement the restaurant-detail flow used by the January consumer iOS app. Its fixture test only proves that a selected name is passed to text search; it does not prove that a restaurant's menu is loaded correctly.

The reference is `ios-january/Shared/API/Restaurant/RestaurantService.swift` and `Shared/Screens/Restaurant/RestaurantView.swift`:

1. Load `GET /logging/place/{restaurantId}` with the ID from the selected restaurant search result.
2. Read the first menu and its sections from that response.
3. Load each section through `GET /logging/place/{restaurantId}/menus/{menuId}/sections/{sectionId}/food`.

These ID-based endpoints already exist in `logging-service`, including its internal router. They read the restaurant/menu relationships directly rather than using text search. The inspected partner-proxy v1.2 restaurant controller, partner API contract, and Android/iOS/web SDK resources expose only restaurant search and proximity menu-item search; they omit this ID-based detail flow. None of the three SDK demos currently implements the reference flow.

The actual correction requires exposing those existing operations through the partner API/SDKs and loading the selected restaurant by ID. The unrelated Taco Bell text-search behavior below is **not** the explanation or acceptance test for this requested detail flow. At the time of this earlier investigation, no ID-based implementation or backend deployment had been made. See the update above for the current status.

## Earlier text-search investigation

The following records the narrower client query change and live text-search observations. It must not be read as completion of the user's restaurant-detail bug.

## Client fix

Restaurant detail previously reused the text from the restaurant search field. A search can return several restaurants, so selecting another result still requested a menu using the original query, then discarded items whose restaurant name did not match the selection.

Android `RestaurantDetailScreen` and iOS `RestaurantDetailViewModel` now send the **selected restaurant's name**. The unused original-query parameter was removed. Name filtering remains in place; unrelated restaurants' dishes must not be presented as the selected menu.

The regression scenario searches for `Fixture`, selects `Fixture Cafe`, and expects `Fixture bowl`. The fixture only returns the menu for the selected name, making the old behavior fail. This does not simulate or resolve the separate production API defect below.

## Live API evidence

All requests used the production partner API with the existing authorized local credential. Coordinates: **37.7749, -122.4194**; radius: **8,000 metres**; limit: **20**. All four returned HTTP 200. These were read-only requests; credentials are omitted from the retained evidence.

| Endpoint / query | Actual returned data | Request ID |
| --- | --- | --- |
| `/v1.2/restaurants`, `Taco Bell` | 20 results, including Taco Bell in San Francisco, 903 m away | `5d16c0b0-d3d8-4c7d-b6be-0567a11b86b4` |
| `/v1.2/restaurants/menu-items`, `Taco Bell` | Only `takoball` at Nara Sushi and `taroball` at Milk Tea Lab; **no Taco Bell items** | `dc948951-0178-457a-b91e-e4bd786a5b10` |
| `/v1.2/restaurants/menu-items`, `taco` | 20 results including Taco Bell's `cantina crispy chicken taco` (231857686) and `cantina soft chicken taco` (209666811), both 903 m away | `709f8dc6-14ec-4825-9ecf-5f98c566edd9` |
| `/v1.2/restaurants/menu-items`, `nara sushi` | 20 items from Nara Sushi | `4b3062d3-9d43-4948-9f54-4f0a9e8e1e54` |

Raw sanitized responses are retained under [restaurant-search evidence](parity/evidence/2026-08-31/restaurant-search/). Counts above describe the returned response, not the total size of the database.

## Remaining backend issue

The API advertises dish **or restaurant name** queries. The captured results show Taco Bell data exists, but that restaurant's exact name misses it. This is upstream of either app's filtering.

Local source path: partner-proxy `RestaurantsService.searchMenuItems` → `/logging/search/restaurants/menu` → food-search `/search/restaurants/menu` → Typesense `searchRestaurantFood`. The local Typesense implementation already lists `food_name,restaurant_name` in `query_by`; source inspection alone does **not** establish which deployed configuration or index behavior causes the mismatch.

Backend follow-up should trace the request IDs, confirm the deployed query fields/index data, and make exact restaurant-name matching return its own menu items while preserving dish search. A proper menu-by-restaurant-ID API would avoid this ambiguity, but adding one is a separate API change, not part of this small client correction.

Do not treat an arbitrary `taco` search as Taco Bell's complete menu, or remove the restaurant filter to hide this defect. The mobile change cannot make missing API results appear.

## Validation

- Android selected-restaurant regression: **passed on the physical Pixel**. Debug APK built and installed.
- iOS selected-restaurant regression: **passed on the isolated simulator** using the current app source.
- [Android screenshot](parity/evidence/2026-08-31/restaurant-search/android-restaurant-query-regression.png) and [iOS screenshot](parity/evidence/2026-08-31/restaurant-search/ios-restaurant-query-regression.png) capture the returned menu after selecting a restaurant from a different search phrase.
- These checks prove the client query correction. They do **not** prove Taco Bell's production exact-name search is fixed.

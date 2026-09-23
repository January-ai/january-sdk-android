# Changelog

## Unreleased

* Breaking: `AlternativeFood.id`, `ServingOption.id`, `RestaurantMenuEntry.id`, and `LoggedFood.id` are non-null; the API always returns these catalog IDs
* Photo analysis uses the API's default, the reasoning-based analyzer, unless `reasoningEffort` is set; `AnalysisEffort.NONE` selects the standard analyzer
* Food analyses wait at least 120 seconds for their answer
* HTTP 409 `conflict` is reported as `ErrorCategory.VALIDATION`
* Water logs (`waterLogs.create`, `list`, `delete`) and weight logs (`weightLogs.create`, `list`) per end user, with daily totals and latest daily weights
* `ServingSummary.weightGrams`, the weight of one catalog serving
* Corrections send the prior scan in the API's new request shape; `foodLogs.update` rejects an empty patch before any network call
* Breaking: `DetectedFood` exposes `serving` and `quantity` instead of `servings`, matching the current Partner API; `0.1.x` fails to decode photo scans
* Food-log summaries per day or week with `foodLogs.getSummary`
* Optional reasoning-based photo analysis with `ScanFoodPhotoRequest.reasoningEffort`
* Reusable microphone speech recognition with live RMS, partial text, and stable lifecycle state
* Coroutine-first Android SDK for API 24+ (core library desugaring below API 26)
* Provider-managed short-lived tokens with single-flight refresh
* Nine-attempt bounded exponential backoff with jitter
* User-scoped Foods, Restaurants, Photo Scanning, Food Logs, and Glucose clients
* Food autocomplete, full hydration, and local portion calculations
* Photo preparation and native photo/barcode scanner
* Typed imperial and metric glucose-profile measurements
* Shared, white-label-ready Jetpack Compose demo components
* Paginated restaurant-menu lookup by restaurant ID, ready after backend deployment

Pin pre-release integrations to the version or revision supplied by January.

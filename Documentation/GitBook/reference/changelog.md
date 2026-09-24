# Changelog

## 0.3.1 - 2026-09-23

* Food, water, and weight logs send and read the API's `created_at` and keep `timestampUtc`, `consumedAt`, and `measuredAt`; 0.3.0's food, water, and weight logs don't work with the current API, so upgrade to 0.3.1
* Water logs take 0.1–101.4 cups

## 0.3.0 - 2026-09-23

* Breaking: `AlternativeFood.id`, `ServingOption.id`, `RestaurantMenuEntry.id`, and `LoggedFood.id` are non-null; the API always returns these catalog IDs
* Photo analysis uses the API's default, the reasoning-based analyzer, unless `reasoningEffort` is set; `AnalysisEffort.NONE` selects the standard analyzer
* Food analyses wait at least 120 seconds for their answer
* HTTP 409 `conflict` is reported as `ErrorCategory.VALIDATION`
* Water logs (`waterLogs.create`, `list`, `delete`) and weight logs (`weightLogs.create`, `list`) per end user, with daily totals and latest daily weights
* Client tokens can carry the `water_logs:read`, `water_logs:write`, `weight_logs:read`, and `weight_logs:write` scopes; a token endpoint that mints least-privilege tokens must add them for these operations
* `ServingSummary.weightGrams`, the weight of one catalog serving
* Corrections send the prior scan in the API's new request shape; `foodLogs.update` rejects an empty patch before any network call
* `glucose.predict` rejects a fractional `GlucosePredictionProfile.age` with `ErrorCategory.VALIDATION`; the API takes whole years
* The example app has a Tracking tab for one day's food logs, nutrient totals, water, and weight

## 0.2.2 - 2026-09-16

* Voice capture waits two seconds of silence before ending a capture; `VoiceCaptureSession(endOfSpeechSilence = …)` tunes it

## 0.2.1 - 2026-09-16

* Minimum Android version lowered from API 26 to API 24 (apps enable core library desugaring)

## 0.2.0 - 2026-09-16

* Breaking: `DetectedFood` exposes `serving` and `quantity` instead of `servings`, matching the current January API; `0.1.x` fails to decode photo scans
* Food-log summaries per day or week with `foodLogs.getSummary`
* Optional reasoning-based photo analysis with `ScanFoodPhotoRequest.reasoningEffort`
* `SearchFoodsRequest` gains `offset` for paging and accepts `limit` up to 50

## 0.1.2 - 2026-09-14

* Token-provider failures surface as `JanuaryException` (`ErrorCategory.AUTHENTICATION`) instead of crashing the process

## 0.1.1 - 2026-09-03

* Compiled with Kotlin 2.1.20 for compatibility with current React Native and Expo Android toolchains

## 0.1.0 - 2026-09-02

* Coroutine-first Android SDK for API 26+, published to Maven Central
* Provider-managed short-lived tokens with single-flight refresh
* Nine-attempt bounded exponential backoff with jitter
* User-scoped foods, restaurants, photo scanning, food log, and glucose clients
* Food autocomplete, full food details, and local portion calculations
* Photo preparation and native photo/barcode scanner
* Reusable microphone speech recognition with live RMS, partial text, and stable lifecycle state
* Typed imperial and metric glucose-profile measurements
* Shared, white-label-ready Jetpack Compose demo components
* Production restaurant-menu search and paginated lookup by restaurant ID
* Single food log retrieval and no-content deletion aligned with API `v1.2`

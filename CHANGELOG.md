# Changelog

All notable changes to the January SDK for Android are documented here. This
project uses Semantic Versioning.

## [Unreleased]

## [0.3.0] - 2026-09-23

- Breaking: `AlternativeFood.id`, `ServingOption.id`, `RestaurantMenuEntry.id`,
  and `LoggedFood.id` are no longer nullable. The API now always returns these
  catalog IDs (food, serving, and menu IDs are 1 to 10 digits with no leading
  zero), so drop any null checks on them.
- A photo analysis without `reasoningEffort` now gets the API's default, the
  reasoning-based analyzer; set `AnalysisEffort.NONE` for the standard one. The
  SDK sends an effort only when you set one.
- Photo, text, and correction analyses wait at least 120 seconds for their
  answer, since the reasoning-based analyzer can take tens of seconds. A longer
  read timeout on your `OkHttpClient.Builder` is kept; other requests keep the
  builder's timeouts.
- An HTTP 409 (`conflict`, a request that clashes with what the API holds) is
  `ErrorCategory.VALIDATION` with `code` `conflict`. Sending it again unchanged
  gets the same answer.
- Added water logs: `waterLogs.create` records an amount in `VolumeUnit.FL_OZ`,
  `VolumeUnit.ML`, or `VolumeUnit.CUP` (the API caps an end user at 24 L per day and answers
  `daily_water_limit_exceeded`), `waterLogs.list` returns one `DailyWaterTotal`
  per local day in the unit you ask for, and `waterLogs.delete` removes an
  entry. The scoped client exposes `user.waterLogs`.
- Added weight logs: `weightLogs.create` records a `Weight` in pounds or
  kilograms and `weightLogs.list` returns the latest `DailyWeight` per local
  day. The scoped client exposes `user.weightLogs`.
- `ServingSummary` gains `weightGrams`, the weight of one catalog serving; the
  API now returns it on scans, alternatives, and logged foods. The
  three-argument constructor remains, so Java callers compile unchanged.
- `foodAnalysis.correct` sends the prior scan in the API's new correction
  request shape. Nothing changes for callers: pass back the `FoodScan` you
  received.
- `foodLogs.update` throws `JanuaryException` (`ErrorCategory.VALIDATION`)
  when nothing is set to change; the API rejects an empty patch. Only the
  fields you set are sent.
- Client tokens can carry the `water_logs:read`, `water_logs:write`,
  `weight_logs:read`, and `weight_logs:write` scopes; a backend token endpoint
  that mints least-privilege tokens must add them for these operations.
- Regenerated the internal transport from the refreshed contract (water and
  weight logs, correction request shape, serving weights, integer profile age,
  required catalog IDs, the `conflict` error code).
- `glucose.predict` throws `JanuaryException` (`ErrorCategory.VALIDATION`) for
  a fractional `GlucosePredictionProfile.age`; the API takes whole years.
- `foodAnalysis.correct` leaves out a hand-built detection without a serving
  size instead of sending one serving.
- Demo: a new Tracking tab shows one day at a time, with the day's food logs and
  nutrient totals, water total, and weight, each with a log action. The Food
  Logs tab is now Logs.

## [0.2.2] - 2026-09-16

- Voice capture waits two seconds of silence before ending a capture (the
  platform default cut people off between words). `VoiceCaptureSession` takes
  an `endOfSpeechSilence` duration to tune it; the value is a hint that some
  recognizers ignore.

## [0.2.1] - 2026-09-16

- Lower the minimum Android version from API 26 to API 24. The SDK uses
  `java.time`, so apps whose `minSdk` is below 26 must enable core library
  desugaring (`isCoreLibraryDesugaringEnabled = true` plus the
  `com.android.tools:desugar_jdk_libs` dependency); the AAR metadata makes a
  build without it fail with a clear message instead of crashing at runtime on
  API 24 and 25 devices.

## [0.2.0] - 2026-09-16

- `SearchFoodsRequest` gains `offset` for paging and accepts `limit` up to 50,
  matching the API.
- The `User-Agent` header reports the SDK version 0.2.0.

Breaking: the Partner API changed the shape of a detected food, and `0.1.x`
clients now fail to decode photo scans and description analyses with a
decoding error. Update to this version to restore them.

- `DetectedFood` no longer has `servings`. It has `serving` (the selected
  catalog serving, a `ServingSummary`) and `quantity` (how many of that serving
  were eaten). `nutrients` are already scaled to `quantity`. `DetectedServing`
  is a deprecated alias of `ServingSummary`; its `selectedQuantity` moved to
  `DetectedFood.quantity`.
- Food alternatives are `AlternativeFood` values with `servings:
  List<ServingSummary>`. `FoodAlternative` remains as an alias.
- Removed the unused `NaturalLanguageFood`, `NaturalLanguageServing`,
  `NaturalLanguageFoodDetection`, and `SearchFoodsByNaturalLanguageResponse`
  types. `analyzeDescription` has returned `FoodScan` since 0.1.0.
- Added `foodLogs.getSummary`: nutrients summed per day or week over a date
  range, with totals and a per-logged-day average (`FoodLogSummary`).
- Added `ScanFoodPhotoRequest.reasoningEffort` (`AnalysisEffort.XHIGH`) to
  opt into the reasoning-based photo analyzer.
- Regenerated the internal transport from contract release 1.2.0
  (`searchFoods` paging, new error codes).

## [0.1.2] - 2026-09-14

- Surface client token provider failures as `JanuaryException`
  (`ErrorCategory.AUTHENTICATION`) instead of crashing the process. The token
  interceptor previously let the provider's exception escape inside an OkHttp
  interceptor, which OkHttp rethrows on its dispatcher thread.

## [0.1.1] - 2026-09-03

- Compile the SDK with Kotlin 2.1.20 so applications using the current React
  Native and Expo Android toolchain can consume its published metadata.

## [0.1.0] - 2026-09-02

- Publish the initial January Android SDK to Maven Central.

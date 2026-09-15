# Changelog

All notable changes to the January SDK for Android are documented here. This
project uses Semantic Versioning.

## [0.2.0] - Unreleased

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

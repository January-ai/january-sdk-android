# Changelog

## Unreleased

* Breaking: `DetectedFood` exposes `serving` and `quantity` instead of `servings`, matching the current Partner API; `0.1.x` fails to decode photo scans
* Food-log summaries per day or week with `foodLogs.getSummary`
* Optional reasoning-based photo analysis with `ScanFoodPhotoRequest.reasoningEffort`
* Reusable microphone speech recognition with live RMS, partial text, and stable lifecycle state
* Coroutine-first Android SDK for API 26+
* Provider-managed short-lived tokens with single-flight refresh
* Nine-attempt bounded exponential backoff with jitter
* User-scoped Foods, Restaurants, Photo Scanning, Food Logs, and Glucose clients
* Food autocomplete, full hydration, and local portion calculations
* Photo preparation and native photo/barcode scanner
* Typed imperial and metric glucose-profile measurements
* Shared, white-label-ready Jetpack Compose demo components
* Paginated restaurant-menu lookup by restaurant ID, ready after backend deployment

Pin pre-release integrations to the version or revision supplied by January.

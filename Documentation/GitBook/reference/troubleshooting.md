# Troubleshooting

## Gradle cannot find `ai.january:january-sdk-android:0.1.0`

Confirm `mavenCentral()` is present in
`dependencyResolutionManagement.repositories`.
Confirm the dependency is exactly
`implementation("ai.january:january-sdk-android:0.1.0")`, then run the
dependency-report command from the [installation guide](../getting-started/installation.md).

## Token provider fails

Verify the app has an explicit token endpoint URL, an authenticated partner
session, TLS outside local development, and a 2xx response containing a
non-empty token plus `expiresIn` or `expires_in` greater than 60 seconds. The SDK
has no default endpoint.

## Provider is called repeatedly

The token may be inside the 60-second refresh window, the provider may be
returning a near-expired token, or requests may receive `401 token_expired`.
Concurrent refreshes normally share one call. Do not create a new
`JanuaryPartnerClient` per request.

## Authentication is rejected

Do not manually send `x-end-user-id` with a client token. Confirm the token was
minted for the signed-in account and the public client is intended for the
production API. Only `token_expired` is automatically refreshed and replayed.

## Food picker has incomplete servings

Call `foods.get` after selecting a search result. Autocomplete and search
objects are discovery data and are not guaranteed to contain all servings.

## Camera is blank or denied

Inspect the merged manifest for `CAMERA`, test runtime permission, and use a
device/emulator with a camera. `JanuaryFoodScanner` uses CameraX; it is not the
system photo picker.

## Photo scan is too large or rotated

Use `PhotoScanImage.dataUri` rather than original camera bytes.

## Support diagnostics

Provide the SDK version, Android/AGP/Java versions, operation, exception
category, HTTP status, and minimal reproduction. Exclude keys, tokens, images,
nutrition records, and health profiles.

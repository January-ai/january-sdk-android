# Troubleshooting

## Client-token provider fails

Confirm the app configured its own backend URL and authentication. The response
must contain a non-empty token and an `expiresIn` greater than 60 seconds. There
is intentionally no default token endpoint.

## Provider is called repeatedly

Tokens refresh one minute before expiration. Concurrent requests share one
refresh. Only `401 token_expired` invalidates the cache and replays once.

## Food picker has incomplete servings

Call `foods.getFood` after selecting a search result. Search and autocomplete
responses are discovery objects, not guaranteed full food records.

## Photo scan is too large

Use `ScanFoodPhotoRequest.fromImageData` or `PhotoScanImage.dataUri` instead of
sending original camera bytes.

## Support diagnostics

Capture the operation, SDK revision, Android version, exception category, and
HTTP status. Never include credentials or user health data.

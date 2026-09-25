# Troubleshooting

## Gradle cannot find the SDK

Confirm your `settings.gradle.kts` has `mavenCentral()` inside the
`dependencyResolutionManagement { repositories { ... } }` block and that the
dependency names an exact released version, then run the `dependencyInsight`
command from [Installation](../getting-started/installation.md).

## The build requires core library desugaring

`Dependency 'ai.january:january-sdk-android:…' requires core library desugaring
to be enabled` means your app module hasn't turned on desugaring. Every app that
uses the SDK needs it, whatever its `minSdk`
([Installation](../getting-started/installation.md)).

## Cleartext traffic is blocked

Requests fail with `ErrorCategory.AUTHENTICATION` (with the sample provider,
after about 47 seconds of token retries), and the exception's `cause` chain
ends in `CLEARTEXT communication to 10.0.2.2 not permitted by network security
policy`. Android
blocks `http://` URLs such as the local token relay's. Allow cleartext in debug
builds only, with the debug manifest from
[First request](../getting-started/quick-start.md).

## Token provider fails

Check that the app has a token endpoint URL and a valid app session, that the
URL uses HTTPS outside local development, and that the endpoint returns a 2xx
response with `token` and `expires_in` (or `expiresIn`) greater than 60
seconds. The SDK has no default endpoint. The underlying failure is in the
`JanuaryException`'s `cause`.

## Provider is called repeatedly

The token may be inside the 60-second refresh window, the provider may be
returning a nearly expired token, or requests may be getting `401 token_expired`.
Concurrent refreshes normally share one call. Create one `JanuaryPartnerClient`
per signed-in account, not one per request
([Client lifecycle](../concepts/client-lifecycle.md)).

## Requests act as the previous account

A new `forUser` scope doesn't change the account: the client keeps the token it
cached for the previous one. Create a new `JanuaryPartnerClient` when the
account changes ([Client lifecycle](../concepts/client-lifecycle.md)).

## Authentication is rejected

Confirm the token was minted for the signed-in account. The SDK removes
`January-End-User-ID` from January requests; the header only matters on
requests to your own token endpoint. Only `token_expired` is refreshed and
replayed automatically.

## `AUTHORIZATION` or `scope_insufficient`

The token lacks the scope the operation needs. Mint it with the scopes for
every feature the app uses ([Scopes](../getting-started/backend-token-endpoint.md#scopes)).
`403 forbidden` when minting means **Enable client tokens** is off in the
Developer Dashboard.

## Food picker has incomplete servings

Call `foods.get` after the user selects a search result. Autocomplete and search
results are discovery data and may not list every serving.

## Camera is blank or denied

Check the merged manifest for `CAMERA`, test the runtime permission, and use a
device or emulator with a camera. `JanuaryFoodScanner` uses CameraX; it isn't
the system photo picker.

## Voice capture fails with `RECOGNIZER_UNAVAILABLE`

The device has no speech recognition service. Many emulator images lack one;
use a physical device or an emulator image with Google Play.

## Photo scan is too large or rotated

Use `PhotoScanImage.dataUri` rather than the original camera bytes.

For anything else, send a [support report](testing-and-support.md#support-report).

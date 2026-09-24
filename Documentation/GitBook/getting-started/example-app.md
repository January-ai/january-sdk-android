# Example app

The `demo` module of the
[January Android SDK repository](https://github.com/January-ai/january-sdk-android)
is a Jetpack Compose application covering autocomplete,
hydrated food servings, photo and barcode scanning, restaurants, food logs,
a per-day tracking view (food logs with totals, water, and weight), glucose
prediction, user context, and imperial/metric inputs.

The Tracking tab also charts the user's history, always ending today: daily
water totals as bars and weight as a line, each with a Week / Month / Year
switch. A Year of water shows one bar per calendar month. Weights appear in the
unit selected on the card, whatever unit they were logged in. The list endpoints
return at most 100 days per call, so the demo fetches a year in consecutive
chunks of 90 days and joins them, a pattern you can reuse for longer ranges.

Install Android Studio with Android SDK 36 and JDK 17. Android Studio normally
creates `local.properties` with `sdk.dir`; command-line users can set
`ANDROID_HOME` instead.

## Configure token mode

Add untracked values to the repository's `local.properties`:

```properties
january.partnerTokenUrl=http://10.0.2.2:8787/api/january/client-token
```

`10.0.2.2` reaches localhost on the host machine from the Android emulator. For
a physical device, start the relay with `HOST=0.0.0.0 ./start.sh`, use the Wi-Fi
URL it prints, and set its generated token as `january.partnerSessionToken`.
The token endpoint URL has no default. The demo sends a `POST` with the selected
stable user ID in `January-End-User-ID`; for a LAN or hosted relay it also sends
`Authorization: Bearer <january.partnerSessionToken>`. A production provider
instead sends the app's normal session to its authenticated backend, which
derives the user ID server-side. The public SDK targets January production and
exposes no API-origin override.

Never commit `local.properties`. For the fastest setup, clone the standalone
[January Token Relay](https://github.com/January-ai/january-token-relay) and run
`./start.sh`. Create an API key and separately enable client tokens in the
dashboard before starting it. The key stays in the relay's `.env` file and
never enters the Android app.

For a hosted development relay, follow the relay's
[Vercel guide](https://github.com/January-ai/january-token-relay#optional-deploy-to-vercel),
then set `january.partnerTokenUrl` to its HTTPS token URL and
`january.partnerSessionToken` to its `RELAY_TOKEN`. This is for development and
testing only; production must use your authenticated backend.

## Build and run

```bash
./gradlew :demo:testDebugUnitTest :demo:assembleDebug
./gradlew :demo:installDebug
```

Launch the app, confirm the connection state, run a food search, select a result,
change its serving, and exercise the scanner. If configuration is missing, the
demo should fail clearly instead of selecting a hidden URL.

## Optional debug-only shortcut

For the quickest local test, omit the token endpoint values and set
`january.apiKey=sk-your-server-api-key` in `local.properties`. The demo accepts
it only in Debug builds and displays a warning. Never commit the key, share the
APK, or distribute the build; switch back to client tokens afterward.

# Example app

The `demo` module is a Jetpack Compose application covering autocomplete,
hydrated food servings, photo and barcode scanning, restaurants, food logs,
glucose prediction, user context, and imperial/metric inputs.

## Configure token mode

Add untracked values to the repository's `local.properties`:

```properties
january.partnerTokenUrl=http://10.0.2.2:8787/january-token
january.partnerSessionToken=your-local-app-session-or-relay-secret
```

`10.0.2.2` reaches localhost on the host machine from the Android emulator. Use
your machine's LAN address for a physical device. The token endpoint URL has no
default. The demo sends a `POST` with your app session in `Authorization` and
the selected stable user ID in `x-end-user-id`; adapt those request details to
your own backend contract. The public SDK targets January production and
exposes no API-origin override.

Never commit `local.properties`.

## Build and run

```bash
./gradlew :demo:testDebugUnitTest :demo:assembleDebug
./gradlew :demo:installDebug
```

Launch the app, confirm the connection state, run a food search, select a result,
change its serving, and exercise the scanner. If configuration is missing, the
demo should fail clearly instead of selecting a hidden URL.

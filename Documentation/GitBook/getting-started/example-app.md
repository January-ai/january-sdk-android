# Example app

The `demo` module of the
[January Android SDK repository](https://github.com/January-ai/january-sdk-android)
is a Jetpack Compose app that uses every resource: food search with full foods
and servings, food analysis and the native scanner, restaurants, food logs, a
Tracking tab with each day's food, water, and weight and week, month, and year
charts, glucose prediction, and user and timezone settings. It creates a new
client whenever you switch users, as [Client lifecycle](../concepts/client-lifecycle.md)
describes.

Open the repository in Android Studio, or set `ANDROID_HOME` for command-line
builds.

## 1. Start the token relay

The demo gets its tokens from the
[token relay](https://docs.january.ai/docs/authentication#develop-with-the-token-relay).
Starting it walks you through creating an API key and switching on **Enable
client tokens** in the Developer Dashboard. The key stays in the relay's `.env`
file and never enters the app.

## 2. Point the demo at the relay

Add the relay's values to the repository's `local.properties`, which is
untracked. Never commit it.

| Where the relay runs | `january.partnerTokenUrl` | `january.partnerSessionToken` |
| --- | --- | --- |
| This computer, used from the emulator | `http://10.0.2.2:8787/api/january/client-token` | Leave it out |
| Your Wi-Fi, used from a device (`HOST=0.0.0.0 ./start.sh`) | The Wi-Fi URL the relay prints | The token the relay generates |
| Hosted ([Vercel guide](https://github.com/January-ai/january-token-relay#optional-deploy-to-vercel)) | The relay's HTTPS token URL | Its `RELAY_TOKEN` |

```properties
january.partnerTokenUrl=http://10.0.2.2:8787/api/january/client-token
```

The demo sends a `POST` with the selected end-user ID in `January-End-User-ID`,
plus `Authorization: Bearer <january.partnerSessionToken>` when that is set. Its
debug build allows cleartext traffic, so the local `http://` URL works.

## 3. Build and run

```bash
./gradlew :demo:installDebug
```

Search for a food, open it, change its serving, and try the scanner. If the
token URL is missing, the demo says so instead of choosing a default.

## Without the relay

For the quickest local test, leave out the token values and set
`january.apiKey=sk-…` in `local.properties`. Debug builds use the key directly
and show a warning. Never commit the key, share the APK, or distribute the
build.

Next: [Client lifecycle](../concepts/client-lifecycle.md)

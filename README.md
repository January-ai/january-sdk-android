# January SDK for Android

The official Kotlin SDK for January food discovery, restaurants, meal scanning,
food logs, and glucose prediction. It supports Android API 24+ (with core library desugaring below API 26), Java 17, and
Kotlin coroutines.

## Quick start: run the demo with client tokens

You can try the Android SDK before your own backend is ready. The standalone
January Token Relay keeps the January API key off the app and temporarily
stands in for your production token endpoint.

You need two terminal windows: one for the January Token Relay, which holds
your API key and hands the app short-lived client tokens, and one for the
demo. The first run takes about ten minutes.

### Terminal 1: start the token relay

1. Open a terminal.
2. Download the relay and move into its folder. It needs Node.js 20.12 or
   newer and nothing else:

   ```bash
   git clone https://github.com/January-ai/january-token-relay.git
   cd january-token-relay
   ```

3. Start it:

   ```bash
   ./start.sh
   ```

   It checks your Node version and then asks
   `Paste your API key (input is hidden):`. Leave it waiting and create the
   key in the next two steps.

4. Create the API key. In a browser,
   [sign up](https://dashboard.january.ai/sign-up) or
   [sign in](https://dashboard.january.ai/sign-in) to the January Developer
   Dashboard, open **API keys → Create key**, and copy the full `sk-…` value.
   It is shown once.
5. Enable client tokens. Open
   [Client tokens](https://dashboard.january.ai/dashboard/client-tokens) and
   switch on **Enable client tokens**. Until this is on, January answers the
   relay with `403`.
6. Back in Terminal 1, paste the key and press Enter. Nothing appears while
   you type. You should see:

   ```text
   ✓ API key accepted by January (sk-abcd…wxyz)
   ✓ Saved to .env (readable only by you; git ignores it)

   January Token Relay is running on this machine (development only).
     Endpoint      http://localhost:8787/api/january/client-token
   ```

   Leave this window open for the whole session. The key is saved in a
   git-ignored `.env`, so the next `./start.sh` starts without asking.

### Terminal 2: run the Android demo

7. Open a second terminal.
8. Download the SDK repository and move into it:

   ```bash
   git clone https://github.com/January-ai/january-sdk-android.git
   cd january-sdk-android
   ```

   You need Android Studio with Android SDK 36 and JDK 17. Android Studio
   normally creates `local.properties` with your SDK path; command-line users
   can set `ANDROID_HOME` instead.

9. Tell the demo where the relay is. Add this line to `local.properties`
   (keep any existing `sdk.dir` line). `10.0.2.2` is how the emulator reaches
   your computer:

   ```properties
   january.partnerTokenUrl=http://10.0.2.2:8787/api/january/client-token
   ```

   This static demo URL is accepted only by Debug builds. A production app
   calls its authenticated backend, which derives the user ID from the
   verified session instead of trusting an app-supplied value.

10. Start an Android Emulator, then build and install the demo:

    ```bash
    ./gradlew :demo:installDebug
    ```

11. Open the installed app and search for `banana`. Terminal 1 prints
    `minted=true status=200` the first time the app asks for a token.

For a physical device, start the relay with `HOST=0.0.0.0 ./start.sh`, use the
Wi-Fi URL it prints for `january.partnerTokenUrl`, and set its generated relay
token as `january.partnerSessionToken`. The demo sends that token as
`Authorization: Bearer <token>`. See the
[example-app guide](Documentation/GitBook/getting-started/example-app.md) for
physical-device networking and troubleshooting.

For production or any shared build, never put the `sk-…` key in an Android
app. The private, debug-only shortcut at the end is the sole local exception.

### Optional: deploy the relay to Vercel

If localhost is inconvenient, follow the relay's
[Vercel deployment guide](https://github.com/January-ai/january-token-relay#deploy).
Set `JANUARY_API_KEY` and a long random `RELAY_TOKEN` in Vercel, then use:

```properties
january.partnerTokenUrl=https://YOUR-PROJECT.vercel.app/api/january/client-token
january.partnerSessionToken=YOUR_RELAY_TOKEN
```

The hosted relay is also for development and testing only. Its relay token is
not a substitute for authenticating your users.

This relay is only for development. In production, keep the SDK token provider
but point it to your authenticated backend.

## Add the SDK to your app

### 1. Install

Make sure your application resolves dependencies from Maven Central, then add
the SDK to `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("ai.january:january-sdk-android:0.2.2")
}
```

If your app's `minSdk` is 24 or 25, also enable core library desugaring (the
SDK uses `java.time`, which Android added in API 26):

```kotlin
android {
    compileOptions { isCoreLibraryDesugaringEnabled = true }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
```

### 2. Connect and make the first request

Connect your authenticated backend endpoint through `JanuaryTokenProvider`, create one
user-scoped client, and reuse it while the same user is signed in:

```kotlin
import ai.january.partner.JanuaryClientToken
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.JanuaryTokenProvider
import ai.january.partner.PartnerUserId
import ai.january.partner.foods.SearchFoodsRequest
import kotlinx.coroutines.runBlocking

val tokenProvider = JanuaryTokenProvider {
    val response = appBackend.fetchJanuaryClientToken()
    JanuaryClientToken(token = response.token, expiresIn = response.expiresIn)
}

val january = JanuaryPartnerClient.withClientTokenProvider(tokenProvider)
val user = january.forUser(
    endUserId = PartnerUserId(session.user.id),
    timezone = "America/New_York",
)

runBlocking {
    val foods = user.foods.search(SearchFoodsRequest(query = "banana"))
    println("Found ${foods.items.size} foods")
}
```

`appBackend.fetchJanuaryClientToken()` represents your app's authenticated call
to its own backend. Copy the
[complete provider implementation](Documentation/GitBook/getting-started/authentication.md)
when wiring the real endpoint. A successful request prints a result count;
an empty result is still a successful connection.

See the [installation guide](Documentation/GitBook/getting-started/installation.md)
for the complete Gradle configuration.

Your production endpoint returns `{ "token": "ct-…", "expiresIn": 1800 }`,
derives the stable end-user ID from the verified app session, and chooses scopes
on the server. See the
[backend token endpoint guide](Documentation/GitBook/getting-started/backend-token-endpoint.md)
for the complete contract.

## Common tasks

After the first request, use the user-scoped client for Foods, Restaurants,
Photo Scanning, Food Logs, and Glucose. Recreate it when the signed-in account
changes.

- [Foods](Documentation/GitBook/guides/foods.md)
- [Restaurants](Documentation/GitBook/guides/restaurants.md)
- [Photo scanning](Documentation/GitBook/guides/photo-scanning.md)
- [Food logs](Documentation/GitBook/guides/food-logs.md)
- [Glucose prediction](Documentation/GitBook/guides/glucose-prediction.md)
- [Voice capture](Documentation/GitBook/guides/voice-capture.md)

## Documentation and development

The [complete Android SDK guide](Documentation/GitBook/README.md) covers
authentication, every resource, permissions, retries, errors, testing, and
troubleshooting.

To work on the SDK itself:

```bash
./gradlew :sdk:testDebugUnitTest :sdk:assembleRelease
```

The demo's end-to-end Maestro suite and how to run it locally are described in
[demo/.maestro/README.md](demo/.maestro/README.md).

## Optional: fastest debug-only shortcut

If you only want to make a request immediately, the demo can use a server API
key directly in a local Debug build. This bypasses the recommended client-token
flow above:

```properties
# local.properties
january.apiKey=sk-your-server-api-key
```

Then run `./gradlew :demo:installDebug`. Never commit `local.properties`, share
the APK, or distribute any build containing the key. Release builds disable
this path. Move to the local token relay or your authenticated backend before
testing anything outside your own machine.

## License

Apache 2.0. January API data and content remain subject to the January API
Developer Terms.

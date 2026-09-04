# January SDK for Android

The official Kotlin SDK for January food discovery, restaurants, meal scanning,
food logs, and glucose prediction. It supports Android API 26+, Java 17, and
Kotlin coroutines.

## Quick start: run the demo with client tokens

You can try the Android SDK before your own backend is ready. A small local
Node server keeps the January API key off the app and issues the same
short-lived client tokens your production backend will issue.

### 1. Create the credentials

Complete both steps—they are on separate dashboard pages:

1. [Sign up](https://dashboard.january.ai/sign-up) or
   [sign in](https://dashboard.january.ai/sign-in), then open
   **API keys → Create key** and copy the full `sk-…` value.
2. Open [Client tokens](https://dashboard.january.ai/dashboard/client-tokens)
   and select **Enable client tokens**.

For production or any shared build, never put the `sk-…` key in an Android
app. The private, debug-only shortcut at the end is the sole local exception.

### 2. Start the local token server

Install Node.js 22 or newer. In a first terminal:

```bash
git clone https://github.com/January-ai/january-server-sdk-node.git
cd january-server-sdk-node
npm ci
cp .env.example .env
# Edit .env and set JANUARY_API_KEY to the key you just created.
npm run demo:token-server
```

Leave it running. The server binds only to your computer and exchanges the API
key for short-lived tokens using the January Server SDK.

### 3. Run the Android demo

Install Android Studio with Android SDK 36 and JDK 17. Android Studio normally
creates `local.properties` with your SDK path; command-line users can instead
set `ANDROID_HOME`.

In a second terminal, clone the demo repository if needed:

```bash
git clone https://github.com/January-ai/january-sdk-android.git
cd january-sdk-android
```

Add these untracked values to `local.properties`, keeping any existing
`sdk.dir` line:

```properties
january.partnerTokenUrl=http://10.0.2.2:8787/api/january/token
january.partnerSessionToken=january-local-demo
```

These static demo values are accepted only by Debug builds. A production app
must create its token provider from its authenticated runtime session instead
of compiling a session credential into `BuildConfig`.

Then start an Android Emulator and run:

```bash
./gradlew :demo:installDebug
```

Open the installed app and search for `banana`. Android Emulator maps
`10.0.2.2` to your development machine's localhost. See the
[example-app guide](Documentation/GitBook/getting-started/example-app.md) for
physical-device networking and troubleshooting.

## Add the SDK to your app

### 1. Install

Make sure your application resolves dependencies from Maven Central, then add
the SDK to `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("ai.january:january-sdk-android:0.1.1")
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
this path. Move to the local token server or your authenticated backend before
testing anything outside your own machine.

## License

Apache 2.0. January API data and content remain subject to the January API
Developer Terms.

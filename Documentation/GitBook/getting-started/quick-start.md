# First request

This smoke-test app creates a token provider and a January client, searches for
a food, and shows the result on screen. Do the [installation](installation.md)
first, then add these files to a minimal Android app module.

## 1. Configure the app module

Put this in `app/build.gradle.kts`. If you did the installation in this module,
add only the three `providers.gradleProperty` lines, the three
`buildConfigField` lines, and `buildFeatures`.

```kotlin
plugins {
    // AGP 9 compiles Kotlin itself. With AGP 8, also apply org.jetbrains.kotlin.android.
    id("com.android.application")
}

val januaryTokenUrl = providers.gradleProperty("januaryTokenUrl").orNull ?: ""
val partnerSessionToken = providers.gradleProperty("partnerSessionToken").orNull ?: ""
val januaryEndUserId = providers.gradleProperty("januaryEndUserId").orNull ?: ""

android {
    namespace = "com.example.januaryquickstart"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.januaryquickstart"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "JANUARY_TOKEN_URL", "\"$januaryTokenUrl\"")
        buildConfigField("String", "PARTNER_SESSION_TOKEN", "\"$partnerSessionToken\"")
        buildConfigField("String", "JANUARY_END_USER_ID", "\"$januaryEndUserId\"")
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Required by the SDK (it uses java.time), at any minSdk.
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation("ai.january:january-sdk-android:0.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
```

## 2. Add the manifests

Create `app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

The SDK's manifest adds the internet, camera, and microphone permissions. This
app doesn't open the camera or the microphone.

Android blocks plain `http://` requests, and the token relay on your computer
serves `http://`. To test against it, allow cleartext traffic in debug builds
only with `app/src/debug/AndroidManifest.xml`:

```xml
<!-- app/src/debug/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:usesCleartextTraffic="true" />
</manifest>
```

## 3. Add the activity and provider

Create
`app/src/main/java/com/example/januaryquickstart/MainActivity.kt`. The provider
is the one from [Authentication](authentication.md), except that it leaves out
`Authorization` when the session is blank, as it is for the local relay.

```kotlin
package com.example.januaryquickstart

import ai.january.partner.JanuaryClientToken
import ai.january.partner.JanuaryException
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.JanuaryTokenProvider
import ai.january.partner.JanuaryTokenProviderException
import ai.january.partner.PartnerUserId
import ai.january.partner.foods.SearchFoodsRequest
import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class BackendTokenProvider(
    endpoint: String,
    private val endUserId: String,
    private val sessionToken: suspend () -> String,
) : JanuaryTokenProvider {
    private val endpointUrl = URL(endpoint)

    override suspend fun fetchClientToken(): JanuaryClientToken {
        val session = sessionToken() // Read the current session on every call.
        return withContext(Dispatchers.IO) {
            val connection = (endpointUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setFixedLengthStreamingMode(0)
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
                if (session.isNotBlank()) setRequestProperty("Authorization", "Bearer $session")
                // Only the token relay reads this header. Your production endpoint
                // takes the user from the session and ignores it.
                setRequestProperty("January-End-User-ID", endUserId)
            }
            try {
                val status = connection.responseCode
                if (status !in 200..299) {
                    throw JanuaryTokenProviderException(
                        "Token endpoint returned HTTP $status",
                        retryable = status == 408 || status == 429 || status >= 500,
                    )
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                try {
                    JanuaryClientToken.fromJson(body)
                } catch (error: Exception) {
                    throw JanuaryTokenProviderException("Token endpoint returned an unreadable body", cause = error)
                }
            } catch (error: IOException) {
                // Offline, refused, or timed out.
                throw JanuaryTokenProviderException("Token endpoint unreachable", retryable = true, cause = error)
            } finally {
                connection.disconnect()
            }
        }
    }
}

class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val output = TextView(this).apply {
            text = "Connecting…"
            textSize = 18f
            setPadding(40, 80, 40, 40)
        }
        setContentView(output)

        if (BuildConfig.JANUARY_TOKEN_URL.isBlank() || BuildConfig.JANUARY_END_USER_ID.isBlank()) {
            output.text = "Pass -PjanuaryTokenUrl and -PjanuaryEndUserId (step 4)."
            return
        }

        val january = JanuaryPartnerClient.withClientTokenProvider(
            BackendTokenProvider(
                endpoint = BuildConfig.JANUARY_TOKEN_URL,
                endUserId = BuildConfig.JANUARY_END_USER_ID,
                sessionToken = { BuildConfig.PARTNER_SESSION_TOKEN },
            ),
        )
        val user = january.forUser(
            endUserId = PartnerUserId(BuildConfig.JANUARY_END_USER_ID),
            timezone = ZoneId.systemDefault().id,
        )

        scope.launch {
            try {
                val response = user.foods.search(
                    SearchFoodsRequest(query = "greek yogurt", limit = 5),
                )
                output.text = buildString {
                    appendLine("Connected")
                    response.items.forEach { appendLine("• ${it.name}") }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: JanuaryException) {
                output.text = "January ${error.category}: ${error.message}"
            } catch (error: Exception) {
                output.text = "Integration error: ${error.message}"
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
```

## 4. Build and run

Pick the token endpoint and pass its values as Gradle properties:

| Token endpoint | `-PjanuaryTokenUrl` | `-PpartnerSessionToken` | Who picks the user |
| --- | --- | --- | --- |
| Your backend | `https://your-backend.example/january-token` | An app session token | Your backend, from the session. It ignores `January-End-User-ID`. |
| Token relay on this computer | `http://10.0.2.2:8787/api/january/client-token` | Leave it out | The relay, from `January-End-User-ID` |
| Hosted or LAN token relay | The relay's URL | The relay's `RELAY_TOKEN` | The relay, from `January-End-User-ID` |

Always pass `-PjanuaryEndUserId`, the end-user ID the token is minted for. The
app scopes requests with it; the SDK removes it from January requests because
the token identifies the user. `10.0.2.2` is how the Android emulator reaches
your computer. Start the relay first
([Develop with the token relay](https://docs.january.ai/docs/authentication#develop-with-the-token-relay)).

```bash
./gradlew :app:installDebug \
  -PjanuaryTokenUrl=https://your-backend.example/january-token \
  -PpartnerSessionToken=YOUR_APP_SESSION_TOKEN \
  -PjanuaryEndUserId=YOUR_END_USER_ID

adb shell am start -n \
  com.example.januaryquickstart/.MainActivity
```

The screen shows `Connected`, followed by up to five food names. An error shows
its January error category, or an integration message. Remove session tokens
from your shell history afterward; a real app reads its session from secure
storage.

## In a Compose app

Make the same call from a `LaunchedEffect`, with `user` created as above, once
per signed-in account. Leaving the composition cancels the request.

```kotlin
import ai.january.partner.JanuaryException
import ai.january.partner.JanuaryPartnerUserClient
import ai.january.partner.foods.SearchFoodsRequest
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun FoodSearchCheck(user: JanuaryPartnerUserClient) {
    var status by remember { mutableStateOf("Connecting…") }
    LaunchedEffect(user) {
        status = try {
            val response = user.foods.search(SearchFoodsRequest(query = "greek yogurt", limit = 5))
            "Connected\n" + response.items.joinToString("\n") { "• ${it.name}" }
        } catch (error: JanuaryException) {
            "January ${error.category}: ${error.message}"
        }
    }
    Text(status)
}
```

Next: [Example app](example-app.md)

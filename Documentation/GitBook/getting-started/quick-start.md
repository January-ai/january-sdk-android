# First request

This smoke app constructs the token provider and January client, performs a food
search, and prints the result on screen. Complete the
[installation](installation.md) first, then add these files to a minimal Android
app module.

## 1. Configure the app module

Place this in `app/build.gradle.kts` (merge the `android` and `dependencies`
blocks into an existing app if necessary):

```kotlin
plugins {
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
        minSdk = 26
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
    }
}

dependencies {
    implementation("ai.january:january-sdk-android:0.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
```

The source-build substitution from the installation page makes the SDK
coordinate resolve.

## 2. Add the manifest

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

The SDK manifest contributes Internet and camera permissions. This smoke app
does not open the camera.

## 3. Add the activity and provider

Create
`app/src/main/java/com/example/januaryquickstart/MainActivity.kt`:

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
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class PartnerBackendTokenProvider(
    endpoint: String,
    private val sessionToken: String,
) : JanuaryTokenProvider {
    private val endpointUrl = URL(endpoint)

    override suspend fun fetchClientToken(): JanuaryClientToken =
        withContext(Dispatchers.IO) {
            val connection = (endpointUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $sessionToken")
            }
            try {
                val status = connection.responseCode
                if (status !in 200..299) {
                    throw JanuaryTokenProviderException(
                        "Token endpoint returned HTTP $status",
                        retryable = status == 408 || status == 429 || status >= 500,
                    )
                }
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                JanuaryClientToken.fromJson(json)
            } finally {
                connection.disconnect()
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

        require(BuildConfig.JANUARY_TOKEN_URL.isNotBlank()) {
            "Pass -PjanuaryTokenUrl=https://your-backend.example/january-token"
        }
        require(BuildConfig.PARTNER_SESSION_TOKEN.isNotBlank()) {
            "Pass -PpartnerSessionToken=<your-app-session-token>"
        }
        require(BuildConfig.JANUARY_END_USER_ID.isNotBlank()) {
            "Pass -PjanuaryEndUserId=<your-stable-user-id>"
        }

        val january = JanuaryPartnerClient.withClientTokenProvider(
            PartnerBackendTokenProvider(
                endpoint = BuildConfig.JANUARY_TOKEN_URL,
                sessionToken = BuildConfig.PARTNER_SESSION_TOKEN,
            ),
        )
        val user = january.forUser(
            endUserId = PartnerUserId(BuildConfig.JANUARY_END_USER_ID),
            timezone = java.util.TimeZone.getDefault().id,
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

Use an HTTPS token endpoint that returns a production client token:

```bash
./gradlew :app:installDebug \
  -PjanuaryTokenUrl=https://your-backend.example/january-token \
  -PpartnerSessionToken=YOUR_APP_SESSION_TOKEN \
  -PjanuaryEndUserId=YOUR_STABLE_USER_ID

adb shell am start -n \
  com.example.januaryquickstart/.MainActivity
```

Expected screen output begins with `Connected`, followed by up to five food
names. An error is rendered with either a January error category or an
integration message. Remove command-line session tokens from shell history after
the smoke test; production apps obtain their session from app-owned secure state.

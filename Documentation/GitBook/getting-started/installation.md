# Installation

The SDK is published to Maven Central as `ai.january:january-sdk-android`.
Requirements and the verified toolchain are in
[Compatibility and permissions](../reference/compatibility.md).

## 1. Enable Maven Central

In your application's `settings.gradle.kts`, include Maven Central in dependency
resolution:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

## 2. Add the dependency

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("ai.january:january-sdk-android:0.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
```

Pin an exact SDK version. The SDK's API uses coroutines (`suspend` functions
and `StateFlow`), so your app needs them too. The SDK also brings in Jetpack
Compose, CameraX, ML Kit barcode scanning, Retrofit, and Moshi, and Gradle
raises your app's versions of these to at least the
[SDK's](../reference/compatibility.md#libraries).

## 3. Enable core library desugaring

The SDK supports API 24 and uses `java.time`. Its AAR metadata requires every
app that depends on it to enable core library desugaring with
`desugar_jdk_libs` 2.1.5 or later, whatever the app's `minSdk`. Without it, the
build fails with `Dependency 'ai.january:january-sdk-android:0.3.2' requires
core library desugaring to be enabled`.

```kotlin
// app/build.gradle.kts
android {
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
```

## 4. Compile against SDK 36

The SDK's AAR metadata also requires compile SDK 36 or later. `minSdk` can
stay as low as 24.

```kotlin
// app/build.gradle.kts
android {
    compileSdk = 36
}
```

## 5. Verify resolution

```bash
./gradlew :app:dependencyInsight \
  --dependency january-sdk-android \
  --configuration debugRuntimeClasspath
./gradlew :app:assembleDebug
```

The dependency report should show `ai.january:january-sdk-android:0.3.2` resolved
from Maven Central.

Next: [Backend token endpoint](backend-token-endpoint.md)

# Installation

## Maven Central

The Android SDK is published to Maven Central as
`ai.january:january-sdk-android`. You do not need to clone this repository or
configure a Gradle composite build. The verified toolchain is Gradle 9.5.1,
Android Gradle Plugin 9.2.1, compile SDK 36, Java 17, and minimum Android API 26.

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
    implementation("ai.january:january-sdk-android:0.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
}
```

Pin an exact SDK version in production builds. Coroutines are used by the
complete token-provider example, and Lifecycle supplies `viewModelScope` in the
first-request example.

## 3. Verify resolution

```bash
./gradlew :app:dependencyInsight \
  --dependency january-sdk-android \
  --configuration debugRuntimeClasspath
./gradlew :app:assembleDebug
```

The dependency report should show `ai.january:january-sdk-android:0.1.0` resolved
from Maven Central.

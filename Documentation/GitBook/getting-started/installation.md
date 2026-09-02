# Installation

## Source distribution

The Android SDK is integrated as a pinned Gradle composite source build. The
verified toolchain is Gradle 9.5.1, Android Gradle Plugin 9.2.1, compile SDK 36,
Java 17, and minimum Android API 26.

## 1. Clone and pin the SDK

Clone the repository next to your application:

```bash
git clone https://github.com/January-ai/january-sdk-android.git
cd january-sdk-android
./gradlew :sdk:testDebugUnitTest
```

Record the resolved commit with `git rev-parse HEAD` and pin that commit in your
release process. Advance it deliberately after running your consumer tests;
never track a moving branch in a production build.

## 2. Include the source build

In the consuming application's `settings.gradle.kts`, substitute the future
module coordinate with the checked-out `:sdk` project:

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

includeBuild("../january-sdk-android") {
    dependencySubstitution {
        substitute(module("ai.january:january-sdk-android"))
            .using(project(":sdk"))
    }
}
```

Change `../january-sdk-android` to the actual relative path from your app.

## 3. Add the dependency

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("ai.january:january-sdk-android:0.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
}
```

The `includeBuild` substitution resolves the coordinate to the checked-out SDK
source. Coroutines are used by the complete token-provider example, and Lifecycle supplies
`viewModelScope` in the first-request example.

## 4. Verify resolution

```bash
./gradlew :app:dependencies --configuration debugRuntimeClasspath
./gradlew :app:assembleDebug
```

The dependency report should map `ai.january:january-sdk-android:0.1.0` to the included
`:sdk` project.

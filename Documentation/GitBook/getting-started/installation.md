# Installation

## Current availability

The Android SDK is a controlled preview. Maven Central returns no artifact for
`ai.january:partner-sdk:0.1.0`, and this repository does not yet define a Maven
publication. Until January publishes a signed release, integrate a pinned source
checkout as a Gradle composite build. The repository is private; ask January to
grant your GitHub account access before starting.

The verified preview toolchain is Gradle 9.5.1, Android Gradle Plugin 9.2.1,
compile SDK 36, Java 17, and minimum Android API 26.

{% hint style="danger" %}
The expected line `implementation("ai.january:partner-sdk:0.1.0")` does **not**
work by itself from `mavenCentral()` today.
{% endhint %}

## 1. Clone and pin the SDK

Authenticate Git for the GitHub account January authorized. A browser-visible
`404` or `Repository not found` from this URL normally means that account does
not have access. Then clone the repository next to your application and pin the
exact revision approved by January:

```bash
git clone https://github.com/January-ai/january-sdk-android.git
cd january-sdk-android
git checkout ca207c04d6f9dd9d8f1c206b5f421b035a955c0c
./gradlew :sdk:testDebugUnitTest
```

That revision is the source checkout used to verify this guide. Move to a newer
revision only when January approves it, and never track a moving branch in a
release build.

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
        substitute(module("ai.january:partner-sdk"))
            .using(project(":sdk"))
    }
}
```

Change `../january-sdk-android` to the actual relative path from your app.

## 3. Add the dependency

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("ai.january:partner-sdk:0.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
}
```

The `includeBuild` substitution is what makes this resolve today. This exact
dependency mapping was verified against the current repository. Coroutines are
used by the complete token-provider example, and Lifecycle supplies
`viewModelScope` in the first-request example.

## 4. Verify resolution

```bash
./gradlew :app:dependencies --configuration debugRuntimeClasspath
./gradlew :app:assembleDebug
```

The dependency report should map `ai.january:partner-sdk:0.1.0` to the included
`:sdk` project.

## Future Maven installation

After January publishes the artifact, this page will replace the composite
build with a repository and versioned dependency snippet. Do not assume that
future distribution exists until the release is visible in its documented
registry and has release notes.

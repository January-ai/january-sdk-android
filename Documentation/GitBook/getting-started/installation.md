# Installation

Add the Android SDK to your application module:

```kotlin
dependencies {
    implementation("ai.january:partner-sdk:0.1.0")
}
```

Ensure `google()` and `mavenCentral()` are available. The SDK requires Android
API 26+, Java 17, and network permission. Camera permission is not required by
the SDK's photo picker; barcode scanning uses the platform camera flow.

{% hint style="info" %}
The SDK is pre-release. Use the version or repository revision supplied by
January until a stable semantic version is published.
{% endhint %}

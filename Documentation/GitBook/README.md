# January SDK for Android
Add food search, food analysis, restaurant search, voice capture, food, water,
and weight logging, and glucose prediction to a native Android app, with Kotlin
coroutines and typed models.

## Start here

1. [Install the SDK](getting-started/installation.md).
2. Add a [token endpoint](getting-started/backend-token-endpoint.md) to your
   backend, or run the token relay while you build.
3. Implement `JanuaryTokenProvider` in the app
   ([Authentication](getting-started/authentication.md)).
4. Run the [first food search](getting-started/quick-start.md).
5. Follow the [food discovery and serving flow](concepts/food-lifecycle.md).

## Security model

The app gets a short-lived client token (`ct-…`) from your backend and sends it
directly to January. Your API key (`sk-…`) stays on your backend. Never put it
in an APK, whether in Gradle properties, `BuildConfig`, resources, or remote
configuration.

```text
Android app ── POST with the app session ──▶ Your backend
                                                  │
                                                  │ POST /v1.2/auth/client-tokens
                                                  │ with your API key
                                                  ▼
                                             January API
                                                  │
Android app ◀── { token, expires_in } ────────────┘
     │
     └── Authorization: Bearer ct-… ──▶ January API
```

The SDK always calls the production January API. It has no base-URL setting,
and it never sees your token endpoint's URL; only your token provider does.
[How authentication works](https://docs.january.ai/docs/authentication) walks through the flow.

Never log tokens, API keys, meal images, nutrition data, or health profiles, and
keep them out of analytics.

## Requirements

Android API 24 or later, compile SDK 36, Java 17, and core library desugaring
in every app that uses the SDK. [Compatibility and permissions](reference/compatibility.md)
lists the verified toolchain and the permissions the SDK adds.

The [example app](getting-started/example-app.md) uses every resource and the
native food scanner.

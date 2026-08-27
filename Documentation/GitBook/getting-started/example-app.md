# Example app

The `demo` module is a native Jetpack Compose application covering discovery,
autocomplete, hydrated food details and servings, meal scanning, food logs,
glucose prediction, user context, and imperial/metric measurements.

For local development, add untracked values to `local.properties`:

```properties
january.partnerTokenUrl=https://your-backend.example/january-token
january.internalApiBaseUrl=https://your-january-development-origin.example
```

There are deliberately no URL defaults. These properties belong to the
January-owned debug demo and are excluded from the release AAR. A production
partner app implements `JanuaryTokenProvider` with its own authenticated backend.

Run with Android Studio or:

```bash
./gradlew :demo:installDebug
```

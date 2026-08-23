# January Partner SDK for Android

Private Kotlin/Android SDK for Partner API v1.2. The public API is coroutine-first:
network operations are `suspend` functions and never block the calling thread.

```kotlin
val client = JanuaryPartnerClient(developmentApiKey = apiKey)
val results = client.foods.search(
    SearchFoodsRequest(
        query = "banana",
        endUserId = PartnerUserId("your-user-id"),
    ),
)
```

The bearer key is temporarily supported only for private development testing.
Never embed it in a distributed Android application. Token-provider authentication
will replace this development initializer before public distribution.

Generated Retrofit transport code is internal. Consumers use the handwritten
`JanuaryPartnerClient`, resource, request, response, identifier, and error types.

## Verify

```sh
./gradlew :sdk:testDebugUnitTest :sdk:assembleRelease
./scripts/check-generated-transport.sh
```

The public SDK covers all 13 v1.2 operations across Foods, Restaurants, Photo
Scanning, Food Logs, and Glucose. The generated Retrofit transport remains an
internal implementation detail.
Opt-in live tests use `JANUARY_API_KEY` and `JANUARY_END_USER_ID` from the process
environment and never print either value.

`Contract/sdk-contract.lock.json` pins release 1.2.0 and archive SHA-256
`959ab95b4a95218fd4e3948ac0841748ec81534eb1c4476c8165920e94a3e361`.
Regenerate the internal transport with `./scripts/generate-transport.sh`.

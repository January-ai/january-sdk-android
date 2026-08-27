# Testing and support

## Verify the SDK checkout

```bash
./gradlew :sdk:testDebugUnitTest
./gradlew :sdk:assembleRelease
```

For camera and Android-runtime coverage, connect an emulator or device and run:

```bash
./gradlew :sdk:connectedDebugAndroidTest
./gradlew :demo:connectedDebugAndroidTest
```

## Verify an integration

Test at least these partner-controlled conditions before shipping:

1. token endpoint success and both expiry-field spellings;
2. unavailable token endpoint and exhausted provider retries;
3. concurrent cold-start requests produce one token fetch;
4. a `token_expired` response fetches a replacement and replays once;
5. cancellation stops waiting work;
6. autocomplete → search → `getFood` exposes complete serving choices;
7. camera denied, camera granted, photo scan, and barcode lookup;
8. account and timezone switching do not leak prior-user state.

## Versioning and updates

There is no published Android release yet. Pin an approved Git commit, review
the repository changelog and public API diff, rerun the checks above, and update
the pin deliberately. Do not infer compatibility from the `0.1.0` source value.

## Support report

Include the pinned commit, Android/AGP/Java versions, failing operation,
`JanuaryException.category`, HTTP status, and reproduction steps. Never include
tokens, server-side credentials, meal images, nutrition records, or health profiles.

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
6. autocomplete → search → `get` exposes complete serving choices;
7. camera denied, camera granted, photo scan, and barcode lookup;
8. account and timezone switching do not leak prior-user state.

## Versioning and updates

Releases are published to Maven Central as
`ai.january:january-sdk-android:<version>` when the matching `v<version>` tag is
pushed; the repository's GitHub Releases page lists the versions that exist. Pin
an exact released version, review the changelog and public API diff, rerun the
checks above, and update the pin deliberately.

## Support report

Include the pinned commit, Android/AGP/Java versions, failing operation,
`JanuaryException.category`, HTTP status, and reproduction steps. Never include
tokens, server-side credentials, meal images, nutrition records, or health profiles.

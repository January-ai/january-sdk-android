# Releasing the Android SDK

## One-time setup

1. Make the repository public.
2. Register the `ai.january` namespace in Sonatype Central Portal.
3. Create a Central Portal user token and add its values as repository secrets:
   `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD`.
4. Create and publish a GPG signing key. Add the complete ASCII-armored private key
   as `MAVEN_GPG_PRIVATE_KEY` and its passphrase as `MAVEN_GPG_PASSWORD`.

## Release

1. Update `version` in `sdk/build.gradle.kts` and record the change in
   `CHANGELOG.md` on `main`.
2. Create and push a tag exactly matching `v<SDK version>`, such as `v0.2.0` or
   `v0.2.0-beta.1`.

The release workflow accepts tags that point to `main`, runs the SDK and complete
demo UI test suites, validates a signed local Maven publication, publishes and
waits for Maven Central, and then creates the GitHub Release.

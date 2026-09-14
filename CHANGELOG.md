# Changelog

All notable changes to the January SDK for Android are documented here. This
project uses Semantic Versioning.

## [0.1.2] - 2026-09-14

- Surface client token provider failures as `JanuaryException`
  (`ErrorCategory.AUTHENTICATION`) instead of crashing the process. The token
  interceptor previously let the provider's exception escape inside an OkHttp
  interceptor, which OkHttp rethrows on its dispatcher thread.

## [0.1.1] - 2026-09-03

- Compile the SDK with Kotlin 2.1.20 so applications using the current React
  Native and Expo Android toolchain can consume its published metadata.

## [0.1.0] - 2026-09-02

- Publish the initial January Android SDK to Maven Central.

# iOS / Android parity review — August 31, 2026

**Full parity is not established.** The local [side-by-side gallery](evidence/2026-08-31/index.html) contains 64 named screen/state entries: 53 paired captures and 11 incomplete, unpaired, or unsuitable captures. Paired means screenshots exist for the same named state; it does not mean pixel equality or exhaustive screen coverage.

## Menu follow-up — completed

After the initial audit, the requested menu fixes were applied to both real app repositories:

- iOS menu rows and menu detail now use the green fork-and-knife image placeholder, replacing the takeout-bag/cup symbol.
- Menu row placeholder size is 24pt/dp; the detail placeholder is 44pt/dp on both platforms. Android now allows the shared image component to receive that detail size.
- iOS `Loading menu` has a trailing spacer, expanding its card to the available container width.
- Fixtures explicitly use an empty image URL. iOS `testMenuPresentation` passed, and the Android restaurant/menu workflow passed on the Pixel. Both apps were run and the three revised states were visually inspected.
- Gallery entries `menu-loading`, `restaurant-detail`, and `menu-detail` contain fresh screenshots. Logs are preserved in the evidence folder.

The initial audit's other limits below still apply. The iOS checkout now contains the above requested source changes; its earlier untouched status described the initial audit only.

## What was exercised

| Area | Captured / exercised | Remaining limits |
| --- | --- | --- |
| Food search | Initial, keyboard, loading, empty, results; HTTP 401/403/404/422/429/500; retry | HTTP 504 captured only on Android; native keyboard/scroll differences |
| Food details | Full food retrieval, serving selection, nutrition, glucose loading/error/retry/result | No exhaustive testing of every serving/input combination |
| Alternatives | Initial on both; loading/error/empty/results/retry on Android | iOS automation stopped receiving UI responses while scrolling to the action |
| Photo scan | Entry, image URL, sample preview, loading, result, correction input/loading/error/retry/result | iOS scan HTTP 500 image is from an early malformed error fixture; corrected recapture was blocked |
| Food logs | Root, empty, loading, results, new log, serving, save loading/error/retry, detail, edit/update | iOS root HTTP 500 recapture blocked; iOS delete action was not hittable in the native popover |
| Delete log | Android confirmation, error, direct retry, empty result; iOS confirmation | Android confirmation screenshot caught animation and needs replacement; iOS delete failure/retry unverified |
| Glucose | Profile, conditions, food picker, serving, loading, error, retry, result | Not every profile/validation combination |
| Restaurants | Search, location filters, loading, error, empty, retry, restaurant detail, menu loading/error/empty, menu-item detail | Keyboard/scroll positions vary; separate global menu search not exhaustively exercised |
| Settings / setup | Settings on both; setup on iOS | QA authentication subtitles intentionally differ; Android setup not captured in this run |
| Shared navigation | Root title collapse, reachable actions, compact detail navigation | Native iOS glass, sheet detents, typography, icons and transition behavior are not identical |

Physical camera optics, actual barcode recognition, camera permissions, photo-library permission variants, real airplane-mode UI, and accessibility/font-size variants do **not** have matched cross-platform evidence in this run. SDK socket timeout and malformed-response classification are unit-tested; this is not equivalent to an offline UI test.

## Android fixes made during this review

- Shared root navigation places the large title below the toolbar and collapses it on scroll; compact detail titles remain centered.
- Bright modal sheets request dark status/navigation indicators.
- API failures retain HTTP status, error code and request ID. Search now uses the same error mapping as the other SDK resources.
- Socket timeouts and malformed JSON are categorized separately; cancellation continues to propagate.
- Photo correction has a working **Try again** action and preserves the correction input.
- Retrying an already-confirmed failed deletion sends the delete again without demanding a second confirmation, following the iOS source.
- Food-picker loading uses the shared spinner treatment.

The original `JanuaryException` constructor and Kotlin defaults remain available to SDK consumers.

## Observed differences that remain

- Native sheets, confirmation popovers/dialogs, segmented controls, typography, icons and glass/shadow treatments differ visibly.
- Keyboard visibility and scroll offsets differ in several captures; screenshots are not normalized to identical viewport coordinates.
- Some iOS HTTP 500 paths display a generic HTTP message; Android now retains the API's structured message. Error headings and retry actions follow the same pattern, but the body text is not always identical.
- Food-log date formatting is localized differently; calorie units display `cal` on iOS and `kcal` on Android.
- Settings' connection subtitle differs because the isolated iOS test app injects a fixture client outside its normal authentication configuration.

## Validation and evidence

- Android: all five `StateParityWorkflowTest` workflows and `AppNavigationTest.largeRootTitleCollapsesWhileNavigationActionsRemainReachable` passed on the connected Pixel 10a, Android 16/API 36.
- After the final delete-retry change, the food-log workflow passed its focused rerun. The final debug and instrumentation APKs built and installed successfully.
- All three `TransportErrorParityTest` SDK tests passed, including after preserving the exception constructor compatibility.
- iOS: actual January UI ran in a separate iOS 26.2 simulator. Search/detail captures reached alternatives, but that workflow did not finish. Correction recovery, menu error/empty and setup checks passed. The final delete test and corrected root-error recapture test failed on UI interaction. **The iOS suite did not pass as a whole.**
- A final normal-app screenshot attempt was blocked by the Pixel's lock/notification overlay. No screenshot of that overlay was included. The device's original stay-awake setting was restored.

The gallery displays raw, unretouched January screenshots at equal CSS widths, preserving each device's aspect ratio. Invalid fixture/error or animation captures are explicitly flagged. No screenshots from other apps are included. `evidence/` is ignored by Git to avoid committing device captures.

Raw run logs and xcresult bundles remain in `/tmp/january-parity-review`. Current screenshots are preserved in `qa/parity/evidence/2026-08-31/screenshots` with machine-readable coverage in `coverage.json`.

## Reproduce Android fixture checks

Run `fixture_server.py` on port 18766 and reverse that port to the selected device:

```sh
/usr/bin/python3 qa/parity/fixture_server.py 18766
adb -s DEVICE_SERIAL reverse tcp:18766 tcp:18766
./gradlew :sdk:testDebugUnitTest --tests ai.january.partner.TransportErrorParityTest :demo:assembleDebug :demo:assembleDebugAndroidTest
adb -s DEVICE_SERIAL install -r demo/build/outputs/apk/debug/demo-debug.apk
adb -s DEVICE_SERIAL install -r demo/build/outputs/apk/androidTest/debug/demo-debug-androidTest.apk
adb -s DEVICE_SERIAL shell am instrument -w -r -e class ai.january.partner.demo.StateParityWorkflowTest ai.january.partner.demo.test/androidx.test.runner.AndroidJUnitRunner
```

Use the project's configured Android SDK/JDK. Do not use `connectedDebugAndroidTest` for the user's installed demo; it uninstalls the app after running. Screenshots are written to the app's external-files `parity` directory. Test user/timezone preferences are restored after each test.

The fixture server binds localhost, records only fixture method/path/body (never auth headers), and never forwards requests to production. Create/update/delete operate only on its in-memory fixture state.

## iOS reference provenance

Reference source: `partner-sdk-ios`, copied to `/tmp/january-parity-ios`; the original checkout was not edited. The copy uses bundle ID `ai.january.partner.demo.parity`, a separate simulator, and an injected fixture client at `http://127.0.0.1:18765`. Its UI implementation was not changed. The `ParityUITests.swift` harness is retained here, including the currently failing interaction scenarios; it is **not** a fully passing reusable suite yet.

To revisit a blocked comparison, use that isolated copy and the `Parity` scheme. Do not run mutation tests against the production API or modify the user's original iOS app configuration.

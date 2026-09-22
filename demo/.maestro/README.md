# Demo end-to-end suite

Maestro flows that drive the demo app against the local fixture server, one
flow per user journey, mirroring the React Native SDK's suite (same flow names,
same kebab-case test tags). They run on every pull request, split across four
emulator shards, and locally against any emulator or device.

## Run locally

```bash
# Terminal 1: deterministic API fixtures on the host
python3 qa/parity/fixture_server.py 18766

# Terminal 2: build, install, and run every flow
./gradlew :demo:installDebug
maestro test demo/.maestro/flows

# One flow, or the shard CI would run as "2 of 4"
maestro test demo/.maestro/flows/09-glucose.yaml
maestro test $(node demo/.maestro/shard.mjs 2 4)
```

`bootstrap.yaml` launches the app with the debug-only `januaryFixtureOrigin`
intent extra, so the client talks to `http://10.0.2.2:18766` (the emulator's
alias for the host) with a stub token. On a physical device, forward the port
and point both sides at it:

```bash
adb reverse tcp:18766 tcp:18766
maestro test -e FIXTURE_ORIGIN=http://127.0.0.1:18766 demo/.maestro/flows
```

Flows change the fixture server's behaviour through `scripts/control-fixture.js`
(HTTP status, delay, empty collections per route), `seed-fixture.js` (one saved
food log) and `reset-fixture.js`, which the bootstrap runs first so no flow
inherits another's configuration.

## Conventions

- Select elements by test tag (`Modifier.testTag`), never by position. The tag
  names are the React Native example's test IDs; `testTagsAsResourceId` on the
  root, sheets and dialogs exposes them to Maestro.
- Controls at the end of a scrolling screen go through `scroll-to.yaml`, which
  lifts them clear of the floating tab bar and the navigation bar.
- Assert on transient loading states with `optional: true`.
- Tag every flow `fixture` or `parity`; CI runs both tags.
- The Tracking tab (`tab-tracking`, `tracking-screen`) is a day view
  (`logs-day-previous`, `logs-day-next`, `logs-day-today`, `logs-day-label`)
  showing that day's food logs and totals (`food-logs-totals`), water
  (`water-total` or `water-empty`, `water-log`, `water-delete-last`) and weight
  (`weight-day` or `weight-empty`, `weight-log`). The Logs tab
  (`tab-food-logs`, `food-logs-screen`) lists food logs over a date range. `seed-fixture.js` dates its saved food log an hour ago, so it
  falls on today; the fixture server buckets logs, water and weight by the
  request's timezone, like the API.

## In CI

`.github/workflows/quality.yml` builds the APK once, then `ui-test-android`
runs four shards (27 flows, dealt round-robin by `shard.mjs`) through
`.github/scripts/android-ui-suite.sh`. Failed flows are
rerun once and named in a workflow warning; each shard uploads its JUnit report
and Maestro's failure screenshots and view hierarchies as `maestro-android-N`.
The `ui-tests` job summarizes the shards.

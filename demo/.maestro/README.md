# Demo end-to-end suite

Maestro flows that drive the demo app against the local fixture server, one
flow per user journey, mirroring the React Native SDK's suite (same flow names,
same kebab-case test tags). They run on every pull request, split across four
emulator shards, and locally against any emulator or device.

## Run locally

```bash
# Terminal 1: deterministic API fixtures on the host
python3 qa/parity/fixture_server.py 18766

# Terminal 2: build, install, and run every fixture flow
./gradlew :demo:installDebug
maestro test --include-tags fixture,parity demo/.maestro/flows

# One flow, or the shard CI would run as "2 of 4"
maestro test demo/.maestro/flows/09-glucose.yaml
maestro test $(node demo/.maestro/shard.mjs 2 4)
```

`bootstrap.yaml` launches the app with the debug-only `januaryFixtureOrigin`
intent extra, so the client talks to `http://10.0.2.2:18766` (the emulator's
alias for the host) with a stub token. `client-token-bootstrap.yaml` adds
`januaryFixtureClientTokens: "true"`, which keeps the demo's own per-user client
tokens instead: it mints them from the fixture server's
`/api/january/client-token`, as it would from a token relay, and the fixture
server keeps each end user's food logs, water and weights apart by token, like
the API (see flows 38 and 39). With the stub token everything belongs to one
shared user. On a physical device, forward the port
and point both sides at it:

```bash
adb reverse tcp:18766 tcp:18766
maestro test --include-tags fixture,parity -e FIXTURE_ORIGIN=http://127.0.0.1:18766 demo/.maestro/flows
```

Flows change the fixture server's behaviour through `scripts/control-fixture.js`
(HTTP status, delay, empty collections per route, optionally for one method, and
`HOLD`, which keeps a request's answer open until `release-fixture.js`), `seed-fixture.js` (one saved
food log, for `USER` when set), `seed-history.js` (about 13 months of water and weight ending today)
and `reset-fixture.js`, which the bootstrap runs first so no flow
inherits another's configuration. `assert-request.js` checks what the demo
sent: a field of its latest request to a route, from the query, the JSON body,
its `auth` header or its `end_user` header. The fixture suggests foods only for queries starting with "ban", so
typing anything else never opens the suggestion list.

## Conventions

- Select elements by test tag (`Modifier.testTag`), never by position. The tag
  names are the React Native example's test IDs; `testTagsAsResourceId` on the
  root, sheets and dialogs exposes them to Maestro.
- Controls at the end of a scrolling screen go through `scroll-to.yaml`, which
  lifts them clear of the floating tab bar and the navigation bar.
- Start and leave voice input through `voice-cancel.yaml`: a capture ends by
  itself after two seconds of silence, so the flow handles both the recording
  bar and the "Voice input unavailable" dialog.
- Type into a number field through `clear-field.yaml`, which empties it first;
  the fields are right-aligned, so a plain tap can leave the cursor before the
  digits. Dismiss the keyboard with `pressKey: Enter`, not `hideKeyboard`,
  which can press Back and close a sheet.
- To assert a loading state, slow its route with `control-fixture.js`
  (`DELAY`) and trigger it with `waitToSettleTimeoutMs: 500` on the tap, so
  the spinner is still there when the assertion runs. Keep a `DELAY` under 10
  seconds, the SDK's read timeout; to act while a request is on its way for
  longer, `HOLD` its answer and release it with `release-fixture.js`.
- Tag every flow `fixture` or `parity`; CI runs both tags. Flows tagged `live`
  run against the January API instead (see below).
- The Tracking tab (`tab-tracking`, `tracking-screen`) is a day view
  (`logs-day-previous`, `logs-day-next`, `logs-day-today`, `logs-day-label`)
  showing that day's food logs and totals (`food-logs-totals`), water
  (`water-total` or `water-empty`, `water-log`, `water-delete-last`) and weight
  (`weight-day` or `weight-empty`, `weight-log`). Each of the water and weight
  cards ends with a history chart that always runs to today (`water-chart` or
  `water-chart-empty`, `weight-chart` or `weight-chart-empty`) and its range
  switch (`water-chart-range-week`, `-month`, `-year`, and the same for
  `weight-chart-range-*`); the chart's accessibility summary names the range
  and what it shows. The fixture's water and weight lists return at most the
  100 most recent days, like the API, so a Year chart takes several requests. The Logs tab
  (`tab-food-logs`, `food-logs-screen`) lists food logs over a date range. `seed-fixture.js` dates its saved food log an hour ago, but no
  earlier than 00:01 in the demo's timezone, so it falls on today even just
  after midnight; the fixture server buckets logs, water and weight by the
  request's timezone, like the API.

## Coverage

```bash
node scripts/ui-coverage.mjs          # add --list to see where each tag is exercised
```

The check lists every test tag the demo declares and every id the fixture and
parity flows use, and fails unless each tag is tapped, typed into, waited for,
scrolled to or asserted visible by at least one flow. Assertions marked
`optional: true`, conditional `runFlow`s and negative checks do not count, and
an id that no longer exists in the demo is an error. Tags built at run time are
expanded from the rules at the top of the script (for example
`water-chart-range-week`), and list rows such as `food-result-0` stand for
their whole list.

## Live flows against a local relay

Flows tagged `live` (`90-` onward) run the demo against the January API through
your token relay and compare what it shows with what the API returns for the
same end user: food logs, the day's totals, water and weight entries, and the
water and weight charts. They create food logs and water entries and delete
them again, and the last one logs two weights, which the API keeps, so use a
test end user.

A full run makes about 170 API requests (the demo's own requests plus the
checks), so check your account's request allowance first. `run-live.mjs`
prints the estimate per flow and can run part of the suite.

1. Start the token relay (Terminal 1 in the [root README](../../README.md)) and
   build the demo against it: `january.partnerTokenUrl` in `local.properties`
   as that README describes, then `./gradlew :demo:installDebug`.
2. Run the live flows:

   ```bash
   node demo/.maestro/run-live.mjs --end-user my-test-user
   # or a part of them, within an estimated number of requests
   node demo/.maestro/run-live.mjs --end-user my-test-user --only 93,94 --budget 100
   ```

   The runner first checks, with one request, that the API answers for this
   user, then runs one flow at a time with a pause between them. When the API
   answers `429 rate_limited` it stops instead of failing every remaining
   step, and it lists the flows it did not start. The weight flow runs only
   when every flow before it passed. Maestro's debug output, the screenshots
   and `live-verification.log` (every check the flows made, tokens removed)
   go to `demo/.maestro/artifacts/live`, which git ignores. A single flow also
   runs directly: `maestro test -e END_USER_ID=my-test-user
   demo/.maestro/flows/94-live-water.yaml`.

`live-bootstrap.yaml` launches the demo normally and enters `END_USER_ID` in
Settings; `live-tracking-context.yaml` reads the day and timezone the Tracking
tab shows, so `scripts/live-verify.js` asks the API about the same calendar
day. The script mints its own client token from the relay and never prints it.
Other settings, each passed with `-e`:

| Setting | Default |
| --- | --- |
| `RELAY_TOKEN_URL`, the relay as your computer reaches it | `http://127.0.0.1:8787/api/january/client-token` |
| `RELAY_TOKEN`, for a relay on your network or on Vercel | none |
| `SHOTS`, where screenshots go | `demo/.maestro/artifacts/live` (git-ignored) |
| `BARCODE`, `RESTAURANT`, `MENU_ITEM` | `049000006346`, `Starbucks`, `latte` |

## In CI

`.github/workflows/quality.yml` runs `scripts/ui-coverage.mjs`, builds the APK
once, then `ui-test-android` runs four shards (40 fixture and parity flows,
dealt round-robin by `shard.mjs`) through
`.github/scripts/android-ui-suite.sh`. Failed flows are
rerun once and named in a workflow warning; each shard uploads its JUnit report
and Maestro's failure screenshots and view hierarchies as `maestro-android-N`.
The `ui-tests` job summarizes the shards.

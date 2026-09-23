// Lets the answers held by a HOLD rule on ROUTE (see control-fixture.js) finish.
//
//   - runScript:
//       file: ../scripts/release-fixture.js
//       env:
//         ROUTE: /v1.2/water-logs
// Maestro runs this on the host; override with -e FIXTURE_CONTROL=... when the
// fixture server listens elsewhere.
const control = typeof FIXTURE_CONTROL === 'string' && FIXTURE_CONTROL ? FIXTURE_CONTROL : 'http://127.0.0.1:18766';
const response = http.get(control + '/__release?route=' + encodeURIComponent(ROUTE));
if (!response.ok) {
  throw new Error('Fixture release failed for ' + ROUTE + ': HTTP ' + response.status);
}

// Fails the flow unless the demo's latest request to ROUTE (optionally only
// METHOD requests) sent VALUE at FIELD, a dot path into the recorded request
// ({ method, path, query, body }: query parameters or the JSON body), so a flow
// can check what the app sent as well as what it shows.
//
//   - runScript:
//       file: ../scripts/assert-request.js
//       env:
//         ROUTE: /v1.2/water-logs
//         METHOD: POST
//         FIELD: body.amount.value
//         VALUE: "16"               # JSON: 16, "cup", ["type_2_diabetes"], ...
// Maestro runs this on the host; override with -e FIXTURE_CONTROL=... when the
// fixture server listens elsewhere.
const control = typeof FIXTURE_CONTROL === 'string' && FIXTURE_CONTROL ? FIXTURE_CONTROL : 'http://127.0.0.1:18766';
const method = typeof METHOD === 'string' && METHOD ? METHOD : null;
const response = http.get(control + '/__requests');
if (!response.ok) {
  throw new Error('Fixture request log failed: HTTP ' + response.status);
}
const matching = JSON.parse(response.body).filter(function (request) {
  return request.path === ROUTE && (method === null || request.method === method);
});
if (matching.length === 0) {
  throw new Error('No ' + (method ? method + ' ' : '') + 'request to ' + ROUTE + ' was recorded');
}
let actual = matching[matching.length - 1];
FIELD.split('.').forEach(function (key) {
  actual = actual === null || actual === undefined ? undefined : actual[key];
});
const expected = JSON.stringify(JSON.parse(VALUE));
if (JSON.stringify(actual) !== expected) {
  throw new Error('The last request to ' + ROUTE + ' sent ' + FIELD + ' = ' + JSON.stringify(actual) + ', not ' + expected);
}

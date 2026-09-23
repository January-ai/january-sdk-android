// Seeds the fixture server with one saved food log ("Fixture breakfast") for
// USER (the end user a client token was minted for; default the shared user of
// the stub token).
// Maestro runs this on the host; override with -e FIXTURE_CONTROL=... when the
// fixture server listens elsewhere.
const control = typeof FIXTURE_CONTROL === 'string' && FIXTURE_CONTROL ? FIXTURE_CONTROL : 'http://127.0.0.1:18766';
const user = typeof USER === 'string' && USER ? USER : '';
const response = http.get(control + '/__seed?user=' + encodeURIComponent(user));
if (!response.ok) {
  throw new Error('Fixture seed failed: HTTP ' + response.status);
}

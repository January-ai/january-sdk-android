// Seeds the fixture server with about 13 months of water and weight history
// ending today, so the Tracking charts have data for every range.
// Maestro runs this on the host; override with -e FIXTURE_CONTROL=... when the
// fixture server listens elsewhere.
const control = typeof FIXTURE_CONTROL === 'string' && FIXTURE_CONTROL ? FIXTURE_CONTROL : 'http://127.0.0.1:18766';
const response = http.get(control + '/__seed_history');
if (!response.ok) {
  throw new Error('Fixture history seed failed: HTTP ' + response.status);
}

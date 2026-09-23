// Live flows only: checks what the demo shows against the January API for the
// same end user, through the same token relay the demo uses. Maestro runs this
// on the host. The client token stays in memory and is never printed.
//
//   - copyTextFrom:
//       id: water-total
//   - runScript:
//       file: ../scripts/live-verify.js
//       env:
//         CHECK: water           # see the switch at the end
//         UNIT: ml
//
// Settings (pass with -e on the maestro command line):
//   END_USER_ID      the end user the flows sign in as (default maestro-live-user)
//   RELAY_TOKEN_URL  the relay's token endpoint as the host reaches it
//                    (default http://127.0.0.1:8787/api/january/client-token)
//   RELAY_TOKEN      the relay token, for a relay that requires one
//   JANUARY_API      default https://partners.january.ai
//
// The day and timezone come from the demo itself (CHECK remember-day and
// remember-timezone copy them from the Tracking tab), so dates match what it shows.
const setting = (value, fallback) => (typeof value === 'string' && value ? value : fallback);
const relayUrl = setting(typeof RELAY_TOKEN_URL === 'undefined' ? undefined : RELAY_TOKEN_URL, 'http://127.0.0.1:8787/api/january/client-token');
const relayToken = setting(typeof RELAY_TOKEN === 'undefined' ? undefined : RELAY_TOKEN, '');
const api = setting(typeof JANUARY_API === 'undefined' ? undefined : JANUARY_API, 'https://partners.january.ai');
const user = setting(typeof END_USER_ID === 'undefined' ? undefined : END_USER_ID, 'maestro-live-user');
const check = CHECK;

function note(message) {
  console.log('LIVE-VERIFY [' + user + '] ' + message);
}

function fail(message) {
  note('FAIL ' + message);
  throw new Error(message);
}

// One token per flow run: minting counts toward the account's request allowance.
function bearer() {
  if (output.liveAuth && output.liveAuth.expires > Date.now()) return output.liveAuth.value;
  const headers = { 'January-End-User-ID': user };
  if (relayToken) headers.Authorization = 'Bearer ' + relayToken;
  const response = http.post(relayUrl, { headers: headers, body: '' });
  stopIfRateLimited(response, 'the token relay');
  if (!response.ok) fail('The token relay answered HTTP ' + response.status);
  const token = JSON.parse(response.body);
  output.liveAuth = { value: token.token, expires: Date.now() + Math.max(60, (token.expires_in || 600) - 60) * 1000 };
  return output.liveAuth.value;
}

// A 429 stops the run: the account's allowance is spent, and retrying into it only delays the reset.
// run-live.mjs looks for "RATE LIMITED" and runs no further flows.
function stopIfRateLimited(response, what) {
  if (response.status !== 429) return;
  let message = String(response.body);
  try { message = JSON.parse(response.body).code + ': ' + JSON.parse(response.body).message; } catch (ignored) { /* keep the raw body */ }
  fail('RATE LIMITED at ' + what + ' (' + message + ')');
}

function get(path) {
  const response = http.get(api + path, { headers: { Authorization: 'Bearer ' + bearer() } });
  note('GET ' + path + ' -> ' + response.status + ' ' + String(response.body).slice(0, 1500));
  stopIfRateLimited(response, 'GET ' + path);
  if (!response.ok) fail('GET ' + path + ' answered HTTP ' + response.status);
  return JSON.parse(response.body);
}

function query(params) {
  return Object.keys(params).map((key) => key + '=' + encodeURIComponent(params[key])).join('&');
}

function day() {
  if (!output.liveDay) fail('No day remembered; run CHECK remember-day first');
  return output.liveDay;
}

function timezone() {
  if (!output.liveTimezone) fail('No timezone remembered; run CHECK remember-timezone first');
  return output.liveTimezone;
}

// The demo's number format: whole numbers without decimals, otherwise at most two places.
function formatLogNumber(value) {
  if (value % 1 === 0) return String(value);
  return value.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
}

function formatChartNumber(value) {
  return formatLogNumber(Math.round(value * 10) / 10);
}

function parseShown(text) {
  const match = /^(-?[0-9.]+)\s+(.+)$/.exec(String(text).trim());
  return match ? { value: Number(match[1]), unit: match[2] } : null;
}

// Calendar arithmetic on YYYY-MM-DD strings, independent of the host's timezone.
function addDays(date, days) {
  const parts = date.split('-').map(Number);
  const moved = new Date(Date.UTC(parts[0], parts[1] - 1, parts[2] + days));
  return moved.toISOString().slice(0, 10);
}

function firstOfMonthMonthsAgo(date, months) {
  const parts = date.split('-').map(Number);
  return new Date(Date.UTC(parts[0], parts[1] - 1 - months, 1)).toISOString().slice(0, 10);
}

function span(range) {
  const today = day();
  if (range === 'week') return { start: addDays(today, -6), end: today };
  if (range === 'month') return { start: addDays(today, -29), end: today };
  return { start: firstOfMonthMonthsAgo(today, 11), end: today };
}

// Lists [start, end] in one request when it fits the endpoint's 100-day page, else in 90-day chunks.
function listDays(path, start, end, extra) {
  const params = Object.assign({ start_date: start, end_date: end, timezone: timezone() }, extra || {});
  const items = get(path + '?' + query(params)).items;
  if (items.length < 100) return items;
  const all = {};
  for (let chunkStart = start; chunkStart <= end; chunkStart = addDays(chunkStart, 90)) {
    const chunkEnd = addDays(chunkStart, 89) < end ? addDays(chunkStart, 89) : end;
    get(path + '?' + query(Object.assign({}, params, { start_date: chunkStart, end_date: chunkEnd }))).items.forEach((item) => { all[item.date] = item; });
  }
  return Object.keys(all).sort().map((date) => all[date]);
}

const unitLabels = { fl_oz: 'fl oz', ml: 'ml', cup: 'cup', lb: 'lb', kg: 'kg' };
const monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
const escape = (text) => text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

function checkWater() {
  const unit = UNIT;
  const shown = String(maestro.copiedText).trim();
  const item = listDays('/v1.2/water-logs', day(), day(), { unit: unit }).filter((entry) => entry.date === day())[0];
  const total = item ? item.total.value : 0;
  if (shown === 'Nothing logged') {
    if (total !== 0) fail('The app shows no water on ' + day() + ' but the API has ' + total + ' ' + unit);
  } else {
    const parsed = parseShown(shown);
    if (!parsed || parsed.unit !== unitLabels[unit]) fail('Unexpected water total text "' + shown + '"');
    if (!item || Math.abs(parsed.value - total) > 0.01) fail('The app shows ' + shown + ' on ' + day() + ' but the API has ' + total + ' ' + unit);
  }
  note('OK water on ' + day() + ': app "' + shown + '", API ' + total + ' ' + unit);
}

function checkWeight() {
  const shown = String(maestro.copiedText).trim();
  const item = listDays('/v1.2/weight-logs', day(), day()).filter((entry) => entry.date === day())[0];
  if (shown === 'Nothing logged') {
    if (item) fail('The app shows no weight on ' + day() + ' but the API has ' + JSON.stringify(item.weight));
  } else {
    // The card shows the weight in its selected unit: as logged, or converted to one decimal.
    const parsed = parseShown(shown);
    const perPound = 0.45359237;
    const inShownUnit = !item || !parsed ? NaN
      : item.weight.unit === parsed.unit ? item.weight.value
      : parsed.unit === 'kg' ? item.weight.value * perPound : item.weight.value / perPound;
    const tolerance = item && parsed && item.weight.unit === parsed.unit ? 0.001 : 0.051;
    if (!(Math.abs(parsed.value - inShownUnit) <= tolerance)) {
      fail('The app shows ' + shown + ' on ' + day() + ' but the API has ' + JSON.stringify(item && item.weight));
    }
  }
  note('OK weight on ' + day() + ': app "' + shown + '", API ' + JSON.stringify(item ? item.weight : null));
}

function checkFoodLog() {
  const present = typeof PRESENT === 'undefined' || PRESENT !== 'false';
  const food = typeof FOOD === 'undefined' ? '' : FOOD;
  const logs = get('/v1.2/food-logs?' + query({ start_date: day(), end_date: day(), timezone: timezone() })).items;
  const found = logs.filter((log) => log.name === NAME);
  if (present && found.length !== 1) fail('Expected one food log named "' + NAME + '" on ' + day() + ', found ' + found.length);
  if (!present && found.length !== 0) fail('Food log "' + NAME + '" is still on the server');
  if (present && food && !found[0].foods.some((entry) => String(entry.name).toLowerCase().indexOf(food.toLowerCase()) >= 0)) {
    fail('Food log "' + NAME + '" does not contain ' + food + ': ' + JSON.stringify(found[0].foods.map((entry) => entry.name)));
  }
  note('OK food log "' + NAME + '" ' + (present ? 'present' : 'absent') + ' on ' + day() + ' (' + logs.length + ' logs that day)');
}

function checkLogCount() {
  const shown = String(maestro.copiedText).trim();
  const summary = get('/v1.2/food-logs/summary?' + query({ start_date: day(), end_date: day(), timezone: timezone(), group_by: 'day' }));
  const count = summary.buckets && summary.buckets[0] ? summary.buckets[0].logs_count : 0;
  const expected = count + ' log' + (count === 1 ? '' : 's');
  if (shown !== expected) fail('The app shows "' + shown + '" but the API summary counts ' + count + ' on ' + day());
  note('OK day totals on ' + day() + ': app "' + shown + '", API logs_count ' + count);
}

function monthKey(date) {
  return date.slice(0, 7);
}

function checkWaterChart() {
  const range = RANGE;
  const unit = UNIT;
  const s = span(range);
  const totals = {};
  listDays('/v1.2/water-logs', s.start, s.end, { unit: unit }).forEach((item) => { totals[item.date] = item.total.value; });
  const bars = [];
  if (range === 'year') {
    for (let month = s.start; month <= s.end; month = firstOfMonthMonthsAgo(month, -1)) {
      const key = monthKey(month);
      bars.push({ date: month, value: Object.keys(totals).filter((date) => monthKey(date) === key).reduce((sum, date) => sum + totals[date], 0) });
    }
  } else {
    for (let date = s.start; date <= s.end; date = addDays(date, 1)) bars.push({ date: date, value: totals[date] || 0 });
  }
  const spoken = { week: 'last 7 days', month: 'last 30 days', year: 'last 12 months' }[range];
  const logged = bars.filter((bar) => bar.value > 0);
  let summary = 'Water, ' + spoken + ': ';
  if (logged.length === 0) {
    summary += 'nothing logged';
  } else {
    const most = logged.reduce((best, bar) => (bar.value > best.value ? bar : best), logged[0]);
    const parts = most.date.split('-').map(Number);
    const when = range === 'year' ? 'in ' + monthNames[parts[1] - 1] : 'on ' + monthNames[parts[1] - 1].slice(0, 3) + ' ' + parts[2];
    summary += logged.length + ' of ' + bars.length + ' ' + (range === 'year' ? 'months' : 'days') + ' logged, ' +
      formatChartNumber(bars.reduce((sum, bar) => sum + bar.value, 0)) + ' ' + unitLabels[unit] + ' in total, most ' +
      formatChartNumber(most.value) + ' ' + unitLabels[unit] + ' ' + when;
  }
  output.expectedChart = escape(summary);
  note('Expect water chart "' + summary + '" (' + s.start + ' to ' + s.end + ')');
}

function checkWeightChart() {
  const range = RANGE;
  const unit = UNIT;
  const s = span(range);
  const perPound = 0.45359237;
  const points = listDays('/v1.2/weight-logs', s.start, s.end).map((item) => {
    const weight = item.weight;
    if (weight.unit === unit) return weight.value;
    return unit === 'kg' ? weight.value * perPound : weight.value / perPound;
  });
  const spoken = { week: 'last 7 days', month: 'last 30 days', year: 'last 12 months' }[range];
  let summary = 'Weight, ' + spoken + ': ';
  if (points.length === 0) summary += 'no entries';
  else if (points.length === 1) summary += '1 entry, ' + formatChartNumber(points[0]) + ' ' + unit;
  else summary += points.length + ' entries, from ' + formatChartNumber(points[0]) + ' ' + unit + ' to ' + formatChartNumber(points[points.length - 1]) + ' ' + unit;
  output.expectedChart = escape(summary);
  note('Expect weight chart "' + summary + '" (' + s.start + ' to ' + s.end + ')');
}

switch (check) {
  case 'remember-day': {
    const shown = String(maestro.copiedText).trim();
    if (!/^\d{4}-\d{2}-\d{2}$/.test(shown)) fail('Not a day: "' + shown + '"');
    output.liveDay = shown;
    note('Day on screen ' + shown);
    break;
  }
  case 'remember-timezone':
    output.liveTimezone = String(maestro.copiedText).trim();
    note('Timezone on screen ' + output.liveTimezone);
    break;
  case 'run-id':
    if (!output.runId) output.runId = String(Date.now()).slice(-6);
    note('Run ' + output.runId);
    break;
  case 'water': checkWater(); break;
  case 'weight': checkWeight(); break;
  case 'food-log': checkFoodLog(); break;
  case 'log-count': checkLogCount(); break;
  case 'water-chart': checkWaterChart(); break;
  case 'weight-chart': checkWeightChart(); break;
  default: fail('Unknown CHECK ' + check);
}

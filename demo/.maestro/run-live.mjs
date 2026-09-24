#!/usr/bin/env node
// Runs the live flows one at a time against the January API through your token
// relay, pacing them and stopping cleanly when the API reports a rate limit.
//
//   node demo/.maestro/run-live.mjs --end-user my-test-user
//   node demo/.maestro/run-live.mjs --end-user my-test-user --only 93,94 --budget 100
//
// Options:
//   --end-user ID     end user the flows sign in as (required)
//   --only N,N        run only these flows, by number
//   --budget N        run flows in order while their estimated API requests fit N
//   --pause S         seconds between flows (default 30), so bursts stay well under a
//                     per-minute limit
//   --device ID       Maestro --device
//   --out DIR         Maestro debug output and the verification log (default
//                     demo/.maestro/artifacts/live)
//   -e KEY=VALUE      passed to every flow (RELAY_TOKEN_URL, RELAY_TOKEN, SHOTS, ...)
//   --dry-run         print the flows and their estimates without calling the API
//
// Before the first flow it checks, with one request, that the API answers for
// this user. A flow that hits HTTP 429 (or fails while the API is answering 429)
// ends the run: later flows are listed, not started. The weight flow, whose
// entries the API keeps, only runs when every flow before it passed.
import { spawnSync } from 'node:child_process';
import { appendFileSync, existsSync, mkdirSync, readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const maestroDir = path.dirname(fileURLToPath(import.meta.url));
const flowsDir = path.join(maestroDir, 'flows');

// Approximate API requests per flow (the demo's own requests plus the checks),
// counted from what each flow does; minting client tokens included.
const ESTIMATES = { 90: 13, 91: 7, 92: 10, 93: 38, 94: 55, 95: 28, 96: 18, 97: 11 };
const WEIGHT_FLOW = 96;

const args = process.argv.slice(2);
const option = (name, fallback) => {
  const at = args.indexOf(name);
  return at >= 0 ? args[at + 1] : fallback;
};
const env = [];
args.forEach((arg, index) => { if (arg === '-e') env.push(args[index + 1]); });
const endUser = option('--end-user');
if (!endUser) {
  console.error('usage: node demo/.maestro/run-live.mjs --end-user <test end user id> [--only 93,94] [--budget N] [-e KEY=VALUE]');
  process.exit(2);
}
const only = option('--only')?.split(',').map(Number);
const budget = Number(option('--budget', 'Infinity'));
const pause = Number(option('--pause', '30'));
const device = option('--device');
const out = path.resolve(option('--out', path.join(maestroDir, 'artifacts', 'live')));
mkdirSync(out, { recursive: true });
const setting = (key, fallback) => env.map((pair) => pair.split('=')).find(([k]) => k === key)?.slice(1).join('=') ?? fallback;
const relayUrl = setting('RELAY_TOKEN_URL', 'http://127.0.0.1:8787/api/january/client-token');
const relayToken = setting('RELAY_TOKEN', '');
const api = setting('JANUARY_API', 'https://partners.january.ai');
const verificationLog = path.join(out, 'live-verification.log');
const redact = (text) => String(text)
  .replace(/Bearer\s+[A-Za-z0-9._\-~+/=]+/g, 'Bearer [redacted]')
  .replace(/"token"\s*:\s*"[^"]*"/g, '"token":"[redacted]"')
  .replace(/\bct-[A-Za-z0-9._\-]+/g, 'ct-[redacted]');

const flows = readdirSync(flowsDir)
  .filter((name) => /^\d+-.*\.yaml$/.test(name) && /\n\s+- live\b/.test(readFileSync(path.join(flowsDir, name), 'utf8')))
  .sort()
  .map((name) => ({ name, number: Number(name.split('-')[0]) }))
  .filter((flow) => !only || only.includes(flow.number));

async function apiAnswers() {
  const headers = { 'January-End-User-ID': endUser, ...(relayToken ? { Authorization: `Bearer ${relayToken}` } : {}) };
  const relay = await fetch(relayUrl, { method: 'POST', headers });
  const relayBody = await relay.text();
  if (relay.status === 429) return { limited: true, detail: redact(relayBody) };
  if (!relay.ok) return { failed: true, detail: `token relay HTTP ${relay.status}: ${redact(relayBody)}` };
  const token = JSON.parse(relayBody).token;
  const today = new Date().toLocaleDateString('en-CA');
  const zone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  const probe = await fetch(`${api}/v1.2/food-logs/summary?start_date=${today}&end_date=${today}&timezone=${encodeURIComponent(zone)}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const body = await probe.text();
  // A successful answer is the end user's nutrition for the day: log only an error's body.
  appendFileSync(verificationLog, `${new Date().toISOString()} preflight GET /v1.2/food-logs/summary -> ${probe.status}${probe.ok ? '' : ` ${redact(body).slice(0, 500)}`}\n`);
  if (probe.status === 429) return { limited: true, detail: redact(body) };
  if (!probe.ok) return { failed: true, detail: `API HTTP ${probe.status}: ${redact(body)}` };
  return { ok: true };
}

// Maestro's debug output records scripts and their console lines; make sure no token survives in it.
function redactTree(dir) {
  if (!existsSync(dir)) return;
  for (const name of readdirSync(dir)) {
    const file = path.join(dir, name);
    if (statSync(file).isDirectory()) redactTree(file);
    else if (/\.(log|json|txt|xml)$/.test(name)) {
      const text = readFileSync(file, 'utf8');
      const clean = redact(text);
      if (clean !== text) writeFileSync(file, clean);
    }
  }
}

function maestroLog(dir) {
  const logs = [];
  const walk = (at) => {
    if (!existsSync(at)) return;
    for (const name of readdirSync(at)) {
      const file = path.join(at, name);
      if (statSync(file).isDirectory()) walk(file);
      else if (name === 'maestro.log') logs.push(readFileSync(file, 'utf8'));
    }
  };
  walk(dir);
  return logs.join('\n');
}

const sleep = (seconds) => new Promise((resolve) => setTimeout(resolve, seconds * 1000));

console.log(`Live flows for end user ${endUser}: ${flows.map((f) => f.number).join(', ')}`);
console.log(`Estimated API requests: ${flows.reduce((sum, f) => sum + (ESTIMATES[f.number] ?? 20), 0)}`);
if (args.includes('--dry-run')) {
  let planned = 1;
  for (const flow of flows) {
    const estimate = ESTIMATES[flow.number] ?? 20;
    const fits = planned + estimate <= budget;
    if (fits) planned += estimate;
    console.log(`  ${flow.name}: about ${estimate} requests${fits ? '' : ' (over the budget, not run)'}`);
  }
  process.exit(0);
}
const preflight = await apiAnswers();
if (preflight.limited) {
  console.log(`The API is rate limiting this account; no flow was run.\n${preflight.detail}`);
  process.exit(3);
}
if (preflight.failed) {
  console.log(`The API is not answering for this user; no flow was run.\n${preflight.detail}`);
  process.exit(2);
}

const results = [];
let spent = 1;
let allPassed = true;
for (const [index, flow] of flows.entries()) {
  const estimate = ESTIMATES[flow.number] ?? 20;
  if (spent + estimate > budget) {
    results.push({ flow, status: 'not run (budget)' });
    continue;
  }
  if (flow.number === WEIGHT_FLOW && !allPassed) {
    results.push({ flow, status: 'not run (an earlier flow failed; weight entries cannot be deleted)' });
    continue;
  }
  if (index > 0) await sleep(pause);
  const dir = path.join(out, flow.name.replace(/\.yaml$/, ''));
  const command = [
    'test', ...(device ? ['--device', device] : []), '--debug-output', dir, '--flatten-debug-output',
    '-e', `END_USER_ID=${endUser}`, '-e', `SHOTS=${out}`, ...env.flatMap((pair) => ['-e', pair]),
    path.join(flowsDir, flow.name),
  ];
  console.log(`\n▶ ${flow.name} (about ${estimate} requests)`);
  const run = spawnSync('maestro', command, { stdio: 'inherit' });
  spent += estimate;
  redactTree(dir);
  const log = maestroLog(dir);
  // Maestro logs each console line from a script as "JsConsole: <line>".
  const checks = log.split('\n').filter((line) => line.includes('JsConsole: LIVE-VERIFY')).map((line) => redact(line.slice(line.indexOf('LIVE-VERIFY'))));
  appendFileSync(verificationLog, `\n# ${flow.name}\n${checks.join('\n')}\n`);
  if (run.status === 0) {
    results.push({ flow, status: 'passed' });
    continue;
  }
  allPassed = false;
  const limited = checks.some((line) => line.includes('RATE LIMITED')) || (await apiAnswers()).limited;
  results.push({ flow, status: limited ? 'stopped: rate limited' : 'failed' });
  if (limited) {
    for (const rest of flows.slice(index + 1)) results.push({ flow: rest, status: 'not run (rate limited)' });
    break;
  }
}

writeFileSync(path.join(out, 'live-results.txt'), results.map((r) => `${r.flow.name}: ${r.status}`).join('\n') + '\n');
console.log('\nLive run:');
for (const result of results) console.log(`  ${result.flow.name}: ${result.status}`);
console.log(`Verification log: ${verificationLog}`);
process.exit(results.every((r) => r.status === 'passed') ? 0 : results.some((r) => r.status.includes('rate limited')) ? 3 : 1);

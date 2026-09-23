#!/usr/bin/env node
// Checks that the Maestro suite exercises every test tag the demo declares.
//
//   node scripts/ui-coverage.mjs            # fails unless coverage is 100%
//   node scripts/ui-coverage.mjs --list     # also prints every tag and where it is exercised
//
// Tags come from demo/src/main: every kebab-case string literal is a tag unless
// NOT_TAGS below says otherwise, and every interpolated tag must match one of the
// INTERPOLATED rules, which expand it into the concrete tags it produces (or, for
// list rows, a pattern such as food-result-<n> covered by any row).
//
// A tag counts as exercised when a flow the CI suite runs (tagged fixture or
// parity; live flows are excluded) taps it, asserts it visible, waits for it,
// scrolls to it, or copies its text, without `optional: true` and outside a
// conditional `runFlow: when:`. Subflows are followed with their env, so
// scroll-to.yaml with TARGET_ID counts as scrolling to that id. Negative checks
// (assertNotVisible, notVisible) do not count.
//
// The check also fails when any flow, live flows included, refers to an id the
// demo does not declare (an id written as a regex must match at least one tag).
import { readdirSync, readFileSync, statSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const sourceDir = path.join(root, 'demo/src/main/java');
const maestroDir = path.join(root, 'demo/.maestro');
const flowsDir = path.join(maestroDir, 'flows');
const SUITE_TAGS = new Set(['fixture', 'parity']);
const listAll = process.argv.includes('--list');

// Kebab-case literals in the demo that are data, not test tags.
const NOT_TAGS = new Map([
  ['january-sdk-demo-user', 'default end user ID'],
  ['san-francisco', 'preset city ID'],
  ['new-york', 'preset city ID'],
  ['los-angeles', 'preset city ID'],
  ['sans-serif-medium', 'font family name'],
  ['partner-user-123', 'sample user ID in a preview'],
  ['menu-${menu.size + index}', 'fallback menu item ID'],
]);

const TAG = /^[a-z][a-z0-9]*(?:-[a-z0-9]+)+$/;

// ---------------------------------------------------------------------------
// Demo source: string literals, with Kotlin string templates kept intact.

function kotlinFiles(dir) {
  return readdirSync(dir).flatMap((name) => {
    const file = path.join(dir, name);
    if (statSync(file).isDirectory()) return kotlinFiles(file);
    return name.endsWith('.kt') ? [file] : [];
  });
}

/** Every "..." literal in [text] with its line, skipping comments and char literals. */
function stringLiterals(text) {
  const literals = [];
  let line = 1;
  let i = 0;
  const readString = () => {
    // i is just past the opening quote.
    let value = '';
    while (i < text.length) {
      const c = text[i];
      if (c === '\\') { value += text.slice(i, i + 2); i += 2; continue; }
      if (c === '"') { i++; return value; }
      if (c === '\n') line++;
      if (c === '$' && text[i + 1] === '{') {
        // A template expression can hold its own strings; copy it whole.
        let depth = 0;
        const start = i;
        i++;
        while (i < text.length) {
          const d = text[i];
          if (d === '{') depth++;
          else if (d === '}') { depth--; if (depth === 0) { i++; break; } }
          else if (d === '"') { i++; readString(); continue; }
          else if (d === '\n') line++;
          i++;
        }
        value += text.slice(start, i);
        continue;
      }
      value += c;
      i++;
    }
    return value;
  };
  while (i < text.length) {
    const c = text[i];
    if (c === '\n') { line++; i++; continue; }
    if (c === '/' && text[i + 1] === '/') { while (i < text.length && text[i] !== '\n') i++; continue; }
    if (c === '/' && text[i + 1] === '*') {
      const end = text.indexOf('*/', i + 2);
      const stop = end < 0 ? text.length : end + 2;
      for (let j = i; j < stop; j++) if (text[j] === '\n') line++;
      i = stop;
      continue;
    }
    if (c === "'") { i += text[i + 1] === '\\' ? 4 : 3; continue; }
    if (c === '"') {
      const startLine = line;
      i++;
      literals.push({ value: readString(), line: startLine });
      continue;
    }
    i++;
  }
  return literals;
}

const sources = kotlinFiles(sourceDir).map((file) => ({
  file: path.relative(root, file),
  text: readFileSync(file, 'utf8'),
}));
const sourceText = (name) => {
  const found = sources.find((s) => s.file.endsWith(`/${name}`));
  if (!found) throw new Error(`demo source ${name} not found`);
  return found.text;
};

/** The string literals passed as `testTag = ...` to every ErrorCard call. */
function errorCardTags() {
  const tags = new Set();
  for (const { text } of sources) {
    for (let at = text.indexOf('ErrorCard('); at >= 0; at = text.indexOf('ErrorCard(', at + 1)) {
      if (/fun\s+$/.test(text.slice(Math.max(0, at - 5), at))) continue; // the declaration
      let depth = 0;
      let end = at + 'ErrorCard'.length;
      for (; end < text.length; end++) {
        if (text[end] === '(') depth++;
        else if (text[end] === ')') { depth--; if (depth === 0) break; }
      }
      const call = text.slice(at, end + 1);
      const argument = call.match(/[^A-Za-z]testTag\s*=\s*([^,]+?(?:else\s*"[^"]*")?)\s*(?:,|\)$)/);
      if (!argument) continue;
      for (const literal of argument[1].matchAll(/"([^"$]+)"/g)) tags.add(literal[1]);
    }
  }
  return [...tags].sort();
}

/** The testTagSuffix of every ChartRange entry. */
function chartRangeSuffixes() {
  const body = sourceText('TrackingChartData.kt').match(/enum class ChartRange[^{]*\{([\s\S]*?)\n\}/)[1];
  return [...body.matchAll(/^\s*[A-Z_]+\("[^"]*",\s*"[^"]*",\s*"([^"]+)"\)/gm)].map((m) => m[1]);
}

/** The literals passed as idPrefix to TrackingChartSection. */
function chartPrefixes() {
  return [...sourceText('TrackingScreen.kt').matchAll(/idPrefix\s*=\s*"([^"]+)"/g)].map((m) => m[1]);
}

/** The entries of the FoodMode enum, lowercased, as search-mode-* uses them. */
function foodModes() {
  const body = sourceText('SearchScreen.kt').match(/enum class FoodMode\s*\{([^}]*)\}/)[1];
  return body.split(',').map((entry) => entry.trim().toLowerCase()).filter(Boolean);
}

const errorTags = errorCardTags();
const rowPattern = (prefix) => ({ pattern: new RegExp(`^${prefix}-\\d+$`), representative: `${prefix}-<n>` });

// Every interpolated tag in the demo, by its exact source text.
const INTERPOLATED = {
  '$it-details': { expand: () => errorTags.map((tag) => `${tag}-details`) },
  '$it-details-body': { expand: () => errorTags.map((tag) => `${tag}-details-body`) },
  '$idPrefix-range-${it.testTagSuffix}': {
    expand: () => chartPrefixes().flatMap((prefix) => chartRangeSuffixes().map((suffix) => `${prefix}-range-${suffix}`)),
  },
  '$idPrefix-loading': { expand: () => chartPrefixes().map((prefix) => `${prefix}-loading`) },
  '$idPrefix-empty': { expand: () => chartPrefixes().map((prefix) => `${prefix}-empty`) },
  'search-mode-${it.name.lowercase()}': { expand: () => foodModes().map((mode) => `search-mode-${mode}`) },
  'alternative-result-$index': rowPattern('alternative-result'),
  'autocomplete-result-$it': rowPattern('autocomplete-result'),
  'food-log-$index': rowPattern('food-log'),
  'food-picker-result-$index': rowPattern('food-picker-result'),
  'food-picker-suggestion-$it': rowPattern('food-picker-suggestion'),
  'food-result-$index': rowPattern('food-result'),
  'food-serving-option-$index': rowPattern('food-serving-option'),
  'glucose-food-$index': rowPattern('glucose-food'),
  'menu-result-$index': rowPattern('menu-result'),
  'restaurant-menu-item-$index': rowPattern('restaurant-menu-item'),
  'restaurant-result-$index': rowPattern('restaurant-result'),
};

const problems = [];
const declared = new Map(); // tag or pattern label -> { where, pattern? }
const usedRules = new Set();
const declare = (label, where, pattern) => {
  if (!declared.has(label)) declared.set(label, { where, pattern });
};

for (const { file, text } of sources) {
  for (const { value, line } of stringLiterals(text)) {
    const where = `${file}:${line}`;
    if (NOT_TAGS.has(value)) continue;
    if (!value.includes('$')) {
      if (TAG.test(value)) declare(value, where);
      continue;
    }
    // An interpolated literal that looks like a tag: kebab-case once its templates are blanked.
    const blanked = value.replace(/\$\{[^}]*\}|\$[A-Za-z_]\w*/g, 'x');
    if (!/^[a-z0-9x-]+$/.test(blanked) || !blanked.includes('-')) continue;
    const rule = INTERPOLATED[value];
    if (!rule) {
      problems.push(`${where}: interpolated tag "${value}" has no rule in scripts/ui-coverage.mjs (INTERPOLATED or NOT_TAGS)`);
      continue;
    }
    usedRules.add(value);
    if (rule.expand) {
      const tags = rule.expand();
      if (tags.length === 0) problems.push(`${where}: the rule for "${value}" expands to no tags`);
      for (const tag of tags) declare(tag, `${where} (${value})`);
    } else {
      declare(rule.representative, where, rule.pattern);
    }
  }
}
for (const literal of Object.keys(INTERPOLATED)) {
  if (!usedRules.has(literal)) problems.push(`the rule for "${literal}" matches nothing in the demo; remove it`);
}

// ---------------------------------------------------------------------------
// Maestro flows: a YAML subset parser (block mappings, block sequences, plain
// and quoted scalars, comments, documents), enough for Maestro's syntax.

function parseYamlDocuments(text) {
  const docs = [[]];
  for (const raw of text.split('\n')) {
    if (/^---\s*$/.test(raw)) { docs.push([]); continue; }
    const stripped = stripComment(raw);
    if (stripped.trim() === '') continue;
    docs[docs.length - 1].push(stripped);
  }
  return docs.filter((lines) => lines.length).map((lines) => parseBlock(lines, 0, lines.length).value);
}

function stripComment(line) {
  let quote = null;
  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    if (quote) { if (c === quote) quote = null; continue; }
    if (c === '"' || c === "'") quote = c;
    else if (c === '#' && (i === 0 || /\s/.test(line[i - 1]))) return line.slice(0, i).replace(/\s+$/, '');
  }
  return line.replace(/\s+$/, '');
}

const indentOf = (line) => line.match(/^ */)[0].length;

function scalar(text) {
  const t = text.trim();
  if (t.startsWith('"') && t.endsWith('"')) return JSON.parse(t);
  if (t.startsWith("'") && t.endsWith("'")) return t.slice(1, -1).replace(/''/g, "'");
  // YAML reads ": " inside an unquoted value as a nested mapping, and Maestro then rejects the file.
  if (/: /.test(t)) throw new Error(`unquoted value contains ": " (quote it): ${t}`);
  if (t === 'true') return true;
  if (t === 'false') return false;
  if (/^-?\d+(\.\d+)?$/.test(t)) return Number(t);
  if (t === '' || t === '~' || t === 'null') return null;
  return t;
}

function splitKey(text) {
  const m = text.match(/^("[^"]*"|'[^']*'|[^:]+?):(?:\s+(.*)|$)/);
  return m ? { key: scalar(m[1]), rest: m[2] ?? '' } : null;
}

// Parses lines[start, end) that share the indentation of lines[start].
function parseBlock(lines, start, end) {
  const indent = indentOf(lines[start]);
  if (lines[start].slice(indent).startsWith('- ') || lines[start].trim() === '-') {
    const items = [];
    let i = start;
    while (i < end) {
      const itemEnd = nextSibling(lines, i, end, indent);
      const first = lines[i].slice(indent + 1);
      const inlineIndent = indent + 1 + first.match(/^ */)[0].length;
      const rest = [' '.repeat(inlineIndent) + first.trimStart(), ...lines.slice(i + 1, itemEnd)].filter((l) => l.trim());
      items.push(first.trim() === '' ? parseBlock(lines, i + 1, itemEnd).value : parseItem(rest));
      i = itemEnd;
    }
    return { value: items };
  }
  const map = {};
  let i = start;
  while (i < end) {
    const next = nextSibling(lines, i, end, indent);
    const { key, rest } = splitKey(lines[i].trim());
    map[key] = rest !== '' ? scalar(rest) : next > i + 1 ? parseBlock(lines, i + 1, next).value : null;
    i = next;
  }
  return { value: map };
}

function parseItem(lines) {
  const only = lines[0].trim();
  if (lines.length === 1 && !splitKey(only)) return scalar(only);
  return parseBlock(lines, 0, lines.length).value;
}

function nextSibling(lines, i, end, indent) {
  let j = i + 1;
  while (j < end && indentOf(lines[j]) > indent) j++;
  return j;
}

// ---------------------------------------------------------------------------
// Walk the suite.

const exercised = new Map(); // id -> Set of "flow: command"
const weak = new Map(); // id -> Set of "flow: command (optional)"
const referenced = new Map(); // id -> Set of flow names (any mention)

const substitute = (value, env) =>
  typeof value === 'string' ? value.replace(/\$\{([A-Z_][A-Z0-9_]*)\}/g, (all, name) => (name in env ? env[name] : all)) : value;

function note(map, id, label) {
  if (!map.has(id)) map.set(id, new Set());
  map.get(id).add(label);
}

function selectorId(selector, env) {
  if (selector && typeof selector === 'object' && typeof selector.id === 'string') return substitute(selector.id, env);
  return null;
}

function walk(commands, context) {
  if (!Array.isArray(commands)) return;
  for (const command of commands) {
    if (!command || typeof command !== 'object') continue;
    const [name] = Object.keys(command);
    const body = command[name];
    const optional = context.optional || (body && typeof body === 'object' && body.optional === true);
    const record = (id, positive) => {
      if (!id) return;
      note(referenced, id, context.flow);
      if (!positive || !context.counts) return;
      note(optional ? weak : exercised, id, `${context.flow}: ${name}${optional ? ' (optional)' : ''}`);
    };
    switch (name) {
      case 'tapOn':
      case 'doubleTapOn':
      case 'longPressOn':
      case 'assertVisible':
      case 'copyTextFrom':
        record(selectorId(body, context.env), true);
        break;
      case 'assertNotVisible':
        record(selectorId(body, context.env), false);
        break;
      case 'scrollUntilVisible':
        record(selectorId(body?.element, context.env), true);
        break;
      case 'extendedWaitUntil':
        record(selectorId(body?.visible, context.env), true);
        record(selectorId(body?.notVisible, context.env), false);
        break;
      case 'runFlow': {
        const spec = typeof body === 'string' ? { file: body } : body ?? {};
        const conditional = Boolean(spec.when);
        if (spec.when) {
          record(selectorId(spec.when.visible, context.env), false);
          record(selectorId(spec.when.notVisible, context.env), false);
        }
        const env = { ...context.env, ...Object.fromEntries(Object.entries(spec.env ?? {}).map(([k, v]) => [k, substitute(String(v), context.env)])) };
        const next = { ...context, env, optional: context.optional || conditional || spec.optional === true };
        if (spec.file) {
          const file = path.resolve(context.dir, spec.file);
          const [, subflow = []] = readFlow(file);
          walk(subflow, { ...next, dir: path.dirname(file) });
        }
        if (spec.commands) walk(spec.commands, next);
        break;
      }
      case 'retry':
      case 'repeat':
        walk(body?.commands, { ...context, optional: context.optional || body?.optional === true });
        break;
      default:
        break;
    }
  }
}

function readFlow(file) {
  let docs;
  try {
    docs = parseYamlDocuments(readFileSync(file, 'utf8'));
  } catch (error) {
    problems.push(`${path.relative(root, file)}: ${error.message}`);
    return [{ tags: [] }, []];
  }
  return docs.length === 1 ? [{}, docs[0]] : docs;
}

const flowFiles = readdirSync(flowsDir).filter((name) => name.endsWith('.yaml')).sort();
const suiteFlows = [];
const skippedFlows = [];
for (const name of flowFiles) {
  const file = path.join(flowsDir, name);
  const [header, commands] = readFlow(file);
  const tags = header.tags ?? [];
  // Flows outside the suite (live) do not count toward coverage, but their ids are still checked.
  const counts = tags.some((tag) => SUITE_TAGS.has(tag));
  if (counts) suiteFlows.push(name);
  else skippedFlows.push(`${name} (${tags.join(', ') || 'untagged'})`);
  walk(commands, { flow: name.replace(/\.yaml$/, ''), dir: flowsDir, env: {}, optional: false, counts });
}

// ---------------------------------------------------------------------------
// Report.

// An id with regex syntax (e.g. "water-(total|empty)") must match at least one declared tag.
const concreteTags = [...declared.entries()].map(([label, d]) => (d.pattern ? label.replace('<n>', '0') : label));
const matchesDeclared = (id) => {
  if (!/^[a-z0-9-]+$/.test(id)) {
    let regex;
    try { regex = new RegExp(`^(?:${id})$`); } catch { return false; }
    return concreteTags.some((tag) => regex.test(tag));
  }
  return declared.has(id) || [...declared.values()].some((d) => d.pattern?.test(id));
};
for (const [id, flows] of referenced) {
  if (!matchesDeclared(id)) problems.push(`flow id "${id}" is not declared in the demo (${[...flows].join(', ')})`);
}

const coverageOf = (label, { pattern }) => {
  const ids = pattern ? [...exercised.keys()].filter((id) => pattern.test(id)) : exercised.has(label) ? [label] : [];
  return ids.flatMap((id) => [...exercised.get(id)]);
};

const rows = [...declared.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([label, info]) => ({ label, info, by: coverageOf(label, info) }));
const uncovered = rows.filter((row) => row.by.length === 0);
const covered = rows.length - uncovered.length;
const percent = rows.length ? ((covered / rows.length) * 100).toFixed(1) : '100.0';

console.log(`Flows in the suite: ${suiteFlows.length}${skippedFlows.length ? `; ids checked but not counted: ${skippedFlows.join(', ')}` : ''}`);
if (listAll) {
  for (const row of rows) console.log(`${row.by.length ? 'ok  ' : 'MISS'} ${row.label}  ${row.by.length ? `← ${[...new Set(row.by.map((b) => b.split(':')[0]))].join(', ')}` : `(${row.info.where})`}`);
}
console.log(`UI coverage: ${covered}/${rows.length} (${percent}%)`);
if (uncovered.length) {
  console.log('Not exercised by any flow:');
  for (const row of uncovered) {
    const weakBy = row.info.pattern
      ? [...weak.keys()].filter((id) => row.info.pattern.test(id)).flatMap((id) => [...weak.get(id)])
      : [...(weak.get(row.label) ?? [])];
    console.log(`  - ${row.label}  (${row.info.where})${weakBy.length ? `  only optionally: ${weakBy.join('; ')}` : ''}`);
  }
}
if (problems.length) {
  console.log('Problems:');
  for (const problem of problems) console.log(`  - ${problem}`);
}
if (uncovered.length || problems.length) process.exit(1);

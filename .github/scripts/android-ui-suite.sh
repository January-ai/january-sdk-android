#!/usr/bin/env bash
# Runs one shard of the Maestro suite against the demo on the Android emulator
# that reactivecircus/android-emulator-runner booted. The action executes every
# line of its `script` input in a separate shell, so the suite lives here.
#
#   android-ui-suite.sh <apk> <shard index> <shard count>
set -euo pipefail

apk="$1"
shard="$2"
shards="$3"

adb install -r "$apk"

# The demo talks to the fixture server through the emulator's host alias
# (10.0.2.2, see demo/.maestro/bootstrap.yaml); Maestro's scripts reach its
# control endpoints on the host directly.
python3 qa/parity/fixture_server.py 18766 > "$RUNNER_TEMP/android-fixture.log" 2>&1 &
fixture_pid=$!
trap 'kill "$fixture_pid" 2>/dev/null || true' EXIT
for _ in $(seq 1 40); do
  curl --fail --silent http://127.0.0.1:18766/__reset >/dev/null && break
  sleep 0.5
done
curl --fail --silent http://127.0.0.1:18766/__reset >/dev/null

flows="$(node demo/.maestro/shard.mjs "$shard" "$shards")"
echo "Flows in shard $shard of $shards: $flows"
test -n "$flows"

# shellcheck disable=SC2086 # the shard is a space-separated list of files
.github/scripts/run-maestro-shard.sh emulator-5554 android-results $flows

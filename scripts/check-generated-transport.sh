#!/bin/bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
baseline="$(mktemp -d)"
trap 'rm -rf "$baseline"' EXIT
cp -R "$root/sdk/src/main/kotlin/ai/january/partner/transport/." "$baseline/"
"$root/scripts/generate-transport.sh"
diff -ru "$baseline" "$root/sdk/src/main/kotlin/ai/january/partner/transport"
echo "Generated Kotlin transport is current."


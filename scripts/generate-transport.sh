#!/bin/bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
lock="$root/Contract/sdk-contract.lock.json"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

value() { sed -n "s/.*\"$1\": \"\([^\"]*\)\".*/\1/p" "$lock"; }
version="$(value contractVersion)"
artifact="$(value artifact)"
archive_root="$(value archiveRoot)"
expected_archive_sha="$(value sha256)"
archive="${JANUARY_CONTRACT_ARCHIVE:-$root/../partner-api-contract/artifacts/releases/$version/$artifact}"

if [[ ! -f "$archive" ]]; then
  archive="$work/$artifact"
  gh api -H "Accept: application/vnd.github.raw+json" \
    "repos/January-ai/partner-api-contract/contents/artifacts/releases/$version/$artifact" > "$archive"
fi
[[ "$(shasum -a 256 "$archive" | awk '{print $1}')" == "$expected_archive_sha" ]] || {
  echo "Contract archive SHA-256 does not match sdk-contract.lock.json." >&2; exit 1;
}

tar -xzf "$archive" -C "$work"
openapi="$work/$archive_root/openapi/partner-api.generator.yaml"
node --input-type=module - "$openapi" <<'NODE'
import fs from 'node:fs';
const path = process.argv[2];
const source = fs.readFileSync(path, 'utf8');
const start = source.indexOf('    CompleteScanNutritionFacts:\n');
const remainder = source.slice(start + 1);
const nextSchema = remainder.search(/^    [A-Za-z0-9_]+:\n/m);
const end = nextSchema < 0 ? -1 : start + 1 + nextSchema;
const schema = source.slice(start, end);
const required = schema.indexOf('\n      required:\n');
const metadata = schema.indexOf('\n      x-january-upstream-schema:', required);
if (start < 0 || end < 0 || required < 0 || metadata < 0) throw new Error('Compatibility schema block was not found.');
fs.writeFileSync(path, source.slice(0, start) + schema.slice(0, required) + schema.slice(metadata) + source.slice(end));
NODE

jar="${JANUARY_OPENAPI_GENERATOR_JAR:-$root/../partner-api-contract/node_modules/.cache/january-generators/openapi-generator-cli-7.24.0.jar}"
if [[ ! -f "$jar" ]]; then
  jar="$work/openapi-generator-cli-7.24.0.jar"
  curl -fsSL "https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/7.24.0/openapi-generator-cli-7.24.0.jar" -o "$jar"
fi
[[ "$(shasum -a 256 "$jar" | awk '{print $1}')" == "4b83ccc6fd43056c8c631cd0195e5100bd0550912502527bab09ac76152dab0c" ]] || {
  echo "OpenAPI Generator SHA-256 is invalid." >&2; exit 1;
}

java -jar "$jar" generate -i "$openapi" -g kotlin -c "$root/Tools/ContractGenerator/config.yaml" -o "$work/generated" >/dev/null
node --input-type=module - "$work/generated" <<'NODE'
import fs from 'node:fs';
import path from 'node:path';
const root = process.argv[2];
const replaceOnce = (relative, before, after) => {
  const file = path.join(root, relative);
  const source = fs.readFileSync(file, 'utf8');
  if (source.split(before).length !== 2) throw new Error(`Expected one generated fragment in ${relative}`);
  fs.writeFileSync(file, source.replace(before, after));
};
replaceOnce('src/main/kotlin/ai/january/partner/transport/auth/HttpBearerAuth.kt', 'class HttpBearerAuth(', 'internal class HttpBearerAuth(');
replaceOnce('src/main/kotlin/ai/january/partner/transport/infrastructure/CollectionFormats.kt', 'class CollectionFormats {', 'internal class CollectionFormats {');
replaceOnce('src/main/kotlin/ai/january/partner/transport/infrastructure/ResponseExt.kt', 'inline fun <reified T>', 'internal inline fun <reified T>');

const client = 'src/main/kotlin/ai/january/partner/transport/infrastructure/ApiClient.kt';
replaceOnce(client, 'import okhttp3.logging.HttpLoggingInterceptor\n', '');
replaceOnce(client, '    var logger: ((String) -> Unit)? = null\n', '');
replaceOnce(client, `    private val defaultClientBuilder: OkHttpClient.Builder by lazy {
        OkHttpClient()
            .newBuilder()
            .addInterceptor(HttpLoggingInterceptor { message -> logger?.invoke(message) }
                .apply { level = HttpLoggingInterceptor.Level.BODY }
            )
    }
`, `    private val defaultClientBuilder: OkHttpClient.Builder by lazy {
        OkHttpClient().newBuilder()
    }
`);
replaceOnce(client, `    fun setLogger(logger: (String) -> Unit): ApiClient {
        this.logger = logger
        return this
    }

`, '');

const adapter = 'src/main/kotlin/ai/january/partner/transport/infrastructure/BigDecimalAdapter.kt';
replaceOnce(adapter, `import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.math.BigDecimal

internal class BigDecimalAdapter {
    @ToJson
    fun toJson(value: BigDecimal): String {
        return value.toPlainString()
    }

    @FromJson
    fun fromJson(value: String): BigDecimal {
        return BigDecimal(value)
    }
}`, `import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.math.BigDecimal

internal class BigDecimalAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, value: BigDecimal?) {
        if (value == null) writer.nullValue() else writer.value(value)
    }

    @FromJson
    fun fromJson(reader: JsonReader): BigDecimal? {
        if (reader.peek() == JsonReader.Token.NULL) return reader.nextNull()
        return BigDecimal(reader.nextString())
    }
}`);

const normalize = (directory) => {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const file = path.join(directory, entry.name);
    if (entry.isDirectory()) normalize(file);
    else {
      const source = fs.readFileSync(file, 'utf8');
      fs.writeFileSync(file, source.replace(/[ \t]+(?=\r?$)/gm, '').replace(/(?:\r?\n)+$/, '\n'));
    }
  }
};
normalize(root);
NODE
destination="$root/sdk/src/main/kotlin/ai/january/partner/transport"
rm -rf "$destination"
mkdir -p "$(dirname "$destination")"
cp -R "$work/generated/src/main/kotlin/ai/january/partner/transport" "$destination"
echo "Generated the internal Kotlin transport from contract release $version."

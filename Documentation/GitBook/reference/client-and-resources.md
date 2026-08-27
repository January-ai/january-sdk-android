# Client and resources

`JanuaryPartnerClient` is the public entry point. The generated transport is an
implementation detail.

| Resource | Methods |
| --- | --- |
| `foods` | `autocomplete`, `search`, `getFood`, `lookupBarcode`, `searchNaturalLanguage`, `suggestAlternatives` |
| `restaurants` | `search`, `searchMenuItems` |
| `photoScanning` | `scan`, `correct` |
| `foodLogs` | `create`, `list`, `update`, `delete` |
| `glucose` | `predict` |

`forUser` returns a lightweight `JanuaryPartnerUserClient` whose `foodLogs` and
`glucose` resources reuse one `PartnerUserContext`. Public clients always target
January production; development-origin overrides exist only in debug tooling.

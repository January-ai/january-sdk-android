# Backend token endpoint

The app gets client tokens from an endpoint on your backend, which mints each
one with your API key through `POST https://partners.january.ai/v1.2/auth/client-tokens`.
The endpoint's contract is the same for every SDK:
[Your token endpoint](https://docs.january.ai/docs/authentication#your-token-endpoint).

{% hint style="info" %}
No backend yet? Run the [token relay](https://docs.january.ai/docs/authentication#develop-with-the-token-relay)
and continue to [Authentication](authentication.md). Come back before launch.
{% endhint %}

## Response format

The Android SDK reads January's response unchanged, so return it as is:

```json
{ "token": "ct-…", "expires_in": 1800 }
```

`JanuaryClientToken.fromJson` reads `token` and `expires_in` (`expiresIn` is
also accepted) and ignores other fields. The SDK refreshes a token 60 seconds
before it expires and rejects one with 60 seconds or less left, so return a
freshly minted token rather than one cached on your server.

## Scopes

The app uses one token for every resource, so mint it with the scopes for every
feature the app uses ([scope table](https://docs.january.ai/rest-api/authentication#client-token-scopes)).
A call outside those scopes fails with `403 scope_insufficient`, which the SDK
reports as `ErrorCategory.AUTHORIZATION`.

Minting returns `403 forbidden` until **Enable client tokens** is switched on in
the [Developer Dashboard](https://dashboard.january.ai/dashboard/client-tokens).

Next: [Authentication](authentication.md)

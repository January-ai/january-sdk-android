package ai.january.partner.transport.apis

import ai.january.partner.transport.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import ai.january.partner.transport.models.ClientToken
import ai.january.partner.transport.models.ClientTokenRevocationResult
import ai.january.partner.transport.models.CreateClientTokenBody
import ai.january.partner.transport.models.ErrorResponse
import ai.january.partner.transport.models.RevokeClientTokensBody

internal interface AuthenticationApi {
    /**
     * POST v1.2/auth/client-tokens
     * Mint a client token
     * **API key only.**  Exchanges your partner API key for a short-lived token bound to one of your end users, so your mobile app can call the v1.2 API directly instead of through a proxy that holds your key.  **This endpoint requires your API key (&#x60;sk-…&#x60;), so always call it from your backend** — behind whatever login already protects your own APIs. Never ship your API key in a mobile app in order to call this from the device: that puts a credential for your whole account in every copy of your app, which is the problem client tokens exist to solve.  The end user is identified by &#x60;end_user_id&#x60; in the body, not the &#x60;January-End-User-ID&#x60; header: the token is bound to it, and requests made with the token act only on that user, whatever headers they carry.  Tokens last 300–7200 seconds and the raw value is returned exactly once — it is stored only as a hash, so it can never be retrieved again. Relay it to the device and let the device refresh when it expires: a &#x60;401&#x60; with code &#x60;token_expired&#x60; is the signal to mint a new one and retry the original request once.
     * Responses:
     *  - 201: Token minted. The raw `token` is returned exactly once — relay it to the device and store nothing; only its hash is kept. The response also echoes `expires_in`/`expires_at`, the bound `end_user_id`, and the exact `scopes` granted.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: `forbidden` — client tokens are not enabled for your account yet (turn them on in the [Developer Dashboard](https://dashboard.january.ai)), or the key is issued for the other API version.  `client_token_not_allowed` — this request was made with a client token, and minting requires your `sk-` API key.
     *  - 429: Minting is capped per partner (`code: rate_limited`) — the only limit that applies here. It is `@NotBillable` and outside the shared v1.2 request ceiling, so this 429 never means credit exhaustion or the daily ceiling. Honor `Retry-After`; mint one token per user session and reuse it until it expires rather than minting per request.
     *  - 503: Client tokens are not configured on this environment.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param createClientTokenBody
     * @return [ClientToken]
     */
    @POST("v1.2/auth/client-tokens")
    suspend fun createClientToken(@Body createClientTokenBody: CreateClientTokenBody): Response<ClientToken>

    /**
     * POST v1.2/auth/client-token-revocations
     * Revoke an end user’s client tokens
     * **API key only.**  Revokes every outstanding client token for one end user. Safe to call repeatedly: it reports how many tokens it actually stopped, so an immediate second call reports 0.  **This endpoint requires your API key (&#x60;sk-…&#x60;), so always call it from your backend.** A client token cannot revoke anything, its own included.  Revocation takes effect within 60 seconds, the authentication cache window. For an immediate cut-off, stop trusting the user in your own app as well — and note that a token minted in the same instant as the revoke may survive it, bounded by its own expiry.  If some tokens cannot be revoked, the call answers &#x60;503&#x60; with code &#x60;client_token_revocation_incomplete&#x60;, naming how many of them succeeded rather than reporting a smaller count as though it were the whole story — **retry until it succeeds**, which only picks up the remainder.  One call stops at most 500 tokens so it cannot time out, so **repeat the call until &#x60;revoked_count&#x60; is 0** if a user is somehow holding more than that (which means your app is minting per request rather than per session).
     * Responses:
     *  - 200: Revocation processed. `revoked_count` is how many live tokens this call actually stopped — repeat the call until it reports 0, since already-revoked and already-expired tokens are not counted.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: `forbidden` — the key is issued for the other API version. Unlike minting, revocation is **not** gated on client tokens being enabled for your account ([Developer Dashboard](https://dashboard.january.ai)): turning the feature off must never take away the ability to revoke what is already out there.  `client_token_not_allowed` — this request was made with a client token, and revocation requires your `sk-` API key.
     *  - 429: Revocation is capped per partner (`code: rate_limited`) — the only limit that applies here. It is `@NotBillable` and outside the shared v1.2 request ceiling, so this 429 never means credit exhaustion or the daily ceiling, and a partner mid-incident can still cut a device off. Honor `Retry-After` and retry; the revoke bucket is separate from minting, so a security sweep cannot spend your sign-in allowance.
     *  - 503: `service_unavailable` — client tokens are not configured on this environment. `client_token_revocation_incomplete` — some tokens could not be revoked; the message names how many succeeded, and repeating the request is safe and picks up the remainder.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @param revokeClientTokensBody
     * @return [ClientTokenRevocationResult]
     */
    @POST("v1.2/auth/client-token-revocations")
    suspend fun revokeClientTokens(@Body revokeClientTokensBody: RevokeClientTokensBody): Response<ClientTokenRevocationResult>

}

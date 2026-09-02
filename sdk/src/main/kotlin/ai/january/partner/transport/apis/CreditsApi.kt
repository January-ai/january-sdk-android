package ai.january.partner.transport.apis

import ai.january.partner.transport.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import ai.january.partner.transport.models.CreditBalance
import ai.january.partner.transport.models.ErrorResponse

internal interface CreditsApi {
    /**
     * GET v1.2/credits
     * Get your credit balance
     * **API key only.**  Your API credit allowance and consumption for the current calendar month (UTC). Each successful billable data operation consumes credits — how many depends on the operation and your plan — while requests that fail cost nothing and v1.1 calls are not counted. Checking your balance, creating client tokens, and revoking client tokens never consume credits. Reading your balance is also exempt from the request limits that bound the rest of the API, so it keeps answering once your allowance is spent or your request limit is reached — it carries only a cap of its own, 60 reads per minute unless we have agreed a different one with you. Read your balance when a request is rejected or on a schedule rather than before every call, and treat that balance — not a fixed per-call price — as the source of truth. When credits run out, v1.2 endpoints return &#x60;429&#x60; with code &#x60;credit_limit_exceeded&#x60; until the allowance resets — retrying does not help before then.
     * Responses:
     *  - 200: Your credit balance for the current billing period: the `plan`, the period bounds and reset instant, `used_credits`, and `included_credits`/`remaining_credits` — always present, and `null` for an uncapped partner whose plan has no ceiling.
     *  - 401: The request carried no `Authorization` header, or the credential in it was rejected.  `unauthorized` — the header is missing or malformed, or the key is not one we recognise. A valid key belonging to the other API version is `403 forbidden` instead.  A client token is rejected in one of three ways, and only the first should be handled automatically:  - `token_expired` — the token is past its TTL. Mint a fresh one from your backend and retry the request once. This is routine and expected once per TTL window. - `token_invalid` — no such token: it was never issued, or it has been purged, which happens shortly after it expires. This code is not an automatic token-refresh signal. - `token_revoked` — the token was revoked, by `POST /v1.2/auth/client-token-revocations` or from the dashboard. The end user signs in again in your app, and your backend decides whether to mint another; a device that just re-mints defeats the revocation.
     *  - 403: The credential is valid but is not allowed to make this request. `forbidden` is the general case — a key issued for the other API version, for example. A client token adds `client_token_not_allowed`, meaning the endpoint takes only an `sk-` API key — its description opens with **API key only.** (in this API: `POST /v1.2/auth/client-tokens`, `POST /v1.2/auth/client-token-revocations`, and `GET /v1.2/credits`).  On an endpoint that opens with **API key or client token.**, `scope_insufficient` means the token was minted without the scope named at the end of that endpoint’s description.
     *  - 429: Balance reads are capped at 60 per minute by default (`code: rate_limited`), and that is the only limit on your account that applies here — neither the monthly credit allowance nor the shared request limit does, so this 429 is never `credit_limit_exceeded`. Honor `Retry-After`; the window reopens one minute after your first read in it, so a retry shortly afterwards succeeds. A burst of traffic from a single IP can also be refused by the service-wide throttle, likewise with `Retry-After`.
     *  - 0: Any other error: the HTTP status plus { code, message }. Retry only rate_limited and the transient 5xx codes.
     *
     * @return [CreditBalance]
     */
    @GET("v1.2/credits")
    suspend fun getCredits(): Response<CreditBalance>

}

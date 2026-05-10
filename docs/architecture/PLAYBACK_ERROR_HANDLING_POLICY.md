# Playback Error Handling Policy

## Principle

Playback errors must be fixed at the credential, device, queue, or provider boundary. UI code must not hide, delay, or rewrite provider errors to make playback look healthy.

## Root Cause Rule

Playback failures must be resolved by identifying and fixing the concrete failing boundary. Do not bypass provider calls, route to a different platform, swap in preview URLs, suppress errors, add timers, or ask users to reconnect until credential refresh and provider account recovery have been verified.

This is the playback-specific application of the project-wide rule in [REAL_IMPLEMENTATION_POLICY.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/REAL_IMPLEMENTATION_POLICY.md). The same root-cause rule applies to PMS, EMS, GMS, auth, storage, SSL, AI, desktop, and provider integrations.

## Spotify Scope Rule

Spotify playback requires these OAuth scopes:

- `streaming`
- `user-read-playback-state`
- `user-modify-playback-state`

The backend playback credential endpoint must reject missing scopes as a reconnect requirement before the browser initializes playback. The frontend may clear stale errors after confirmed playback state, but it must not special-case `Invalid token scopes` with timers or suppression.

## TIDAL Playback Rule

TIDAL playback must resolve a real TIDAL track target and request a verified `FULL` playback stream before browser playback starts.

- PMS TIDAL tracks must resolve to a real TIDAL track id from `tidal:track:{id}`, a TIDAL track URL, or the imported `external_track_id`.
- Non-TIDAL PMS/EMS/GMS tracks may be played through TIDAL only by resolving their title, artist, ISRC, duration, and source identifiers through `POST /api/v1/platforms/playback/tidal/resolve-track`.
- Playback target search results are transient queue data. They must not be inserted into EMS/PMS canonical tables unless the user later performs an explicit save/import action.
- TIDAL stream playback uses `GET /api/v1/platforms/playback/tidal/tracks/{track_id}/stream`, which must request `assetpresentation=FULL`.
- TIDAL stream playback requires valid TIDAL credential scopes for playbackinfo access. The backend must reject missing/expired/unusable credentials before browser playback starts.
- Existing TIDAL credentials must recover the provider account id from the access token `uid` claim before asking users to reconnect. Reconnect prompts are allowed only after credential refresh/profile recovery genuinely fails.
- TIDAL token exchange and refresh must use the same client-auth boundary as the provider credential: confidential clients with `client_secret` use HTTP Basic; public clients send `client_id` in the form body.
- TIDAL playback must not route TIDAL-targeted playback to Spotify, preview URLs, mock URLs, or another provider.
- App volume must remain normalized to `0..1`.
- A response with `asset_presentation` other than `FULL` is not a successful full-track playback. The player must reset/stop and surface the provider state with track/product context.
- Subscription, location, preview, and DRM errors must be surfaced as provider errors with state/context details. Do not hide them with timers or temporary UI cleanup.
- When a new item starts, the frontend must reset the previous player state before showing the new pending player. During resolve and stream startup it must show a loading indicator or status message rather than leaving the UI silent.

## Regression Harness

Run this before changing playback behavior:

```bash
cd apps/web
npm run test:playback
```

The harness fails when code reintroduces message-hiding patterns, adds workaround markers to playback-critical code, removes required Spotify or TIDAL playback scope checks, sends TIDAL volume outside `0..1`, accepts preview playback as success, or configures public-domain SSL through mkcert/local certificate shortcuts.

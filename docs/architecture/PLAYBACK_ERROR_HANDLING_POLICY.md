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

## TIDAL SDK Rule

TIDAL playback uses the official browser SDK boundary. The frontend must initialize the SDK credentials provider, event producer, and player before loading a TIDAL media product, then use the SDK-reported playback context as the source of truth for playback state.

- PMS TIDAL tracks must resolve to a real TIDAL track id from `tidal:track:{id}`, a TIDAL track URL, or the imported `external_track_id`.
- TIDAL SDK playback requires `playback` and `entitlements.read`. The OAuth request may include broader product scopes such as `user.read`, `collection.read`, `playlists.read`, and `search.read`, but the playback credentials endpoint must reject tokens missing the required SDK playback scopes before browser playback starts.
- Existing TIDAL credentials must recover the provider account id from the access token `uid` claim before asking users to reconnect. Reconnect prompts are allowed only after credential refresh/profile recovery genuinely fails.
- TIDAL token exchange and refresh must use the same client-auth boundary as the provider credential: confidential clients with `client_secret` use HTTP Basic; public clients send `client_id` in the form body.
- TIDAL SDK playback must not route TIDAL tracks to Spotify, legacy stream URLs, preview URLs, mock URLs, or another provider.
- App volume must remain normalized to `0..1` before calling the TIDAL SDK.
- A SDK playback context with `actualAssetPresentation=PREVIEW` or a `previewReason` is not a successful full-track playback. The player must reset/stop and surface the provider state with track/product context.
- Subscription, location, preview, and DRM errors must be surfaced as provider errors with state/context details. Do not hide them with timers or temporary UI cleanup.
- Complex TIDAL playlist playback bugs must be reproduced in the isolated TIDAL SDK playlist page first, without legacy manifest diagnostics mixed into the playback path.

## Regression Harness

Run this before changing playback behavior:

```bash
cd apps/web
npm run test:playback
```

The harness fails when code reintroduces message-hiding patterns, adds workaround markers to playback-critical code, removes required Spotify or TIDAL playback scope checks, drops the TIDAL SDK/event-producer path, sends TIDAL volume outside `0..1`, mixes the isolated SDK test page with legacy stream diagnostics, or configures public-domain SSL through mkcert/local certificate shortcuts.

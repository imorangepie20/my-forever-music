# Playback Error Handling Policy

## Principle

Playback errors must be fixed at the credential, device, queue, or provider boundary. UI code must not hide, delay, or rewrite provider errors to make playback look healthy.

## Spotify Scope Rule

Spotify playback requires these OAuth scopes:

- `streaming`
- `user-read-playback-state`
- `user-modify-playback-state`

The backend playback credential endpoint must reject missing scopes as a reconnect requirement before the browser initializes playback. The frontend may clear stale errors after confirmed playback state, but it must not special-case `Invalid token scopes` with timers or suppression.

## Regression Harness

Run this before changing playback behavior:

```bash
cd apps/web
npm run test:playback
```

The harness fails when code reintroduces message-hiding patterns or removes required Spotify playback scope checks.

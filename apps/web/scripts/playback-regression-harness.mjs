import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { cwd, exit } from 'node:process'

const root = cwd()

const read = (path) => readFileSync(join(root, path), 'utf8')

const files = {
    playbackContext: read('src/contexts/PlaybackContext.tsx'),
    spotifySdk: read('src/lib/spotifyPlaybackSdk.ts'),
    applicationYml: read('../../services/api/src/main/resources/application.yml'),
    oauthProperties: read('../../services/api/src/main/java/io/myforevermusic/api/modules/platform/application/PlatformOAuthProperties.java'),
    playbackCredentialController: read('../../services/api/src/main/java/io/myforevermusic/api/modules/platform/presentation/PlatformPlaybackCredentialsController.java'),
}

const requiredSpotifyScopes = [
    'streaming',
    'user-read-playback-state',
    'user-modify-playback-state',
]

const checks = []

const check = (name, passed, detail) => {
    checks.push({ name, passed, detail })
}

check(
    'PlaybackContext does not delay or suppress Invalid token scopes',
    !/TRANSIENT_SPOTIFY_ERROR_DELAY_MS|invalid token scopes|setTimeout\(/i.test(files.playbackContext),
    'Do not hide scope/authentication errors with timers or special-case message suppression.',
)

check(
    'Spotify token cache invalidates old missing-scope credentials',
    /getMissingSpotifyPlaybackScopes/.test(files.spotifySdk) &&
        /tokenCache\.delete\(cacheKey\)/.test(files.spotifySdk) &&
        /assertSpotifyPlaybackScopes\(credentials\)/.test(files.spotifySdk),
    'Cached credentials missing playback scopes must be discarded and fresh backend credentials must be checked.',
)

for (const scope of requiredSpotifyScopes) {
    check(
        `application.yml includes ${scope}`,
        files.applicationYml.includes(scope),
        'Default Spotify OAuth scopes must request playback permissions.',
    )
    check(
        `PlatformOAuthProperties includes ${scope}`,
        files.oauthProperties.includes(scope),
        'Java property defaults must match playback OAuth requirements.',
    )
    check(
        `Playback credential API enforces ${scope}`,
        files.playbackCredentialController.includes(scope),
        'Backend playback credentials must fail before browser playback when scopes are missing.',
    )
}

check(
    'Playback credential API returns reconnect_required for missing scopes',
    /PlatformReconnectRequiredException/.test(files.playbackCredentialController) &&
        /missingPlaybackScopes/.test(files.playbackCredentialController),
    'Missing scopes should be treated as a reconnect requirement, not as a client-side cosmetic warning.',
)

const failed = checks.filter((result) => !result.passed)

for (const result of checks) {
    const prefix = result.passed ? 'PASS' : 'FAIL'
    console.log(`${prefix} ${result.name}`)
    if (!result.passed) {
        console.log(`     ${result.detail}`)
    }
}

if (failed.length > 0) {
    console.error(`\nPlayback regression harness failed: ${failed.length} check(s).`)
    exit(1)
}

console.log('\nPlayback regression harness passed.')

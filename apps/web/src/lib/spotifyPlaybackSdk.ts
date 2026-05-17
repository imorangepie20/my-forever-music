import { fetchPlaybackCredentials } from '@/services/api'
import type { PlatformPlaybackCredentialsResponse } from '@/types/api'

const SPOTIFY_SDK_URL = 'https://sdk.scdn.co/spotify-player.js'
const SPOTIFY_API_BASE_URL = 'https://api.spotify.com/v1'
const TOKEN_REFRESH_BUFFER_MS = 30_000
const DEVICE_READY_TIMEOUT_MS = 10_000
const REQUIRED_SPOTIFY_PLAYBACK_SCOPES = [
    'streaming',
    'user-read-playback-state',
    'user-modify-playback-state',
]

interface SpotifySdkError {
    message?: string
}

interface SpotifyPlayerReadyEvent {
    device_id: string
}

export interface SpotifyPlaybackTrack {
    id: string
    uri: string
    name: string
    artists: Array<{ name: string }>
    album: {
        name: string
        images: Array<{ url: string }>
    }
}

export interface SpotifyPlaybackState {
    paused: boolean
    position: number
    duration: number
    track_window: {
        current_track: SpotifyPlaybackTrack | null
    }
}

interface SpotifyWebPlaybackPlayer {
    addListener(eventName: 'ready', callback: (event: SpotifyPlayerReadyEvent) => void): boolean
    addListener(eventName: 'not_ready', callback: (event: SpotifyPlayerReadyEvent) => void): boolean
    addListener(eventName: 'player_state_changed', callback: (state: SpotifyPlaybackState | null) => void): boolean
    addListener(eventName: string, callback: (event: SpotifySdkError) => void): boolean
    connect(): Promise<boolean>
    disconnect(): void
    getCurrentState(): Promise<SpotifyPlaybackState | null>
    pause(): Promise<void>
    resume(): Promise<void>
    nextTrack(): Promise<void>
    previousTrack(): Promise<void>
    seek(positionMs: number): Promise<void>
    setVolume(volume: number): Promise<void>
    activateElement?: () => Promise<void>
}

interface SpotifyWebPlaybackSdk {
    Player: new (options: {
        name: string
        getOAuthToken: (callback: (token: string) => void) => void
        volume?: number
    }) => SpotifyWebPlaybackPlayer
}

declare global {
    interface Window {
        Spotify?: SpotifyWebPlaybackSdk
        onSpotifyWebPlaybackSDKReady?: () => void
    }
}

interface SpotifyPlayerCallbacks {
    onReady?: (deviceId: string) => void
    onStateChange?: (state: SpotifyPlaybackState | null) => void
    onError?: (message: string) => void
}

interface SpotifyPlayerSession {
    userId: string
    deviceId: string
    player: SpotifyWebPlaybackPlayer
}

interface CachedToken {
    accessToken: string
    expiresAtMs: number | null
    credentials: PlatformPlaybackCredentialsResponse
}

let sdkLoadPromise: Promise<void> | null = null
let playerSession: SpotifyPlayerSession | null = null
let playerSessionPromise: Promise<SpotifyPlayerSession> | null = null
let activeCallbacks: SpotifyPlayerCallbacks = {}
let playerSessionResetId = 0
const tokenCache = new Map<string, CachedToken>()

const delay = (ms: number) => new Promise((resolve) => window.setTimeout(resolve, ms))

const clampVolume = (volume: number) => Math.min(1, Math.max(0, Number.isFinite(volume) ? volume : 0.5))

const getMissingSpotifyPlaybackScopes = (credentials: PlatformPlaybackCredentialsResponse) =>
    REQUIRED_SPOTIFY_PLAYBACK_SCOPES.filter((scope) => !credentials.scopes.includes(scope))

const assertSpotifyPlaybackScopes = (credentials: PlatformPlaybackCredentialsResponse) => {
    const missingScopes = getMissingSpotifyPlaybackScopes(credentials)
    if (missingScopes.length > 0) {
        throw new Error(`Spotify token is missing playback scopes: ${missingScopes.join(', ')}. Reconnect Spotify to grant playback access.`)
    }
}

const loadSpotifySdk = () => {
    if (window.Spotify) {
        return Promise.resolve()
    }

    if (sdkLoadPromise) {
        return sdkLoadPromise
    }

    sdkLoadPromise = new Promise<void>((resolve, reject) => {
        const previousReady = window.onSpotifyWebPlaybackSDKReady
        window.onSpotifyWebPlaybackSDKReady = () => {
            previousReady?.()
            resolve()
        }

        const existingScript = document.querySelector<HTMLScriptElement>(`script[src="${SPOTIFY_SDK_URL}"]`)
        if (existingScript) {
            existingScript.addEventListener('error', () => reject(new Error('Spotify Web Playback SDK failed to load.')))
            return
        }

        const script = document.createElement('script')
        script.src = SPOTIFY_SDK_URL
        script.async = true
        script.onerror = () => reject(new Error('Spotify Web Playback SDK failed to load.'))
        document.body.appendChild(script)
    })

    return sdkLoadPromise
}

export const fetchSpotifyAccessToken = async (userId: string, forceRefresh = false) => {
    const cacheKey = `${userId}:spotify`
    const cached = tokenCache.get(cacheKey)
    if (!forceRefresh && cached) {
        if (!cached.expiresAtMs || cached.expiresAtMs - TOKEN_REFRESH_BUFFER_MS > Date.now()) {
            const missingCachedScopes = getMissingSpotifyPlaybackScopes(cached.credentials)
            if (missingCachedScopes.length === 0) {
                return cached
            }

            tokenCache.delete(cacheKey)
        }
    }

    const credentials = await fetchPlaybackCredentials(userId, 'spotify')
    if (!credentials.access_token) {
        throw new Error('Spotify playback credential did not include an access token.')
    }
    assertSpotifyPlaybackScopes(credentials)

    const expiresAtMs = credentials.expires_at ? Date.parse(credentials.expires_at) : null
    const nextCached = {
        accessToken: credentials.access_token,
        expiresAtMs: Number.isFinite(expiresAtMs) ? expiresAtMs : null,
        credentials,
    }
    tokenCache.set(cacheKey, nextCached)
    return nextCached
}

const createSpotifyPlayer = async (userId: string) => {
    await loadSpotifySdk()
    if (!window.Spotify) {
        throw new Error('Spotify Web Playback SDK is not available on window.')
    }

    const player = new window.Spotify.Player({
        name: 'My Forever Music Web Player',
        getOAuthToken: (callback) => {
            void fetchSpotifyAccessToken(userId)
                .then(({ accessToken }) => callback(accessToken))
                .catch((error: unknown) => {
                    const message = error instanceof Error ? error.message : 'Spotify token request failed.'
                    activeCallbacks.onError?.(message)
                })
        },
        volume: 0.5,
    })

    const deviceIdPromise = new Promise<string>((resolve, reject) => {
        const timeout = window.setTimeout(() => {
            reject(new Error('Spotify player device did not become ready in time.'))
        }, DEVICE_READY_TIMEOUT_MS)

        player.addListener('ready', ({ device_id }) => {
            window.clearTimeout(timeout)
            activeCallbacks.onReady?.(device_id)
            resolve(device_id)
        })
    })

    player.addListener('not_ready', ({ device_id }) => {
        activeCallbacks.onError?.(`Spotify player device went offline: ${device_id}`)
    })
    player.addListener('player_state_changed', (state) => activeCallbacks.onStateChange?.(state))
    player.addListener('initialization_error', ({ message }) => {
        activeCallbacks.onError?.(message ?? 'Spotify player initialization failed.')
    })
    player.addListener('authentication_error', ({ message }) => {
        tokenCache.delete(`${userId}:spotify`)
        activeCallbacks.onError?.(message ?? 'Spotify player authentication failed.')
    })
    player.addListener('account_error', ({ message }) => {
        activeCallbacks.onError?.(message ?? 'Spotify account cannot use Web Playback.')
    })
    player.addListener('playback_error', ({ message }) => {
        activeCallbacks.onError?.(message ?? 'Spotify playback failed.')
    })

    const connected = await player.connect()
    if (!connected) {
        throw new Error('Spotify player connection was rejected by the SDK.')
    }

    const deviceId = await deviceIdPromise
    return { userId, deviceId, player }
}

export const ensureSpotifyWebPlayer = async (userId: string, callbacks: SpotifyPlayerCallbacks = {}) => {
    activeCallbacks = callbacks

    if (playerSession?.userId === userId) {
        return playerSession
    }

    if (playerSession && playerSession.userId !== userId) {
        playerSession.player.disconnect()
        playerSession = null
    }

    if (playerSessionPromise) {
        return playerSessionPromise
    }

    const resetId = playerSessionResetId
    playerSessionPromise = createSpotifyPlayer(userId)
        .then((session) => {
            if (resetId !== playerSessionResetId) {
                session.player.disconnect()
                throw new Error('Spotify player session was reset.')
            }
            playerSession = session
            return session
        })
        .finally(() => {
            playerSessionPromise = null
        })

    return playerSessionPromise
}

export const resetSpotifyWebPlayer = (userId?: string) => {
    playerSessionResetId += 1
    activeCallbacks = {}
    if (userId) {
        tokenCache.delete(`${userId}:spotify`)
    } else {
        tokenCache.clear()
    }
    if (!playerSession || (userId && playerSession.userId !== userId)) {
        playerSessionPromise = null
        return
    }
    playerSession.player.disconnect()
    playerSession = null
    playerSessionPromise = null
}

const spotifyApiRequest = async (
    userId: string,
    path: string,
    init: RequestInit,
    retryOnUnauthorized = true,
) => {
    const { accessToken } = await fetchSpotifyAccessToken(userId)
    const headers = new Headers(init.headers)
    headers.set('Authorization', `Bearer ${accessToken}`)
    if (init.body && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json')
    }

    const response = await fetch(`${SPOTIFY_API_BASE_URL}${path}`, {
        ...init,
        headers,
    })

    const detail = response.ok ? '' : await response.text()
    if (response.status === 401 && detail.includes('Permissions missing')) {
        throw new Error('Spotify token is missing playback permissions. Reconnect Spotify to grant streaming and player control scopes.')
    }

    if (response.status === 401 && retryOnUnauthorized) {
        tokenCache.delete(`${userId}:spotify`)
        await fetchSpotifyAccessToken(userId, true)
        return spotifyApiRequest(userId, path, init, false)
    }

    if (!response.ok) {
        throw new Error(`Spotify Web API request failed (${response.status}): ${detail || response.statusText}`)
    }

    return response
}

const transferPlaybackToDevice = async (userId: string, deviceId: string) => {
    await spotifyApiRequest(userId, '/me/player', {
        method: 'PUT',
        body: JSON.stringify({
            device_ids: [deviceId],
            play: false,
        }),
    })
}

export const playSpotifyUris = async (userId: string, uris: string[], startIndex = 0) => {
    const playableUris = uris.filter((uri) => /^spotify:track:[A-Za-z0-9]{22}$/.test(uri))
    if (playableUris.length === 0) {
        throw new Error('No valid Spotify track URIs were provided for playback.')
    }

    const session = await ensureSpotifyWebPlayer(userId, activeCallbacks)
    await session.player.activateElement?.()
    await transferPlaybackToDevice(userId, session.deviceId)
    await delay(250)

    const safeStartIndex = Math.min(Math.max(0, startIndex), playableUris.length - 1)
    const body = JSON.stringify({
        uris: playableUris,
        offset: { position: safeStartIndex },
        position_ms: 0,
    })

    try {
        await spotifyApiRequest(userId, `/me/player/play?device_id=${encodeURIComponent(session.deviceId)}`, {
            method: 'PUT',
            body,
        })
    } catch (error) {
        const message = error instanceof Error ? error.message : String(error)
        if (!message.includes('(404)')) {
            throw error
        }

        await delay(900)
        await transferPlaybackToDevice(userId, session.deviceId)
        await spotifyApiRequest(userId, `/me/player/play?device_id=${encodeURIComponent(session.deviceId)}`, {
            method: 'PUT',
            body,
        })
    }
}

export const playSpotifyContext = async (userId: string, contextUri: string) => {
    if (!/^spotify:(playlist|album):[A-Za-z0-9]+$/.test(contextUri)) {
        throw new Error('Spotify context URI must be a playlist or album URI.')
    }

    const session = await ensureSpotifyWebPlayer(userId, activeCallbacks)
    await session.player.activateElement?.()
    await transferPlaybackToDevice(userId, session.deviceId)
    await delay(250)
    const body = JSON.stringify({
        context_uri: contextUri,
        position_ms: 0,
    })

    try {
        await spotifyApiRequest(userId, `/me/player/play?device_id=${encodeURIComponent(session.deviceId)}`, {
            method: 'PUT',
            body,
        })
    } catch (error) {
        const message = error instanceof Error ? error.message : String(error)
        if (!message.includes('(404)')) {
            throw error
        }

        await delay(900)
        await transferPlaybackToDevice(userId, session.deviceId)
        await spotifyApiRequest(userId, `/me/player/play?device_id=${encodeURIComponent(session.deviceId)}`, {
            method: 'PUT',
            body,
        })
    }
}

export const addSpotifyUriToQueue = async (userId: string, uri: string) => {
    if (!/^spotify:track:[A-Za-z0-9]{22}$/.test(uri)) {
        throw new Error('Spotify queue item must be a valid track URI.')
    }

    const session = await ensureSpotifyWebPlayer(userId, activeCallbacks)
    await spotifyApiRequest(
        userId,
        `/me/player/queue?uri=${encodeURIComponent(uri)}&device_id=${encodeURIComponent(session.deviceId)}`,
        { method: 'POST' },
    )
}

export const spotifyPause = async (userId: string) => {
    const session = await ensureSpotifyWebPlayer(userId, activeCallbacks)
    await session.player.pause()
}

export const spotifyResume = async (userId: string) => {
    const session = await ensureSpotifyWebPlayer(userId, activeCallbacks)
    await session.player.activateElement?.()
    await session.player.resume()
}

export const spotifyNextTrack = async (userId: string) => {
    const session = await ensureSpotifyWebPlayer(userId, activeCallbacks)
    await session.player.nextTrack()
}

export const spotifyPreviousTrack = async (userId: string) => {
    const session = await ensureSpotifyWebPlayer(userId, activeCallbacks)
    await session.player.previousTrack()
}

export const spotifySeek = async (userId: string, positionMs: number) => {
    const session = await ensureSpotifyWebPlayer(userId, activeCallbacks)
    await session.player.seek(Math.max(0, positionMs))
}

export const spotifySetVolume = async (userId: string, volume: number) => {
    const session = await ensureSpotifyWebPlayer(userId, activeCallbacks)
    await session.player.setVolume(clampVolume(volume))
}

export const spotifySetShuffle = async (userId: string, enabled: boolean) => {
    const session = await ensureSpotifyWebPlayer(userId, activeCallbacks)
    await spotifyApiRequest(userId, `/me/player/shuffle?state=${enabled ? 'true' : 'false'}&device_id=${encodeURIComponent(session.deviceId)}`, {
        method: 'PUT',
    })
}

export const spotifySetRepeat = async (userId: string, mode: 'off' | 'track' | 'context') => {
    const session = await ensureSpotifyWebPlayer(userId, activeCallbacks)
    await spotifyApiRequest(userId, `/me/player/repeat?state=${encodeURIComponent(mode)}&device_id=${encodeURIComponent(session.deviceId)}`, {
        method: 'PUT',
    })
}

export const getSpotifyCurrentState = async (userId: string) => {
    const session = await ensureSpotifyWebPlayer(userId, activeCallbacks)
    return session.player.getCurrentState()
}

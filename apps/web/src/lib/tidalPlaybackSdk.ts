import { messageTypes, type CredentialsProvider } from '@tidal-music/common'
import * as tidalEventProducer from '@tidal-music/event-producer'
import {
    bootstrap,
    events,
    getAssetPosition,
    getMediaElement,
    getPlaybackContext,
    getPlaybackState,
    load,
    pause,
    play,
    reset,
    seek,
    setAudioAdaptiveBitrateStreaming,
    setCredentialsProvider,
    setEventSender,
    setNext,
    setStreamingWifiAudioQuality,
    setVolumeLevel,
    type EndedEvent,
    type MediaProduct,
    type MediaProductTransition,
    type PlaybackContext as TidalSdkPlaybackContext,
    type PlaybackState,
    type PlaybackStateChange,
} from '@tidal-music/player'
import { resolveTidalTrackId, type PlaybackMediaItem } from '@/lib/musicPlayback'
import { fetchPlaybackCredentials } from '@/services/api'
import type { PlatformPlaybackCredentialsResponse } from '@/types/api'

const TOKEN_REFRESH_BUFFER_MS = 30_000
const TIDAL_DEVICE_ID = 'tidal-web-player'
const TIDAL_SESSION_TAGS = ['my-forever-music']
const TIDAL_EVENT_BATCH_URI = '/api/v1/platforms/playback/tidal/events/batch'
const TIDAL_PUBLIC_EVENT_BATCH_URI = '/api/v1/platforms/playback/tidal/events/public-batch'
const TIDAL_APP_VERSION = '0.0.0'
const REQUIRED_TIDAL_PLAYBACK_SCOPES = [
    'playback',
    'entitlements.read',
]

interface CachedToken {
    accessToken: string
    expiresAtMs: number | null
    credentials: PlatformPlaybackCredentialsResponse
}

export interface TidalPlaybackSnapshot {
    state: PlaybackState
    positionMs: number
    durationMs: number
    productId: string | null
    presentation?: string | null
    previewReason?: string | null
}

export interface TidalPlayerCallbacks {
    onReady?: (deviceId: string) => void
    onStateChange?: (state: PlaybackState, snapshot: TidalPlaybackSnapshot) => void
    onTransition?: (productId: string, snapshot: TidalPlaybackSnapshot) => void
    onEnded?: (productId: string | null) => void
    onError?: (message: string) => void
}

const tokenCache = new Map<string, CachedToken>()
const credentialBusCallbacks = new Set<Parameters<CredentialsProvider['bus']>[0]>()
let bootstrapped = false
let activeUserId: string | null = null
let activeCallbacks: TidalPlayerCallbacks = {}
let activeCredentialsProvider: CredentialsProvider | null = null
let eventsAttached = false
let eventProducerInitialized = false
let eventProducerInitPromise: Promise<void> | null = null

const clampVolume01 = (volume: number) => Math.min(1, Math.max(0, Number.isFinite(volume) ? volume : 0.5))

const toTimestampMs = (value?: string | null) => {
    if (!value) {
        return null
    }

    const timestamp = Date.parse(value)
    return Number.isFinite(timestamp) ? timestamp : null
}

const firstNonBlank = (...values: Array<string | null | undefined>) => {
    for (const value of values) {
        if (value?.trim()) {
            return value.trim()
        }
    }
    return undefined
}

const toTidalVolumeLevel = (volume: number) => clampVolume01(volume)

const getMissingTidalPlaybackScopes = (credentials: PlatformPlaybackCredentialsResponse) =>
    REQUIRED_TIDAL_PLAYBACK_SCOPES.filter((scope) => !credentials.scopes.includes(scope))

const assertTidalPlaybackScopes = (credentials: PlatformPlaybackCredentialsResponse) => {
    const missingScopes = getMissingTidalPlaybackScopes(credentials)
    if (missingScopes.length > 0) {
        throw new Error(`TIDAL token is missing playback scopes: ${missingScopes.join(', ')}. Reconnect TIDAL to grant full playback access.`)
    }
}

export const getTidalDeviceId = () => TIDAL_DEVICE_ID

export const isTidalPreviewSnapshot = (snapshot: TidalPlaybackSnapshot) =>
    snapshot.presentation === 'PREVIEW' || Boolean(snapshot.previewReason)

export const describeTidalPreviewFailure = (snapshot: TidalPlaybackSnapshot) => {
    const details = [
        snapshot.productId ? `product=${snapshot.productId}` : null,
        snapshot.presentation ? `presentation=${snapshot.presentation}` : null,
        snapshot.previewReason ? `preview=${snapshot.previewReason}` : null,
        snapshot.durationMs ? `duration=${Math.round(snapshot.durationMs / 1000)}s` : null,
    ].filter(Boolean)

    return `TIDAL returned preview playback instead of the full track.${details.length ? ` ${details.join(' ')}` : ''}`
}

export const fetchTidalAccessToken = async (userId: string, forceRefresh = false) => {
    const cacheKey = `${userId}:tidal`
    const cached = tokenCache.get(cacheKey)
    if (!forceRefresh && cached) {
        if (!cached.expiresAtMs || cached.expiresAtMs - TOKEN_REFRESH_BUFFER_MS > Date.now()) {
            const missingCachedScopes = getMissingTidalPlaybackScopes(cached.credentials)
            if (missingCachedScopes.length === 0) {
                return cached
            }
        }

        tokenCache.delete(cacheKey)
    }

    const credentials = await fetchPlaybackCredentials(userId, 'tidal')
    if (!credentials.access_token) {
        throw new Error('TIDAL playback credential did not include an access token.')
    }

    if (!credentials.client_id) {
        throw new Error('TIDAL playback credential did not include a client id. Set TIDAL_CLIENT_ID and restart the API.')
    }
    assertTidalPlaybackScopes(credentials)

    const nextCached = {
        accessToken: credentials.access_token,
        expiresAtMs: toTimestampMs(credentials.expires_at),
        credentials,
    }
    tokenCache.set(cacheKey, nextCached)
    notifyCredentialUpdate(nextCached.credentials)
    return nextCached
}

const notifyCredentialUpdate = (credentials: PlatformPlaybackCredentialsResponse) => {
    const event = new CustomEvent('tidal-credentials-updated', {
        detail: {
            type: messageTypes.credentialsUpdated,
            payload: toTidalCredentials(credentials),
        },
    })

    credentialBusCallbacks.forEach((callback) => callback(event))
}

const toTidalCredentials = (credentials: PlatformPlaybackCredentialsResponse) => ({
    clientId: credentials.client_id ?? '',
    clientUniqueKey: TIDAL_DEVICE_ID,
    token: credentials.access_token,
    requestedScopes: credentials.scopes,
    grantedScopes: credentials.scopes,
    expires: toTimestampMs(credentials.expires_at) ?? undefined,
    userId: firstNonBlank(credentials.external_user_id, credentials.user_id),
})

const createCredentialsProvider = (userId: string): CredentialsProvider => ({
    bus: (callback) => {
        credentialBusCallbacks.add(callback)
    },
    getCredentials: async (apiErrorSubStatus?: string) => {
        const { credentials } = await fetchTidalAccessToken(userId, Boolean(apiErrorSubStatus))
        return toTidalCredentials(credentials)
    },
})

const resolvePlatformData = () => ({
    browserName: 'browser',
    browserVersion: typeof navigator === 'undefined' ? 'unknown' : navigator.userAgent.slice(0, 120),
    osName: typeof navigator === 'undefined' ? 'unknown' : navigator.platform || 'unknown',
})

const registerTidalEventSender = () => {
    setEventSender(tidalEventProducer)
}

const ensureTidalEventProducer = async (credentialsProvider: CredentialsProvider) => {
    if (eventProducerInitialized) {
        tidalEventProducer.setCredentialsProvider(credentialsProvider)
        registerTidalEventSender()
        return
    }

    if (!eventProducerInitPromise) {
        eventProducerInitPromise = tidalEventProducer
            .init({
                appInfo: {
                    appName: 'my-forever-music',
                    appVersion: TIDAL_APP_VERSION,
                },
                blockedConsentCategories: {
                    NECESSARY: false,
                    PERFORMANCE: true,
                    TARGETING: true,
                },
                credentialsProvider,
                eventBatchInterval: 2_000,
                platform: resolvePlatformData(),
                strictMode: false,
                tlConsumerUri: TIDAL_EVENT_BATCH_URI,
                tlPublicConsumerUri: TIDAL_PUBLIC_EVENT_BATCH_URI,
            })
            .then(() => {
                eventProducerInitialized = true
                registerTidalEventSender()
            })
            .catch((error: unknown) => {
                eventProducerInitPromise = null
                throw new Error(errorMessage(error, 'TIDAL event producer could not initialize.'))
            })
    }

    await eventProducerInitPromise
}

const secondsToMs = (seconds?: number | null) => {
    if (!seconds || seconds <= 0 || !Number.isFinite(seconds)) {
        return 0
    }

    return Math.round(seconds * 1000)
}

const snapshotFromPlaybackContext = (
    playbackContext?: TidalSdkPlaybackContext | null,
    state: PlaybackState = getPlaybackState(),
): TidalPlaybackSnapshot => {
    const mediaElement = getMediaElement()
    const mediaDuration = mediaElement?.duration
    const mediaPosition = mediaElement?.currentTime
    const durationSeconds = playbackContext?.actualDuration ?? (Number.isFinite(mediaDuration) ? mediaDuration : 0)
    const positionSeconds =
        Number.isFinite(mediaPosition)
            ? mediaPosition
            : playbackContext?.assetPosition ?? getAssetPosition()

    return {
        state,
        positionMs: secondsToMs(positionSeconds),
        durationMs: secondsToMs(durationSeconds),
        productId: playbackContext?.actualProductId ?? null,
        presentation: playbackContext?.actualAssetPresentation ?? null,
        previewReason: playbackContext?.previewReason ?? null,
    }
}

export const getTidalCurrentSnapshot = (): TidalPlaybackSnapshot => {
    const playbackContext = getPlaybackContext()
    return snapshotFromPlaybackContext(playbackContext)
}

const describeTidalPlaybackContext = () => {
    const snapshot = getTidalCurrentSnapshot()
    const details = [
        `state=${snapshot.state}`,
        snapshot.productId ? `product=${snapshot.productId}` : null,
        snapshot.presentation ? `presentation=${snapshot.presentation}` : null,
        snapshot.previewReason ? `preview=${snapshot.previewReason}` : null,
        snapshot.durationMs ? `duration=${Math.round(snapshot.durationMs / 1000)}s` : null,
    ].filter(Boolean)

    return details.length ? ` ${details.join(' ')}` : ''
}

const errorMessage = (error: unknown, fallback: string) => {
    if (error instanceof Error && error.message) {
        return error.message
    }

    if (typeof error === 'string' && error) {
        return error
    }

    return fallback
}

const describeTidalSdkError = (event: Event) => {
    const detail = 'detail' in event ? (event as CustomEvent<Record<string, unknown>>).detail : null
    const detailText = detail
        ? Object.entries(detail)
            .filter(([, value]) => value !== null && value !== undefined && value !== '')
            .map(([key, value]) => `${key}=${String(value)}`)
            .join(' ')
        : ''

    return `TIDAL playback failed.${detailText ? ` ${detailText}` : ''}${describeTidalPlaybackContext()}`
}

const attachTidalEvents = () => {
    if (eventsAttached) {
        return
    }

    events.addEventListener('playback-state-change', (event: Event) => {
        const state = (event as PlaybackStateChange).detail.state
        activeCallbacks.onStateChange?.(state, getTidalCurrentSnapshot())
    })
    events.addEventListener('media-product-transition', (event: Event) => {
        const transition = event as MediaProductTransition
        const productId = transition.detail.mediaProduct.productId
        activeCallbacks.onTransition?.(productId, snapshotFromPlaybackContext(transition.detail.playbackContext))
    })
    events.addEventListener('ended', (event: Event) => {
        const ended = event as EndedEvent
        activeCallbacks.onEnded?.(ended.detail.mediaProduct?.productId ?? null)
    })
    events.addEventListener('error', (event: Event) => {
        activeCallbacks.onError?.(describeTidalSdkError(event))
    })

    eventsAttached = true
}

const ensureTidalBootstrapped = async (userId: string, callbacks: TidalPlayerCallbacks = {}) => {
    activeCallbacks = callbacks
    activeUserId = userId
    activeCredentialsProvider = createCredentialsProvider(userId)
    setCredentialsProvider(activeCredentialsProvider)
    await ensureTidalEventProducer(activeCredentialsProvider)

    if (!bootstrapped) {
        bootstrap({
            outputDevices: false,
            players: [
                {
                    player: 'shaka',
                    itemTypes: ['track'],
                    qualities: ['LOW', 'HIGH', 'LOSSLESS', 'HI_RES_LOSSLESS'],
                },
            ],
        })
        setAudioAdaptiveBitrateStreaming(true)
        setStreamingWifiAudioQuality('HIGH')
        bootstrapped = true
    }

    attachTidalEvents()
    activeCallbacks.onReady?.(TIDAL_DEVICE_ID)
}

export const ensureTidalWebPlayer = async (userId: string, callbacks: TidalPlayerCallbacks = {}) => {
    await ensureTidalBootstrapped(userId, callbacks)
    await fetchTidalAccessToken(userId, true)
}

const toTidalMediaProduct = (item: PlaybackMediaItem): MediaProduct => {
    const tidalTrackId = resolveTidalTrackId(item)
    if (!tidalTrackId) {
        throw new Error(`TIDAL track id is missing for "${item.title}".`)
    }

    return {
        productId: tidalTrackId,
        productType: 'track',
        sourceId: item.supportingText ?? item.id,
        sourceType: item.kind === 'playlist' ? 'playlist' : 'track',
        referenceId: item.id,
        extras: {
            title: item.title,
            artist: item.subtitle,
            platformUri: item.platformUri,
        },
    }
}

const loadTidalMediaProduct = async (mediaProduct: MediaProduct) => {
    let transitionSnapshot: TidalPlaybackSnapshot | null = null
    const captureTransition = (event: Event) => {
        const transition = event as MediaProductTransition
        if (transition.detail.mediaProduct.productId !== mediaProduct.productId) {
            return
        }
        transitionSnapshot = snapshotFromPlaybackContext(transition.detail.playbackContext, 'NOT_PLAYING')
    }

    events.addEventListener('media-product-transition', captureTransition)
    try {
        await load(mediaProduct, 0)
        return transitionSnapshot ?? getTidalCurrentSnapshot()
    } finally {
        events.removeEventListener('media-product-transition', captureTransition)
    }
}

export const playTidalMediaItem = async (
    userId: string,
    item: PlaybackMediaItem,
    nextItem?: PlaybackMediaItem | null,
    callbacks: TidalPlayerCallbacks = {},
) => {
    await ensureTidalWebPlayer(userId, callbacks)
    const mediaProduct = toTidalMediaProduct(item)
    const nextProduct = nextItem ? toTidalMediaProduct(nextItem) : undefined

    try {
        const snapshot = await loadTidalMediaProduct(mediaProduct)
        if (isTidalPreviewSnapshot(snapshot)) {
            await reset()
            throw new Error(describeTidalPreviewFailure(snapshot))
        }

        await setNext(nextProduct, TIDAL_SESSION_TAGS)
        await play()
    } catch (error: unknown) {
        throw new Error(`${errorMessage(error, 'TIDAL playback could not start.')}${describeTidalPlaybackContext()}`)
    }
}

export const tidalSetNextMediaItem = async (item?: PlaybackMediaItem | null) => {
    await setNext(item ? toTidalMediaProduct(item) : undefined, TIDAL_SESSION_TAGS)
}

export const tidalPause = async () => {
    pause()
}

export const tidalResume = async () => {
    await play()
}

export const tidalReset = async () => {
    await reset()
}

export const tidalSeek = async (positionMs: number) => {
    await seek(Math.max(0, positionMs) / 1000)
}

export const tidalSetVolume = async (volume: number) => {
    setVolumeLevel(toTidalVolumeLevel(volume))
}

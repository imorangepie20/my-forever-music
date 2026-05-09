import Hls from 'hls.js'
import { resolveTidalTrackId, type PlaybackMediaItem } from '@/lib/musicPlayback'
import { fetchTidalPlaybackStream } from '@/services/api'
import type { TidalPlaybackStreamResponse } from '@/types/api'

const TIDAL_STREAM_DEVICE_ID = 'tidal-stream-player'

export interface TidalPlaybackSnapshot {
    state: 'IDLE' | 'NOT_PLAYING' | 'PLAYING' | 'STALLED'
    positionMs: number
    durationMs: number
    productId: string | null
    presentation?: string | null
    previewReason?: string | null
}

export interface TidalPlayerCallbacks {
    onReady?: (deviceId: string) => void
    onStateChange?: (state: TidalPlaybackSnapshot['state'], snapshot: TidalPlaybackSnapshot) => void
    onTransition?: (productId: string, snapshot: TidalPlaybackSnapshot) => void
    onEnded?: (productId: string | null) => void
    onError?: (message: string) => void
}

let audioElement: HTMLAudioElement | null = null
let hls: Hls | null = null
let activeCallbacks: TidalPlayerCallbacks = {}
let currentProductId: string | null = null
let currentPresentation: string | null = null
let lastDurationMs = 0
let listenersAttached = false

const clampVolume01 = (volume: number) => Math.min(1, Math.max(0, Number.isFinite(volume) ? volume : 0.5))

const secondsToMs = (seconds: number) =>
    Number.isFinite(seconds) && seconds > 0 ? Math.round(seconds * 1000) : 0

const currentState = (): TidalPlaybackSnapshot['state'] => {
    if (!audioElement || !currentProductId) {
        return 'IDLE'
    }
    if (audioElement.readyState < HTMLMediaElement.HAVE_FUTURE_DATA && !audioElement.paused) {
        return 'STALLED'
    }
    return audioElement.paused ? 'NOT_PLAYING' : 'PLAYING'
}

export const getTidalDeviceId = () => TIDAL_STREAM_DEVICE_ID

export const getTidalCurrentSnapshot = (): TidalPlaybackSnapshot => {
    const durationMs = secondsToMs(audioElement?.duration ?? 0) || lastDurationMs
    return {
        state: currentState(),
        positionMs: secondsToMs(audioElement?.currentTime ?? 0),
        durationMs,
        productId: currentProductId,
        presentation: currentPresentation,
        previewReason: null,
    }
}

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

const emitState = (state = currentState()) => {
    activeCallbacks.onStateChange?.(state, getTidalCurrentSnapshot())
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

const ensureAudioElement = () => {
    if (audioElement) {
        return audioElement
    }

    audioElement = document.createElement('audio')
    audioElement.preload = 'auto'
    audioElement.volume = 0.5
    attachAudioListeners(audioElement)
    return audioElement
}

const attachAudioListeners = (audio: HTMLAudioElement) => {
    if (listenersAttached) {
        return
    }

    audio.addEventListener('play', () => emitState('PLAYING'))
    audio.addEventListener('pause', () => {
        if (audio.ended) {
            return
        }
        emitState('NOT_PLAYING')
    })
    audio.addEventListener('waiting', () => emitState('STALLED'))
    audio.addEventListener('canplay', () => emitState(currentState()))
    audio.addEventListener('timeupdate', () => emitState(currentState()))
    audio.addEventListener('durationchange', () => {
        lastDurationMs = secondsToMs(audio.duration)
        emitState(currentState())
    })
    audio.addEventListener('ended', () => {
        const endedProductId = currentProductId
        emitState('IDLE')
        activeCallbacks.onEnded?.(endedProductId)
    })
    audio.addEventListener('error', () => {
        const mediaError = audio.error
        const detail = mediaError
            ? ` code=${mediaError.code}${mediaError.message ? ` message=${mediaError.message}` : ''}`
            : ''
        activeCallbacks.onError?.(`TIDAL stream media element failed to play the resolved full-track URL.${detail}`)
    })

    listenersAttached = true
}

const resetSource = () => {
    if (hls) {
        hls.destroy()
        hls = null
    }

    if (audioElement) {
        audioElement.pause()
        audioElement.removeAttribute('src')
        audioElement.load()
    }
}

const isHlsStream = (stream: TidalPlaybackStreamResponse) => {
    const mimeType = stream.manifest_mime_type?.toLowerCase() ?? ''
    return mimeType.includes('mpegurl') || stream.stream_url.includes('.m3u8')
}

const isDashStream = (stream: TidalPlaybackStreamResponse) => {
    const mimeType = stream.manifest_mime_type?.toLowerCase() ?? ''
    return mimeType.includes('dash') || stream.stream_url.includes('.mpd')
}

const playHlsStream = async (audio: HTMLAudioElement, streamUrl: string) => {
    if (Hls.isSupported()) {
        hls = new Hls({
            enableWorker: true,
            lowLatencyMode: false,
            backBufferLength: 90,
        })

        await new Promise<void>((resolve, reject) => {
            hls?.once(Hls.Events.MANIFEST_PARSED, () => resolve())
            hls?.once(Hls.Events.ERROR, (_event, data) => {
                if (data.fatal) {
                    reject(new Error(`TIDAL HLS stream failed: ${data.type}/${data.details}`))
                }
            })
            hls?.loadSource(streamUrl)
            hls?.attachMedia(audio)
        })
        await audio.play()
        return
    }

    if (audio.canPlayType('application/vnd.apple.mpegurl')) {
        audio.src = streamUrl
        await audio.play()
        return
    }

    throw new Error('This browser cannot play the HLS stream returned by TIDAL.')
}

const playDirectStream = async (audio: HTMLAudioElement, streamUrl: string) => {
    audio.src = streamUrl
    await audio.play()
}

export const ensureTidalWebPlayer = async (_userId: string, callbacks: TidalPlayerCallbacks = {}) => {
    activeCallbacks = callbacks
    ensureAudioElement()
    activeCallbacks.onReady?.(TIDAL_STREAM_DEVICE_ID)
}

export const playTidalMediaItem = async (
    userId: string,
    item: PlaybackMediaItem,
    _nextItem?: PlaybackMediaItem | null,
    callbacks: TidalPlayerCallbacks = {},
) => {
    await ensureTidalWebPlayer(userId, callbacks)
    const tidalTrackId = resolveTidalTrackId(item)
    if (!tidalTrackId) {
        throw new Error(`TIDAL track id is missing for "${item.title}".`)
    }

    const audio = ensureAudioElement()
    resetSource()
    currentProductId = tidalTrackId
    currentPresentation = null
    lastDurationMs = item.durationMs ?? 0
    emitState('NOT_PLAYING')

    try {
        const stream = await fetchTidalPlaybackStream(userId, tidalTrackId)
        currentPresentation = stream.asset_presentation
        if (stream.asset_presentation !== 'FULL') {
            throw new Error(`TIDAL stream endpoint did not return FULL playback. presentation=${stream.asset_presentation ?? 'unknown'}`)
        }
        if (stream.duration_seconds && stream.duration_seconds > 0) {
            lastDurationMs = Math.round(stream.duration_seconds * 1000)
        }

        activeCallbacks.onTransition?.(tidalTrackId, getTidalCurrentSnapshot())

        if (isDashStream(stream)) {
            throw new Error(`TIDAL returned a DASH stream (${stream.manifest_mime_type ?? 'unknown'}), which is not supported by the direct browser player yet.`)
        }
        if (isHlsStream(stream)) {
            await playHlsStream(audio, stream.stream_url)
        } else {
            await playDirectStream(audio, stream.stream_url)
        }
    } catch (error: unknown) {
        resetSource()
        emitState('IDLE')
        throw new Error(errorMessage(error, 'TIDAL stream playback could not start.'))
    }
}

export const tidalSetNextMediaItem = async (_item?: PlaybackMediaItem | null) => {
}

export const tidalPause = async () => {
    audioElement?.pause()
}

export const tidalResume = async () => {
    if (!audioElement || !currentProductId) {
        throw new Error('No TIDAL stream is loaded.')
    }
    await audioElement.play()
}

export const tidalReset = async () => {
    resetSource()
    currentProductId = null
    currentPresentation = null
    lastDurationMs = 0
    emitState('IDLE')
}

export const tidalSeek = async (positionMs: number) => {
    if (!audioElement) {
        return
    }
    audioElement.currentTime = Math.max(0, positionMs) / 1000
}

export const tidalSetVolume = async (volume: number) => {
    ensureAudioElement().volume = clampVolume01(volume)
}

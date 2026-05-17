import {
    resolveSpotifyTrackId,
    resolveTidalTrackId,
    resolveYouTubeVideoId,
    type PlaybackMediaItem,
} from '@/lib/musicPlayback'
import { resolveYouTubePlaybackTarget } from '@/services/api'

type YouTubePlayerState = 'IDLE' | 'NOT_PLAYING' | 'PLAYING' | 'BUFFERING'

export interface YouTubePlaybackSnapshot {
    state: YouTubePlayerState
    positionMs: number
    durationMs: number
    videoId: string | null
}

export interface YouTubePlayerCallbacks {
    onReady?: (deviceId: string) => void
    onStateChange?: (state: YouTubePlayerState, snapshot: YouTubePlaybackSnapshot) => void
    onEnded?: (videoId: string | null) => void
    onError?: (message: string) => void
}

interface YouTubePlayerEvent {
    target: YouTubePlayer
    data: number
}

interface YouTubePlayer {
    loadVideoById: (videoId: string) => void
    playVideo: () => void
    pauseVideo: () => void
    stopVideo: () => void
    seekTo: (seconds: number, allowSeekAhead: boolean) => void
    setVolume: (volume: number) => void
    getCurrentTime: () => number
    getDuration: () => number
    getPlayerState: () => number
    destroy: () => void
}

interface YouTubeApi {
    Player: new (
        element: HTMLElement,
        options: {
            width: string
            height: string
            videoId?: string
            playerVars: Record<string, string | number>
            events: {
                onReady: (event: YouTubePlayerEvent) => void
                onStateChange: (event: YouTubePlayerEvent) => void
                onError: (event: YouTubePlayerEvent) => void
            }
        },
    ) => YouTubePlayer
    PlayerState: {
        ENDED: number
        PLAYING: number
        PAUSED: number
        BUFFERING: number
        CUED: number
    }
}

declare global {
    interface Window {
        YT?: YouTubeApi
        onYouTubeIframeAPIReady?: () => void
    }
}

const YOUTUBE_DEVICE_ID = 'youtube-iframe-player'
const PLAYER_HOST_WAIT_MS = 2_500
const PLAYER_READY_WAIT_MS = 7_000

let playerHost: HTMLElement | null = null
let player: YouTubePlayer | null = null
let apiReadyPromise: Promise<void> | null = null
let playerReadyPromise: Promise<YouTubePlayer> | null = null
let activeCallbacks: YouTubePlayerCallbacks = {}
let activeVideoId: string | null = null
let lastVolume = 50
let hostWaiters: Array<{
    resolve: () => void
    reject: (error: Error) => void
    timeoutId: number
}> = []

export const resolveYouTubePlayableItem = async (
    userId: string,
    item: PlaybackMediaItem,
    excludedVideoIds: string[] = [],
): Promise<PlaybackMediaItem> => {
    const youtubeVideoId = resolveYouTubeVideoId(item)
    if (youtubeVideoId) {
        return {
            ...item,
            playbackPlatformId: 'youtube',
            youtubeVideoId,
            externalUrl: item.externalUrl ?? `https://www.youtube.com/watch?v=${youtubeVideoId}`,
        }
    }

    const target = await resolveYouTubePlaybackTarget({
        user_id: userId,
        title: item.title,
        artist_name: item.subtitle.split(' · ')[0] || item.subtitle || item.title,
        source_platform: item.sourcePlatform,
        external_track_id: item.externalTrackId,
        platform_uri: item.platformUri,
        spotify_track_id: resolveSpotifyTrackId(item),
        tidal_track_id: resolveTidalTrackId(item),
        isrc: item.isrc,
        duration_ms: item.durationMs,
        excluded_video_ids: excludedVideoIds,
    })

    return {
        ...item,
        playbackPlatformId: 'youtube',
        youtubeVideoId: target.youtube_video_id,
        externalUrl: target.youtube_url,
        platformUri: target.youtube_url,
        imageUrl: target.thumbnail_url ?? item.imageUrl,
        durationMs: target.duration_ms ?? item.durationMs,
        supportingText: item.supportingText ?? `YouTube match: ${target.match_reason}`,
    }
}

export const setYouTubePlayerHost = (element: HTMLElement | null) => {
    if (playerHost === element) {
        return
    }

    if (player && element && playerHost !== element) {
        player.destroy()
        player = null
        playerReadyPromise = null
    }

    playerHost = element
    if (element) {
        resolveHostWaiters()
        return
    }
}

export const playYouTubeVideo = async (
    videoId: string,
    callbacks: YouTubePlayerCallbacks = {},
    volume = 0.5,
) => {
    activeVideoId = videoId
    activeCallbacks = callbacks
    lastVolume = volumeToPlayerVolume(volume)

    await waitForPlayerHost()
    const nextPlayer = await ensurePlayer()
    nextPlayer.setVolume(lastVolume)
    nextPlayer.loadVideoById(videoId)
    nextPlayer.playVideo()
}

export const youtubePause = async () => {
    player?.pauseVideo()
}

export const youtubeResume = async () => {
    player?.playVideo()
}

export const youtubeSeek = async (positionMs: number) => {
    player?.seekTo(Math.max(0, positionMs / 1000), true)
}

export const youtubeSetVolume = async (volume: number) => {
    lastVolume = volumeToPlayerVolume(volume)
    player?.setVolume(lastVolume)
}

export const youtubeStop = async () => {
    activeVideoId = null
    activeCallbacks = {}
    player?.stopVideo()
}

export const getYouTubeCurrentSnapshot = (): YouTubePlaybackSnapshot => ({
    state: playerStateToSnapshotState(player?.getPlayerState() ?? -1),
    positionMs: secondsToMs(player?.getCurrentTime() ?? 0),
    durationMs: secondsToMs(player?.getDuration() ?? 0),
    videoId: activeVideoId,
})

const ensurePlayer = async () => {
    if (player) {
        return player
    }
    if (playerReadyPromise) {
        return playerReadyPromise
    }
    if (!playerHost) {
        throw new Error('YouTube player surface is not mounted.')
    }

    playerReadyPromise = ensureYouTubeApi().then(() => new Promise<YouTubePlayer>((resolve, reject) => {
        const readyTimeoutId = window.setTimeout(() => {
            playerReadyPromise = null
            reject(new Error('YouTube iframe player did not become ready in time.'))
        }, PLAYER_READY_WAIT_MS)
        const nextPlayer = new window.YT!.Player(playerHost!, {
            width: '100%',
            height: '100%',
            videoId: activeVideoId ?? undefined,
            playerVars: {
                autoplay: 1,
                controls: 1,
                rel: 0,
                playsinline: 1,
                origin: window.location.origin,
            },
            events: {
                onReady: (event) => {
                    window.clearTimeout(readyTimeoutId)
                    player = event.target
                    event.target.setVolume(lastVolume)
                    activeCallbacks.onReady?.(YOUTUBE_DEVICE_ID)
                    resolve(event.target)
                },
                onStateChange: (event) => {
                    const snapshot = getYouTubeCurrentSnapshot()
                    const state = playerStateToSnapshotState(event.data)
                    activeCallbacks.onStateChange?.(state, { ...snapshot, state })
                    if (event.data === window.YT?.PlayerState.ENDED) {
                        activeCallbacks.onEnded?.(activeVideoId)
                    }
                },
                onError: (event) => {
                    activeCallbacks.onError?.(`YouTube iframe playback failed. code=${event.data}`)
                },
            },
        })
    }))
    return playerReadyPromise
}

const ensureYouTubeApi = () => {
    if (window.YT?.Player) {
        return Promise.resolve()
    }
    if (apiReadyPromise) {
        return apiReadyPromise
    }

    apiReadyPromise = new Promise<void>((resolve, reject) => {
        const readyTimeoutId = window.setTimeout(() => {
            apiReadyPromise = null
            reject(new Error('YouTube iframe API did not load in time.'))
        }, PLAYER_READY_WAIT_MS)
        const previousCallback = window.onYouTubeIframeAPIReady
        window.onYouTubeIframeAPIReady = () => {
            window.clearTimeout(readyTimeoutId)
            previousCallback?.()
            resolve()
        }

        if (!document.querySelector('script[src="https://www.youtube.com/iframe_api"]')) {
            const script = document.createElement('script')
            script.src = 'https://www.youtube.com/iframe_api'
            script.async = true
            script.onerror = () => {
                window.clearTimeout(readyTimeoutId)
                apiReadyPromise = null
                reject(new Error('YouTube iframe API script failed to load.'))
            }
            document.head.appendChild(script)
        }
    })
    return apiReadyPromise
}

const waitForPlayerHost = () => {
    if (playerHost) {
        return Promise.resolve()
    }

    return new Promise<void>((resolve, reject) => {
        const waiter = {
            resolve,
            reject,
            timeoutId: window.setTimeout(() => {
                hostWaiters = hostWaiters.filter((entry) => entry !== waiter)
                reject(new Error('YouTube player surface did not mount in time.'))
            }, PLAYER_HOST_WAIT_MS),
        }
        hostWaiters.push(waiter)
    })
}

const resolveHostWaiters = () => {
    const waiters = hostWaiters
    hostWaiters = []
    waiters.forEach((waiter) => {
        window.clearTimeout(waiter.timeoutId)
        waiter.resolve()
    })
}

const playerStateToSnapshotState = (state: number): YouTubePlayerState => {
    if (state === window.YT?.PlayerState.PLAYING) {
        return 'PLAYING'
    }
    if (state === window.YT?.PlayerState.BUFFERING) {
        return 'BUFFERING'
    }
    if (state === window.YT?.PlayerState.PAUSED || state === window.YT?.PlayerState.CUED) {
        return 'NOT_PLAYING'
    }
    return 'IDLE'
}

const volumeToPlayerVolume = (volume: number) =>
    Math.round(Math.min(1, Math.max(0, Number.isFinite(volume) ? volume : 0.5)) * 100)

const secondsToMs = (seconds: number) =>
    Number.isFinite(seconds) && seconds > 0 ? Math.round(seconds * 1000) : 0

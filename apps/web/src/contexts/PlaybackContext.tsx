import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import {
    resolvePlaybackPlatformId,
    resolveSpotifyContextUri,
    resolveSpotifyTrackId,
    type PlaybackMediaItem,
} from '@/lib/musicPlayback'
import {
    ensureSpotifyWebPlayer,
    getSpotifyCurrentState,
    playSpotifyContext,
    playSpotifyUris,
    spotifyNextTrack,
    spotifyPause,
    spotifyPreviousTrack,
    spotifyResume,
    spotifySeek,
    spotifySetVolume,
    type SpotifyPlaybackState,
} from '@/lib/spotifyPlaybackSdk'

interface PlaybackContextValue {
    currentItem: PlaybackMediaItem | null
    queue: PlaybackMediaItem[]
    currentIndex: number
    isPlaying: boolean
    isLoading: boolean
    error: string | null
    positionMs: number
    durationMs: number
    volume: number
    deviceId: string | null
    playItem: (item: PlaybackMediaItem) => Promise<void>
    playQueue: (items: PlaybackMediaItem[], startIndex?: number) => Promise<void>
    pause: () => Promise<void>
    resume: () => Promise<void>
    skipNext: () => Promise<void>
    skipPrevious: () => Promise<void>
    seek: (positionMs: number) => Promise<void>
    setVolume: (volume: number) => Promise<void>
    clearItem: () => void
}

const PlaybackContext = createContext<PlaybackContextValue | null>(null)

const clampIndex = (index: number, length: number) => Math.min(Math.max(0, index), Math.max(0, length - 1))
const toSpotifyUri = (item: PlaybackMediaItem) => {
    const spotifyTrackId = resolveSpotifyTrackId(item)
    return spotifyTrackId ? `spotify:track:${spotifyTrackId}` : null
}

export const PlaybackProvider = ({ children }: { children: ReactNode }) => {
    const { session } = useAuthSession()
    const [currentItem, setCurrentItem] = useState<PlaybackMediaItem | null>(null)
    const [queue, setQueue] = useState<PlaybackMediaItem[]>([])
    const [currentIndex, setCurrentIndex] = useState(0)
    const [isPlaying, setIsPlaying] = useState(false)
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [positionMs, setPositionMs] = useState(0)
    const [durationMs, setDurationMs] = useState(0)
    const [volumeState, setVolumeState] = useState(0.5)
    const [deviceId, setDeviceId] = useState<string | null>(null)
    const queueRef = useRef(queue)
    const currentIndexRef = useRef(currentIndex)

    useEffect(() => {
        queueRef.current = queue
    }, [queue])

    useEffect(() => {
        currentIndexRef.current = currentIndex
    }, [currentIndex])

    const clearPlaybackError = useCallback(() => {
        setError(null)
    }, [])

    const handleSpotifyStateChange = useCallback((state: SpotifyPlaybackState | null) => {
        if (!state) {
            return
        }

        setIsPlaying(!state.paused)
        setPositionMs(state.position ?? 0)
        setDurationMs(state.duration ?? 0)

        const spotifyTrackId = state.track_window.current_track?.id
        if (!state.paused && spotifyTrackId) {
            clearPlaybackError()
        }

        if (!spotifyTrackId) {
            return
        }

        const nextIndex = queueRef.current.findIndex((item) => resolveSpotifyTrackId(item) === spotifyTrackId)
        if (nextIndex >= 0) {
            setCurrentIndex(nextIndex)
            setCurrentItem(queueRef.current[nextIndex])
        }
    }, [clearPlaybackError])

    const spotifyCallbacks = useMemo(
        () => ({
            onReady: (nextDeviceId: string) => setDeviceId(nextDeviceId),
            onStateChange: handleSpotifyStateChange,
            onError: (message: string) => setError(message),
        }),
        [handleSpotifyStateChange],
    )

    const requireUserId = useCallback(() => {
        if (!session?.userId) {
            throw new Error('Sign in before starting playback.')
        }
        return session.userId
    }, [session?.userId])

    const playQueue = useCallback(
        async (items: PlaybackMediaItem[], startIndex = 0) => {
            if (items.length === 0) {
                setError('No tracks were provided for playback.')
                return
            }

            const userId = requireUserId()
            const safeStartIndex = clampIndex(startIndex, items.length)
            const selectedItem = items[safeStartIndex]
            const playbackPlatformId = resolvePlaybackPlatformId(selectedItem, session?.preferredPlatformId)

            setIsLoading(true)
            clearPlaybackError()

            try {
                if (playbackPlatformId !== 'spotify') {
                    throw new Error(`Playback harness is configured for Spotify first. Current target is ${playbackPlatformId ?? 'unknown'}.`)
                }

                await ensureSpotifyWebPlayer(userId, spotifyCallbacks)
                await spotifySetVolume(userId, volumeState)

                const spotifyContextUri = items.length === 1 ? resolveSpotifyContextUri(selectedItem) : null
                if (spotifyContextUri) {
                    setQueue([selectedItem])
                    setCurrentIndex(0)
                    setCurrentItem(selectedItem)
                    setPositionMs(0)
                    setDurationMs(selectedItem.durationMs ?? 0)
                    await playSpotifyContext(userId, spotifyContextUri)
                    setIsPlaying(true)
                    clearPlaybackError()
                    return
                }

                const spotifyEntries = items
                    .map((item, originalIndex) => ({
                        item,
                        originalIndex,
                        uri: toSpotifyUri(item),
                    }))
                    .filter((entry): entry is { item: PlaybackMediaItem; originalIndex: number; uri: string } => Boolean(entry.uri))

                const selectedEntry = spotifyEntries.find((entry) => entry.originalIndex === safeStartIndex)
                if (!selectedEntry) {
                    throw new Error('Selected track does not have a valid Spotify track id or URI.')
                }

                const nextQueue = spotifyEntries.map((entry) => entry.item)
                const nextIndex = spotifyEntries.findIndex((entry) => entry.originalIndex === safeStartIndex)

                setQueue(nextQueue)
                setCurrentIndex(nextIndex)
                setCurrentItem(nextQueue[nextIndex])
                setPositionMs(0)
                setDurationMs(nextQueue[nextIndex]?.durationMs ?? 0)
                await playSpotifyUris(userId, spotifyEntries.map((entry) => entry.uri), nextIndex)
                setIsPlaying(true)
                clearPlaybackError()
            } catch (playbackError: unknown) {
                const message = playbackError instanceof Error ? playbackError.message : 'Playback failed.'
                setError(message)
                setIsPlaying(false)
            } finally {
                setIsLoading(false)
            }
        },
        [clearPlaybackError, requireUserId, session?.preferredPlatformId, spotifyCallbacks, volumeState],
    )

    const playItem = useCallback((item: PlaybackMediaItem) => playQueue([item], 0), [playQueue])

    const pause = useCallback(async () => {
        if (!currentItem) {
            return
        }

        const userId = requireUserId()
        await spotifyPause(userId)
        setIsPlaying(false)
    }, [currentItem, requireUserId])

    const resume = useCallback(async () => {
        if (!currentItem) {
            return
        }

        const userId = requireUserId()
        clearPlaybackError()
        await ensureSpotifyWebPlayer(userId, spotifyCallbacks)
        await spotifyResume(userId)
        setIsPlaying(true)
    }, [clearPlaybackError, currentItem, requireUserId, spotifyCallbacks])

    const skipNext = useCallback(async () => {
        if (!currentItem) {
            return
        }

        const userId = requireUserId()
        await spotifyNextTrack(userId)
        const nextIndex = clampIndex(currentIndexRef.current + 1, queueRef.current.length)
        setCurrentIndex(nextIndex)
        setCurrentItem(queueRef.current[nextIndex] ?? currentItem)
    }, [currentItem, requireUserId])

    const skipPrevious = useCallback(async () => {
        if (!currentItem) {
            return
        }

        const userId = requireUserId()
        await spotifyPreviousTrack(userId)
        const nextIndex = clampIndex(currentIndexRef.current - 1, queueRef.current.length)
        setCurrentIndex(nextIndex)
        setCurrentItem(queueRef.current[nextIndex] ?? currentItem)
    }, [currentItem, requireUserId])

    const seek = useCallback(
        async (nextPositionMs: number) => {
            if (!currentItem) {
                return
            }

            const userId = requireUserId()
            const safePosition = Math.min(Math.max(0, nextPositionMs), Math.max(durationMs, 0))
            setPositionMs(safePosition)
            await spotifySeek(userId, safePosition)
        },
        [currentItem, durationMs, requireUserId],
    )

    const setVolume = useCallback(
        async (nextVolume: number) => {
            const safeVolume = Math.min(1, Math.max(0, Number.isFinite(nextVolume) ? nextVolume : 0.5))
            setVolumeState(safeVolume)
            if (!session?.userId) {
                return
            }

            await spotifySetVolume(session.userId, safeVolume)
        },
        [session?.userId],
    )

    const clearItem = useCallback(() => {
        if (session?.userId && currentItem) {
            void spotifyPause(session.userId).catch(() => undefined)
        }
        setCurrentItem(null)
        setQueue([])
        setCurrentIndex(0)
        setIsPlaying(false)
        setIsLoading(false)
        clearPlaybackError()
        setPositionMs(0)
        setDurationMs(0)
    }, [clearPlaybackError, currentItem, session?.userId])

    useEffect(() => {
        if (!session?.userId || !currentItem) {
            return
        }

        const intervalId = window.setInterval(() => {
            void getSpotifyCurrentState(session.userId)
                .then(handleSpotifyStateChange)
                .catch(() => undefined)
        }, 2_500)

        return () => window.clearInterval(intervalId)
    }, [currentItem, handleSpotifyStateChange, session?.userId])

    const value = useMemo<PlaybackContextValue>(
        () => ({
            currentItem,
            queue,
            currentIndex,
            isPlaying,
            isLoading,
            error,
            positionMs,
            durationMs,
            volume: volumeState,
            deviceId,
            playItem,
            playQueue,
            pause,
            resume,
            skipNext,
            skipPrevious,
            seek,
            setVolume,
            clearItem,
        }),
        [
            currentItem,
            queue,
            currentIndex,
            isPlaying,
            isLoading,
            error,
            positionMs,
            durationMs,
            volumeState,
            deviceId,
            playItem,
            playQueue,
            pause,
            resume,
            skipNext,
            skipPrevious,
            seek,
            setVolume,
            clearItem,
        ],
    )

    return <PlaybackContext.Provider value={value}>{children}</PlaybackContext.Provider>
}

export const usePlayback = () => {
    const context = useContext(PlaybackContext)
    if (!context) {
        throw new Error('usePlayback must be used within PlaybackProvider')
    }
    return context
}

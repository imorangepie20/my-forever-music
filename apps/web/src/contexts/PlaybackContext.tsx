import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import {
    resolvePlaybackPlatformId,
    resolveSpotifyContextUri,
    resolveSpotifyTrackId,
    resolveTidalTrackId,
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
import {
    describeTidalPreviewFailure,
    ensureTidalWebPlayer,
    getTidalCurrentSnapshot,
    getTidalDeviceId,
    isTidalPreviewSnapshot,
    playTidalMediaItem,
    tidalPause,
    tidalReset,
    tidalResume,
    tidalSeek,
    tidalSetVolume,
    type TidalPlaybackSnapshot,
    type TidalPlayerCallbacks,
} from '@/lib/tidalStreamPlayback'

interface PlaybackContextValue {
    currentItem: PlaybackMediaItem | null
    queue: PlaybackMediaItem[]
    currentIndex: number
    isPlaying: boolean
    isLoading: boolean
    error: string | null
    notice: string | null
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
    const [notice, setNotice] = useState<string | null>(null)
    const [positionMs, setPositionMs] = useState(0)
    const [durationMs, setDurationMs] = useState(0)
    const [volumeState, setVolumeState] = useState(0.5)
    const [deviceId, setDeviceId] = useState<string | null>(null)
    const queueRef = useRef(queue)
    const currentIndexRef = useRef(currentIndex)
    const volumeStateRef = useRef(volumeState)
    const tidalCallbacksRef = useRef<TidalPlayerCallbacks>({})
    const tidalPreviewBlockedRef = useRef(false)

    useEffect(() => {
        queueRef.current = queue
    }, [queue])

    useEffect(() => {
        currentIndexRef.current = currentIndex
    }, [currentIndex])

    useEffect(() => {
        volumeStateRef.current = volumeState
    }, [volumeState])

    const clearPlaybackError = useCallback(() => {
        setError(null)
    }, [])

    const handleSpotifyStateChange = useCallback((state: SpotifyPlaybackState | null) => {
        if (!state) {
            return
        }

        setNotice(null)
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

    const handleTidalStateChange = useCallback((state: TidalPlaybackSnapshot['state'], snapshot: TidalPlaybackSnapshot) => {
        if (isTidalPreviewSnapshot(snapshot)) {
            setNotice(null)
            setError(describeTidalPreviewFailure(snapshot))
            setIsPlaying(false)
            setPositionMs(0)
            if (!tidalPreviewBlockedRef.current) {
                tidalPreviewBlockedRef.current = true
                void tidalReset().catch(() => undefined)
            }
            return
        }

        tidalPreviewBlockedRef.current = false
        setNotice(null)
        setIsPlaying(state === 'PLAYING' || state === 'STALLED')
        setPositionMs(snapshot.positionMs)
        if (snapshot.durationMs > 0) {
            setDurationMs(snapshot.durationMs)
        }

        if (state === 'PLAYING') {
            clearPlaybackError()
        }

        if (!snapshot.productId) {
            return
        }

        const nextIndex = queueRef.current.findIndex((item) => resolveTidalTrackId(item) === snapshot.productId)
        if (nextIndex >= 0) {
            setCurrentIndex(nextIndex)
            setCurrentItem(queueRef.current[nextIndex])
        }
    }, [clearPlaybackError])

    const handleTidalEnded = useCallback(() => {
        const nextIndex = currentIndexRef.current + 1
        const nextQueue = queueRef.current
        const nextItem = nextQueue[nextIndex]

        if (!session?.userId || !nextItem) {
            setIsPlaying(false)
            return
        }

        setCurrentIndex(nextIndex)
        setCurrentItem(nextItem)
        setPositionMs(0)
        setDurationMs(nextItem.durationMs ?? 0)
        void (async () => {
            await tidalSetVolume(volumeStateRef.current)
            await playTidalMediaItem(
                session.userId,
                nextItem,
                nextQueue[nextIndex + 1],
                tidalCallbacksRef.current,
            )
        })().catch((playbackError: unknown) => {
            const message = playbackError instanceof Error ? playbackError.message : 'TIDAL playback failed.'
            setError(message)
            setIsPlaying(false)
        })
    }, [session?.userId])

    const tidalCallbacks = useMemo<TidalPlayerCallbacks>(
        () => ({
            onReady: (nextDeviceId: string) => setDeviceId(nextDeviceId),
            onStateChange: handleTidalStateChange,
            onTransition: (_productId, snapshot) => handleTidalStateChange(snapshot.state, snapshot),
            onEnded: handleTidalEnded,
            onError: (message: string) => setError(message),
        }),
        [handleTidalEnded, handleTidalStateChange],
    )

    useEffect(() => {
        tidalCallbacksRef.current = tidalCallbacks
    }, [tidalCallbacks])

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
            setNotice(null)
            tidalPreviewBlockedRef.current = false

            try {
                if (playbackPlatformId === 'spotify') {
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
                    return
                }

                if (playbackPlatformId === 'tidal') {
                    await ensureTidalWebPlayer(userId, tidalCallbacks)
                    await tidalSetVolume(volumeState)

                    const tidalEntries = items
                        .map((item, originalIndex) => ({
                            item,
                            originalIndex,
                            tidalTrackId: resolveTidalTrackId(item),
                        }))
                        .filter((entry): entry is { item: PlaybackMediaItem; originalIndex: number; tidalTrackId: string } => Boolean(entry.tidalTrackId))

                    const selectedEntry = tidalEntries.find((entry) => entry.originalIndex === safeStartIndex)
                    if (!selectedEntry) {
                        throw new Error('Selected track does not have a valid TIDAL track id or URI.')
                    }

                    const nextQueue = tidalEntries.map((entry) => entry.item)
                    const nextIndex = tidalEntries.findIndex((entry) => entry.originalIndex === safeStartIndex)

                    setDeviceId(getTidalDeviceId())
                    setQueue(nextQueue)
                    setCurrentIndex(nextIndex)
                    setCurrentItem(nextQueue[nextIndex])
                    setPositionMs(0)
                    setDurationMs(nextQueue[nextIndex]?.durationMs ?? 0)
                    await playTidalMediaItem(userId, nextQueue[nextIndex], nextQueue[nextIndex + 1], tidalCallbacks)
                    setIsPlaying(true)
                    clearPlaybackError()
                    return
                }

                throw new Error(`Playback is not implemented for ${playbackPlatformId ?? 'unknown'} tracks yet.`)
            } catch (playbackError: unknown) {
                const message = playbackError instanceof Error ? playbackError.message : 'Playback failed.'
                setError(message)
                setNotice(null)
                setIsPlaying(false)
            } finally {
                setIsLoading(false)
            }
        },
        [clearPlaybackError, requireUserId, session?.preferredPlatformId, spotifyCallbacks, tidalCallbacks, volumeState],
    )

    const playItem = useCallback((item: PlaybackMediaItem) => playQueue([item], 0), [playQueue])

    const pause = useCallback(async () => {
        if (!currentItem) {
            return
        }

        const userId = requireUserId()
        const playbackPlatformId = resolvePlaybackPlatformId(currentItem, session?.preferredPlatformId)
        if (playbackPlatformId === 'tidal') {
            await tidalPause()
        } else {
            await spotifyPause(userId)
        }
        setIsPlaying(false)
    }, [currentItem, requireUserId, session?.preferredPlatformId])

    const resume = useCallback(async () => {
        if (!currentItem) {
            return
        }

        const userId = requireUserId()
        clearPlaybackError()
        const playbackPlatformId = resolvePlaybackPlatformId(currentItem, session?.preferredPlatformId)
        if (playbackPlatformId === 'tidal') {
            await ensureTidalWebPlayer(userId, tidalCallbacks)
            await tidalSetVolume(volumeState)
            await tidalResume()
        } else {
            await ensureSpotifyWebPlayer(userId, spotifyCallbacks)
            await spotifyResume(userId)
        }
        setIsPlaying(true)
    }, [clearPlaybackError, currentItem, requireUserId, session?.preferredPlatformId, spotifyCallbacks, tidalCallbacks, volumeState])

    const skipNext = useCallback(async () => {
        if (!currentItem) {
            return
        }

        const nextIndex = clampIndex(currentIndexRef.current + 1, queueRef.current.length)
        const nextItem = queueRef.current[nextIndex] ?? currentItem
        const userId = requireUserId()
        const playbackPlatformId = resolvePlaybackPlatformId(currentItem, session?.preferredPlatformId)
        if (playbackPlatformId === 'tidal') {
            if (currentIndexRef.current >= queueRef.current.length - 1) {
                await tidalReset()
                setIsPlaying(false)
                setPositionMs(0)
                return
            }

            setCurrentIndex(nextIndex)
            setCurrentItem(nextItem)
            setPositionMs(0)
            setDurationMs(nextItem.durationMs ?? 0)
            await tidalSetVolume(volumeState)
            await playTidalMediaItem(userId, nextItem, queueRef.current[nextIndex + 1], tidalCallbacks)
            return
        }

        await spotifyNextTrack(userId)
        setCurrentIndex(nextIndex)
        setCurrentItem(nextItem)
    }, [currentItem, requireUserId, session?.preferredPlatformId, tidalCallbacks, volumeState])

    const skipPrevious = useCallback(async () => {
        if (!currentItem) {
            return
        }

        const nextIndex = clampIndex(currentIndexRef.current - 1, queueRef.current.length)
        const nextItem = queueRef.current[nextIndex] ?? currentItem
        const userId = requireUserId()
        const playbackPlatformId = resolvePlaybackPlatformId(currentItem, session?.preferredPlatformId)
        if (playbackPlatformId === 'tidal') {
            setCurrentIndex(nextIndex)
            setCurrentItem(nextItem)
            setPositionMs(0)
            setDurationMs(nextItem.durationMs ?? 0)
            await tidalSetVolume(volumeState)
            await playTidalMediaItem(userId, nextItem, queueRef.current[nextIndex + 1], tidalCallbacks)
            return
        }

        await spotifyPreviousTrack(userId)
        setCurrentIndex(nextIndex)
        setCurrentItem(nextItem)
    }, [currentItem, requireUserId, session?.preferredPlatformId, tidalCallbacks, volumeState])

    const seek = useCallback(
        async (nextPositionMs: number) => {
            if (!currentItem) {
                return
            }

            const userId = requireUserId()
            const safePosition = Math.min(Math.max(0, nextPositionMs), Math.max(durationMs, 0))
            setPositionMs(safePosition)
            const playbackPlatformId = resolvePlaybackPlatformId(currentItem, session?.preferredPlatformId)
            if (playbackPlatformId === 'tidal') {
                await tidalSeek(safePosition)
            } else {
                await spotifySeek(userId, safePosition)
            }
        },
        [currentItem, durationMs, requireUserId, session?.preferredPlatformId],
    )

    const setVolume = useCallback(
        async (nextVolume: number) => {
            const safeVolume = Math.min(1, Math.max(0, Number.isFinite(nextVolume) ? nextVolume : 0.5))
            setVolumeState(safeVolume)
            if (!session?.userId) {
                return
            }

            if (currentItem && resolvePlaybackPlatformId(currentItem, session.preferredPlatformId) === 'tidal') {
                await tidalSetVolume(safeVolume)
                return
            }

            await spotifySetVolume(session.userId, safeVolume)
        },
        [currentItem, session],
    )

    const clearItem = useCallback(() => {
        if (session?.userId && currentItem) {
            const playbackPlatformId = resolvePlaybackPlatformId(currentItem, session.preferredPlatformId)
            if (playbackPlatformId === 'tidal') {
                void tidalReset().catch(() => undefined)
            } else {
                void spotifyPause(session.userId).catch(() => undefined)
            }
        }
        setCurrentItem(null)
        setQueue([])
        setCurrentIndex(0)
        setIsPlaying(false)
        setIsLoading(false)
        clearPlaybackError()
        setNotice(null)
        setPositionMs(0)
        setDurationMs(0)
    }, [clearPlaybackError, currentItem, session])

    useEffect(() => {
        if (!session?.userId || !currentItem) {
            return
        }

        const playbackPlatformId = resolvePlaybackPlatformId(currentItem, session.preferredPlatformId)
        const intervalId = window.setInterval(() => {
            if (playbackPlatformId === 'tidal') {
                const snapshot = getTidalCurrentSnapshot()
                handleTidalStateChange(snapshot.state, snapshot)
                return
            }

            void getSpotifyCurrentState(session.userId)
                .then(handleSpotifyStateChange)
                .catch(() => undefined)
        }, 2_500)

        return () => window.clearInterval(intervalId)
    }, [currentItem, handleSpotifyStateChange, handleTidalStateChange, session])

    const value = useMemo<PlaybackContextValue>(
        () => ({
            currentItem,
            queue,
            currentIndex,
            isPlaying,
            isLoading,
            error,
            notice,
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
            notice,
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

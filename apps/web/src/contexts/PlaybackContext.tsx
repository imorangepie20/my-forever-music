import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import { recordUserMusicEvent } from '@/services/api'
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
    spotifySetRepeat,
    spotifySetShuffle,
    spotifySetVolume,
    type SpotifyPlaybackState,
} from '@/lib/spotifyPlaybackSdk'
import { resolveSpotifyPlayableItem } from '@/lib/spotifyResolvedPlayback'
import {
    describeTidalPreviewFailure,
    ensureTidalWebPlayer,
    getTidalCurrentSnapshot,
    getTidalDeviceId,
    isTidalPreviewSnapshot,
    playTidalMediaItem,
    resolveTidalPlayableItem,
    tidalPause,
    tidalReset,
    tidalResume,
    tidalSeek,
    tidalSetVolume,
    type TidalPlaybackSnapshot,
    type TidalPlayerCallbacks,
} from '@/lib/tidalStreamPlayback'
import type { UserMusicEventType } from '@/types/api'

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
    shuffleEnabled: boolean
    repeatMode: PlaybackRepeatMode
    audioQualityLabel: string | null
    playItem: (item: PlaybackMediaItem) => Promise<void>
    playQueue: (items: PlaybackMediaItem[], startIndex?: number) => Promise<void>
    pause: () => Promise<void>
    resume: () => Promise<void>
    skipNext: () => Promise<void>
    skipPrevious: () => Promise<void>
    seek: (positionMs: number) => Promise<void>
    setVolume: (volume: number) => Promise<void>
    toggleShuffle: () => Promise<void>
    cycleRepeatMode: () => Promise<void>
    clearItem: () => void
}

const PlaybackContext = createContext<PlaybackContextValue | null>(null)
export type PlaybackRepeatMode = 'off' | 'all' | 'one'

const clampIndex = (index: number, length: number) => Math.min(Math.max(0, index), Math.max(0, length - 1))
const toSpotifyUri = (item: PlaybackMediaItem) => {
    const spotifyTrackId = resolveSpotifyTrackId(item)
    return spotifyTrackId ? `spotify:track:${spotifyTrackId}` : null
}
const nextRepeatMode = (mode: PlaybackRepeatMode): PlaybackRepeatMode =>
    mode === 'off' ? 'all' : mode === 'all' ? 'one' : 'off'
const toSpotifyRepeatMode = (mode: PlaybackRepeatMode) =>
    mode === 'one' ? 'track' : mode === 'all' ? 'context' : 'off'
const shuffledQueueWithStart = (items: PlaybackMediaItem[], startIndex: number) => {
    const safeStartIndex = clampIndex(startIndex, items.length)
    const selectedItem = items[safeStartIndex]
    const remainingItems = items.filter((_, index) => index !== safeStartIndex)
    for (let index = remainingItems.length - 1; index > 0; index -= 1) {
        const swapIndex = Math.floor(Math.random() * (index + 1))
        ;[remainingItems[index], remainingItems[swapIndex]] = [remainingItems[swapIndex], remainingItems[index]]
    }
    return [selectedItem, ...remainingItems]
}
const formatTidalAudioQuality = (snapshot: TidalPlaybackSnapshot) => {
    const quality = snapshot.audioQuality ?? snapshot.requestedQuality
    const codec = snapshot.codec
    const sampleRate = snapshot.sampleRate
        ? `${Number.isInteger(snapshot.sampleRate / 1000) ? snapshot.sampleRate / 1000 : (snapshot.sampleRate / 1000).toFixed(1)} kHz`
        : null
    const bitDepth = snapshot.bitDepth ? `${snapshot.bitDepth}-bit` : null
    const resolution = [sampleRate, bitDepth].filter(Boolean).join(' / ')
    const parts = [quality, codec, resolution || null].filter(Boolean)
    return parts.length > 0 ? parts.join(' · ') : null
}
const replaceQueueItem = (items: PlaybackMediaItem[], index: number, item: PlaybackMediaItem) =>
    items.map((entry, entryIndex) => entryIndex === index ? item : entry)
const readArtistName = (item: PlaybackMediaItem) => item.subtitle.split(' · ')[0]?.trim() || item.subtitle || null

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
    const [shuffleEnabled, setShuffleEnabled] = useState(false)
    const [repeatMode, setRepeatMode] = useState<PlaybackRepeatMode>('off')
    const [audioQualityLabel, setAudioQualityLabel] = useState<string | null>(null)
    const queueRef = useRef(queue)
    const currentIndexRef = useRef(currentIndex)
    const volumeStateRef = useRef(volumeState)
    const shuffleEnabledRef = useRef(shuffleEnabled)
    const repeatModeRef = useRef(repeatMode)
    const positionMsRef = useRef(positionMs)
    const durationMsRef = useRef(durationMs)
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

    useEffect(() => {
        shuffleEnabledRef.current = shuffleEnabled
    }, [shuffleEnabled])

    useEffect(() => {
        repeatModeRef.current = repeatMode
    }, [repeatMode])

    useEffect(() => {
        positionMsRef.current = positionMs
    }, [positionMs])

    useEffect(() => {
        durationMsRef.current = durationMs
    }, [durationMs])

    const clearPlaybackError = useCallback(() => {
        setError(null)
    }, [])

    const recordPlaybackEvent = useCallback(
        (
            eventType: UserMusicEventType,
            item: PlaybackMediaItem,
            overrides: { positionMs?: number; durationMs?: number } = {},
        ) => {
            if (!session?.userId) {
                return
            }

            const nextDurationMs = overrides.durationMs ?? item.durationMs ?? durationMsRef.current
            const nextPositionMs = overrides.positionMs ?? positionMsRef.current
            const playRatio = nextDurationMs && nextDurationMs > 0
                ? Math.min(1, Math.max(0, nextPositionMs / nextDurationMs))
                : null
            const playbackPlatformId = resolvePlaybackPlatformId(item, session.preferredPlatformId)

            void recordUserMusicEvent({
                user_id: session.userId,
                event_type: eventType,
                source_space: 'player',
                source_platform: item.sourcePlatform,
                playback_platform_id: playbackPlatformId,
                item_id: item.id,
                item_kind: item.kind,
                track_id: item.kind === 'track' ? item.id : null,
                external_track_id: item.externalTrackId ?? item.spotifyTrackId ?? item.tidalTrackId ?? null,
                platform_uri: item.platformUri ?? null,
                title: item.title,
                artist_name: readArtistName(item),
                album_title: item.albumTitle ?? null,
                isrc: item.isrc ?? null,
                duration_ms: nextDurationMs && nextDurationMs > 0 ? Math.round(nextDurationMs) : null,
                position_ms: nextPositionMs && nextPositionMs > 0 ? Math.round(nextPositionMs) : null,
                play_ratio: playRatio,
                occurred_at: new Date().toISOString(),
            }).catch(() => undefined)
        },
        [session?.preferredPlatformId, session?.userId],
    )

    const handleSpotifyStateChange = useCallback((state: SpotifyPlaybackState | null) => {
        if (!state) {
            return
        }

        setNotice(null)
        setIsPlaying(!state.paused)
        setPositionMs(state.position ?? 0)
        setDurationMs(state.duration ?? 0)
        setAudioQualityLabel('Spotify')

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
        setAudioQualityLabel(formatTidalAudioQuality(snapshot))
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
        const nextQueue = queueRef.current
        const completedItem = nextQueue[currentIndexRef.current]
        if (completedItem) {
            const completedDurationMs = durationMsRef.current || completedItem.durationMs || 0
            recordPlaybackEvent('play_completed', completedItem, {
                durationMs: completedDurationMs,
                positionMs: completedDurationMs,
            })
        }

        let nextIndex = currentIndexRef.current + 1
        if (repeatModeRef.current === 'one') {
            nextIndex = currentIndexRef.current
        } else if (nextIndex >= nextQueue.length && repeatModeRef.current === 'all') {
            nextIndex = 0
        }
        const nextItem = nextQueue[nextIndex]

        if (!session?.userId || !nextItem) {
            setIsPlaying(false)
            return
        }

        setPositionMs(0)
        void (async () => {
            setNotice('Searching TIDAL for playable track...')
            const playableItem = await resolveTidalPlayableItem(session.userId, nextItem)
            const resolvedQueue = replaceQueueItem(nextQueue, nextIndex, playableItem)
            queueRef.current = resolvedQueue
            setQueue(resolvedQueue)
            setCurrentIndex(nextIndex)
            setCurrentItem(playableItem)
            setDurationMs(playableItem.durationMs ?? 0)
            await tidalSetVolume(volumeStateRef.current)
            await playTidalMediaItem(
                session.userId,
                playableItem,
                resolvedQueue[nextIndex + 1],
                tidalCallbacksRef.current,
            )
            setNotice(null)
        })().catch((playbackError: unknown) => {
            const message = playbackError instanceof Error ? playbackError.message : 'TIDAL playback failed.'
            setError(message)
            setNotice(null)
            setIsPlaying(false)
        })
    }, [recordPlaybackEvent, session?.userId])

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
            const nextPlaybackItems = shuffleEnabledRef.current
                ? shuffledQueueWithStart(items, startIndex)
                : items
            const safeStartIndex = shuffleEnabledRef.current ? 0 : clampIndex(startIndex, nextPlaybackItems.length)
            const selectedItem = nextPlaybackItems[safeStartIndex]
            const playbackPlatformId = resolvePlaybackPlatformId(selectedItem, session?.preferredPlatformId)
            const pendingItems = nextPlaybackItems.map((item, index) =>
                index === safeStartIndex ? { ...item, playbackPlatformId } : item
            )
            const pendingSelectedItem = pendingItems[safeStartIndex]
            const previousItem = currentItem

            setIsLoading(true)
            clearPlaybackError()
            setNotice(null)
            setQueue([])
            setCurrentIndex(0)
            setCurrentItem(null)
            setPositionMs(0)
            setDurationMs(0)
            setAudioQualityLabel(null)
            setIsPlaying(false)
            tidalPreviewBlockedRef.current = false

            try {
                if (previousItem) {
                    const previousPlatformId = resolvePlaybackPlatformId(previousItem, session?.preferredPlatformId)
                    if (previousPlatformId === 'tidal') {
                        await tidalReset()
                    } else if (previousPlatformId === 'spotify') {
                        await spotifyPause(userId)
                    }
                }

                setQueue(pendingItems)
                setCurrentIndex(safeStartIndex)
                setCurrentItem(pendingSelectedItem)
                setPositionMs(0)
                setDurationMs(pendingSelectedItem.durationMs ?? 0)

                if (playbackPlatformId === 'spotify') {
                    setNotice('Preparing Spotify playback...')
                    await ensureSpotifyWebPlayer(userId, spotifyCallbacks)
                    await spotifySetVolume(userId, volumeState)
                    await spotifySetShuffle(userId, shuffleEnabledRef.current)
                    await spotifySetRepeat(userId, toSpotifyRepeatMode(repeatModeRef.current))
                    setAudioQualityLabel('Spotify')

                    const spotifyContextUri = nextPlaybackItems.length === 1 ? resolveSpotifyContextUri(selectedItem) : null
                    if (spotifyContextUri) {
                        setQueue([selectedItem])
                        setCurrentIndex(0)
                        setCurrentItem(selectedItem)
                        setPositionMs(0)
                        setDurationMs(selectedItem.durationMs ?? 0)
                        await playSpotifyContext(userId, spotifyContextUri)
                        setIsPlaying(true)
                        recordPlaybackEvent('play_started', selectedItem, {
                            durationMs: selectedItem.durationMs ?? 0,
                            positionMs: 0,
                        })
                        clearPlaybackError()
                        return
                    }

                    setNotice('Searching Spotify for playable tracks...')
                    const resolvedSpotifyItems = await Promise.all(
                        nextPlaybackItems.map((item) => resolveSpotifyPlayableItem(userId, item))
                    )
                    const resolvedSelectedItem = resolvedSpotifyItems[safeStartIndex] ?? pendingSelectedItem
                    setQueue(resolvedSpotifyItems)
                    setCurrentIndex(safeStartIndex)
                    setCurrentItem(resolvedSelectedItem)
                    setDurationMs(resolvedSelectedItem.durationMs ?? 0)
                    setNotice('Starting Spotify playback...')

                    const spotifyEntries = resolvedSpotifyItems
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
                    recordPlaybackEvent('play_started', nextQueue[nextIndex], {
                        durationMs: nextQueue[nextIndex]?.durationMs ?? 0,
                        positionMs: 0,
                    })
                    clearPlaybackError()
                    return
                }

                if (playbackPlatformId === 'tidal') {
                    setNotice('Preparing TIDAL playback...')
                    await ensureTidalWebPlayer(userId, tidalCallbacks)
                    await tidalSetVolume(volumeState)

                    setDeviceId(getTidalDeviceId())
                    setPositionMs(0)
                    setNotice('Searching TIDAL for playable track...')
                    const playableSelectedItem = await resolveTidalPlayableItem(userId, pendingSelectedItem)
                    const resolvedItems = replaceQueueItem(pendingItems, safeStartIndex, playableSelectedItem)
                    setQueue(resolvedItems)
                    setCurrentIndex(safeStartIndex)
                    setCurrentItem(playableSelectedItem)
                    setDurationMs(playableSelectedItem.durationMs ?? 0)
                    setNotice('Starting TIDAL stream...')
                    await playTidalMediaItem(userId, playableSelectedItem, resolvedItems[safeStartIndex + 1], tidalCallbacks)
                    setIsPlaying(true)
                    recordPlaybackEvent('play_started', playableSelectedItem, {
                        durationMs: playableSelectedItem.durationMs ?? 0,
                        positionMs: 0,
                    })
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
        [clearPlaybackError, currentItem, recordPlaybackEvent, requireUserId, session?.preferredPlatformId, spotifyCallbacks, tidalCallbacks, volumeState],
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
        recordPlaybackEvent('play_paused', currentItem)
    }, [currentItem, recordPlaybackEvent, requireUserId, session?.preferredPlatformId])

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
        recordPlaybackEvent('play_resumed', currentItem)
    }, [clearPlaybackError, currentItem, recordPlaybackEvent, requireUserId, session?.preferredPlatformId, spotifyCallbacks, tidalCallbacks, volumeState])

    const skipNext = useCallback(async () => {
        if (!currentItem) {
            return
        }

        const isAtQueueEnd = currentIndexRef.current >= queueRef.current.length - 1
        const nextIndex = isAtQueueEnd && repeatModeRef.current === 'all'
            ? 0
            : clampIndex(currentIndexRef.current + 1, queueRef.current.length)
        const nextItem = queueRef.current[nextIndex] ?? currentItem
        const userId = requireUserId()
        const playbackPlatformId = resolvePlaybackPlatformId(currentItem, session?.preferredPlatformId)
        if (playbackPlatformId === 'tidal') {
            if (isAtQueueEnd && repeatModeRef.current !== 'all') {
                await tidalReset()
                setIsPlaying(false)
                setPositionMs(0)
                recordPlaybackEvent('skip_next', currentItem)
                return
            }

            setPositionMs(0)
            setNotice('Searching TIDAL for playable track...')
            const playableItem = await resolveTidalPlayableItem(userId, nextItem)
            const resolvedQueue = replaceQueueItem(queueRef.current, nextIndex, playableItem)
            queueRef.current = resolvedQueue
            setQueue(resolvedQueue)
            setCurrentIndex(nextIndex)
            setCurrentItem(playableItem)
            setDurationMs(playableItem.durationMs ?? 0)
            await tidalSetVolume(volumeState)
            await playTidalMediaItem(userId, playableItem, resolvedQueue[nextIndex + 1], tidalCallbacks)
            setNotice(null)
            recordPlaybackEvent('skip_next', currentItem)
            return
        }

        await spotifyNextTrack(userId)
        setCurrentIndex(nextIndex)
        setCurrentItem(nextItem)
        recordPlaybackEvent('skip_next', currentItem)
    }, [currentItem, recordPlaybackEvent, requireUserId, session?.preferredPlatformId, tidalCallbacks, volumeState])

    const skipPrevious = useCallback(async () => {
        if (!currentItem) {
            return
        }

        const nextIndex = clampIndex(currentIndexRef.current - 1, queueRef.current.length)
        const nextItem = queueRef.current[nextIndex] ?? currentItem
        const userId = requireUserId()
        const playbackPlatformId = resolvePlaybackPlatformId(currentItem, session?.preferredPlatformId)
        if (playbackPlatformId === 'tidal') {
            setPositionMs(0)
            setNotice('Searching TIDAL for playable track...')
            const playableItem = await resolveTidalPlayableItem(userId, nextItem)
            const resolvedQueue = replaceQueueItem(queueRef.current, nextIndex, playableItem)
            queueRef.current = resolvedQueue
            setQueue(resolvedQueue)
            setCurrentIndex(nextIndex)
            setCurrentItem(playableItem)
            setDurationMs(playableItem.durationMs ?? 0)
            await tidalSetVolume(volumeState)
            await playTidalMediaItem(userId, playableItem, resolvedQueue[nextIndex + 1], tidalCallbacks)
            setNotice(null)
            recordPlaybackEvent('skip_previous', currentItem)
            return
        }

        await spotifyPreviousTrack(userId)
        setCurrentIndex(nextIndex)
        setCurrentItem(nextItem)
        recordPlaybackEvent('skip_previous', currentItem)
    }, [currentItem, recordPlaybackEvent, requireUserId, session?.preferredPlatformId, tidalCallbacks, volumeState])

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

    const toggleShuffle = useCallback(async () => {
        const nextShuffleEnabled = !shuffleEnabledRef.current
        shuffleEnabledRef.current = nextShuffleEnabled
        setShuffleEnabled(nextShuffleEnabled)

        if (nextShuffleEnabled && queueRef.current.length > 1) {
            const nextQueue = shuffledQueueWithStart(queueRef.current, currentIndexRef.current)
            setQueue(nextQueue)
            setCurrentIndex(0)
            setCurrentItem(nextQueue[0])
        }

        if (session?.userId && currentItem && resolvePlaybackPlatformId(currentItem, session.preferredPlatformId) === 'spotify') {
            await spotifySetShuffle(session.userId, nextShuffleEnabled)
        }
    }, [currentItem, session])

    const cycleRepeatMode = useCallback(async () => {
        const nextMode = nextRepeatMode(repeatModeRef.current)
        repeatModeRef.current = nextMode
        setRepeatMode(nextMode)

        if (session?.userId && currentItem && resolvePlaybackPlatformId(currentItem, session.preferredPlatformId) === 'spotify') {
            await spotifySetRepeat(session.userId, toSpotifyRepeatMode(nextMode))
        }
    }, [currentItem, session])

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
        setAudioQualityLabel(null)
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
            shuffleEnabled,
            repeatMode,
            audioQualityLabel,
            playItem,
            playQueue,
            pause,
            resume,
            skipNext,
            skipPrevious,
            seek,
            setVolume,
            toggleShuffle,
            cycleRepeatMode,
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
            shuffleEnabled,
            repeatMode,
            audioQualityLabel,
            playItem,
            playQueue,
            pause,
            resume,
            skipNext,
            skipPrevious,
            seek,
            setVolume,
            toggleShuffle,
            cycleRepeatMode,
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

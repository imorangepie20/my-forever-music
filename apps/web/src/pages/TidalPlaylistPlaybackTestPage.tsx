import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
    ArrowLeft,
    AlertCircle,
    CheckCircle2,
    Pause,
    Play,
    RefreshCw,
    SkipForward,
    Square,
    Volume2,
} from 'lucide-react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import Button from '@/components/common/Button'
import HudCard from '@/components/common/HudCard'
import MusicArtwork from '@/components/music/MusicArtwork'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import {
    formatDuration,
    resolveTidalTrackId,
    type PlaybackMediaItem,
} from '@/lib/musicPlayback'
import { toPmsTrackPlaybackItem } from '@/lib/pmsPlayback'
import {
    describeTidalPreviewFailure,
    ensureTidalWebPlayer,
    getTidalCurrentSnapshot,
    isTidalPreviewSnapshot,
    playTidalMediaItem,
    tidalPause,
    tidalReset,
    tidalResume,
    tidalSetNextMediaItem,
    tidalSetVolume,
    type TidalPlaybackSnapshot,
    type TidalPlayerCallbacks,
} from '@/lib/tidalStreamPlayback'
import {
    ApiError,
    fetchPmsPlaylistDetail,
    fetchPmsWorkspaceBootstrap,
} from '@/services/api'
import type {
    PmsPlaylistDetailResponse,
    PmsWorkspaceBootstrapResponse,
} from '@/types/api'

type TidalWorkspacePlaylist = PmsWorkspaceBootstrapResponse['playlists'][number]

interface EventLogEntry {
    id: string
    createdAt: string
    level: 'info' | 'success' | 'error'
    message: string
}

const eventId = () => `${Date.now()}-${Math.random().toString(36).slice(2)}`

const errorMessage = (error: unknown, fallback: string) => {
    if (error instanceof Error && error.message) {
        return error.message
    }

    if (typeof error === 'string' && error) {
        return error
    }

    return fallback
}

const snapshotSummary = (snapshot: TidalPlaybackSnapshot | null) => {
    if (!snapshot) {
        return 'state=IDLE'
    }

    const parts = [
        `state=${snapshot.state}`,
        snapshot.productId ? `product=${snapshot.productId}` : null,
        snapshot.presentation ? `presentation=${snapshot.presentation}` : null,
        snapshot.previewReason ? `preview=${snapshot.previewReason}` : null,
        snapshot.durationMs ? `duration=${Math.round(snapshot.durationMs / 1000)}s` : null,
        `position=${Math.round(snapshot.positionMs / 1000)}s`,
    ].filter(Boolean)

    return parts.join(' ')
}

const sortByPlaylistTitle = (left: TidalWorkspacePlaylist, right: TidalWorkspacePlaylist) =>
    left.title.localeCompare(right.title)

const TidalPlaylistPlaybackTestPage = () => {
    const navigate = useNavigate()
    const [searchParams, setSearchParams] = useSearchParams()
    const { session } = useAuthSession()
    const selectedPlaylistId = searchParams.get('playlist_id') ?? ''
    const [playlists, setPlaylists] = useState<TidalWorkspacePlaylist[]>([])
    const [detail, setDetail] = useState<PmsPlaylistDetailResponse | null>(null)
    const [isLoadingPlaylists, setIsLoadingPlaylists] = useState(false)
    const [isLoadingDetail, setIsLoadingDetail] = useState(false)
    const [isStarting, setIsStarting] = useState(false)
    const [currentIndex, setCurrentIndex] = useState<number | null>(null)
    const [snapshot, setSnapshot] = useState<TidalPlaybackSnapshot | null>(null)
    const [volume, setVolume] = useState(0.74)
    const [error, setError] = useState<string | null>(null)
    const [logs, setLogs] = useState<EventLogEntry[]>([])
    const queueItemsRef = useRef<PlaybackMediaItem[]>([])
    const currentIndexRef = useRef<number | null>(null)
    const volumeRef = useRef(volume)
    const playAtRef = useRef<(index: number) => Promise<void>>(async () => undefined)

    const appendLog = useCallback((level: EventLogEntry['level'], message: string) => {
        setLogs((current) => [
            {
                id: eventId(),
                createdAt: new Date().toLocaleTimeString(),
                level,
                message,
            },
            ...current,
        ].slice(0, 48))
    }, [])

    const setSelectedPlaylistId = useCallback(
        (playlistId: string) => {
            setSearchParams(playlistId ? { playlist_id: playlistId } : {})
        },
        [setSearchParams],
    )

    const playableItems = useMemo(
        () =>
            detail?.tracks
                .map((track) => toPmsTrackPlaybackItem(track, detail.playlist.title))
                .filter((item) => item.sourcePlatform === 'tidal' && Boolean(resolveTidalTrackId(item))) ?? [],
        [detail],
    )

    const excludedTrackCount = Math.max(0, (detail?.tracks.length ?? 0) - playableItems.length)

    useEffect(() => {
        queueItemsRef.current = playableItems
    }, [playableItems])

    useEffect(() => {
        volumeRef.current = volume
    }, [volume])

    const setNextTrack = useCallback(async (nextIndex: number) => {
        const nextItem = queueItemsRef.current[nextIndex]
        await tidalSetNextMediaItem(nextItem ?? null)
    }, [])

    const callbacks = useMemo<TidalPlayerCallbacks>(
        () => ({
            onReady: (deviceId) => {
                appendLog('success', `TIDAL SDK player ready device=${deviceId}`)
            },
            onStateChange: (state, nextSnapshot) => {
                setSnapshot(nextSnapshot)
                appendLog(state === 'PLAYING' ? 'success' : 'info', `state ${snapshotSummary(nextSnapshot)}`)
            },
            onTransition: (productId, nextSnapshot) => {
                const nextIndex = queueItemsRef.current.findIndex((item) => resolveTidalTrackId(item) === productId)
                if (nextIndex >= 0) {
                    currentIndexRef.current = nextIndex
                    setCurrentIndex(nextIndex)
                    void setNextTrack(nextIndex + 1).catch((nextError: unknown) => {
                        const message = errorMessage(nextError, 'TIDAL next track could not be prepared.')
                        setError(message)
                        appendLog('error', message)
                    })
                }

                setSnapshot(nextSnapshot)
                if (isTidalPreviewSnapshot(nextSnapshot)) {
                    const message = describeTidalPreviewFailure(nextSnapshot)
                    setError(message)
                    appendLog('error', message)
                    return
                }

                appendLog('success', `transition ${snapshotSummary(nextSnapshot)}`)
            },
            onEnded: (productId) => {
                const endedIndex = queueItemsRef.current.findIndex((item) => resolveTidalTrackId(item) === productId)
                appendLog('info', `ended product=${productId ?? 'unknown'}`)

                if (endedIndex < 0 || currentIndexRef.current !== endedIndex) {
                    return
                }

                if (endedIndex + 1 >= queueItemsRef.current.length) {
                    currentIndexRef.current = null
                    setCurrentIndex(null)
                    appendLog('success', 'playlist completed')
                    return
                }

                void playAtRef.current(endedIndex + 1).catch((nextError: unknown) => {
                    const message = errorMessage(nextError, 'TIDAL next track could not start.')
                    setError(message)
                    appendLog('error', message)
                })
            },
            onError: (message) => {
                setError(message)
                appendLog('error', message)
            },
        }),
        [appendLog, setNextTrack],
    )

    const loadPlaylists = useCallback(async () => {
        if (!session?.userId) {
            setPlaylists([])
            setDetail(null)
            setError('Sign in before testing TIDAL playlist playback.')
            return
        }

        setIsLoadingPlaylists(true)
        setError(null)

        try {
            const response = await fetchPmsWorkspaceBootstrap(session.userId)
            const tidalPlaylists = response.playlists
                .filter((playlist) => playlist.source_platform === 'tidal')
                .sort(sortByPlaylistTitle)
            setPlaylists(tidalPlaylists)

            if (!selectedPlaylistId && tidalPlaylists[0]) {
                setSelectedPlaylistId(tidalPlaylists[0].playlist_id)
            }

            appendLog('success', `loaded ${tidalPlaylists.length} TIDAL PMS playlists`)
        } catch (requestError: unknown) {
            const message = requestError instanceof ApiError
                ? requestError.message
                : errorMessage(requestError, 'Unable to load PMS playlists.')
            setError(message)
            appendLog('error', message)
        } finally {
            setIsLoadingPlaylists(false)
        }
    }, [appendLog, selectedPlaylistId, session?.userId, setSelectedPlaylistId])

    useEffect(() => {
        void loadPlaylists()
    }, [loadPlaylists])

    useEffect(() => {
        const controller = new AbortController()

        currentIndexRef.current = null
        setCurrentIndex(null)
        setSnapshot(null)
        setError(null)

        if (!session?.userId || !selectedPlaylistId) {
            setDetail(null)
            setIsLoadingDetail(false)
            return () => controller.abort()
        }

        setIsLoadingDetail(true)

        const loadDetail = async () => {
            try {
                const response = await fetchPmsPlaylistDetail(session.userId, selectedPlaylistId, controller.signal)
                setDetail(response)
                appendLog('success', `loaded playlist tracks playlist=${response.playlist.playlist_id} tracks=${response.tracks.length}`)
            } catch (requestError: unknown) {
                if (requestError instanceof DOMException && requestError.name === 'AbortError') {
                    return
                }

                const message = requestError instanceof ApiError
                    ? requestError.message
                    : errorMessage(requestError, 'Unable to load selected TIDAL playlist.')
                setDetail(null)
                setError(message)
                appendLog('error', message)
            } finally {
                setIsLoadingDetail(false)
            }
        }

        void loadDetail()

        return () => controller.abort()
    }, [appendLog, selectedPlaylistId, session?.userId])

    const playAt = useCallback(
        async (index: number) => {
            if (!session?.userId) {
                const message = 'Sign in before testing TIDAL playlist playback.'
                setError(message)
                appendLog('error', message)
                return
            }

            const item = queueItemsRef.current[index]
            if (!item) {
                const message = `TIDAL queue item is missing at index ${index}.`
                setError(message)
                appendLog('error', message)
                return
            }
            const tidalTrackId = resolveTidalTrackId(item)
            if (!tidalTrackId) {
                const message = `TIDAL track id is missing for "${item.title}".`
                setError(message)
                appendLog('error', message)
                return
            }

            setIsStarting(true)
            setError(null)
            currentIndexRef.current = index
            setCurrentIndex(index)
            appendLog('info', `play request ${index + 1}/${queueItemsRef.current.length} ${item.title}`)

            try {
                await ensureTidalWebPlayer(session.userId, callbacks)
                await tidalSetVolume(volumeRef.current)
                await playTidalMediaItem(session.userId, item, queueItemsRef.current[index + 1], callbacks)
                setSnapshot(getTidalCurrentSnapshot())
            } catch (playError: unknown) {
                const message = errorMessage(playError, 'TIDAL playlist playback could not start.')
                setError(message)
                appendLog('error', message)
            } finally {
                setIsStarting(false)
            }
        },
        [appendLog, callbacks, session?.userId],
    )

    useEffect(() => {
        playAtRef.current = playAt
    }, [playAt])

    useEffect(
        () => () => {
            void tidalReset()
        },
        [],
    )

    const selectedPlaylist = playlists.find((playlist) => playlist.playlist_id === selectedPlaylistId) ?? null
    const activeItem = currentIndex === null ? null : playableItems[currentIndex] ?? null
    const canPlay = Boolean(session?.userId && playableItems.length > 0 && !isStarting)

    const handlePause = async () => {
        tidalPause()
        setSnapshot(getTidalCurrentSnapshot())
        appendLog('info', 'pause requested')
    }

    const handleResume = async () => {
        setError(null)
        try {
            await tidalSetVolume(volumeRef.current)
            await tidalResume()
            setSnapshot(getTidalCurrentSnapshot())
            appendLog('success', 'resume requested')
        } catch (resumeError: unknown) {
            const message = errorMessage(resumeError, 'TIDAL playback could not resume.')
            setError(message)
            appendLog('error', message)
        }
    }

    const handleStop = async () => {
        await tidalReset()
        currentIndexRef.current = null
        setCurrentIndex(null)
        setSnapshot(null)
        appendLog('info', 'player reset')
    }

    const handleNext = async () => {
        if (currentIndex === null) {
            await playAt(0)
            return
        }

        await playAt(currentIndex + 1)
    }

    const handleVolumeChange = async (nextVolume: number) => {
        const normalizedVolume = Math.min(1, Math.max(0, nextVolume))
        setVolume(normalizedVolume)
        volumeRef.current = normalizedVolume
        try {
            await tidalSetVolume(normalizedVolume)
        } catch (volumeError: unknown) {
            const message = errorMessage(volumeError, 'TIDAL volume could not be changed.')
            setError(message)
            appendLog('error', message)
        }
    }

    return (
        <main className="min-h-screen hud-grid-bg px-4 py-5 sm:px-6 lg:px-8">
            <div className="mx-auto flex max-w-7xl flex-col gap-5">
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-hud-border-secondary pb-5">
                    <div className="flex items-center gap-3">
                        <Button type="button" variant="ghost" onClick={() => navigate(-1)}>
                            <ArrowLeft size={18} />
                            Back
                        </Button>
                        <div>
                            <p className="text-xs uppercase tracking-[0.24em] text-hud-accent-primary">
                                Playback Harness
                            </p>
                            <h1 className="mt-1 text-2xl font-semibold text-hud-text-primary">
                                TIDAL Playlist SDK Test
                            </h1>
                        </div>
                    </div>
                    <Button
                        type="button"
                        variant="outline"
                        onClick={() => void loadPlaylists()}
                        disabled={isLoadingPlaylists}
                    >
                        <RefreshCw size={17} className={isLoadingPlaylists ? 'animate-spin' : undefined} />
                        Refresh
                    </Button>
                </div>

                {error && (
                    <div className="flex items-start gap-3 rounded-lg border border-hud-accent-danger/40 bg-hud-accent-danger/10 p-4 text-sm leading-6 text-hud-text-secondary">
                        <AlertCircle size={18} className="mt-0.5 shrink-0 text-hud-accent-danger" />
                        <span>{error}</span>
                    </div>
                )}

                <section className="grid gap-5 xl:grid-cols-[360px_minmax(0,1fr)]">
                    <HudCard title="TIDAL PMS Playlists" subtitle={`${playlists.length} playlists`}>
                        <div className="space-y-3">
                            {playlists.map((playlist) => (
                                <button
                                    key={playlist.playlist_id}
                                    type="button"
                                    onClick={() => setSelectedPlaylistId(playlist.playlist_id)}
                                    className={`grid w-full grid-cols-[64px_minmax(0,1fr)] gap-3 rounded-lg border p-3 text-left transition-hud ${
                                        playlist.playlist_id === selectedPlaylistId
                                            ? 'border-hud-border-primary bg-hud-accent-primary/10'
                                            : 'border-hud-border-secondary bg-hud-bg-primary/60 hover:border-hud-border-primary'
                                    }`}
                                >
                                    <div className="aspect-square overflow-hidden rounded-lg">
                                        <MusicArtwork
                                            imageUrl={playlist.cover_image_url}
                                            seed={`${playlist.source_platform}-${playlist.title}`}
                                            label={playlist.title}
                                        />
                                    </div>
                                    <span className="min-w-0">
                                        <span className="block truncate text-sm font-semibold text-hud-text-primary">
                                            {playlist.title}
                                        </span>
                                        <span className="mt-1 block text-xs uppercase tracking-[0.18em] text-hud-text-muted">
                                            {playlist.track_count} tracks
                                        </span>
                                    </span>
                                </button>
                            ))}
                            {!isLoadingPlaylists && playlists.length === 0 && (
                                <p className="rounded-lg border border-hud-border-secondary bg-hud-bg-primary/70 p-4 text-sm leading-6 text-hud-text-secondary">
                                    No imported TIDAL PMS playlist is available for this session.
                                </p>
                            )}
                        </div>
                    </HudCard>

                    <div className="grid gap-5">
                        <HudCard noPadding>
                            <div className="grid gap-5 p-5 lg:grid-cols-[180px_minmax(0,1fr)]">
                                <div className="aspect-square overflow-hidden rounded-lg border border-hud-border-secondary bg-hud-bg-primary/70">
                                    <MusicArtwork
                                        imageUrl={detail?.playlist.cover_image_url ?? selectedPlaylist?.cover_image_url ?? null}
                                        seed={detail?.playlist.title ?? selectedPlaylist?.title ?? 'tidal-playlist'}
                                        label={detail?.playlist.title ?? selectedPlaylist?.title ?? 'TIDAL'}
                                    />
                                </div>

                                <div className="min-w-0">
                                    <div className="flex flex-wrap gap-2">
                                        <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-[11px] uppercase tracking-[0.2em] text-hud-text-muted">
                                            TIDAL
                                        </span>
                                        <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-[11px] uppercase tracking-[0.2em] text-hud-text-muted">
                                            {playableItems.length} playable
                                        </span>
                                        {excludedTrackCount > 0 && (
                                            <span className="rounded-full border border-hud-accent-warning/40 px-3 py-1 text-[11px] uppercase tracking-[0.2em] text-hud-accent-warning">
                                                {excludedTrackCount} without TIDAL id
                                            </span>
                                        )}
                                    </div>
                                    <h2 className="mt-5 truncate text-3xl font-semibold text-hud-text-primary">
                                        {detail?.playlist.title ?? selectedPlaylist?.title ?? 'Select a playlist'}
                                    </h2>
                                    <p className="mt-2 text-sm text-hud-text-secondary">
                                        {activeItem
                                            ? `${currentIndex === null ? '-' : currentIndex + 1}. ${activeItem.title} · ${activeItem.subtitle}`
                                            : 'No TIDAL track is loaded in the SDK player.'}
                                    </p>
                                    <p className="mt-4 rounded-lg border border-hud-border-secondary bg-hud-bg-primary/70 px-3 py-2 text-xs leading-5 text-hud-text-muted">
                                        {snapshotSummary(snapshot)}
                                    </p>

                                    <div className="mt-5 flex flex-wrap items-center gap-3">
                                        <Button
                                            type="button"
                                            onClick={() => void playAt(0)}
                                            disabled={!canPlay}
                                            glow
                                        >
                                            <Play size={18} />
                                            Play Playlist
                                        </Button>
                                        <Button
                                            type="button"
                                            variant="outline"
                                            onClick={() => void handleResume()}
                                            disabled={!session?.userId || isStarting}
                                        >
                                            <Play size={17} />
                                            Resume
                                        </Button>
                                        <Button
                                            type="button"
                                            variant="outline"
                                            onClick={() => void handlePause()}
                                            disabled={!session?.userId}
                                        >
                                            <Pause size={17} />
                                            Pause
                                        </Button>
                                        <Button
                                            type="button"
                                            variant="outline"
                                            onClick={() => void handleNext()}
                                            disabled={!canPlay || (currentIndex !== null && currentIndex + 1 >= playableItems.length)}
                                        >
                                            <SkipForward size={17} />
                                            Next
                                        </Button>
                                        <Button
                                            type="button"
                                            variant="ghost"
                                            onClick={() => void handleStop()}
                                            disabled={!session?.userId}
                                        >
                                            <Square size={17} />
                                            Stop
                                        </Button>
                                    </div>

                                    <label className="mt-5 flex max-w-sm items-center gap-3 text-sm text-hud-text-secondary">
                                        <Volume2 size={18} />
                                        <input
                                            type="range"
                                            min="0"
                                            max="1"
                                            step="0.01"
                                            value={volume}
                                            onChange={(event) => void handleVolumeChange(Number(event.currentTarget.value))}
                                            className="min-w-0 flex-1 accent-hud-accent-primary"
                                        />
                                        <span className="w-12 text-right text-xs text-hud-text-muted">
                                            {Math.round(volume * 100)}%
                                        </span>
                                    </label>
                                </div>
                            </div>
                        </HudCard>

                        <HudCard
                            title="Stream Event Log"
                            subtitle={`${logs.length} recent events`}
                            action={
                                <Button type="button" variant="ghost" size="sm" onClick={() => setLogs([])}>
                                    Clear
                                </Button>
                            }
                        >
                            <div className="max-h-64 overflow-auto rounded-lg border border-hud-border-secondary">
                                {logs.map((entry) => (
                                    <div
                                        key={entry.id}
                                        className="grid gap-3 border-b border-hud-border-secondary bg-hud-bg-primary/60 px-4 py-3 text-xs last:border-b-0 md:grid-cols-[90px_80px_minmax(0,1fr)]"
                                    >
                                        <span className="text-hud-text-muted">{entry.createdAt}</span>
                                        <span
                                            className={
                                                entry.level === 'success'
                                                    ? 'text-hud-accent-primary'
                                                    : entry.level === 'error'
                                                        ? 'text-hud-accent-danger'
                                                        : 'text-hud-text-secondary'
                                            }
                                        >
                                            {entry.level}
                                        </span>
                                        <span className="min-w-0 break-words text-hud-text-secondary">
                                            {entry.message}
                                        </span>
                                    </div>
                                ))}
                                {logs.length === 0 && (
                                    <div className="p-4 text-sm text-hud-text-secondary">
                                        Event log is empty.
                                    </div>
                                )}
                            </div>
                        </HudCard>
                    </div>
                </section>

                <HudCard
                    title="Tracks"
                    subtitle={isLoadingDetail ? 'Loading selected playlist' : `${playableItems.length} SDK queue tracks`}
                >
                    {isLoadingDetail ? (
                        <div className="flex items-center gap-3 text-sm text-hud-text-secondary">
                            <RefreshCw size={16} className="animate-spin" />
                            Loading TIDAL playlist detail
                        </div>
                    ) : (
                        <div className="divide-y divide-hud-border-secondary overflow-hidden rounded-lg border border-hud-border-secondary">
                            {playableItems.map((item, index) => {
                                const tidalTrackId = resolveTidalTrackId(item)
                                const active = index === currentIndex

                                return (
                                    <div
                                        key={`${item.id}-${tidalTrackId}`}
                                        className={`grid gap-4 p-4 md:grid-cols-[52px_minmax(0,1.3fr)_minmax(160px,0.7fr)_auto] ${
                                            active ? 'bg-hud-accent-primary/10' : 'bg-hud-bg-primary/70'
                                        }`}
                                    >
                                        <button
                                            type="button"
                                            onClick={() => void playAt(index)}
                                            disabled={isStarting}
                                            className="flex h-12 w-12 items-center justify-center rounded-lg border border-hud-border-secondary text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-accent-primary disabled:cursor-not-allowed disabled:opacity-50"
                                            aria-label={`Play ${item.title}`}
                                        >
                                            {active ? <CheckCircle2 size={18} /> : <Play size={18} />}
                                        </button>
                                        <div className="min-w-0">
                                            <p className="truncate font-medium text-hud-text-primary">
                                                {index + 1}. {item.title}
                                            </p>
                                            <p className="mt-1 truncate text-sm text-hud-text-secondary">
                                                {item.subtitle}
                                            </p>
                                        </div>
                                        <div className="min-w-0">
                                            <p className="truncate text-sm text-hud-text-secondary">
                                                {item.albumTitle ?? 'Single'}
                                            </p>
                                            <p className="mt-1 truncate text-xs uppercase tracking-[0.18em] text-hud-accent-primary">
                                                tidal:{tidalTrackId}
                                            </p>
                                        </div>
                                        <span className="flex items-center text-sm text-hud-text-muted md:justify-end">
                                            {formatDuration(item.durationMs) ?? '--:--'}
                                        </span>
                                    </div>
                                )
                            })}
                            {!isLoadingDetail && playableItems.length === 0 && (
                                <div className="p-4 text-sm leading-6 text-hud-text-secondary">
                                    The selected PMS playlist has no tracks with a resolvable TIDAL track id.
                                </div>
                            )}
                        </div>
                    )}
                </HudCard>
            </div>
        </main>
    )
}

export default TidalPlaylistPlaybackTestPage

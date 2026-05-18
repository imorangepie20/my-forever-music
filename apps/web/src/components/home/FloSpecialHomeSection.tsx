import { useEffect, useMemo, useState } from 'react'
import { ArrowRight, RefreshCw } from 'lucide-react'
import { Link } from 'react-router-dom'
import PlaylistFeatureCard from '@/components/music/PlaylistFeatureCard'
import { usePlayback } from '@/contexts/PlaybackContext'
import { buildEmsPlaylistDetailPath, toEmsTrackPlaybackItem } from '@/lib/emsPlayback'
import { ApiError, fetchEmsCollectedPlaylistDetail, fetchEmsFloSpecial } from '@/services/api'
import type { EmsCollectionPlaylistItem, EmsFloSpecialSection } from '@/types/api'

const HOME_FLO_LIMIT = 6

const openExternal = (url?: string | null) => {
    if (!url) {
        return
    }
    window.open(url, '_blank', 'noopener,noreferrer')
}

const FloSpecialHomeSection = () => {
    const playback = usePlayback()
    const [sections, setSections] = useState<EmsFloSpecialSection[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [preparingPlaylistId, setPreparingPlaylistId] = useState<number | null>(null)

    useEffect(() => {
        const controller = new AbortController()
        setIsLoading(true)
        setError(null)

        fetchEmsFloSpecial(controller.signal, HOME_FLO_LIMIT)
            .then((response) => setSections(response.sections))
            .catch((requestError: unknown) => {
                if (requestError instanceof DOMException && requestError.name === 'AbortError') {
                    return
                }
                const message = requestError instanceof ApiError
                    ? requestError.message
                    : 'FLO Special 코너를 불러올 수 없습니다.'
                setError(message)
            })
            .finally(() => setIsLoading(false))

        return () => controller.abort()
    }, [])

    const playlists = useMemo(
        () => sections.flatMap((section) => section.playlists.map((playlist) => ({ sectionTitle: section.title, playlist }))).slice(0, HOME_FLO_LIMIT),
        [sections],
    )

    const handlePlayPlaylist = async (playlist: EmsCollectionPlaylistItem) => {
        setPreparingPlaylistId(playlist.id)
        setError(null)
        try {
            const detail = await fetchEmsCollectedPlaylistDetail(playlist.id)
            const items = detail.tracks.map((track) => toEmsTrackPlaybackItem(track, detail.playlist.title))
            if (items.length === 0) {
                setError('FLO Special 플레이리스트에 저장된 트랙이 없습니다.')
                return
            }
            await playback.playQueue(items, 0)
        } catch (requestError: unknown) {
            const message = requestError instanceof ApiError
                ? requestError.message
                : requestError instanceof Error
                    ? requestError.message
                    : 'FLO Special 플레이리스트를 재생할 수 없습니다.'
            setError(message)
        } finally {
            setPreparingPlaylistId(null)
        }
    }

    if (!isLoading && !error && playlists.length === 0) {
        return null
    }

    return (
        <section className="space-y-4">
            <header className="flex flex-wrap items-end justify-between gap-3">
                <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.24em] text-hud-accent-primary">
                        FLO Special in EMS
                    </p>
                    <h2 className="mt-2 text-lg font-semibold text-hud-text-primary">FLO 특집 코너</h2>
                    <p className="mt-1 text-sm text-hud-text-secondary">
                        FLO 공개 큐레이션을 EMS에 저장해 바로 탐색하고 재생합니다.
                    </p>
                </div>
                <Link
                    to="/ems"
                    className="inline-flex items-center gap-1 text-xs font-semibold text-hud-accent-primary transition-hud hover:text-hud-accent-primary/80"
                >
                    EMS에서 더 보기
                    <ArrowRight size={14} />
                </Link>
            </header>

            {isLoading && (
                <span className="inline-flex items-center gap-2 text-xs text-hud-text-muted">
                    <RefreshCw size={14} className="animate-spin" />
                    FLO Special 로딩 중
                </span>
            )}

            {error && (
                <div className="rounded-2xl border border-hud-accent-warning/40 bg-hud-accent-warning/10 p-3 text-xs leading-5 text-hud-text-secondary">
                    {error}
                </div>
            )}

            {playlists.length > 0 && (
                <div className="grid gap-4 xl:grid-cols-2">
                    {playlists.slice(0, 4).map(({ sectionTitle, playlist }) => (
                        <PlaylistFeatureCard
                            key={playlist.id}
                            title={playlist.title}
                            sourcePlatform={playlist.source_platform}
                            curator={playlist.curator || 'FLO Special'}
                            trackCount={playlist.track_count}
                            description={playlist.description || sectionTitle}
                            supportingText={sectionTitle}
                            imageUrl={playlist.cover_image_url}
                            detailPath={buildEmsPlaylistDetailPath(playlist.id)}
                            actionLabel="열기"
                            isPlayLoading={preparingPlaylistId === playlist.id}
                            onPlay={() => void handlePlayPlaylist(playlist)}
                            onOpenExternal={() => openExternal(playlist.platform_external_url)}
                        />
                    ))}
                </div>
            )}
        </section>
    )
}

export default FloSpecialHomeSection

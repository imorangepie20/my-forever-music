import { useEffect, useMemo, useRef, useState } from 'react'
import { Pause, Play, Volume2, VolumeX } from 'lucide-react'
import { Link } from 'react-router-dom'
import BarsVisualizer from '@/components/visualizer/animations/BarsVisualizer'
import MusicArtwork from '@/components/music/MusicArtwork'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import { usePlayback } from '@/contexts/PlaybackContext'
import { useHeroTrack } from '@/hooks/useHeroTrack'
import { usePreviewAudioAnalyser } from '@/hooks/usePreviewAudioAnalyser'
import type { PlaybackMediaItem } from '@/lib/musicPlayback'
import type { HeroTrackResponse } from '@/types/api'

const formatTime = (seconds: number) => {
    if (!Number.isFinite(seconds) || seconds <= 0) {
        return '0:00'
    }
    const total = Math.round(seconds)
    const m = Math.floor(total / 60)
    const s = total % 60
    return `${m}:${String(s).padStart(2, '0')}`
}

const toPlaybackItem = (hero: HeroTrackResponse): PlaybackMediaItem => ({
    id: hero.spotify_track_id ?? hero.external_track_id,
    kind: 'track',
    title: hero.title,
    subtitle: hero.album_title ? `${hero.artist_name} · ${hero.album_title}` : hero.artist_name,
    sourcePlatform: hero.source_platform,
    spotifyTrackId: hero.spotify_track_id,
    externalTrackId: hero.external_track_id,
    imageUrl: hero.image_url,
    albumTitle: hero.album_title,
    externalUrl: hero.platform_external_url,
    durationMs: hero.duration_ms,
    previewUrl: hero.preview_url,
})

const HeroEqBanner = () => {
    const { session } = useAuthSession()
    const heroState = useHeroTrack(session?.userId ?? null)
    const audioRef = useRef<HTMLAudioElement | null>(null)
    const [isMuted, setIsMuted] = useState(true)
    const [isPlaying, setIsPlaying] = useState(false)
    const [isEnded, setIsEnded] = useState(false)
    const [position, setPosition] = useState(0)
    const [duration, setDuration] = useState(0)
    const playback = usePlayback()

    const track = heroState.status === 'ready' ? heroState.track : null
    const previewUrl = track?.preview_url ?? null
    const analyser = usePreviewAudioAnalyser(previewUrl, audioRef.current)

    useEffect(() => {
        const audio = audioRef.current
        if (!audio || !previewUrl) {
            return
        }

        const handlePlay = () => setIsPlaying(true)
        const handlePause = () => setIsPlaying(false)
        const handleEnded = () => {
            setIsPlaying(false)
            setIsEnded(true)
        }
        const handleTimeUpdate = () => setPosition(audio.currentTime)
        const handleDurationChange = () => setDuration(audio.duration || 0)

        audio.addEventListener('play', handlePlay)
        audio.addEventListener('pause', handlePause)
        audio.addEventListener('ended', handleEnded)
        audio.addEventListener('timeupdate', handleTimeUpdate)
        audio.addEventListener('durationchange', handleDurationChange)

        audio.muted = true
        audio.currentTime = 0
        setIsMuted(true)
        setIsEnded(false)
        setIsPlaying(false)
        setPosition(0)
        setDuration(audio.duration || 0)
        audio.play().catch(() => {
            // Muted autoplay may still be blocked in restrictive browsers — surface as
            // an obvious "press play" affordance via the existing isPlaying=false state.
        })

        return () => {
            audio.removeEventListener('play', handlePlay)
            audio.removeEventListener('pause', handlePause)
            audio.removeEventListener('ended', handleEnded)
            audio.removeEventListener('timeupdate', handleTimeUpdate)
            audio.removeEventListener('durationchange', handleDurationChange)
        }
    }, [previewUrl])

    const handlePrimary = () => {
        const audio = audioRef.current
        if (!audio) {
            return
        }
        if (isEnded) {
            audio.currentTime = 0
            setIsEnded(false)
            audio.muted = false
            setIsMuted(false)
            void audio.play().catch(() => undefined)
            return
        }
        if (audio.paused) {
            audio.muted = false
            setIsMuted(false)
            void audio.play().catch(() => undefined)
            return
        }
        if (isMuted) {
            audio.muted = false
            setIsMuted(false)
            return
        }
        audio.pause()
    }

    const handlePlayFullTrack = () => {
        if (!track) {
            return
        }
        const audio = audioRef.current
        if (audio) {
            audio.pause()
        }
        const item = toPlaybackItem(track)
        void playback.playItem(item).catch(() => undefined)
    }

    const primaryLabel = useMemo(() => {
        if (isEnded) {
            return 'Replay preview'
        }
        if (!isPlaying) {
            return 'Listen preview'
        }
        if (isMuted) {
            return 'Unmute preview'
        }
        return 'Pause preview'
    }, [isEnded, isPlaying, isMuted])

    if (heroState.status === 'loading') {
        return (
            <section className="relative h-72 animate-pulse rounded-3xl border border-hud-border-secondary bg-hud-bg-primary/60" />
        )
    }
    if (heroState.status === 'empty' || heroState.status === 'error' || !track) {
        return null
    }

    const trackKey = `${track.source_platform}-${track.external_track_id}`
    const totalDisplay = duration > 0 ? duration : 30
    const progressRatio = totalDisplay > 0 ? Math.min(1, position / totalDisplay) : 0

    return (
        <section className="relative overflow-hidden rounded-3xl border border-white/10 bg-black text-white shadow-[0_30px_80px_-30px_rgba(0,0,0,0.7)]">
            {track.image_url ? (
                <img
                    src={track.image_url}
                    alt=""
                    aria-hidden
                    className="absolute inset-0 h-full w-full scale-110 object-cover opacity-50 blur-2xl"
                />
            ) : null}
            <div className="absolute inset-0 bg-gradient-to-br from-black/70 via-black/40 to-black/70" />

            <div className="relative grid gap-8 px-6 py-8 sm:px-10 md:grid-cols-[240px_1fr] md:gap-10 md:py-10">
                <div className="mx-auto h-56 w-56 overflow-hidden rounded-2xl border border-white/10 bg-black/40 shadow-[0_30px_60px_-20px_rgba(0,0,0,0.8)] md:mx-0 md:h-60 md:w-60">
                    <MusicArtwork
                        imageUrl={track.image_url}
                        seed={trackKey}
                        label={track.title}
                    />
                </div>
                <div className="flex flex-col justify-between gap-6">
                    <div>
                        {track.source_label && (
                            <p className="text-[11px] uppercase tracking-[0.32em] text-white/60">{track.source_label}</p>
                        )}
                        <h2 className="mt-3 text-3xl font-semibold leading-tight tracking-tight md:text-4xl">{track.title}</h2>
                        <p className="mt-2 text-base text-white/70">
                            {track.artist_name}
                            {track.album_title ? <span className="text-white/40"> · {track.album_title}</span> : null}
                        </p>
                    </div>
                    <div className="flex flex-wrap items-center gap-3">
                        <button
                            type="button"
                            onClick={handlePrimary}
                            className="inline-flex h-12 items-center gap-2 rounded-full bg-white px-5 text-sm font-semibold text-black shadow-md transition hover:bg-white/90"
                            aria-label={primaryLabel}
                        >
                            {!isPlaying || isEnded ? <Play size={18} /> : isMuted ? <VolumeX size={18} /> : <Pause size={18} />}
                            <span>{primaryLabel}</span>
                        </button>
                        {isEnded ? (
                            session?.userId ? (
                                <button
                                    type="button"
                                    onClick={handlePlayFullTrack}
                                    className="inline-flex h-12 items-center gap-2 rounded-full border border-white/40 px-5 text-sm font-semibold text-white transition hover:bg-white/10"
                                >
                                    전체 듣기
                                </button>
                            ) : (
                                <Link
                                    to="/signin"
                                    className="inline-flex h-12 items-center gap-2 rounded-full border border-white/40 px-5 text-sm font-semibold text-white transition hover:bg-white/10"
                                >
                                    로그인하고 전체 듣기
                                </Link>
                            )
                        ) : null}
                        <span className="ml-auto inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/5 px-3 py-1 text-xs text-white/70">
                            {isMuted ? <VolumeX size={14} /> : <Volume2 size={14} />}
                            {isMuted ? 'muted' : 'sound on'}
                        </span>
                    </div>
                </div>
            </div>

            <div className="relative px-6 pb-6 sm:px-10">
                <div className="h-28 w-full">
                    <BarsVisualizer analyser={analyser} accentHex="#ffffff" isPlaying={isPlaying && !isMuted} />
                </div>
                <div className="mt-3 flex items-center gap-3 text-xs font-mono text-white/60">
                    <span className="w-10 text-right">{formatTime(position)}</span>
                    <div className="relative h-1 flex-1 overflow-hidden rounded-full bg-white/15">
                        <div
                            className="absolute inset-y-0 left-0 bg-white/80 transition-[width] duration-100"
                            style={{ width: `${progressRatio * 100}%` }}
                        />
                    </div>
                    <span className="w-10">{formatTime(totalDisplay)}</span>
                </div>
            </div>

            <audio
                ref={audioRef}
                src={previewUrl ?? undefined}
                preload="auto"
                playsInline
                className="hidden"
            />
        </section>
    )
}

export default HeroEqBanner

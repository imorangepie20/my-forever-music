import { useEffect, useMemo, useRef, useState } from 'react'
import { ApiError, fetchPlaybackCredentials } from '@/services/api'
import type { VisualizerSample } from '@/components/visualizer/Visualizer'
import {
    buildBars,
    type SpotifyAudioAnalysis,
} from '@/components/visualizer/spotifyAudioAnalysis'

interface SpotifyAudioAnalysisAdapterInput {
    userId: string | null | undefined
    spotifyTrackId: string | null | undefined
    positionMs: number
}

interface SpotifyAudioAnalysisAdapterResult {
    heightsAt: ((sample: VisualizerSample) => number[]) | null
    ready: boolean
    error: string | null
}

const SPOTIFY_API_BASE = 'https://api.spotify.com/v1'

/**
 * 현재 재생 중인 Spotify 트랙의 audio-analysis 를 가져와서 Visualizer 에 줄
 * heightsAt 함수를 만든다.
 *
 * 시스템 정책상 silent fallback 을 만들지 않고, 401/403/네트워크 실패는 error
 * 문자열로 surface 한다 (페이지에서 안내 메시지로 보여줌). analysis 가 아직
 * 도착하지 않은 동안은 heightsAt=null 을 반환 → Visualizer 의 procedural
 * envelope 가 자연스럽게 자리를 채운다.
 */
export function useSpotifyAudioAnalysisAdapter(
    input: SpotifyAudioAnalysisAdapterInput,
): SpotifyAudioAnalysisAdapterResult {
    const { userId, spotifyTrackId, positionMs } = input
    const [analysis, setAnalysis] = useState<SpotifyAudioAnalysis | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [activeTrackId, setActiveTrackId] = useState<string | null>(null)
    const positionRef = useRef(positionMs)

    // positionMs 가 매 tick 마다 바뀌어도 heightsAt 의 identity 는 유지되어야 raf 루프가 안 깨진다.
    useEffect(() => {
        positionRef.current = positionMs
    }, [positionMs])

    useEffect(() => {
        if (!userId || !spotifyTrackId) {
            setAnalysis(null)
            setActiveTrackId(null)
            setError(null)
            return
        }
        let cancelled = false
        const controller = new AbortController()
        const run = async () => {
            setError(null)
            try {
                const credentials = await fetchPlaybackCredentials(userId, 'spotify', controller.signal)
                if (!credentials.access_token) {
                    throw new Error('Spotify access token missing for audio-analysis request.')
                }
                const response = await fetch(
                    `${SPOTIFY_API_BASE}/audio-analysis/${encodeURIComponent(spotifyTrackId)}`,
                    {
                        headers: { Authorization: `Bearer ${credentials.access_token}` },
                        signal: controller.signal,
                    },
                )
                if (!response.ok) {
                    if (response.status === 401 || response.status === 403) {
                        throw new Error(
                            `Spotify audio-analysis is not authorized for this app (status ${response.status}). 신규 앱은 deprecation 으로 endpoint 사용이 막힙니다.`,
                        )
                    }
                    if (response.status === 404) {
                        throw new Error(`Spotify audio-analysis 가 이 트랙에 없습니다 (404).`)
                    }
                    throw new Error(`Spotify audio-analysis 요청 실패 (status ${response.status}).`)
                }
                const payload = (await response.json()) as SpotifyAudioAnalysis
                if (cancelled) {
                    return
                }
                setAnalysis(payload)
                setActiveTrackId(spotifyTrackId)
            } catch (requestError: unknown) {
                if (cancelled || (requestError instanceof DOMException && requestError.name === 'AbortError')) {
                    return
                }
                const message = requestError instanceof ApiError
                    ? requestError.message
                    : requestError instanceof Error
                        ? requestError.message
                        : 'Spotify audio-analysis 요청에 실패했습니다.'
                setAnalysis(null)
                setActiveTrackId(null)
                setError(message)
            }
        }
        void run()
        return () => {
            cancelled = true
            controller.abort()
        }
    }, [userId, spotifyTrackId])

    const heightsAt = useMemo(() => {
        if (!analysis || activeTrackId !== spotifyTrackId) {
            return null
        }
        return (sample: VisualizerSample) => buildBars({
            analysis,
            positionSeconds: positionRef.current / 1000,
            count: sample.count,
        })
    }, [analysis, activeTrackId, spotifyTrackId])

    return {
        heightsAt,
        ready: heightsAt !== null,
        error,
    }
}

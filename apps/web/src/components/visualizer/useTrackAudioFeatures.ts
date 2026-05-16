import { useEffect, useState } from 'react'
import { ApiError, fetchPmsTrackAudioFeatures } from '@/services/api'
import type { TrackAudioFeatures } from './proceduralEnvelope'

interface UseTrackAudioFeaturesInput {
    userId: string | null
    audioFeatureTrackId: string | null
}

interface UseTrackAudioFeaturesResult {
    features: TrackAudioFeatures | null
    /** true when PMS has filled BPM/energy/valence for this track */
    filled: boolean
    /** non-blocking diagnostic — visualizer just falls back to mode preset on error */
    error: string | null
}

/**
 * Phase 1 옵션 A 의 audio feature 조회 hook.
 *
 * - `audioFeatureTrackId` (보통 Spotify track id) 를 가진 currentItem 에 한해 PMS endpoint 호출
 * - id 가 없거나 PMS 에 row 가 없으면 features=null 반환 → procedural envelope 는 mode preset 으로 동작
 * - 호출 자체는 audio 와 무관한 metadata fetch — 재생 path 에 영향 없음
 */
export function useTrackAudioFeatures(input: UseTrackAudioFeaturesInput): UseTrackAudioFeaturesResult {
    const { userId, audioFeatureTrackId } = input
    const [features, setFeatures] = useState<TrackAudioFeatures | null>(null)
    const [filled, setFilled] = useState(false)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        setFeatures(null)
        setFilled(false)
        setError(null)

        if (!userId || !audioFeatureTrackId) {
            return
        }

        const controller = new AbortController()
        let cancelled = false

        fetchPmsTrackAudioFeatures(userId, audioFeatureTrackId, controller.signal)
            .then((response) => {
                if (cancelled) {
                    return
                }
                if (response.audio_features_filled) {
                    setFeatures({
                        tempo: response.tempo,
                        energy: response.energy,
                        valence: response.valence,
                    })
                    setFilled(true)
                } else {
                    setFeatures(null)
                    setFilled(false)
                }
            })
            .catch((ex) => {
                if (cancelled || (ex instanceof DOMException && ex.name === 'AbortError')) {
                    return
                }
                const message = ex instanceof ApiError
                    ? `audio-features API ${ex.status}: ${ex.message}`
                    : ex instanceof Error
                        ? ex.message
                        : 'audio-features 조회 실패'
                setError(message)
            })

        return () => {
            cancelled = true
            controller.abort()
        }
    }, [userId, audioFeatureTrackId])

    return { features, filled, error }
}

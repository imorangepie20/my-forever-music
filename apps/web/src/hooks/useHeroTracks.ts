import { useEffect, useState } from 'react'
import { ApiError, fetchHeroTracks } from '@/services/api'
import type { HeroTrackResponse } from '@/types/api'

export type HeroTracksState =
    | { status: 'loading'; tracks: []; error: null }
    | { status: 'ready'; tracks: HeroTrackResponse[]; error: null }
    | { status: 'empty'; tracks: []; error: null }
    | { status: 'error'; tracks: []; error: string }

export function useHeroTracks(
    userId: string | null | undefined,
    limit = 5,
): HeroTracksState {
    const [state, setState] = useState<HeroTracksState>({ status: 'loading', tracks: [], error: null })

    useEffect(() => {
        const controller = new AbortController()
        setState({ status: 'loading', tracks: [], error: null })

        fetchHeroTracks(userId, limit, controller.signal)
            .then((tracks) => {
                if (controller.signal.aborted) {
                    return
                }
                if (tracks.length > 0) {
                    setState({ status: 'ready', tracks, error: null })
                } else {
                    setState({ status: 'empty', tracks: [], error: null })
                }
            })
            .catch((error: unknown) => {
                if (error instanceof DOMException && error.name === 'AbortError') {
                    return
                }
                const message = error instanceof ApiError
                    ? error.message
                    : error instanceof Error
                        ? error.message
                        : 'Unable to load hero tracks.'
                setState({ status: 'error', tracks: [], error: message })
            })

        return () => controller.abort()
    }, [userId, limit])

    return state
}

import { useEffect, useState } from 'react'
import { ApiError, fetchHeroTrack } from '@/services/api'
import type { HeroTrackResponse } from '@/types/api'

export type HeroTrackState =
    | { status: 'loading'; track: null; error: null }
    | { status: 'ready'; track: HeroTrackResponse; error: null }
    | { status: 'empty'; track: null; error: null }
    | { status: 'error'; track: null; error: string }

export function useHeroTrack(userId: string | null | undefined): HeroTrackState {
    const [state, setState] = useState<HeroTrackState>({ status: 'loading', track: null, error: null })

    useEffect(() => {
        const controller = new AbortController()
        setState({ status: 'loading', track: null, error: null })

        fetchHeroTrack(userId, controller.signal)
            .then((track) => {
                if (controller.signal.aborted) {
                    return
                }
                if (track) {
                    setState({ status: 'ready', track, error: null })
                } else {
                    setState({ status: 'empty', track: null, error: null })
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
                        : 'Unable to load hero track.'
                setState({ status: 'error', track: null, error: message })
            })

        return () => controller.abort()
    }, [userId])

    return state
}

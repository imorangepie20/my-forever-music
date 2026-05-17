import { useEffect, useState } from 'react'
import { ApiError, fetchGmsPlaylistPreview } from '@/services/api'
import type { GmsPlaylistPreviewItem } from '@/types/api'

export type GmsRecommendedPlaylistsState =
    | { status: 'anonymous'; playlists: []; error: null }
    | { status: 'loading'; playlists: []; error: null }
    | { status: 'ready'; playlists: GmsPlaylistPreviewItem[]; error: null }
    | { status: 'cold_start'; playlists: []; error: string }
    | { status: 'empty'; playlists: []; error: null }
    | { status: 'error'; playlists: []; error: string }

export function useGmsRecommendedPlaylists(
    userId: string | null | undefined,
    limit = 5,
): GmsRecommendedPlaylistsState {
    const [state, setState] = useState<GmsRecommendedPlaylistsState>(() =>
        userId ? { status: 'loading', playlists: [], error: null } : { status: 'anonymous', playlists: [], error: null }
    )

    useEffect(() => {
        if (!userId) {
            setState({ status: 'anonymous', playlists: [], error: null })
            return
        }

        const controller = new AbortController()
        setState({ status: 'loading', playlists: [], error: null })

        fetchGmsPlaylistPreview(userId, limit, controller.signal)
            .then((response) => {
                if (controller.signal.aborted) {
                    return
                }
                const candidates = response.candidates ?? []
                if (candidates.length === 0) {
                    setState({ status: 'empty', playlists: [], error: null })
                    return
                }
                setState({ status: 'ready', playlists: candidates.slice(0, limit), error: null })
            })
            .catch((error: unknown) => {
                if (error instanceof DOMException && error.name === 'AbortError') {
                    return
                }
                if (error instanceof ApiError && error.status === 409) {
                    setState({ status: 'cold_start', playlists: [], error: error.message })
                    return
                }
                const message = error instanceof ApiError
                    ? error.message
                    : error instanceof Error
                        ? error.message
                        : 'Unable to load GMS recommendations.'
                setState({ status: 'error', playlists: [], error: message })
            })

        return () => controller.abort()
    }, [userId, limit])

    return state
}

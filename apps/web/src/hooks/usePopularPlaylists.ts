import { useEffect, useState } from 'react'
import { ApiError, fetchPopularPlaylists } from '@/services/api'
import type { PopularPlaylistResponse } from '@/types/api'

export type PopularPlaylistsState =
    | { status: 'loading'; playlists: []; error: null }
    | { status: 'ready'; playlists: PopularPlaylistResponse[]; error: null }
    | { status: 'empty'; playlists: []; error: null }
    | { status: 'error'; playlists: []; error: string }

export function usePopularPlaylists(limit = 6): PopularPlaylistsState {
    const [state, setState] = useState<PopularPlaylistsState>({ status: 'loading', playlists: [], error: null })

    useEffect(() => {
        const controller = new AbortController()
        setState({ status: 'loading', playlists: [], error: null })

        fetchPopularPlaylists(limit, controller.signal)
            .then((playlists) => {
                if (controller.signal.aborted) {
                    return
                }
                if (playlists.length > 0) {
                    setState({ status: 'ready', playlists, error: null })
                } else {
                    setState({ status: 'empty', playlists: [], error: null })
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
                        : 'Unable to load popular playlists.'
                setState({ status: 'error', playlists: [], error: message })
            })

        return () => controller.abort()
    }, [limit])

    return state
}

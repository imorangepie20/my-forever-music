import { useEffect, useState } from 'react'
import { ApiError, fetchMelonHot100 } from '@/services/api'
import type { MelonChartTrack } from '@/types/api'

export type MelonHot100State =
    | { status: 'loading'; tracks: []; snapshotAt: null; error: null }
    | { status: 'ready'; tracks: MelonChartTrack[]; snapshotAt: string | null; error: null }
    | { status: 'empty'; tracks: []; snapshotAt: null; error: null }
    | { status: 'error'; tracks: []; snapshotAt: null; error: string }

export function useMelonHot100(limit: number | null, full = false): MelonHot100State {
    const [state, setState] = useState<MelonHot100State>({ status: 'loading', tracks: [], snapshotAt: null, error: null })

    useEffect(() => {
        const controller = new AbortController()
        setState({ status: 'loading', tracks: [], snapshotAt: null, error: null })

        fetchMelonHot100(limit, full, controller.signal)
            .then(({ snapshotAt, tracks }) => {
                if (controller.signal.aborted) {
                    return
                }
                if (tracks.length === 0) {
                    setState({ status: 'empty', tracks: [], snapshotAt: null, error: null })
                    return
                }
                setState({ status: 'ready', tracks, snapshotAt, error: null })
            })
            .catch((error: unknown) => {
                if (error instanceof DOMException && error.name === 'AbortError') {
                    return
                }
                const message = error instanceof ApiError
                    ? error.message
                    : error instanceof Error
                        ? error.message
                        : 'Unable to load Melon chart.'
                setState({ status: 'error', tracks: [], snapshotAt: null, error: message })
            })

        return () => controller.abort()
    }, [limit, full])

    return state
}

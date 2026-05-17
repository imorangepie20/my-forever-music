import { useCallback, useEffect, useState } from 'react'
import { ApiError, fetchUserTrackLikeState, toggleUserTrackLike } from '@/services/api'

export interface TrackLikeIdentity {
    userId: string | null | undefined
    sourcePlatform: string | null | undefined
    externalTrackId: string | null | undefined
}

export interface TrackLikeSnapshot {
    title?: string | null
    artistName?: string | null
    albumTitle?: string | null
    imageUrl?: string | null
    spotifyTrackId?: string | null
    platformExternalUrl?: string | null
}

export interface TrackLikeController {
    liked: boolean
    loading: boolean
    available: boolean
    error: string | null
    toggle: () => Promise<void>
}

const canQuery = (identity: TrackLikeIdentity): identity is {
    userId: string
    sourcePlatform: string
    externalTrackId: string
} =>
    Boolean(identity.userId && identity.sourcePlatform && identity.externalTrackId)

export function useTrackLike(
    identity: TrackLikeIdentity,
    snapshot: TrackLikeSnapshot,
): TrackLikeController {
    const available = canQuery(identity)
    const [liked, setLiked] = useState(false)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        if (!available) {
            setLiked(false)
            setError(null)
            return
        }
        const controller = new AbortController()
        setLoading(true)
        setError(null)
        fetchUserTrackLikeState(
            identity.userId,
            identity.sourcePlatform,
            identity.externalTrackId,
            controller.signal,
        )
            .then((response) => {
                if (controller.signal.aborted) {
                    return
                }
                setLiked(Boolean(response.liked))
            })
            .catch((cause: unknown) => {
                if (cause instanceof DOMException && cause.name === 'AbortError') {
                    return
                }
                const message = cause instanceof ApiError
                    ? cause.message
                    : cause instanceof Error
                        ? cause.message
                        : 'Unable to load like state.'
                setError(message)
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false)
                }
            })

        return () => controller.abort()
    }, [available, identity.userId, identity.sourcePlatform, identity.externalTrackId])

    const toggle = useCallback(async () => {
        if (!available) {
            return
        }
        const previous = liked
        setLiked(!previous)
        setError(null)
        try {
            const response = await toggleUserTrackLike({
                user_id: identity.userId,
                source_platform: identity.sourcePlatform,
                external_track_id: identity.externalTrackId,
                title: snapshot.title ?? null,
                artist_name: snapshot.artistName ?? null,
                album_title: snapshot.albumTitle ?? null,
                image_url: snapshot.imageUrl ?? null,
                spotify_track_id: snapshot.spotifyTrackId ?? null,
                platform_external_url: snapshot.platformExternalUrl ?? null,
            })
            setLiked(Boolean(response.liked))
        } catch (cause) {
            setLiked(previous)
            const message = cause instanceof ApiError
                ? cause.message
                : cause instanceof Error
                    ? cause.message
                    : 'Unable to update like state.'
            setError(message)
        }
    }, [available, liked, identity.userId, identity.sourcePlatform, identity.externalTrackId, snapshot.title, snapshot.artistName, snapshot.albumTitle, snapshot.imageUrl, snapshot.spotifyTrackId, snapshot.platformExternalUrl])

    return { liked, loading, available, error, toggle }
}

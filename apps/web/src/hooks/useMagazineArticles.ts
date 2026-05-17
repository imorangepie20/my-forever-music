import { useEffect, useState } from 'react'
import { ApiError, fetchMagazineArticles } from '@/services/api'
import type { MagazineArticleResponse } from '@/types/api'

export type MagazineArticlesState =
    | { status: 'loading'; articles: []; error: null }
    | { status: 'ready'; articles: MagazineArticleResponse[]; error: null }
    | { status: 'empty'; articles: []; error: null }
    | { status: 'error'; articles: []; error: string }

export function useMagazineArticles(limit = 8): MagazineArticlesState {
    const [state, setState] = useState<MagazineArticlesState>({ status: 'loading', articles: [], error: null })

    useEffect(() => {
        const controller = new AbortController()
        setState({ status: 'loading', articles: [], error: null })

        fetchMagazineArticles(limit, controller.signal)
            .then((articles) => {
                if (controller.signal.aborted) {
                    return
                }
                if (articles.length > 0) {
                    setState({ status: 'ready', articles, error: null })
                } else {
                    setState({ status: 'empty', articles: [], error: null })
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
                        : 'Unable to load magazine articles.'
                setState({ status: 'error', articles: [], error: message })
            })

        return () => controller.abort()
    }, [limit])

    return state
}

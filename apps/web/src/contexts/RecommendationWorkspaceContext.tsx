import {
    createContext,
    useContext,
    useEffect,
    useMemo,
    useState,
    type ReactNode,
} from 'react'
import {
    defaultRecommendationWorkspace,
    type RecommendationWorkspaceState,
} from '@/types/workspace'

interface RecommendationWorkspaceContextValue {
    workspace: RecommendationWorkspaceState
    updateWorkspace: (patch: Partial<RecommendationWorkspaceState>) => void
    resetWorkspace: () => void
    seedTrackCount: number
    seedArtistCount: number
    seedGenreCount: number
}

const STORAGE_KEY = 'my-forever-music.recommendation-workspace'

const RecommendationWorkspaceContext = createContext<RecommendationWorkspaceContextValue | null>(null)

const countItems = (value: string) =>
    value
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean).length

const getInitialWorkspace = (): RecommendationWorkspaceState => {
    if (typeof window === 'undefined') {
        return defaultRecommendationWorkspace
    }

    const storedValue = window.localStorage.getItem(STORAGE_KEY)
    if (!storedValue) {
        return defaultRecommendationWorkspace
    }

    try {
        const parsed = JSON.parse(storedValue) as Partial<RecommendationWorkspaceState>
        return {
            ...defaultRecommendationWorkspace,
            ...parsed,
        }
    } catch {
        return defaultRecommendationWorkspace
    }
}

export const RecommendationWorkspaceProvider = ({ children }: { children: ReactNode }) => {
    const [workspace, setWorkspace] = useState<RecommendationWorkspaceState>(getInitialWorkspace)

    useEffect(() => {
        window.localStorage.setItem(STORAGE_KEY, JSON.stringify(workspace))
    }, [workspace])

    const value = useMemo<RecommendationWorkspaceContextValue>(
        () => ({
            workspace,
            updateWorkspace: (patch) => {
                setWorkspace((current) => ({
                    ...current,
                    ...patch,
                }))
            },
            resetWorkspace: () => {
                setWorkspace(defaultRecommendationWorkspace)
            },
            seedTrackCount: countItems(workspace.seedTrackIdsText),
            seedArtistCount: countItems(workspace.seedArtistNamesText),
            seedGenreCount: countItems(workspace.seedGenresText),
        }),
        [workspace],
    )

    return (
        <RecommendationWorkspaceContext.Provider value={value}>
            {children}
        </RecommendationWorkspaceContext.Provider>
    )
}

export const useRecommendationWorkspace = () => {
    const context = useContext(RecommendationWorkspaceContext)

    if (!context) {
        throw new Error('useRecommendationWorkspace must be used within RecommendationWorkspaceProvider')
    }

    return context
}

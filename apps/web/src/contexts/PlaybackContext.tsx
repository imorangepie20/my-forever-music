import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import type { PlaybackMediaItem } from '@/lib/musicPlayback'

interface PlaybackContextValue {
    currentItem: PlaybackMediaItem | null
    playItem: (item: PlaybackMediaItem) => void
    clearItem: () => void
}

const PlaybackContext = createContext<PlaybackContextValue | null>(null)

export const PlaybackProvider = ({ children }: { children: ReactNode }) => {
    const [currentItem, setCurrentItem] = useState<PlaybackMediaItem | null>(null)

    const value = useMemo<PlaybackContextValue>(
        () => ({
            currentItem,
            playItem: (item) => setCurrentItem(item),
            clearItem: () => setCurrentItem(null),
        }),
        [currentItem],
    )

    return <PlaybackContext.Provider value={value}>{children}</PlaybackContext.Provider>
}

export const usePlayback = () => {
    const context = useContext(PlaybackContext)
    if (!context) {
        throw new Error('usePlayback must be used within PlaybackProvider')
    }
    return context
}

export type WorkspaceMood = 'focus' | 'calm' | 'upbeat' | 'melancholy' | 'discovery'
export type WorkspacePlatformId = 'spotify' | 'apple-music' | 'tidal'

export interface RecommendationWorkspaceState {
    preferredPlatformId: WorkspacePlatformId
    userId: string
    playlistId: string
    seedTrackIdsText: string
    seedArtistNamesText: string
    seedGenresText: string
    mood: WorkspaceMood
    energyLevel: number
    familiarityBias: number
    limit: number
    includeExplanations: boolean
}

export const defaultRecommendationWorkspace: RecommendationWorkspaceState = {
    preferredPlatformId: 'spotify',
    userId: 'user-001',
    playlistId: 'playlist-001',
    seedTrackIdsText: 'track-alpha, track-beta',
    seedArtistNamesText: 'Artist One',
    seedGenresText: 'synth-pop, dream-pop',
    mood: 'upbeat',
    energyLevel: 4,
    familiarityBias: 3,
    limit: 5,
    includeExplanations: true,
}

export interface SystemInfoResponse {
    service: string
    status: string
    message: string
    timestamp: string
}

export interface PlatformCatalogResponse {
    service: string
    status: string
    generated_at: string
    primary_audio_feature_source: string
    onboarding_flow: string[]
    platforms: Array<{
        platform_id: 'spotify' | 'apple-music' | 'tidal'
        display_name: string
        integration_stage: string
        pms_import_supported: boolean
        ems_collection_supported: boolean
        audio_feature_strategy: string
        pms_role: string
        ems_role: string
        notes: string[]
    }>
}

export interface PmsWorkspaceBootstrapResponse {
    service: string
    status: string
    generated_at: string
    workspace_defaults: {
        user_id: string
        playlist_id: string
        seed_track_ids: string[]
        seed_artist_names: string[]
        seed_genres: string[]
    }
    playlists: Array<{
        playlist_id: string
        title: string
        source_platform: string
        track_count: number
        curator: string
        highlight: string
    }>
    suggested_tracks: Array<{
        track_id: string
        title: string
        artist_name: string
        source_platform: string
        spotify_track_id: string | null
        spotify_audio_features_filled: boolean
        spotify_audio_feature_source: string
    }>
    suggested_artists: Array<{
        artist_name: string
        affinity_score: number
        reason: string
    }>
    suggested_genres: Array<{
        genre: string
        weight: number
        reason: string
    }>
}

export interface EmsWorkspaceAnalysisRequest {
    user_id?: string
    playlist_id?: string
    seed_track_ids?: string[]
    seed_artist_names?: string[]
    seed_genres?: string[]
}

export interface EmsWorkspaceAnalysisResponse {
    service: string
    status: string
    generated_at: string
    context: {
        strategy: string
        playlist_id: string | null
        track_seed_count: number
        artist_seed_count: number
        genre_seed_count: number
        matched_catalog_track_count: number
    }
    workspace_recommendation: {
        mood: 'focus' | 'calm' | 'upbeat' | 'melancholy' | 'discovery'
        energy_level: number
        familiarity_bias: number
        confidence_score: number
    }
    top_signals: Array<{
        type: 'genre' | 'artist'
        label: string
        weight: number
        reason: string
    }>
    notes: string[]
    warnings: string[]
}

export interface GmsRecommendationPreviewRequest {
    request_id?: string
    user_id?: string
    playlist_id?: string
    mode?: 'gms'
    mood?: 'focus' | 'calm' | 'upbeat' | 'melancholy' | 'discovery'
    energy_level?: number
    familiarity_bias?: number
    limit?: number
    seed_track_ids?: string[]
    seed_artist_names?: string[]
    seed_genres?: string[]
    include_explanations?: boolean
}

export interface GmsRecommendationPreviewResponse {
    request_id: string
    generated_at: string
    service: string
    status: string
    context: {
        strategy: string
        engine: string
        mode: string
        mood: string | null
        energy_level: number
        seed_basis: string[]
    }
    input_summary: {
        user_id: string | null
        playlist_id: string | null
        track_seed_count: number
        artist_seed_count: number
        genre_seed_count: number
        familiarity_bias: number
        limit: number
    }
    items: Array<{
        rank: number
        track_id: string
        title: string
        artist_name: string
        score: number
        source_space: string
        energy_level: number
        reason?: string | null
    }>
    warnings: string[]
}

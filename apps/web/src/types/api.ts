export type WorkspacePlatformId = 'spotify' | 'apple-music' | 'tidal' | 'youtube-music' | 'last-fm'

export interface RichPlaylistArtwork {
    cover_image_url: string | null
    platform_external_url: string | null
    platform_uri: string | null
}

export interface RichTrackArtwork {
    album_title: string | null
    album_image_url: string | null
    platform_external_url: string | null
    platform_uri: string | null
    preview_url: string | null
    duration_ms: number | null
}

export interface SystemInfoResponse {
    service: string
    status: string
    message: string
    timestamp: string
}

export interface AuthRegistrationRequest {
    display_name: string
    email: string
    password: string
    preferred_platform_id: WorkspacePlatformId
    marketing_opt_in: boolean
    accepted_terms: boolean
    accepted_privacy_policy: boolean
}

export interface AuthRegistrationResponse {
    service: string
    status: string
    registered_at: string
    user: {
        user_id: string
        email: string
        display_name: string
        email_verified: boolean
    }
    onboarding: {
        stage: string
        preferred_platform_id: WorkspacePlatformId
        platform_connection_required: boolean
        next_step_path: string
        next_step_message: string
    }
}

export interface AuthLoginRequest {
    email: string
    password: string
}

export interface AuthLoginResponse {
    service: string
    status: string
    authenticated_at: string
    user: {
        user_id: string
        email: string
        display_name: string
        email_verified: boolean
    }
    onboarding: {
        stage: string
        preferred_platform_id: WorkspacePlatformId
        platform_connection_required: boolean
        next_step_path: string
        next_step_message: string
    }
}

export interface PlatformCatalogResponse {
    service: string
    status: string
    generated_at: string
    primary_audio_feature_source: string
    onboarding_flow: string[]
    platforms: Array<{
        platform_id: WorkspacePlatformId
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

export interface PlatformConnectionBootstrapResponse {
    service: string
    status: string
    generated_at: string
    user: {
        user_id: string
        display_name: string
        email: string
        preferred_platform_id: WorkspacePlatformId
        last_fm_username: string | null
        last_fm_connected_at: string | null
    }
    summary: {
        connected_platform_count: number
        preferred_platform_connected: boolean
        preferred_platform_reconnect_required: boolean
        onboarding_stage: string
        next_step_path: string
        next_step_message: string
    }
    connections: Array<{
        platform_id: WorkspacePlatformId
        display_name: string
        preferred: boolean
        connected: boolean
        connection_status: string
        connection_mode: string | null
        external_account_label: string | null
        sync_ready: boolean
        credential_status: 'ready' | 'missing' | 'reconnect_required'
        reconnect_required: boolean
        connected_at: string | null
        next_action_label: string
    }>
}

export interface PlatformConnectRequest {
    user_id: string
    platform_id: WorkspacePlatformId
    connection_mode?: string
    external_account_label?: string
}

export interface PlatformDisconnectRequest {
    user_id: string
    platform_id: WorkspacePlatformId
}

export interface PlatformConnectionCommandResponse {
    service: string
    status: string
    processed_at: string
    connection: {
        user_id: string
        platform_id: WorkspacePlatformId
        display_name: string
        connected: boolean
        connection_status: string
        connection_mode: string
        external_account_label: string | null
        scope_summary: string | null
        sync_ready: boolean
        connected_at: string | null
    }
    next_step: {
        path: string
        message: string
    }
}

export interface LastFmProfileConnectRequest {
    user_id: string
    username: string
}

export interface PlatformAuthorizationStartRequest {
    user_id: string
    platform_id: WorkspacePlatformId
}

export interface PlatformAuthorizationStartResponse {
    service: string
    status: string
    generated_at: string
    user: {
        user_id: string
        display_name: string
        email: string
    }
    authorization: {
        state: string
        platform_id: WorkspacePlatformId
        platform_display_name: string
        authorization_mode: string
        authorization_channel: 'internal_approval_page' | 'external_browser_redirect'
        requested_scopes: string[]
        expires_at: string
        approval_page_path: string | null
        callback_path: string
        approval_code: string | null
        external_authorization_url: string | null
        redirect_uri: string | null
    }
}

export interface PlatformAuthorizationCompleteRequest {
    user_id: string
    platform_id: WorkspacePlatformId
    state: string
    approval_code?: string
    authorization_code?: string
}

export interface PlatformAuthorizationCompleteResponse {
    service: string
    status: string
    processed_at: string
    authorization: {
        state: string
        platform_id: WorkspacePlatformId
        platform_display_name: string
        authorization_mode: string
        requested_scopes: string[]
        completed_at: string
    }
    connection: {
        user_id: string
        platform_id: WorkspacePlatformId
        connected: boolean
        connection_status: string
        connection_mode: string
        external_account_label: string | null
        scope_summary: string | null
        sync_ready: boolean
        connected_at: string | null
    }
    next_step: {
        path: string
        message: string
    }
}

export interface LastFmSignalPreviewResponse {
    service: string
    status: string
    generated_at: string
    request: {
        username: string
        period: 'overall' | '7day' | '1month' | '3month' | '6month' | '12month'
        recent_limit: number
        top_limit: number
    }
    user: {
        username: string
        real_name: string | null
        country: string | null
        playcount: number | null
        profile_url: string | null
        avatar_url: string | null
        registered_at: string | null
    }
    summary: {
        source: string
        recent_track_count: number
        top_artist_count: number
        top_track_count: number
        now_playing: boolean
        distinct_recent_artist_count: number
        next_step_message: string
    }
    insights: Array<{
        insight_id: string
        title: string
        detail: string
    }>
    recent_tracks: Array<{
        track_name: string | null
        artist_name: string | null
        album_name: string | null
        track_url: string | null
        image_url: string | null
        now_playing: boolean
        played_at: string | null
        loved: boolean
    }>
    top_artists: Array<{
        artist_name: string | null
        rank: number | null
        playcount: number | null
        artist_url: string | null
        image_url: string | null
    }>
    top_tracks: Array<{
        track_name: string | null
        artist_name: string | null
        rank: number | null
        playcount: number | null
        track_url: string | null
        artist_url: string | null
        image_url: string | null
    }>
}

export interface LastFmScrobbleBootstrapResponse {
    service: string
    status: string
    generated_at: string
    user: {
        user_id: string
        last_fm_username: string | null
        last_fm_connected_at: string | null
    }
    summary: {
        stored_scrobble_count: number
        last_synced_at: string | null
        returned_scrobble_count: number
        next_step_message: string
    }
    recent_scrobbles: Array<{
        track_name: string
        artist_name: string
        album_name: string | null
        track_url: string | null
        image_url: string | null
        played_at: string
        loved: boolean
        synced_at: string
    }>
}

export interface LastFmScrobbleSyncRequest {
    user_id: string
    limit?: number
}

export interface LastFmScrobbleSyncResponse {
    service: string
    status: string
    processed_at: string
    sync: {
        user_id: string
        last_fm_username: string
        fetched_track_count: number
        inserted_scrobble_count: number
        duplicate_scrobble_count: number
        skipped_now_playing_count: number
        stored_scrobble_count: number
        last_synced_at: string | null
    }
    recent_scrobbles: Array<{
        track_name: string
        artist_name: string
        album_name: string | null
        track_url: string | null
        image_url: string | null
        played_at: string
        loved: boolean
        synced_at: string
    }>
    notes: string[]
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
    playlists: Array<RichPlaylistArtwork & {
        playlist_id: string
        title: string
        source_platform: string
        track_count: number
        curator: string
        highlight: string
    }>
    suggested_tracks: Array<RichTrackArtwork & {
        track_id: string
        title: string
        artist_name: string
        source_platform: string
        seed: boolean
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

export interface PmsPlaylistImportBootstrapResponse {
    service: string
    status: string
    generated_at: string
    user: {
        user_id: string
        display_name: string
        preferred_platform_id: WorkspacePlatformId
    }
    platform_connection: {
        platform_id: WorkspacePlatformId
        display_name: string
        pms_import_supported: boolean
        connected: boolean
        connection_mode: string | null
        external_account_label: string | null
        sync_ready: boolean
        credential_status: 'ready' | 'missing' | 'reconnect_required'
        reconnect_required: boolean
    }
    summary: {
        preferred_platform_connected: boolean
        reconnect_required: boolean
        available_playlist_count: number
        imported_playlist_count: number
        next_step_path: string
        next_step_message: string
    }
    available_playlists: Array<RichPlaylistArtwork & {
        external_playlist_id: string
        title: string
        source_platform: string
        track_count: number
        curator: string
        description: string
        already_imported: boolean
        audio_feature_policy: string
    }>
    imported_playlists: Array<RichPlaylistArtwork & {
        playlist_id: string
        external_playlist_id: string
        title: string
        source_platform: string
        track_count: number
        imported_at: string
    }>
}

export interface PmsPlaylistImportRequest {
    user_id: string
    platform_id: WorkspacePlatformId
    external_playlist_ids: string[]
}

export interface PmsPlaylistImportResponse {
    service: string
    status: string
    processed_at: string
    import_result: {
        user_id: string
        platform_id: WorkspacePlatformId
        platform_display_name: string
        imported_playlist_count: number
        imported_track_count: number
        complete_spotify_audio_feature_track_count: number
        connection_mode: string
        library_synced_playlist_count: number
        library_synced_track_count: number
    }
    playlists: Array<{
        playlist_id: string
        external_playlist_id: string
        title: string
        source_platform: string
        track_count: number
        imported_at: string
    }>
    next_step: {
        path: string
        message: string
    }
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
    items: Array<RichTrackArtwork & {
        rank: number
        track_id: string
        title: string
        artist_name: string
        source_platform: string
        source_playlist_id: string | null
        source_playlist_title: string | null
        spotify_track_id: string | null
        score: number
        source_space: string
        energy_level: number
        reason?: string | null
    }>
    warnings: string[]
}

import type {
    AuthLoginRequest,
    AuthLoginResponse,
    AuthRegistrationRequest,
    AuthRegistrationResponse,
    PlatformAuthorizationCompleteRequest,
    PlatformAuthorizationCompleteResponse,
    PlatformAuthorizationStartRequest,
    PlatformAuthorizationStartResponse,
    PlatformConnectionBootstrapResponse,
    PlatformConnectionCommandResponse,
    PlatformPlaybackCredentialsResponse,
    TidalPlaybackManifestDiagnosticsResponse,
    TidalPlaybackStreamResponse,
    TidalDeviceAuthorizationPollRequest,
    TidalDeviceAuthorizationPollResponse,
    TidalDeviceAuthorizationStartRequest,
    TidalDeviceAuthorizationStartResponse,
    PlatformConnectRequest,
    PlatformDisconnectRequest,
    PlatformCatalogResponse,
    LastFmProfileConnectRequest,
    LastFmScrobbleBootstrapResponse,
    LastFmScrobbleSyncRequest,
    LastFmScrobbleSyncResponse,
    LastFmSignalPreviewResponse,
    EmsCollectionSearchRequest,
    EmsCollectionSearchResponse,
    EmsCollectionPlaylistBrowseResponse,
    EmsCollectionTrackBrowseResponse,
    EmsWorkspaceAnalysisRequest,
    EmsWorkspaceAnalysisResponse,
    GmsRecommendationPreviewRequest,
    GmsRecommendationPreviewResponse,
    GmsRecommendationFeedbackRequest,
    GmsRecommendationFeedbackResponse,
    PmsWorkspaceBootstrapResponse,
    PmsPlaylistDetailResponse,
    PmsPlaylistImportBootstrapResponse,
    PmsPlaylistImportRequest,
    PmsPlaylistImportResponse,
    PmsPersonalPlaylistBootstrapResponse,
    PmsPersonalPlaylistCommandResponse,
    PmsPersonalPlaylistCreateRequest,
    PmsPersonalPlaylistTrackSaveRequest,
    SystemInfoResponse,
} from '@/types/api'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

export class ApiError extends Error {
    readonly status: number
    readonly code: string | null

    constructor(message: string, status: number, code: string | null = null) {
        super(message)
        this.name = 'ApiError'
        this.status = status
        this.code = code
    }
}

const buildApiUrl = (path: string) => `${API_BASE_URL}${path}`

const resolveDocsUrl = (
    overrideUrl: string | undefined,
    sameOriginPath: string,
    localPort: string,
    localPath: string,
) => {
    if (overrideUrl) {
        return overrideUrl
    }

    if (typeof window === 'undefined') {
        return sameOriginPath
    }

    if (window.location.port === '5173') {
        return `${window.location.protocol}//${window.location.hostname}:${localPort}${localPath}`
    }

    return sameOriginPath
}

const readErrorPayload = async (response: Response) => {
    const contentType = response.headers.get('content-type') ?? ''

    if (contentType.includes('application/json')) {
        const payload = (await response.json()) as Record<string, unknown>
        const detail = typeof payload.detail === 'string' ? payload.detail : null
        const message = typeof payload.message === 'string' ? payload.message : null
        const code = typeof payload.code === 'string' ? payload.code : null

        return {
            code,
            message: detail ?? message ?? `Request failed with status ${response.status}`,
        }
    }

    const fallback = await response.text()
    return {
        code: null,
        message: fallback || `Request failed with status ${response.status}`,
    }
}

async function requestJson<T>(path: string, init?: RequestInit) {
    const headers = new Headers(init?.headers)
    headers.set('Accept', 'application/json')

    const response = await fetch(buildApiUrl(path), {
        ...init,
        headers,
    })

    if (!response.ok) {
        const payload = await readErrorPayload(response)
        throw new ApiError(payload.message, response.status, payload.code)
    }

    return (await response.json()) as T
}

export const getApiConnectionLabel = () =>
    API_BASE_URL || 'same-origin (/api via Vite proxy)'

export const getApiDocsUrl = () =>
    resolveDocsUrl(import.meta.env.VITE_API_DOCS_URL, '/docs', '8081', '/docs')

export const getAiDocsUrl = () =>
    resolveDocsUrl(import.meta.env.VITE_AI_DOCS_URL, '/ai/docs', '8000', '/docs')

export const fetchSystemInfo = (signal?: AbortSignal) =>
    requestJson<SystemInfoResponse>('/api/v1/system/info', { signal })

export const registerAccount = (payload: AuthRegistrationRequest) =>
    requestJson<AuthRegistrationResponse>('/api/v1/auth/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })

export const loginAccount = (payload: AuthLoginRequest) =>
    requestJson<AuthLoginResponse>('/api/v1/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })

export const fetchPlatformCatalog = (signal?: AbortSignal) =>
    requestJson<PlatformCatalogResponse>('/api/v1/platforms/catalog', { signal })

export const fetchPlatformConnectionBootstrap = (userId: string, signal?: AbortSignal) =>
    requestJson<PlatformConnectionBootstrapResponse>(
        `/api/v1/platforms/connections/bootstrap?user_id=${encodeURIComponent(userId)}`,
        { signal },
    )

export const fetchPlaybackCredentials = (userId: string, platformId: string, signal?: AbortSignal) =>
    requestJson<PlatformPlaybackCredentialsResponse>(
        `/api/v1/platforms/playback/credentials?user_id=${encodeURIComponent(userId)}&platform_id=${encodeURIComponent(platformId)}`,
        { signal, cache: 'no-store' },
    )

export const fetchTidalPlaybackManifestDiagnostics = (
    userId: string,
    trackId: string,
    signal?: AbortSignal,
) =>
    requestJson<TidalPlaybackManifestDiagnosticsResponse>(
        `/api/v1/platforms/playback/tidal/manifest-diagnostics?user_id=${encodeURIComponent(userId)}&track_id=${encodeURIComponent(trackId)}`,
        { signal },
    )

export const fetchTidalPlaybackStream = (
    userId: string,
    trackId: string,
    quality = 'LOSSLESS',
    signal?: AbortSignal,
) =>
    requestJson<TidalPlaybackStreamResponse>(
        `/api/v1/platforms/playback/tidal/tracks/${encodeURIComponent(trackId)}/stream?user_id=${encodeURIComponent(userId)}&quality=${encodeURIComponent(quality)}`,
        { signal },
    )

export const connectPlatformAccount = (payload: PlatformConnectRequest) =>
    requestJson<PlatformConnectionCommandResponse>('/api/v1/platforms/connections/connect', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })

export const disconnectPlatformAccount = (payload: PlatformDisconnectRequest) =>
    requestJson<PlatformConnectionCommandResponse>('/api/v1/platforms/connections/disconnect', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })

export const startPlatformAuthorization = (payload: PlatformAuthorizationStartRequest) =>
    requestJson<PlatformAuthorizationStartResponse>('/api/v1/platforms/oauth/start', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })

export const completePlatformAuthorization = (payload: PlatformAuthorizationCompleteRequest) =>
    requestJson<PlatformAuthorizationCompleteResponse>('/api/v1/platforms/oauth/complete', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })

export const startTidalDeviceAuthorization = (payload: TidalDeviceAuthorizationStartRequest) =>
    requestJson<TidalDeviceAuthorizationStartResponse>('/api/v1/platforms/oauth/tidal/device/start', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })

export const pollTidalDeviceAuthorization = (payload: TidalDeviceAuthorizationPollRequest) =>
    requestJson<TidalDeviceAuthorizationPollResponse>('/api/v1/platforms/oauth/tidal/device/poll', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })

export const fetchLastFmSignalPreview = (
    username: string,
    period: 'overall' | '7day' | '1month' | '3month' | '6month' | '12month' = '1month',
    recentLimit = 8,
    topLimit = 6,
    signal?: AbortSignal,
) =>
    requestJson<LastFmSignalPreviewResponse>(
        `/api/v1/platforms/lastfm/preview?username=${encodeURIComponent(username)}&period=${encodeURIComponent(period)}&recent_limit=${recentLimit}&top_limit=${topLimit}`,
        { signal },
    )

export const connectLastFmProfile = (payload: LastFmProfileConnectRequest) =>
    requestJson<PlatformConnectionCommandResponse>('/api/v1/platforms/lastfm/profile', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })

export const fetchLastFmScrobbleBootstrap = (userId: string, signal?: AbortSignal) =>
    requestJson<LastFmScrobbleBootstrapResponse>(
        `/api/v1/platforms/lastfm/scrobbles/bootstrap?user_id=${encodeURIComponent(userId)}`,
        { signal },
    )

export const syncLastFmScrobbles = (payload: LastFmScrobbleSyncRequest) =>
    requestJson<LastFmScrobbleSyncResponse>('/api/v1/platforms/lastfm/scrobbles/sync', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })

export const fetchPmsWorkspaceBootstrap = (
    userId?: string,
    playlistId?: string,
    signal?: AbortSignal,
) => {
    const searchParams = new URLSearchParams()

    if (userId) {
        searchParams.set('user_id', userId)
    }

    if (playlistId) {
        searchParams.set('playlist_id', playlistId)
    }

    const suffix = searchParams.size > 0 ? `?${searchParams.toString()}` : ''
    return requestJson<PmsWorkspaceBootstrapResponse>(`/api/v1/pms/workspace/bootstrap${suffix}`, { signal })
}

export const fetchPmsPlaylistDetail = (
    userId: string,
    playlistId: string,
    signal?: AbortSignal,
) =>
    requestJson<PmsPlaylistDetailResponse>(
        `/api/v1/pms/playlists/${encodeURIComponent(playlistId)}?user_id=${encodeURIComponent(userId)}`,
        { signal },
    )

export const fetchPmsPlaylistImportBootstrap = (userId: string, signal?: AbortSignal) =>
    requestJson<PmsPlaylistImportBootstrapResponse>(
        `/api/v1/pms/import/bootstrap?user_id=${encodeURIComponent(userId)}`,
        { signal },
    )

export const importPmsPlaylists = (payload: PmsPlaylistImportRequest) =>
    requestJson<PmsPlaylistImportResponse>('/api/v1/pms/import/playlists', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })

export const fetchPmsPersonalPlaylists = (userId: string, signal?: AbortSignal) =>
    requestJson<PmsPersonalPlaylistBootstrapResponse>(
        `/api/v1/pms/personal-playlists/bootstrap?user_id=${encodeURIComponent(userId)}`,
        { signal },
    )

export const createPmsPersonalPlaylist = (payload: PmsPersonalPlaylistCreateRequest) =>
    requestJson<PmsPersonalPlaylistCommandResponse>('/api/v1/pms/personal-playlists', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })

export const saveTrackToPmsPersonalPlaylist = (payload: PmsPersonalPlaylistTrackSaveRequest) =>
    requestJson<PmsPersonalPlaylistCommandResponse>('/api/v1/pms/personal-playlists/tracks', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })

export const searchEmsCollection = (payload: EmsCollectionSearchRequest) =>
    requestJson<EmsCollectionSearchResponse>('/api/v1/ems/collection/search', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })

export const fetchEmsCollectedPlaylists = (platformId: string = 'spotify', signal?: AbortSignal) =>
    requestJson<EmsCollectionPlaylistBrowseResponse>(
        `/api/v1/ems/collection/playlists?platform_id=${encodeURIComponent(platformId)}`,
        { signal },
    )

export const fetchEmsCollectedTracks = (platformId: string = 'spotify', signal?: AbortSignal) =>
    requestJson<EmsCollectionTrackBrowseResponse>(
        `/api/v1/ems/collection/tracks?platform_id=${encodeURIComponent(platformId)}`,
        { signal },
    )

export const fetchEmsPlaylistTracks = (playlistId: number, signal?: AbortSignal) =>
    requestJson<EmsCollectionTrackBrowseResponse>(
        `/api/v1/ems/collection/playlists/${playlistId}/tracks`,
        { signal },
    )

export const analyzeEmsWorkspace = (payload: EmsWorkspaceAnalysisRequest, signal?: AbortSignal) =>
    requestJson<EmsWorkspaceAnalysisResponse>('/api/v1/ems/workspace/analysis', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
        signal,
    })

export const previewGmsRecommendations = (payload: GmsRecommendationPreviewRequest) =>
    requestJson<GmsRecommendationPreviewResponse>('/api/v1/gms/recommendations/preview', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            mode: 'gms',
            ...payload,
        }),
    })

export const recordGmsRecommendationFeedback = (payload: GmsRecommendationFeedbackRequest) =>
    requestJson<GmsRecommendationFeedbackResponse>('/api/v1/gms/recommendations/feedback', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })

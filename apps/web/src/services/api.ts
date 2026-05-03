import type {
    AuthRegistrationRequest,
    AuthRegistrationResponse,
    PlatformAuthorizationCompleteRequest,
    PlatformAuthorizationCompleteResponse,
    PlatformAuthorizationStartRequest,
    PlatformAuthorizationStartResponse,
    PlatformConnectionBootstrapResponse,
    PlatformConnectionCommandResponse,
    PlatformConnectRequest,
    PlatformDisconnectRequest,
    PlatformCatalogResponse,
    EmsWorkspaceAnalysisRequest,
    EmsWorkspaceAnalysisResponse,
    GmsRecommendationPreviewRequest,
    GmsRecommendationPreviewResponse,
    PmsWorkspaceBootstrapResponse,
    PmsPlaylistImportBootstrapResponse,
    PmsPlaylistImportRequest,
    PmsPlaylistImportResponse,
    SystemInfoResponse,
} from '@/types/api'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

export class ApiError extends Error {
    readonly status: number

    constructor(message: string, status: number) {
        super(message)
        this.name = 'ApiError'
        this.status = status
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

const readErrorMessage = async (response: Response) => {
    const contentType = response.headers.get('content-type') ?? ''

    if (contentType.includes('application/json')) {
        const payload = (await response.json()) as Record<string, unknown>
        const detail = typeof payload.detail === 'string' ? payload.detail : null
        const message = typeof payload.message === 'string' ? payload.message : null
        return detail ?? message ?? `Request failed with status ${response.status}`
    }

    const fallback = await response.text()
    return fallback || `Request failed with status ${response.status}`
}

async function requestJson<T>(path: string, init?: RequestInit) {
    const headers = new Headers(init?.headers)
    headers.set('Accept', 'application/json')

    const response = await fetch(buildApiUrl(path), {
        ...init,
        headers,
    })

    if (!response.ok) {
        throw new ApiError(await readErrorMessage(response), response.status)
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

export const fetchPlatformCatalog = (signal?: AbortSignal) =>
    requestJson<PlatformCatalogResponse>('/api/v1/platforms/catalog', { signal })

export const fetchPlatformConnectionBootstrap = (userId: string, signal?: AbortSignal) =>
    requestJson<PlatformConnectionBootstrapResponse>(
        `/api/v1/platforms/connections/bootstrap?user_id=${encodeURIComponent(userId)}`,
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

export const fetchPmsWorkspaceBootstrap = (userId?: string, signal?: AbortSignal) =>
    requestJson<PmsWorkspaceBootstrapResponse>(
        userId
            ? `/api/v1/pms/workspace/bootstrap?user_id=${encodeURIComponent(userId)}`
            : '/api/v1/pms/workspace/bootstrap',
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

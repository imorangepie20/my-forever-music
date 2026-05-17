import {
    createContext,
    useContext,
    useEffect,
    useMemo,
    useState,
    type ReactNode,
} from 'react'
import type { AuthLoginResponse, AuthRegistrationResponse, WorkspacePlatformId } from '@/types/api'

interface AuthSessionState {
    userId: string
    email: string
    displayName: string
    preferredPlatformId: WorkspacePlatformId
    onboardingStage: string
    registeredAt: string
    platformConnectionRequired: boolean
    nextStepPath: string
    nextStepMessage: string
}

interface AuthSessionContextValue {
    session: AuthSessionState | null
    setSessionFromAuthentication: (response: AuthRegistrationResponse | AuthLoginResponse) => void
    updateSession: (patch: Partial<AuthSessionState>) => void
    clearSession: () => void
}

const STORAGE_KEY = 'my-forever-music.auth-session'
const PLATFORM_OAUTH_STORAGE_PREFIX = 'my-forever-music.platform-oauth-session.'

const AuthSessionContext = createContext<AuthSessionContextValue | null>(null)

const clearPendingPlatformOAuthSessions = () => {
    if (typeof window === 'undefined') {
        return
    }

    for (let index = window.sessionStorage.length - 1; index >= 0; index -= 1) {
        const key = window.sessionStorage.key(index)
        if (key?.startsWith(PLATFORM_OAUTH_STORAGE_PREFIX)) {
            window.sessionStorage.removeItem(key)
        }
    }
}

const getInitialSession = (): AuthSessionState | null => {
    if (typeof window === 'undefined') {
        return null
    }

    const stored = window.localStorage.getItem(STORAGE_KEY)
    if (!stored) {
        return null
    }

    try {
        return JSON.parse(stored) as AuthSessionState
    } catch {
        return null
    }
}

const toSessionState = (response: AuthRegistrationResponse | AuthLoginResponse): AuthSessionState => ({
    userId: response.user.user_id,
    email: response.user.email,
    displayName: response.user.display_name,
    preferredPlatformId: response.onboarding.preferred_platform_id,
    onboardingStage: response.onboarding.stage,
    registeredAt: 'registered_at' in response ? response.registered_at : response.authenticated_at,
    platformConnectionRequired: response.onboarding.platform_connection_required,
    nextStepPath: response.onboarding.next_step_path,
    nextStepMessage: response.onboarding.next_step_message,
})

export const AuthSessionProvider = ({ children }: { children: ReactNode }) => {
    const [session, setSession] = useState<AuthSessionState | null>(getInitialSession)

    useEffect(() => {
        if (typeof window === 'undefined') {
            return
        }

        if (!session) {
            window.localStorage.removeItem(STORAGE_KEY)
            return
        }

        window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    }, [session])

    const value = useMemo<AuthSessionContextValue>(
        () => ({
            session,
            setSessionFromAuthentication: (response) => {
                clearPendingPlatformOAuthSessions()
                setSession(toSessionState(response))
            },
            updateSession: (patch) => {
                setSession((current) => (current ? { ...current, ...patch } : current))
            },
            clearSession: () => {
                clearPendingPlatformOAuthSessions()
                setSession(null)
            },
        }),
        [session],
    )

    return <AuthSessionContext.Provider value={value}>{children}</AuthSessionContext.Provider>
}

export const useAuthSession = () => {
    const context = useContext(AuthSessionContext)

    if (!context) {
        throw new Error('useAuthSession must be used within AuthSessionProvider')
    }

    return context
}

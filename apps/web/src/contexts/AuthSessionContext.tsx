import {
    createContext,
    useContext,
    useEffect,
    useMemo,
    useState,
    type ReactNode,
} from 'react'
import type { AuthRegistrationResponse, WorkspacePlatformId } from '@/types/api'

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
    setSessionFromRegistration: (response: AuthRegistrationResponse) => void
    updateSession: (patch: Partial<AuthSessionState>) => void
    clearSession: () => void
}

const STORAGE_KEY = 'my-forever-music.auth-session'

const AuthSessionContext = createContext<AuthSessionContextValue | null>(null)

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

const toSessionState = (response: AuthRegistrationResponse): AuthSessionState => ({
    userId: response.user.user_id,
    email: response.user.email,
    displayName: response.user.display_name,
    preferredPlatformId: response.onboarding.preferred_platform_id,
    onboardingStage: response.onboarding.stage,
    registeredAt: response.registered_at,
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
            setSessionFromRegistration: (response) => setSession(toSessionState(response)),
            updateSession: (patch) => {
                setSession((current) => (current ? { ...current, ...patch } : current))
            },
            clearSession: () => setSession(null),
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

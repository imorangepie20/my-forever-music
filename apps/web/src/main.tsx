import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { AuthSessionProvider } from './contexts/AuthSessionContext'
import { PlaybackProvider } from './contexts/PlaybackContext'
import { RecommendationWorkspaceProvider } from './contexts/RecommendationWorkspaceContext'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <AuthSessionProvider>
            <PlaybackProvider>
                <RecommendationWorkspaceProvider>
                    <App />
                </RecommendationWorkspaceProvider>
            </PlaybackProvider>
        </AuthSessionProvider>
    </StrictMode>,
)

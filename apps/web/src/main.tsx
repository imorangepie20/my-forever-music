import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RecommendationWorkspaceProvider } from './contexts/RecommendationWorkspaceContext'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <RecommendationWorkspaceProvider>
            <App />
        </RecommendationWorkspaceProvider>
    </StrictMode>,
)

import { BrowserRouter, Route, Routes } from 'react-router-dom'
import MainLayout from './layouts/MainLayout'
import EmsPage from './pages/EmsPage'
import GmsPreviewPage from './pages/GmsPreviewPage'
import HomePage from './pages/HomePage'
import NotFoundPage from './pages/NotFoundPage'
import PmsPage from './pages/PmsPage'
import PlatformsPage from './pages/PlatformsPage'
import Login from './pages/auth/Login'
import Register from './pages/auth/Register'
import PlatformOAuthCallbackPage from './pages/platforms/PlatformOAuthCallbackPage'

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<MainLayout />}>
                    <Route index element={<HomePage />} />
                    <Route path="platforms" element={<PlatformsPage />} />
                    <Route path="platforms/oauth/callback" element={<PlatformOAuthCallbackPage />} />
                    <Route path="pms" element={<PmsPage />} />
                    <Route path="ems" element={<EmsPage />} />
                    <Route path="gms-preview" element={<GmsPreviewPage />} />
                </Route>
                <Route path="login" element={<Login />} />
                <Route path="signin" element={<Login />} />
                <Route path="signup" element={<Register />} />
                <Route path="register" element={<Register />} />
                <Route path="*" element={<NotFoundPage />} />
            </Routes>
        </BrowserRouter>
    )
}

export default App

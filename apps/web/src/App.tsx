import { BrowserRouter, Route, Routes } from 'react-router-dom'
import MainLayout from './layouts/MainLayout'
import EmsPage from './pages/EmsPage'
import EmsPlaylistDetailPage from './pages/EmsPlaylistDetailPage'
import GmsPreviewPage from './pages/GmsPreviewPage'
import HomePage from './pages/HomePage'
import NotFoundPage from './pages/NotFoundPage'
import PlaybackHarnessPage from './pages/PlaybackHarnessPage'
import PmsPlaylistDetailPage from './pages/PmsPlaylistDetailPage'
import PmsPage from './pages/PmsPage'
import PlatformsPage from './pages/PlatformsPage'
import TidalPlaylistPlaybackTestPage from './pages/TidalPlaylistPlaybackTestPage'
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
                    <Route path="playback-harness" element={<PlaybackHarnessPage />} />
                    <Route path="playlists/ems/:playlistId" element={<EmsPlaylistDetailPage />} />
                    <Route path="playlists/pms/:playlistId" element={<PmsPlaylistDetailPage />} />
                </Route>
                <Route path="login" element={<Login />} />
                <Route path="signin" element={<Login />} />
                <Route path="signup" element={<Register />} />
                <Route path="register" element={<Register />} />
                <Route path="tidal-playlist-test" element={<TidalPlaylistPlaybackTestPage />} />
                <Route path="*" element={<NotFoundPage />} />
            </Routes>
        </BrowserRouter>
    )
}

export default App

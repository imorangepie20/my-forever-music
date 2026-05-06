import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import PlaybackDock from '@/components/music/PlaybackDock'
import Sidebar from '../components/layout/Sidebar'
import Header from '../components/layout/Header'

const MainLayout = () => {
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
    const [sidebarOpen, setSidebarOpen] = useState(false)

    return (
        <div className="min-h-screen bg-hud-bg-primary hud-grid-bg">
            <Sidebar
                collapsed={sidebarCollapsed}
                open={sidebarOpen}
                onClose={() => setSidebarOpen(false)}
                onCollapseToggle={() => setSidebarCollapsed((current) => !current)}
            />

            <div
                className={`min-h-screen transition-all duration-300 ${
                    sidebarCollapsed ? 'lg:pl-24' : 'lg:pl-72'
                }`}
            >
                <Header onMenuToggle={() => setSidebarOpen((current) => !current)} />

                <main className="px-4 pb-44 pt-6 sm:px-6 lg:px-8">
                    <Outlet />
                </main>
            </div>

            <PlaybackDock sidebarCollapsed={sidebarCollapsed} />
        </div>
    )
}

export default MainLayout

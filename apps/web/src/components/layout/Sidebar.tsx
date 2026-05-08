import type { ReactNode } from 'react'
import {
    Activity,
    ChevronLeft,
    ChevronRight,
    Globe,
    Home,
    Music2,
    PlayCircle,
    Radio,
    SlidersHorizontal,
    Sparkles,
} from 'lucide-react'
import { NavLink } from 'react-router-dom'

interface SidebarProps {
    collapsed: boolean
    open: boolean
    onClose: () => void
    onCollapseToggle: () => void
}

interface MenuItem {
    label: string
    description: string
    icon: ReactNode
    path: string
}

const menuItems: MenuItem[] = [
    {
        label: 'Overview',
        description: 'Project status and service map',
        icon: <Home size={20} />,
        path: '/',
    },
    {
        label: 'Platforms',
        description: 'Subscription and import sources',
        icon: <Radio size={20} />,
        path: '/platforms',
    },
    {
        label: 'PMS Library',
        description: 'Playlists and approved saves',
        icon: <Music2 size={20} />,
        path: '/pms',
    },
    {
        label: 'EMS Model',
        description: 'Candidate evaluation signals',
        icon: <SlidersHorizontal size={20} />,
        path: '/ems',
    },
    {
        label: 'GMS Approval',
        description: 'Review and save candidates',
        icon: <Sparkles size={20} />,
        path: '/gms-preview',
    },
    {
        label: 'Playback',
        description: 'Spotify player harness',
        icon: <PlayCircle size={20} />,
        path: '/playback-harness',
    },
]

const Sidebar = ({ collapsed, open, onClose, onCollapseToggle }: SidebarProps) => {
    return (
        <>
            <div
                className={`fixed inset-0 z-40 bg-black/60 backdrop-blur-sm transition-opacity lg:hidden ${
                    open ? 'opacity-100' : 'pointer-events-none opacity-0'
                }`}
                onClick={onClose}
            />

            <aside
                className={`fixed inset-y-0 left-0 z-50 flex ${
                    collapsed ? 'lg:w-24' : 'lg:w-72'
                } w-72 flex-col border-r border-hud-border-secondary bg-hud-bg-secondary/95 backdrop-blur-xl transition-transform duration-300 lg:translate-x-0 ${
                    open ? 'translate-x-0' : '-translate-x-full'
                }`}
            >
                <div className="border-b border-hud-border-secondary px-5 py-5">
                    <div className="flex items-center gap-3">
                        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-hud-accent-primary via-cyan-300 to-hud-accent-secondary text-sm font-black tracking-[0.24em] text-hud-bg-primary">
                            FM
                        </div>
                        {!collapsed && (
                            <div>
                                <p className="text-[11px] font-semibold uppercase tracking-[0.3em] text-hud-accent-primary">
                                    My Forever Music
                                </p>
                                <h2 className="mt-1 text-lg font-semibold text-hud-text-primary">
                                    Rebuild Shell
                                </h2>
                            </div>
                        )}
                    </div>
                </div>

                <nav className="flex-1 px-3 py-5">
                    <div className="mb-3 px-3 text-[11px] font-semibold uppercase tracking-[0.28em] text-hud-text-muted">
                        {!collapsed ? 'Workspace' : 'WS'}
                    </div>
                    <ul className="space-y-2">
                        {menuItems.map((item) => (
                            <li key={item.path}>
                                <NavLink
                                    to={item.path}
                                    end={item.path === '/'}
                                    onClick={onClose}
                                    className={({ isActive }) =>
                                        `group flex items-center gap-3 rounded-2xl border px-3 py-3 transition-hud ${
                                            isActive
                                                ? 'border-hud-border-primary bg-hud-accent-primary/10 text-hud-accent-primary shadow-hud'
                                                : 'border-transparent text-hud-text-secondary hover:border-hud-border-secondary hover:bg-hud-bg-hover hover:text-hud-text-primary'
                                        }`
                                    }
                                >
                                    <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-hud-bg-primary/80">
                                        {item.icon}
                                    </span>
                                    {!collapsed && (
                                        <span className="min-w-0">
                                            <span className="block text-sm font-medium">{item.label}</span>
                                            <span className="mt-0.5 block text-xs text-hud-text-muted">
                                                {item.description}
                                            </span>
                                        </span>
                                    )}
                                </NavLink>
                            </li>
                        ))}
                    </ul>
                </nav>

                <div className="border-t border-hud-border-secondary px-3 py-4">
                    <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                        <div className="flex items-center gap-3">
                            <span className="rounded-xl bg-hud-accent-primary/10 p-2 text-hud-accent-primary">
                                <Globe size={18} />
                            </span>
                            {!collapsed && (
                                <div>
                                    <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">
                                        Flow
                                    </p>
                                    <p className="mt-1 text-sm text-hud-text-primary">
                                        {'PMS -> EMS -> GMS'}
                                    </p>
                                </div>
                            )}
                        </div>
                        {!collapsed && (
                            <p className="mt-3 text-xs leading-5 text-hud-text-muted">
                                Current focus is the GMS preview loop between React, Spring Boot, and FastAPI.
                            </p>
                        )}
                    </div>

                    <button
                        onClick={onCollapseToggle}
                        className="mt-3 hidden w-full items-center justify-center gap-2 rounded-xl border border-hud-border-secondary bg-hud-bg-primary/80 px-3 py-2.5 text-sm text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary lg:flex"
                    >
                        {collapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
                        {!collapsed && 'Collapse'}
                    </button>

                    <button
                        onClick={onClose}
                        className="mt-3 flex w-full items-center justify-center gap-2 rounded-xl border border-hud-border-secondary bg-hud-bg-primary/80 px-3 py-2.5 text-sm text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary lg:hidden"
                    >
                        <Activity size={16} />
                        Close Menu
                    </button>
                </div>
            </aside>
        </>
    )
}

export default Sidebar

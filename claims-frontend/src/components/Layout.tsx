import { Outlet, NavLink, useLocation } from 'react-router-dom'

const navItems = [
  { to: '/dashboard',     label: 'Dashboard',     icon: DashboardIcon },
  { to: '/inbox',         label: 'Inbox',         icon: InboxIcon },
  { to: '/claim-overview',label: 'Claim Overview', icon: ClaimIcon },
  { to: '/entities',      label: 'Entities',      icon: EntitiesIcon },
  { to: '/forensics',     label: 'Forensics',     icon: ForensicsIcon },
  { to: '/duplicates',    label: 'Duplicates',    icon: DuplicatesIcon },
  { to: '/hitl-console',  label: 'HITL Console',  icon: HITLIcon },
  { to: '/sync',          label: 'Sync Queue',    icon: SyncIcon },
  { to: '/settings',      label: 'Settings',      icon: SettingsIcon },
]

function breadcrumb(pathname: string): string {
  const seg = pathname.split('/').filter(Boolean)
  return seg.map(s => s.replace(/-/g, ' ').replace(/\b\w/g, c => c.toUpperCase())).join(' / ')
}

export default function Layout() {
  const location = useLocation()

  return (
    <div className="flex h-screen bg-md-bg overflow-hidden">
      {/* ── Sidebar ─────────────────────────── */}
      <aside
        className="w-[220px] shrink-0 flex flex-col"
        style={{ background: '#0c0e14', borderRight: '1px solid rgba(70,69,85,0.3)' }}
      >
        {/* Logo */}
        <div className="h-14 flex items-center gap-3 px-4 shrink-0" style={{ borderBottom: '1px solid rgba(70,69,85,0.3)' }}>
          <div
            className="w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold shrink-0"
            style={{ background: 'linear-gradient(135deg, #4b4dd8 0%, #c3c0ff 100%)', color: '#fff' }}
          >
            H
          </div>
          <div>
            <div className="text-sm font-display font-semibold text-md-on-surface tracking-tight">Horus</div>
            <div className="text-[10px] text-md-on-surface-var tracking-widest uppercase">Claims</div>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex-1 py-3 overflow-y-auto space-y-0.5">
          <div className="px-4 mb-2 mt-1 text-[9px] font-semibold text-md-on-surface-var/60 uppercase tracking-[0.12em]">
            Workspace
          </div>
          {navItems.slice(0, 7).map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to}>
              {({ isActive }) => (
                <span className={`nav-item ${isActive ? 'active' : ''}`}>
                  <Icon />
                  <span>{label}</span>
                  {isActive && (
                    <span className="ml-auto w-1.5 h-1.5 rounded-full bg-md-primary" />
                  )}
                </span>
              )}
            </NavLink>
          ))}

          <div className="px-4 mb-2 mt-4 text-[9px] font-semibold text-md-on-surface-var/60 uppercase tracking-[0.12em]">
            Config
          </div>
          {navItems.slice(7).map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to}>
              {({ isActive }) => (
                <span className={`nav-item ${isActive ? 'active' : ''}`}>
                  <Icon />
                  <span>{label}</span>
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        {/* Bottom user chip */}
        <div className="px-3 pb-4 pt-2 shrink-0" style={{ borderTop: '1px solid rgba(70,69,85,0.3)' }}>
          <div className="flex items-center gap-2.5 px-2 py-2 rounded-lg hover:bg-md-container-high/60 cursor-pointer transition-colors">
            <div className="w-7 h-7 rounded-full bg-md-primary-container flex items-center justify-center text-white text-xs font-semibold shrink-0">
              A
            </div>
            <div className="min-w-0">
              <div className="text-xs font-medium text-md-on-surface truncate">Adjuster</div>
              <div className="text-[10px] text-md-on-surface-var truncate">admin@horus.io</div>
            </div>
          </div>
        </div>
      </aside>

      {/* ── Main area ───────────────────────── */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Header */}
        <header
          className="h-12 flex items-center px-4 gap-4 shrink-0"
          style={{ background: '#111319', borderBottom: '1px solid rgba(70,69,85,0.3)' }}
        >
          <div className="text-xs text-md-on-surface-var font-medium">
            {breadcrumb(location.pathname) || 'Dashboard'}
          </div>
          <div className="flex-1" />

          {/* Search */}
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-md-container-high text-md-on-surface-var text-xs cursor-text hover:bg-md-container-highest transition-colors w-48">
            <svg className="w-3.5 h-3.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-4.35-4.35M17 11A6 6 0 115 11a6 6 0 0112 0z" />
            </svg>
            <span>Search claims…</span>
            <span className="ml-auto text-[10px] text-md-on-surface-var/40">⌘K</span>
          </div>

          {/* Live indicator */}
          <div className="flex items-center gap-1.5">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-status-verified opacity-60" />
              <span className="relative inline-flex rounded-full h-2 w-2 bg-status-verified" />
            </span>
            <span className="text-xs text-md-on-surface-var">Live</span>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-auto p-4 bg-md-bg">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

/* ── Icons ─────────────────────────────────── */
function DashboardIcon() {
  return (
    <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zm10 0a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zm10 0a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
    </svg>
  )
}

function InboxIcon() {
  return (
    <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
    </svg>
  )
}

function ClaimIcon() {
  return (
    <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
    </svg>
  )
}

function EntitiesIcon() {
  return (
    <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
    </svg>
  )
}

function ForensicsIcon() {
  return (
    <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
    </svg>
  )
}

function DuplicatesIcon() {
  return (
    <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
    </svg>
  )
}

function HITLIcon() {
  return (
    <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
    </svg>
  )
}

function SettingsIcon() {
  return (
    <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
    </svg>
  )
}

function SyncIcon() {
  return (
    <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
    </svg>
  )
}

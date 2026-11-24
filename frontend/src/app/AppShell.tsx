import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '@/features/auth/AuthContext'
import { ChangePasswordModal } from '@/features/auth/ChangePasswordModal'
import { useSearch } from '@/features/search/SearchContext'
import { useTheme } from '@/features/theme/ThemeContext'
import { fetchAiracCurrent } from '@/services/notamApi'

const nav = [
  { to: '/overview', label: 'Overview' },
  { to: '/notams', label: 'NOTAMs' },
  { to: '/publications', label: 'AIP & AIC' },
  { to: '/library', label: 'AIS Library' },
  { to: '/complaints', label: 'Complaints' },
  { to: '/tasks', label: 'Tasks' },
  { to: '/audit', label: 'Audit' },
]

function roleLabel(role: string) {
  switch (role) {
    case 'ADMINISTRATOR':
      return 'Administrator'
    case 'EDITOR':
      return 'Editor'
    case 'VIEWER':
      return 'Viewer'
    default:
      return role
  }
}

export function AppShell() {
  const { user, lock, signOut } = useAuth()
  const { query, setQuery } = useSearch()
  const { theme, toggleTheme } = useTheme()
  const navigate = useNavigate()
  const [changePasswordOpen, setChangePasswordOpen] = useState(false)
  const airac = useQuery({ queryKey: ['airac-current'], queryFn: fetchAiracCurrent })
  const initials = (user?.displayName ?? '?')
    .split(/\s+/)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()

  const dateLabel = new Intl.DateTimeFormat('en-GB', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    timeZone: 'Africa/Kampala',
  }).format(new Date())

  const links = user?.role === 'ADMINISTRATOR'
    ? [...nav, { to: '/users', label: 'Users' }]
    : nav

  return (
    <div className="min-h-screen min-w-0 nav:grid nav:grid-cols-[240px_1fr]">
      <aside className="flex flex-col border-b border-[var(--color-border)] bg-[var(--color-panel)] nav:border-b-0 nav:border-r">
        <div className="flex items-center gap-3 px-4 py-4">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-brand text-white" aria-hidden>
            ✈
          </div>
          <div>
            <div className="font-display text-sm font-bold uppercase tracking-wide text-ink">
              AIS Doc Manager
            </div>
            <div className="text-xs text-muted">UCAA · AIM Entebbe</div>
          </div>
        </div>

        <nav
          className="flex gap-1 overflow-x-auto px-2 pb-3 nav:flex-col nav:overflow-visible"
          aria-label="Main"
        >
          {links.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                [
                  'whitespace-nowrap rounded-md px-3 py-2 text-sm font-medium',
                  isActive ? 'bg-sky-50 text-brand' : 'text-ink hover:bg-slate-50',
                ].join(' ')
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="mt-auto hidden border-t border-[var(--color-border)] px-4 py-4 nav:block">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-brand text-xs font-semibold text-white">
              {initials}
            </div>
            <div className="min-w-0">
              <div className="truncate text-sm font-semibold text-ink">{user?.displayName}</div>
              <div className="truncate text-xs text-muted">Safety · Efficiency · Regularity</div>
            </div>
          </div>
        </div>
      </aside>

      <div className="flex min-w-0 flex-col">
        <header className="flex flex-wrap items-center gap-3 border-b border-[var(--color-border)] bg-[var(--color-panel)] px-4 py-3">
          <label className="sr-only" htmlFor="global-search">Search</label>
          <input
            id="global-search"
            type="search"
            value={query}
            onChange={(e) => {
              const value = e.target.value
              setQuery(value)
              if (value.trim()) {
                navigate('/overview')
              }
            }}
            placeholder="Search NOTAMs, publications, library, complaints, tasks…"
            className="min-w-0 flex-1 rounded-md border border-[var(--color-border)] bg-[var(--color-panel)] px-3 py-2 text-sm text-ink"
          />
          <div className="flex flex-wrap items-center gap-3 text-sm">
            <div className="font-mono text-xs text-muted">
              {dateLabel}
              {airac.data ? (
                <>
                  {' · AIRAC '}
                  <span className="font-semibold text-airac">{airac.data.ident}</span>
                </>
              ) : null}
            </div>
            <div className="text-ink">
              <span className="font-semibold">{user?.displayName}</span>
              <span className="text-muted">
                {' '}
                · {roleLabel(user?.role ?? '')}
                {user?.designation ? ` · ${user.designation}` : ''}
              </span>
            </div>
            <button
              type="button"
              className="font-medium text-brand"
              onClick={toggleTheme}
              aria-label={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
            >
              {theme === 'dark' ? 'Light' : 'Dark'}
            </button>
            <button type="button" className="font-medium text-brand" onClick={() => setChangePasswordOpen(true)}>
              Change password
            </button>
            <button type="button" className="font-medium text-brand" onClick={lock}>
              Lock
            </button>
            <button type="button" className="font-medium text-brand" onClick={() => void signOut()}>
              Sign out
            </button>
          </div>
        </header>

        <main className="min-w-0 flex-1 p-4 compact:p-4 nav:p-6">
          <Outlet />
        </main>
      </div>

      <ChangePasswordModal open={changePasswordOpen} onClose={() => setChangePasswordOpen(false)} />
    </div>
  )
}

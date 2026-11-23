import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '@/features/auth/AuthContext'
import { ChangePasswordModal } from '@/features/auth/ChangePasswordModal'

const nav = [
  { to: '/overview', label: 'Overview' },
  { to: '/notams', label: 'NOTAMs' },
  { to: '/publications', label: 'AIP & AIC' },
  { to: '/library', label: 'AIS Library' },
  { to: '/complaints', label: 'Complaints' },
  { to: '/tasks', label: 'Tasks' },
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
  const [changePasswordOpen, setChangePasswordOpen] = useState(false)
  const initials = (user?.displayName ?? '?')
    .split(/\s+/)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()

  const links = user?.role === 'ADMINISTRATOR'
    ? [...nav, { to: '/users', label: 'Users' }]
    : nav

  return (
    <div className="min-h-screen lg:grid lg:grid-cols-[240px_1fr]">
      <aside className="flex flex-col border-b border-slate-200 bg-white lg:border-b-0 lg:border-r">
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

        <nav className="flex gap-1 overflow-x-auto px-2 pb-3 lg:flex-col lg:overflow-visible" aria-label="Main">
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

        <div className="mt-auto border-t border-slate-100 px-4 py-4">
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
        <header className="flex flex-wrap items-center gap-3 border-b border-slate-200 bg-white px-4 py-3">
          <label className="sr-only" htmlFor="global-search">Search</label>
          <input
            id="global-search"
            type="search"
            placeholder="Search NOTAM numbers, titles, originators..."
            className="min-w-0 flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          <div className="flex flex-wrap items-center gap-3 text-sm">
            <div className="text-ink">
              <span className="font-semibold">{user?.displayName}</span>
              <span className="text-muted">
                {' '}
                · {roleLabel(user?.role ?? '')}
                {user?.designation ? ` · ${user.designation}` : ''}
              </span>
            </div>
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

        <main className="flex-1 p-4 md:p-6">
          <Outlet />
        </main>
      </div>

      <ChangePasswordModal open={changePasswordOpen} onClose={() => setChangePasswordOpen(false)} />
    </div>
  )
}

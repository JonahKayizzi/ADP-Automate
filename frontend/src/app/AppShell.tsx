import { NavLink, Outlet } from 'react-router-dom'

const nav = [
  { to: '/overview', label: 'Overview' },
  { to: '/notams', label: 'NOTAMs' },
  { to: '/publications', label: 'AIP & AIC' },
  { to: '/library', label: 'AIS Library' },
  { to: '/complaints', label: 'Complaints' },
  { to: '/tasks', label: 'Tasks' },
]

export function AppShell() {
  return (
    <div className="min-h-screen lg:grid lg:grid-cols-[240px_1fr]">
      <aside className="border-b border-slate-200 bg-white lg:border-b-0 lg:border-r">
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
          {nav.map((item) => (
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

        <div className="mt-auto hidden border-t border-slate-100 px-4 py-4 text-xs text-muted lg:block">
          Safety · Efficiency · Regularity
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
          <div className="font-mono text-xs text-muted">
            AIRAC setup pending
          </div>
        </header>

        <main className="flex-1 p-4 md:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

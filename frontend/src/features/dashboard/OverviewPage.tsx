import { useQuery } from '@tanstack/react-query'
import { useDeferredValue } from 'react'
import { Link } from 'react-router-dom'
import { useSearch } from '@/features/search/SearchContext'
import * as notamApi from '@/services/notamApi'
import * as publicationApi from '@/services/publicationApi'
import * as searchApi from '@/services/searchApi'
import type { AiracCycle, AttentionItem } from '@/types/notam'

function hitHref(href: string, id: string) {
  const url = new URL(href, window.location.origin)
  url.searchParams.set('focus', id)
  return `${url.pathname}${url.search}`
}

function formatDate(iso: string) {
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(iso + 'T00:00:00Z'))
}

function AiracCard({ cycle }: { cycle: AiracCycle }) {
  const deadlinePassed = cycle.daysToDeadline < 0
  return (
    <div className={`rounded-lg border p-3 ${cycle.inForce ? 'border-airac/40 bg-pink-50/40' : 'border-slate-200 bg-white'}`}>
      <div className="flex items-center justify-between gap-2">
        <span className="font-mono text-sm font-semibold text-airac">{cycle.ident}</span>
        {cycle.inForce ? (
          <span className="text-[10px] font-bold uppercase tracking-wide text-airac">In force</span>
        ) : null}
      </div>
      <p className="mt-2 text-xs text-muted">Effective {formatDate(cycle.effective)}</p>
      {cycle.inForce ? null : deadlinePassed ? (
        <p className="mt-1 text-xs font-semibold text-danger">
          Distribution deadline passed {formatDate(cycle.distributeBy)}
        </p>
      ) : (
        <p className="mt-1 text-xs text-muted">
          Distribute by <strong>{formatDate(cycle.distributeBy)}</strong>
          {' · '}
          {cycle.daysToDeadline} days
        </p>
      )}
      <p className="mt-1 text-xs text-muted">
        {cycle.publicationCount} AIRAC publication{cycle.publicationCount === 1 ? '' : 's'}
      </p>
    </div>
  )
}

function statusChipClass(status: string) {
  if (status === 'Distributed') return 'bg-emerald-50 text-ok'
  if (status === 'Cancelled') return 'bg-slate-100 text-slate-600'
  return 'bg-amber-50 text-warn'
}

const groupLabels: Record<string, string> = {
  notams: 'NOTAMs',
  publications: 'Publications',
  library: 'AIS library',
  complaints: 'Complaints',
  tasks: 'Tasks',
}

function SearchResults({ query }: { query: string }) {
  const { setQuery } = useSearch()
  const deferred = useDeferredValue(query.trim())
  const results = useQuery({
    queryKey: ['search', deferred],
    queryFn: () => searchApi.searchAll(deferred),
    enabled: deferred.length >= 2,
  })

  if (deferred.length < 2) {
    return <p className="text-sm text-muted">Type at least two characters to search.</p>
  }

  if (results.isLoading) {
    return <p className="text-sm text-muted">Searching…</p>
  }

  const groups = results.data?.groups
  const entries = groups
    ? (Object.keys(groupLabels) as (keyof typeof groupLabels)[])
        .map((key) => ({ key, label: groupLabels[key], hits: groups[key as keyof typeof groups] ?? [] }))
        .filter((g) => g.hits.length > 0)
    : []

  if (entries.length === 0) {
    return <p className="text-sm text-muted">No matches for “{deferred}”.</p>
  }

  return (
    <div className="space-y-5">
      {entries.map((group) => (
        <div key={group.key}>
          <h2 className="font-display text-lg font-semibold text-ink">{group.label}</h2>
          <ul className="mt-2 space-y-2">
            {group.hits.map((hit) => (
              <li key={`${hit.type}-${hit.id}`}>
                <Link
                  to={hitHref(hit.href, hit.id)}
                  onClick={() => setQuery('')}
                  className="block rounded-lg border border-slate-200 bg-white px-3 py-2 hover:border-brand/40"
                >
                  <p className="text-sm font-semibold text-ink">{hit.title}</p>
                  {hit.subtitle ? <p className="text-xs text-muted">{hit.subtitle}</p> : null}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </div>
  )
}

function AttentionList({ items }: { items: AttentionItem[] }) {
  if (items.length === 0) {
    return <p className="mt-2 text-sm text-muted">Nothing overdue or incomplete yet.</p>
  }
  return (
    <ul className="mt-3 space-y-2">
      {items.map((item, idx) => (
        <li key={`${item.type}-${item.title}-${idx}`}>
          <Link
            to={item.href}
            className={`flex flex-wrap items-start justify-between gap-2 rounded-lg border px-3 py-2 ${
              item.severity === 'critical'
                ? 'border-red-200 bg-red-50/60'
                : 'border-amber-200 bg-amber-50/50'
            }`}
          >
            <div>
              <p className="text-sm font-semibold text-ink">{item.title}</p>
              <p className="text-xs text-muted">{item.detail}</p>
            </div>
            <span
              className={`rounded px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide ${
                item.severity === 'critical' ? 'bg-red-100 text-danger' : 'bg-amber-100 text-warn'
              }`}
            >
              {item.severity}
            </span>
          </Link>
        </li>
      ))}
    </ul>
  )
}

export function OverviewPage() {
  const { query } = useSearch()
  const summary = useQuery({
    queryKey: ['dashboard'],
    queryFn: notamApi.fetchDashboardSummary,
  })
  const publications = useQuery({
    queryKey: ['publications'],
    queryFn: publicationApi.listPublications,
  })

  if (query.trim()) {
    return (
      <section className="space-y-4">
        <div>
          <h1 className="font-display text-3xl font-semibold text-ink">Search</h1>
          <p className="mt-2 text-muted">Results across NOTAMs, publications, library, complaints and tasks.</p>
        </div>
        <SearchResults query={query} />
      </section>
    )
  }

  const tiles = [
    { label: 'NOTAMs logged', value: summary.data?.notamsLogged ?? '—', to: '/notams' },
    { label: 'Processing files not signed off', value: summary.data?.processingFilesNotSignedOff ?? 0, to: '/notams' },
    { label: 'Publications not yet effective', value: summary.data?.publicationsNotYetEffective ?? 0, to: '/publications' },
    { label: 'Open complaints', value: summary.data?.openComplaints ?? 0, to: '/complaints' },
    { label: 'Overdue tasks', value: summary.data?.overdueTasks ?? 0, to: '/tasks' },
  ]

  const airac = summary.data?.airac ?? []
  const day = airac[0]?.dayWithinCycle
  const today = summary.data?.today ?? ''
  const comingIntoEffect = (publications.data ?? [])
    .filter((p) => p.status !== 'Cancelled' && p.effective >= today)
    .slice(0, 6)
  const attention = summary.data?.needsAttention ?? []

  return (
    <section className="space-y-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink">AIS document control</h1>
        <p className="mt-2 max-w-3xl text-muted">
          NOTAM metadata, AIP/AIC publications, AIS library, complaints and tasks for AIM Entebbe.
        </p>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-4">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <h2 className="font-display text-lg font-semibold">AIRAC cycle</h2>
          <p className="text-xs text-muted">
            {day != null ? `Day ${day} of 28` : '…'} · distribution deadline is 42 days before the effective date
          </p>
        </div>
        {summary.isLoading ? <p className="text-sm text-muted">Loading AIRAC…</p> : null}
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {airac.map((cycle) => (
            <AiracCard key={cycle.ident} cycle={cycle} />
          ))}
        </div>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        {tiles.map((tile) => (
          <Link
            key={tile.label}
            to={tile.to}
            className="rounded-xl border border-slate-200 bg-white p-4 hover:border-brand/40"
          >
            <div className="font-display text-3xl font-semibold text-ink">{tile.value}</div>
            <div className="mt-1 text-sm text-muted">{tile.label}</div>
          </Link>
        ))}
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-4">
        <h2 className="font-display text-lg font-semibold">Needs attention</h2>
        <AttentionList items={attention} />
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-4">
        <div className="mb-3 flex items-center justify-between gap-2">
          <h2 className="font-display text-lg font-semibold">Coming into effect</h2>
          <Link to="/publications" className="text-sm font-medium text-brand">
            All publications
          </Link>
        </div>
        {comingIntoEffect.length === 0 ? (
          <p className="text-sm text-muted">No upcoming publications.</p>
        ) : (
          <ul className="space-y-3">
            {comingIntoEffect.map((pub) => (
              <li key={pub.id} className="flex flex-wrap items-start justify-between gap-2 border-b border-slate-100 pb-3 last:border-0 last:pb-0">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    {pub.airacIdent ? (
                      <span className="rounded bg-pink-50 px-2 py-0.5 font-mono text-xs font-semibold text-airac">
                        {pub.airacIdent}
                      </span>
                    ) : (
                      <span className="rounded bg-slate-100 px-2 py-0.5 text-xs text-muted">
                        {formatDate(pub.effective).slice(0, 6)}
                      </span>
                    )}
                    <span className="font-semibold text-ink">
                      {pub.type} {pub.number}
                    </span>
                  </div>
                  <p className="mt-1 text-sm text-muted">
                    {pub.title} · {formatDate(pub.effective)}
                  </p>
                </div>
                <span className={`rounded px-2 py-0.5 text-xs font-semibold ${statusChipClass(pub.status)}`}>
                  {pub.status}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}

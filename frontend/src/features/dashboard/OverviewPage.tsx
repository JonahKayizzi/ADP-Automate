import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import * as notamApi from '@/services/notamApi'
import type { AiracCycle } from '@/types/notam'

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
      {cycle.inForce ? (
        <p className="mt-1 text-xs text-muted">No AIRAC publications yet</p>
      ) : deadlinePassed ? (
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

export function OverviewPage() {
  const summary = useQuery({
    queryKey: ['dashboard'],
    queryFn: notamApi.fetchDashboardSummary,
  })

  const tiles = [
    { label: 'NOTAMs logged', value: summary.data?.notamsLogged ?? '—', to: '/notams' },
    { label: 'Processing files not signed off', value: summary.data?.processingFilesNotSignedOff ?? 0, to: '/notams' },
    { label: 'Publications not yet effective', value: summary.data?.publicationsNotYetEffective ?? 0, to: '/publications' },
    { label: 'Open complaints', value: summary.data?.openComplaints ?? 0, to: '/complaints' },
    { label: 'Overdue tasks', value: summary.data?.overdueTasks ?? 0, to: '/tasks' },
  ]

  const airac = summary.data?.airac ?? []
  const day = airac[0]?.dayWithinCycle

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
        <p className="mt-2 text-sm text-muted">Nothing overdue or incomplete yet.</p>
      </div>
    </section>
  )
}

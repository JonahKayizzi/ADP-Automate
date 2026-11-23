import { useQuery } from '@tanstack/react-query'

async function fetchPing() {
  const res = await fetch('/api/ping')
  if (!res.ok) throw new Error('API unavailable')
  return res.json() as Promise<{ status: string; service: string; version: string }>
}

export function OverviewPage() {
  const ping = useQuery({ queryKey: ['ping'], queryFn: fetchPing, retry: false })

  return (
    <section className="space-y-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink">AIS document control</h1>
        <p className="mt-2 max-w-3xl text-muted">
          NOTAM metadata, AIP/AIC publications, AIS library, complaints and tasks for AIM Entebbe.
        </p>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-4">
        <h2 className="font-display text-lg font-semibold">Foundation status</h2>
        <p className="mt-2 text-sm text-muted">
          Phase 1 scaffolds the monorepo, API health check and shell UI. Domain modules follow.
        </p>
        <div className="mt-4 rounded-md bg-slate-50 px-3 py-2 font-mono text-sm">
          {ping.isLoading && 'Checking API…'}
          {ping.isError && 'API offline — start the Spring Boot backend on :8080'}
          {ping.data && (
            <span>
              API {ping.data.status} · {ping.data.service} · {ping.data.version}
            </span>
          )}
        </div>
      </div>
    </section>
  )
}

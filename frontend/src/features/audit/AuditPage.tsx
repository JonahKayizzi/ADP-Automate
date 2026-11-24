import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import * as auditApi from '@/services/auditApi'

function formatWhen(iso: string) {
  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(iso))
}

export function AuditPage() {
  const [entityType, setEntityType] = useState('')
  const [entityId, setEntityId] = useState('')
  const events = useQuery({
    queryKey: ['audit', entityType, entityId],
    queryFn: () =>
      auditApi.listAudit({
        entityType: entityType || undefined,
        entityId: entityId || undefined,
        limit: 100,
      }),
  })

  return (
    <section className="space-y-4">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink">Audit &amp; history</h1>
        <p className="mt-2 max-w-3xl text-muted">
          Record changes, signatures and form revisions. Filter by entity type and id when tracing a
          specific document.
        </p>
      </div>

      <div className="flex flex-wrap gap-3 rounded-xl border border-slate-200 bg-white p-4">
        <label className="text-sm font-semibold">
          Entity type
          <input
            className="mt-1 block w-48 rounded-md border px-3 py-2 text-sm font-normal"
            value={entityType}
            onChange={(e) => setEntityType(e.target.value)}
            placeholder="e.g. processing_form"
          />
        </label>
        <label className="text-sm font-semibold">
          Entity id
          <input
            className="mt-1 block w-72 rounded-md border px-3 py-2 text-sm font-normal"
            value={entityId}
            onChange={(e) => setEntityId(e.target.value)}
            placeholder="UUID"
          />
        </label>
      </div>

      {events.isLoading ? <p className="text-sm text-muted">Loading audit events…</p> : null}
      {events.isError ? <p className="text-sm text-danger">Could not load audit events.</p> : null}

      <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
        <table className="w-full min-w-[40rem] border-collapse text-sm">
          <thead>
            <tr className="bg-slate-50 text-left text-xs uppercase tracking-wide text-muted">
              <th className="px-3 py-2">When</th>
              <th className="px-3 py-2">Actor</th>
              <th className="px-3 py-2">Action</th>
              <th className="px-3 py-2">Entity</th>
              <th className="px-3 py-2">Detail</th>
            </tr>
          </thead>
          <tbody>
            {(events.data ?? []).map((event) => (
              <tr key={event.id} className="border-t border-slate-100 align-top">
                <td className="px-3 py-2 whitespace-nowrap text-xs text-muted">
                  {formatWhen(event.occurredAt)}
                </td>
                <td className="px-3 py-2 font-mono text-xs">{event.actorUsername ?? '—'}</td>
                <td className="px-3 py-2 font-semibold">{event.action}</td>
                <td className="px-3 py-2 font-mono text-xs">
                  {event.entityType ?? '—'}
                  {event.entityId ? (
                    <>
                      <br />
                      <span className="text-muted">{event.entityId}</span>
                    </>
                  ) : null}
                </td>
                <td className="px-3 py-2 text-xs text-muted">
                  <pre className="max-w-md overflow-x-auto whitespace-pre-wrap font-mono">
                    {JSON.stringify(event.detail ?? {}, null, 0)}
                  </pre>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {(events.data ?? []).length === 0 && !events.isLoading ? (
          <p className="px-3 py-4 text-sm text-muted">No audit events yet.</p>
        ) : null}
      </div>
    </section>
  )
}

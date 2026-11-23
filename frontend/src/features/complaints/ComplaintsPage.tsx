import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, type FormEvent } from 'react'
import { useAuth } from '@/features/auth/AuthContext'
import { ApiError } from '@/services/api'
import * as complaintsApi from '@/services/complaintsApi'
import type { Complaint, UpsertComplaint } from '@/types/complaint'

function todayIso() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Africa/Kampala' }).format(new Date())
}

function formatDate(iso: string) {
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(iso + 'T00:00:00Z'))
}

const emptyForm = (): UpsertComplaint => ({
  title: '',
  complainant: '',
  received: todayIso(),
  channel: 'Email',
  status: 'Open',
  description: '',
  actionTaken: '',
})

export function ComplaintsPage() {
  const { user } = useAuth()
  const canEdit = user?.role !== 'VIEWER'
  const qc = useQueryClient()
  const [filter, setFilter] = useState('Open')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Complaint | null>(null)
  const [form, setForm] = useState<UpsertComplaint>(emptyForm())
  const [error, setError] = useState('')
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)

  const complaints = useQuery({
    queryKey: ['complaints', filter],
    queryFn: () => complaintsApi.listComplaints(filter),
  })
  const channels = useQuery({ queryKey: ['complaint-channels'], queryFn: complaintsApi.listChannels })

  useEffect(() => {
    if (!confirmDeleteId) return
    const t = window.setTimeout(() => setConfirmDeleteId(null), 4000)
    return () => window.clearTimeout(t)
  }, [confirmDeleteId])

  const save = useMutation({
    mutationFn: async () => {
      if (editing) return complaintsApi.updateComplaint(editing.id, form)
      return complaintsApi.createComplaint(form)
    },
    onSuccess: async () => {
      setOpen(false)
      setEditing(null)
      setForm(emptyForm())
      setError('')
      await qc.invalidateQueries({ queryKey: ['complaints'] })
      await qc.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (err) => setError(err instanceof ApiError ? err.detail : 'Could not save complaint.'),
  })

  const remove = useMutation({
    mutationFn: (id: string) => complaintsApi.deleteComplaint(id),
    onSuccess: async () => {
      setConfirmDeleteId(null)
      await qc.invalidateQueries({ queryKey: ['complaints'] })
      await qc.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })

  function openCreate() {
    setEditing(null)
    setForm(emptyForm())
    setError('')
    setOpen(true)
  }

  function openEdit(row: Complaint) {
    setEditing(row)
    setForm({
      title: row.title,
      complainant: row.complainant,
      received: row.received,
      channel: row.channel,
      status: row.status,
      description: row.description ?? '',
      actionTaken: row.actionTaken ?? '',
    })
    setError('')
    setOpen(true)
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    save.mutate()
  }

  const rows = complaints.data ?? []

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-semibold text-ink">Customer complaints log</h1>
          <p className="mt-2 max-w-3xl text-muted">
            Track complaints received by AIM Entebbe, the channel used, and action taken until closed.
          </p>
        </div>
        {canEdit ? (
          <button type="button" className="rounded-md bg-brand px-4 py-2 text-sm font-semibold text-white" onClick={openCreate}>
            Add complaint
          </button>
        ) : null}
      </div>

      <div className="flex flex-wrap gap-2">
        {['Open', 'Closed', 'All'].map((f) => (
          <button
            key={f}
            type="button"
            className={`rounded-md px-3 py-1.5 text-sm font-semibold ${
              filter === f ? 'bg-brand text-white' : 'border bg-white text-ink'
            }`}
            onClick={() => setFilter(f)}
          >
            {f}
          </button>
        ))}
      </div>

      {complaints.isLoading ? <p className="text-sm text-muted">Loading complaints…</p> : null}
      {!complaints.isLoading && rows.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-300 bg-white p-8 text-center text-muted">
          No complaints in this filter.
        </div>
      ) : null}

      <div className="space-y-3">
        {rows.map((row) => (
          <article key={row.id} className="rounded-xl border border-slate-200 bg-white p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="font-display text-lg font-semibold text-ink">{row.title}</h2>
                  <span
                    className={`rounded px-2 py-0.5 text-xs font-semibold ${
                      row.status === 'Open' ? 'bg-amber-50 text-warn' : 'bg-slate-100 text-slate-600'
                    }`}
                  >
                    {row.status}
                  </span>
                </div>
                <p className="mt-2 text-sm text-muted">
                  {row.complainant} · Received {formatDate(row.received)} · {row.channel}
                </p>
                {row.description ? (
                  <p className="mt-3 text-sm">
                    <span className="font-semibold">Description:</span> {row.description}
                  </p>
                ) : null}
                {row.actionTaken ? (
                  <p className="mt-2 text-sm">
                    <span className="font-semibold">Action taken:</span> {row.actionTaken}
                  </p>
                ) : null}
              </div>
              {canEdit ? (
                <div className="flex gap-2">
                  <button type="button" className="rounded-md border px-3 py-1.5 text-sm" onClick={() => openEdit(row)}>
                    Edit
                  </button>
                  <button
                    type="button"
                    className="rounded-md border border-red-200 px-3 py-1.5 text-sm text-danger"
                    onClick={() => {
                      if (confirmDeleteId === row.id) remove.mutate(row.id)
                      else setConfirmDeleteId(row.id)
                    }}
                  >
                    {confirmDeleteId === row.id ? 'Confirm delete' : 'Delete'}
                  </button>
                </div>
              ) : null}
            </div>
          </article>
        ))}
      </div>

      {open ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
          <form onSubmit={onSubmit} className="w-full max-w-lg rounded-xl border bg-white p-6 shadow-lg">
            <div className="flex items-start justify-between">
              <h2 className="font-display text-xl font-semibold">{editing ? 'Edit complaint' : 'Add complaint'}</h2>
              <button type="button" className="text-sm text-muted" onClick={() => setOpen(false)}>
                Close
              </button>
            </div>
            <div className="mt-4 space-y-3">
              <label className="block text-sm font-semibold">
                Subject
                <input className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
              </label>
              <label className="block text-sm font-semibold">
                Complainant
                <input className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal" value={form.complainant} onChange={(e) => setForm({ ...form, complainant: e.target.value })} required />
              </label>
              <div className="grid gap-3 sm:grid-cols-2">
                <label className="block text-sm font-semibold">
                  Received
                  <input type="date" className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal" value={form.received} onChange={(e) => setForm({ ...form, received: e.target.value })} required />
                </label>
                <label className="block text-sm font-semibold">
                  Channel
                  <select className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal" value={form.channel} onChange={(e) => setForm({ ...form, channel: e.target.value })}>
                    {(channels.data ?? [form.channel]).map((c) => (
                      <option key={c} value={c}>
                        {c}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <label className="block text-sm font-semibold">
                Status
                <select className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                  <option>Open</option>
                  <option>Closed</option>
                </select>
              </label>
              <label className="block text-sm font-semibold">
                Description
                <textarea className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal" rows={3} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
              </label>
              <label className="block text-sm font-semibold">
                Action taken
                <textarea className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal" rows={3} value={form.actionTaken} onChange={(e) => setForm({ ...form, actionTaken: e.target.value })} />
              </label>
            </div>
            {error ? <p className="mt-3 text-sm text-danger">{error}</p> : null}
            <div className="mt-4 flex justify-end gap-2">
              <button type="button" className="rounded-md border px-3 py-2 text-sm" onClick={() => setOpen(false)}>
                Cancel
              </button>
              <button type="submit" disabled={save.isPending} className="rounded-md bg-brand px-3 py-2 text-sm font-semibold text-white disabled:opacity-60">
                {save.isPending ? 'Saving…' : editing ? 'Save changes' : 'Add complaint'}
              </button>
            </div>
          </form>
        </div>
      ) : null}
    </section>
  )
}

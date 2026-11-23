import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, type FormEvent } from 'react'
import { useAuth } from '@/features/auth/AuthContext'
import { ApiError } from '@/services/api'
import * as notamApi from '@/services/notamApi'
import type { Notam, UpsertNotam } from '@/types/notam'

function todayIso() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Africa/Kampala' }).format(new Date())
}

function formatIssued(iso: string) {
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(iso + 'T00:00:00Z'))
}

const emptyForm = (): UpsertNotam => ({
  number: '',
  issued: todayIso(),
  title: '',
  originator: 'AIM Entebbe',
  validTo: '',
})

export function NotamsPage() {
  const { user } = useAuth()
  const canEdit = user?.role !== 'VIEWER'
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Notam | null>(null)
  const [form, setForm] = useState<UpsertNotam>(emptyForm())
  const [error, setError] = useState('')
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)

  const notams = useQuery({ queryKey: ['notams'], queryFn: notamApi.listNotams })
  const originators = useQuery({ queryKey: ['originators'], queryFn: notamApi.listOriginators })

  useEffect(() => {
    if (!confirmDeleteId) return
    const t = window.setTimeout(() => setConfirmDeleteId(null), 4000)
    return () => window.clearTimeout(t)
  }, [confirmDeleteId])

  const save = useMutation({
    mutationFn: async () => {
      if (editing) return notamApi.updateNotam(editing.id, form)
      return notamApi.createNotam(form)
    },
    onSuccess: async () => {
      setOpen(false)
      setEditing(null)
      setForm(emptyForm())
      setError('')
      await qc.invalidateQueries({ queryKey: ['notams'] })
      await qc.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (err) => setError(err instanceof ApiError ? err.detail : 'Could not save NOTAM.'),
  })

  const remove = useMutation({
    mutationFn: (id: string) => notamApi.deleteNotam(id),
    onSuccess: async () => {
      setConfirmDeleteId(null)
      await qc.invalidateQueries({ queryKey: ['notams'] })
      await qc.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })

  function openCreate() {
    setEditing(null)
    setForm(emptyForm())
    setError('')
    setOpen(true)
  }

  function openEdit(notam: Notam) {
    setEditing(notam)
    setForm({
      number: notam.number,
      issued: notam.issued,
      title: notam.title,
      originator: notam.originator,
      validTo: notam.validTo ?? '',
    })
    setError('')
    setOpen(true)
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    save.mutate()
  }

  const rows = notams.data ?? []

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-semibold text-ink">NOTAM publication log</h1>
          <p className="mt-2 max-w-3xl text-muted">
            Each NOTAM will carry its processing file in a later phase: raw data quality check, metadata,
            processing and product quality check.
          </p>
        </div>
        {canEdit ? (
          <button
            type="button"
            className="rounded-md bg-brand px-4 py-2 text-sm font-semibold text-white"
            onClick={openCreate}
          >
            Add NOTAM
          </button>
        ) : null}
      </div>

      {notams.isLoading ? <p className="text-sm text-muted">Loading NOTAMs…</p> : null}
      {notams.isError ? <p className="text-sm text-danger">Could not load NOTAMs.</p> : null}

      {!notams.isLoading && rows.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-300 bg-white p-8 text-center text-muted">
          No NOTAMs logged yet.
        </div>
      ) : null}

      <div className="space-y-3">
        {rows.map((notam) => (
          <article key={notam.id} className="rounded-xl border border-slate-200 bg-white p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <span className="rounded bg-pink-50 px-2 py-0.5 font-mono text-sm font-semibold text-airac">
                    {notam.number}
                  </span>
                  <h2 className="font-display text-lg font-semibold text-ink">{notam.title}</h2>
                  {notam.example ? (
                    <span className="rounded border border-dashed border-slate-300 px-2 py-0.5 text-xs text-muted">
                      Example
                    </span>
                  ) : null}
                </div>
                <p className="mt-2 text-sm text-muted">
                  Originator: <strong className="text-ink">{notam.originator}</strong>
                  {' · '}Issued: <strong className="text-ink">{formatIssued(notam.issued)}</strong>
                  {notam.validTo ? (
                    <>
                      {' · '}Valid until: <span className="font-mono text-ink">{notam.validTo}</span>
                    </>
                  ) : null}
                </p>
              </div>
              {canEdit ? (
                <div className="flex gap-2">
                  <button
                    type="button"
                    className="rounded-md border px-3 py-1.5 text-sm"
                    onClick={() => openEdit(notam)}
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    className="rounded-md border border-red-200 px-3 py-1.5 text-sm text-danger"
                    onClick={() => {
                      if (confirmDeleteId === notam.id) remove.mutate(notam.id)
                      else setConfirmDeleteId(notam.id)
                    }}
                  >
                    {confirmDeleteId === notam.id ? 'Confirm delete' : 'Delete'}
                  </button>
                </div>
              ) : null}
            </div>
            <div className="mt-4 rounded-lg bg-slate-50 px-3 py-2 text-xs text-muted">
              Processing file panel arrives with the OF/82–83d forms in the next slice.
            </div>
          </article>
        ))}
      </div>

      {open ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
          <form onSubmit={onSubmit} className="w-full max-w-lg rounded-xl border bg-white p-6 shadow-lg">
            <div className="flex items-start justify-between">
              <h2 className="font-display text-xl font-semibold">
                {editing ? 'Edit NOTAM' : 'Add NOTAM'}
              </h2>
              <button type="button" className="text-sm text-muted" onClick={() => setOpen(false)}>
                Close
              </button>
            </div>
            <div className="mt-4 space-y-3">
              <label className="block text-sm font-semibold">
                Number
                <input
                  className="mt-1 w-full rounded-md border px-3 py-2 font-mono text-sm font-normal"
                  value={form.number}
                  onChange={(e) => setForm({ ...form, number: e.target.value })}
                  placeholder="A0234/26"
                  required
                />
              </label>
              <label className="block text-sm font-semibold">
                Subject
                <input
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                  value={form.title}
                  onChange={(e) => setForm({ ...form, title: e.target.value })}
                  required
                />
              </label>
              <label className="block text-sm font-semibold">
                Originator
                <select
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                  value={form.originator}
                  onChange={(e) => setForm({ ...form, originator: e.target.value })}
                >
                  {(originators.data ?? [form.originator]).map((o) => (
                    <option key={o} value={o}>
                      {o}
                    </option>
                  ))}
                </select>
              </label>
              <label className="block text-sm font-semibold">
                Issued
                <input
                  type="date"
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                  value={form.issued}
                  onChange={(e) => setForm({ ...form, issued: e.target.value })}
                  required
                />
              </label>
              <label className="block text-sm font-semibold">
                Valid until (item C)
                <input
                  className="mt-1 w-full rounded-md border px-3 py-2 font-mono text-sm font-normal"
                  value={form.validTo}
                  onChange={(e) => setForm({ ...form, validTo: e.target.value })}
                  placeholder="2611152359 or PERM"
                />
              </label>
            </div>
            {error ? <p className="mt-3 text-sm text-danger">{error}</p> : null}
            <div className="mt-4 flex justify-end gap-2">
              <button type="button" className="rounded-md border px-3 py-2 text-sm" onClick={() => setOpen(false)}>
                Cancel
              </button>
              <button
                type="submit"
                disabled={save.isPending}
                className="rounded-md bg-brand px-3 py-2 text-sm font-semibold text-white disabled:opacity-60"
              >
                {save.isPending ? 'Saving…' : editing ? 'Save changes' : 'Add NOTAM'}
              </button>
            </div>
          </form>
        </div>
      ) : null}
    </section>
  )
}

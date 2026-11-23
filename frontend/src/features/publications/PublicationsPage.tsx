import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { AttachmentsPanel } from '@/features/attachments/AttachmentsPanel'
import { ProcessingFilePanel } from '@/features/processing/ProcessingFilePanel'
import { useAuth } from '@/features/auth/AuthContext'
import { ApiError } from '@/services/api'
import * as publicationApi from '@/services/publicationApi'
import type { AiracOption, Publication, PublicationFilter, UpsertPublication } from '@/types/publication'

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

function isAiracType(type: string) {
  return type.startsWith('AIRAC ')
}

function statusClass(pub: Publication) {
  if (pub.status === 'Distributed') return 'bg-emerald-50 text-ok'
  if (pub.status === 'Cancelled') return 'bg-slate-100 text-slate-600'
  if (
    pub.status === 'Draft' &&
    pub.airac &&
    pub.daysToDeadline != null &&
    pub.daysToDeadline < 0
  ) {
    return 'bg-red-50 text-danger'
  }
  return 'bg-amber-50 text-warn'
}

const filters: { id: PublicationFilter; label: string }[] = [
  { id: 'ALL', label: 'All' },
  { id: 'AIRAC', label: 'AIRAC' },
  { id: 'NON_AIRAC', label: 'Non-AIRAC' },
  { id: 'AIC', label: 'AIC' },
]

function defaultForm(airacOptions: AiracOption[]): UpsertPublication {
  const next = airacOptions.find((o) => o.nextDefault) ?? airacOptions[0]
  return {
    type: 'AIRAC AIP AMDT',
    number: '',
    title: '',
    effective: next?.effective ?? todayIso(),
    status: 'Draft',
  }
}

export function PublicationsPage() {
  const { user } = useAuth()
  const canEdit = user?.role !== 'VIEWER'
  const qc = useQueryClient()
  const [filter, setFilter] = useState<PublicationFilter>('ALL')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Publication | null>(null)
  const [form, setForm] = useState<UpsertPublication>(defaultForm([]))
  const [error, setError] = useState('')
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)

  const publications = useQuery({ queryKey: ['publications'], queryFn: publicationApi.listPublications })
  const types = useQuery({ queryKey: ['publication-types'], queryFn: publicationApi.listPublicationTypes })
  const statuses = useQuery({ queryKey: ['publication-statuses'], queryFn: publicationApi.listPublicationStatuses })
  const airacOptions = useQuery({ queryKey: ['airac-options'], queryFn: publicationApi.listAiracOptions })

  useEffect(() => {
    if (!confirmDeleteId) return
    const t = window.setTimeout(() => setConfirmDeleteId(null), 4000)
    return () => window.clearTimeout(t)
  }, [confirmDeleteId])

  const rows = useMemo(() => {
    const all = publications.data ?? []
    return all.filter((p) => {
      if (filter === 'ALL') return true
      if (filter === 'AIRAC') return p.airac
      if (filter === 'AIC') return p.type === 'AIC'
      return !p.airac && p.type !== 'AIC'
    })
  }, [publications.data, filter])

  const save = useMutation({
    mutationFn: async () => {
      if (editing) return publicationApi.updatePublication(editing.id, form)
      return publicationApi.createPublication(form)
    },
    onSuccess: async () => {
      setOpen(false)
      setEditing(null)
      setError('')
      await qc.invalidateQueries({ queryKey: ['publications'] })
      await qc.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (err) => setError(err instanceof ApiError ? err.detail : 'Could not save publication.'),
  })

  const remove = useMutation({
    mutationFn: (id: string) => publicationApi.deletePublication(id),
    onSuccess: async () => {
      setConfirmDeleteId(null)
      await qc.invalidateQueries({ queryKey: ['publications'] })
      await qc.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })

  function openCreate() {
    setEditing(null)
    setForm(defaultForm(airacOptions.data ?? []))
    setError('')
    setOpen(true)
  }

  function openEdit(pub: Publication) {
    setEditing(pub)
    setForm({
      type: pub.type,
      number: pub.number,
      title: pub.title,
      effective: pub.effective,
      status: pub.status,
    })
    setError('')
    setOpen(true)
  }

  function onTypeChange(type: string) {
    const options = airacOptions.data ?? []
    if (isAiracType(type)) {
      const next = options.find((o) => o.nextDefault) ?? options[0]
      setForm((prev) => ({ ...prev, type, effective: next?.effective ?? prev.effective }))
    } else {
      setForm((prev) => ({ ...prev, type }))
    }
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    save.mutate()
  }

  const selectedAirac = (airacOptions.data ?? []).find((o) => o.effective === form.effective)
  const airacForm = isAiracType(form.type)

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-semibold text-ink">AIP and AIC publications</h1>
          <p className="mt-2 max-w-3xl text-muted">
            Amendments, supplements and circulars with AIRAC effective dates and distribution deadlines.
          </p>
        </div>
        {canEdit ? (
          <button
            type="button"
            className="rounded-md bg-brand px-4 py-2 text-sm font-semibold text-white"
            onClick={openCreate}
          >
            Add publication
          </button>
        ) : null}
      </div>

      <div className="flex flex-wrap gap-2" role="group" aria-label="Publication filters">
        {filters.map((f) => (
          <button
            key={f.id}
            type="button"
            aria-pressed={filter === f.id}
            className={[
              'rounded-md px-3 py-1.5 text-sm font-medium',
              filter === f.id ? 'bg-sky-50 text-brand' : 'bg-white text-ink border border-slate-200',
            ].join(' ')}
            onClick={() => setFilter(f.id)}
          >
            {f.label}
          </button>
        ))}
      </div>

      {publications.isLoading ? <p className="text-sm text-muted">Loading publications…</p> : null}
      {!publications.isLoading && rows.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-300 bg-white p-8 text-center text-muted">
          No publications yet.
        </div>
      ) : null}

      <div className="space-y-3">
        {rows.map((pub) => (
          <article key={pub.id} className="rounded-xl border border-slate-200 bg-white p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  {pub.airacIdent ? (
                    <span className="rounded bg-pink-50 px-2 py-0.5 font-mono text-sm font-semibold text-airac">
                      AIRAC {pub.airacIdent}
                    </span>
                  ) : null}
                  <h2 className="font-display text-lg font-semibold text-ink">
                    {pub.type} {pub.number}
                  </h2>
                  <span className={`rounded px-2 py-0.5 text-xs font-semibold ${statusClass(pub)}`}>
                    {pub.status}
                  </span>
                  {pub.example ? (
                    <span className="rounded border border-dashed border-slate-300 px-2 py-0.5 text-xs text-muted">
                      Example
                    </span>
                  ) : null}
                </div>
                <p className="mt-2 text-sm text-ink">{pub.title}</p>
                <p className="mt-2 text-sm text-muted">
                  Effective <strong className="text-ink">{formatDate(pub.effective)}</strong>
                  {pub.distributeBy && pub.status !== 'Cancelled' ? (
                    <>
                      {' · '}Distribute by <strong className="text-ink">{formatDate(pub.distributeBy)}</strong>
                      {pub.daysToDeadline != null && pub.daysToDeadline < 0 ? (
                        <span className="font-semibold text-danger"> (deadline passed)</span>
                      ) : null}
                    </>
                  ) : null}
                </p>
                {pub.airac && !pub.effectiveIsAiracDate ? (
                  <p className="mt-2 text-sm font-semibold text-danger">
                    Stored effective date is not an AIRAC date.
                  </p>
                ) : null}
              </div>
              {canEdit ? (
                <div className="flex gap-2">
                  <button type="button" className="rounded-md border px-3 py-1.5 text-sm" onClick={() => openEdit(pub)}>
                    Edit
                  </button>
                  <button
                    type="button"
                    className="rounded-md border border-red-200 px-3 py-1.5 text-sm text-danger"
                    onClick={() => {
                      if (confirmDeleteId === pub.id) remove.mutate(pub.id)
                      else setConfirmDeleteId(pub.id)
                    }}
                  >
                    {confirmDeleteId === pub.id ? 'Confirm delete' : 'Delete'}
                  </button>
                </div>
              ) : null}
            </div>
            {pub.status !== 'Cancelled' ? (
              <ProcessingFilePanel
                ownerType="publication"
                ownerId={pub.id}
                canEdit={canEdit}
                label={pub.type.replace(/^AIRAC /, '')}
              />
            ) : null}
            <AttachmentsPanel
              ownerType="publication"
              ownerId={pub.id}
              canEdit={canEdit}
              emptyHint="No file attached"
            />
          </article>
        ))}
      </div>

      {open ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
          <form onSubmit={onSubmit} className="w-full max-w-lg rounded-xl border bg-white p-6 shadow-lg">
            <div className="flex items-start justify-between">
              <h2 className="font-display text-xl font-semibold">
                {editing ? 'Edit publication' : 'Add publication'}
              </h2>
              <button type="button" className="text-sm text-muted" onClick={() => setOpen(false)}>
                Close
              </button>
            </div>
            <div className="mt-4 space-y-3">
              <label className="block text-sm font-semibold">
                Type
                <select
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                  value={form.type}
                  onChange={(e) => onTypeChange(e.target.value)}
                >
                  {(types.data ?? [form.type]).map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </select>
              </label>
              <label className="block text-sm font-semibold">
                Number
                <input
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                  value={form.number}
                  onChange={(e) => setForm({ ...form, number: e.target.value })}
                  placeholder="13/2026"
                  required
                />
              </label>
              <label className="block text-sm font-semibold">
                Title
                <input
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                  value={form.title}
                  onChange={(e) => setForm({ ...form, title: e.target.value })}
                  required
                />
              </label>
              <label className="block text-sm font-semibold">
                Effective date
                {airacForm ? (
                  <select
                    className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                    value={form.effective}
                    onChange={(e) => setForm({ ...form, effective: e.target.value })}
                  >
                    {(airacOptions.data ?? []).map((o) => (
                      <option key={o.ident + o.effective} value={o.effective}>
                        AIRAC {o.ident} — {formatDate(o.effective)}
                      </option>
                    ))}
                  </select>
                ) : (
                  <input
                    type="date"
                    className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                    value={form.effective}
                    onChange={(e) => setForm({ ...form, effective: e.target.value })}
                    required
                  />
                )}
              </label>
              {airacForm && selectedAirac ? (
                <p
                  className={`text-xs ${selectedAirac.daysToDeadline < 0 ? 'font-semibold text-danger' : 'text-muted'}`}
                >
                  Distribute by {formatDate(selectedAirac.distributeBy)}
                  {selectedAirac.daysToDeadline < 0
                    ? ' (deadline passed)'
                    : ` · ${selectedAirac.daysToDeadline} days`}
                </p>
              ) : null}
              <label className="block text-sm font-semibold">
                Status
                <select
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                  value={form.status}
                  onChange={(e) => setForm({ ...form, status: e.target.value })}
                >
                  {(statuses.data ?? ['Draft', 'Distributed', 'Cancelled']).map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </select>
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
                {save.isPending ? 'Saving…' : editing ? 'Save changes' : 'Add publication'}
              </button>
            </div>
          </form>
        </div>
      ) : null}
    </section>
  )
}

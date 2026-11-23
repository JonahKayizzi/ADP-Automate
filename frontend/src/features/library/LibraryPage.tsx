import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, type FormEvent } from 'react'
import { AttachmentsPanel } from '@/features/attachments/AttachmentsPanel'
import { useAuth } from '@/features/auth/AuthContext'
import { ApiError } from '@/services/api'
import * as libraryApi from '@/services/libraryApi'
import type { LibraryDocument, UpsertLibraryDocument } from '@/types/library'

const emptyForm = (): UpsertLibraryDocument => ({
  docType: 'ICAO Annex',
  title: '',
  edition: '',
})

export function LibraryPage() {
  const { user } = useAuth()
  const canEdit = user?.role !== 'VIEWER'
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<LibraryDocument | null>(null)
  const [form, setForm] = useState<UpsertLibraryDocument>(emptyForm())
  const [error, setError] = useState('')
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)

  const documents = useQuery({ queryKey: ['library'], queryFn: libraryApi.listLibraryDocuments })
  const types = useQuery({ queryKey: ['library-types'], queryFn: libraryApi.listLibraryTypes })

  useEffect(() => {
    if (!confirmDeleteId) return
    const t = window.setTimeout(() => setConfirmDeleteId(null), 4000)
    return () => window.clearTimeout(t)
  }, [confirmDeleteId])

  const save = useMutation({
    mutationFn: async () => {
      if (editing) return libraryApi.updateLibraryDocument(editing.id, form)
      return libraryApi.createLibraryDocument(form)
    },
    onSuccess: async () => {
      setOpen(false)
      setEditing(null)
      setForm(emptyForm())
      setError('')
      await qc.invalidateQueries({ queryKey: ['library'] })
    },
    onError: (err) => setError(err instanceof ApiError ? err.detail : 'Could not save document.'),
  })

  const remove = useMutation({
    mutationFn: (id: string) => libraryApi.deleteLibraryDocument(id),
    onSuccess: async () => {
      setConfirmDeleteId(null)
      await qc.invalidateQueries({ queryKey: ['library'] })
    },
  })

  function openCreate() {
    setEditing(null)
    setForm(emptyForm())
    setError('')
    setOpen(true)
  }

  function openEdit(doc: LibraryDocument) {
    setEditing(doc)
    setForm({
      docType: doc.docType,
      title: doc.title,
      edition: doc.edition ?? '',
    })
    setError('')
    setOpen(true)
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    save.mutate()
  }

  const rows = documents.data ?? []

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-semibold text-ink">AIS library</h1>
          <p className="mt-2 max-w-3xl text-muted">
            Reference documents the unit works from: ICAO Annexes and Docs, the eAIP, and UCAA regulations
            and procedures.
          </p>
        </div>
        {canEdit ? (
          <button
            type="button"
            className="rounded-md bg-brand px-4 py-2 text-sm font-semibold text-white"
            onClick={openCreate}
          >
            Add library document
          </button>
        ) : null}
      </div>

      {documents.isLoading ? <p className="text-sm text-muted">Loading library…</p> : null}
      {!documents.isLoading && rows.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-300 bg-white p-8 text-center text-muted">
          No library documents yet.
        </div>
      ) : null}

      <div className="space-y-3">
        {rows.map((doc) => (
          <article key={doc.id} className="rounded-xl border border-slate-200 bg-white p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <span className="rounded bg-slate-100 px-2 py-0.5 text-xs font-semibold text-ink">
                    {doc.docType}
                  </span>
                  {doc.example ? (
                    <span className="rounded border border-dashed border-slate-300 px-2 py-0.5 text-xs text-muted">
                      Example
                    </span>
                  ) : null}
                </div>
                <h2 className="mt-2 font-display text-lg font-semibold text-ink">{doc.title}</h2>
                {doc.edition ? (
                  <p className="mt-1 text-sm text-muted">
                    Edition <strong className="text-ink">{doc.edition}</strong>
                  </p>
                ) : null}
              </div>
              {canEdit ? (
                <div className="flex gap-2">
                  <button type="button" className="rounded-md border px-3 py-1.5 text-sm" onClick={() => openEdit(doc)}>
                    Edit
                  </button>
                  <button
                    type="button"
                    className="rounded-md border border-red-200 px-3 py-1.5 text-sm text-danger"
                    onClick={() => {
                      if (confirmDeleteId === doc.id) remove.mutate(doc.id)
                      else setConfirmDeleteId(doc.id)
                    }}
                  >
                    {confirmDeleteId === doc.id ? 'Confirm delete' : 'Delete'}
                  </button>
                </div>
              ) : null}
            </div>
            <AttachmentsPanel ownerType="library" ownerId={doc.id} canEdit={canEdit} />
          </article>
        ))}
      </div>

      {open ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
          <form onSubmit={onSubmit} className="w-full max-w-lg rounded-xl border bg-white p-6 shadow-lg">
            <div className="flex items-start justify-between">
              <h2 className="font-display text-xl font-semibold">
                {editing ? 'Edit library document' : 'Add library document'}
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
                  value={form.docType}
                  onChange={(e) => setForm({ ...form, docType: e.target.value })}
                >
                  {(types.data ?? [form.docType]).map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </select>
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
                Edition (optional)
                <input
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                  value={form.edition}
                  onChange={(e) => setForm({ ...form, edition: e.target.value })}
                  placeholder="e.g. 16th edition, July 2018"
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
                {save.isPending ? 'Saving…' : editing ? 'Save changes' : 'Add document'}
              </button>
            </div>
          </form>
        </div>
      ) : null}
    </section>
  )
}

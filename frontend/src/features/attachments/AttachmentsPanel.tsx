import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { ApiError } from '@/services/api'
import * as attachmentsApi from '@/services/attachmentsApi'
import type { Attachment, AttachmentOwnerType, AttachmentSlot } from '@/types/attachment'

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export type AttachmentSlotDef = {
  key: AttachmentSlot
  label: string
}

type Props = {
  ownerType: AttachmentOwnerType
  ownerId: string
  canEdit: boolean
  /** When set, each slot holds at most one file (replace on upload). */
  slots?: AttachmentSlotDef[]
  emptyHint?: string
}

export function AttachmentsPanel({ ownerType, ownerId, canEdit, slots, emptyHint }: Props) {
  const qc = useQueryClient()
  const [error, setError] = useState('')
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)
  const queryKey = ['attachments', ownerType, ownerId] as const

  const list = useQuery({
    queryKey,
    queryFn: () => attachmentsApi.listAttachments(ownerType, ownerId),
  })

  const upload = useMutation({
    mutationFn: ({ file, slot }: { file: File; slot?: AttachmentSlot }) =>
      attachmentsApi.uploadAttachment(ownerType, ownerId, file, slot),
    onSuccess: async () => {
      setError('')
      await qc.invalidateQueries({ queryKey })
    },
    onError: (err) => setError(err instanceof ApiError ? err.detail : 'Could not upload the file.'),
  })

  const remove = useMutation({
    mutationFn: (id: string) => attachmentsApi.deleteAttachment(id),
    onSuccess: async () => {
      setConfirmDeleteId(null)
      await qc.invalidateQueries({ queryKey })
    },
    onError: (err) => setError(err instanceof ApiError ? err.detail : 'Could not remove the file.'),
  })

  const rows = list.data ?? []

  if (slots && slots.length > 0) {
    const unslotted = rows.filter((r) => !r.slot || !slots.some((s) => s.key === r.slot))
    return (
      <div className="mt-4 space-y-2">
        <div className={`grid gap-2 ${slots.length > 1 ? 'sm:grid-cols-2' : ''}`}>
          {slots.map((slot) => {
            const file = rows.find((r) => r.slot === slot.key) ?? null
            return (
              <SlotCard
                key={slot.key}
                label={slot.label}
                file={file}
                canEdit={canEdit}
                busy={upload.isPending}
                confirmDeleteId={confirmDeleteId}
                onConfirmDelete={setConfirmDeleteId}
                onUpload={(f) => upload.mutate({ file: f, slot: slot.key })}
                onRemove={(id) => remove.mutate(id)}
                onDownloadError={setError}
              />
            )
          })}
        </div>
        {unslotted.length > 0 ? (
          <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
            <h3 className="text-xs font-semibold uppercase tracking-wide text-muted">Other attachments</h3>
            <FileList
              rows={unslotted}
              canEdit={canEdit}
              confirmDeleteId={confirmDeleteId}
              onConfirmDelete={setConfirmDeleteId}
              onRemove={(id) => remove.mutate(id)}
              onDownloadError={setError}
            />
          </div>
        ) : null}
        {error ? <p className="text-xs text-danger">{error}</p> : null}
        <p className="text-[11px] text-muted">PDF, TXT, CSV, JSON, XML, GML, MD, PNG, JPEG · max 20 MB</p>
      </div>
    )
  }

  return (
    <div className="mt-4 rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-xs font-semibold uppercase tracking-wide text-muted">Attachments</h3>
        {canEdit ? (
          <UploadButton
            busy={upload.isPending}
            onFile={(file) => upload.mutate({ file })}
          />
        ) : null}
      </div>

      {list.isLoading ? <p className="mt-2 text-xs text-muted">Loading files…</p> : null}

      {!list.isLoading && rows.length === 0 ? (
        <p className="mt-2 text-xs text-muted">{emptyHint ?? 'No file attached'}</p>
      ) : null}

      {rows.length > 0 ? (
        <FileList
          rows={rows}
          canEdit={canEdit}
          confirmDeleteId={confirmDeleteId}
          onConfirmDelete={setConfirmDeleteId}
          onRemove={(id) => remove.mutate(id)}
          onDownloadError={setError}
        />
      ) : null}

      {error ? <p className="mt-2 text-xs text-danger">{error}</p> : null}
      <p className="mt-2 text-[11px] text-muted">PDF, TXT, CSV, JSON, XML, GML, MD, PNG, JPEG · max 20 MB</p>
    </div>
  )
}

function SlotCard({
  label,
  file,
  canEdit,
  busy,
  confirmDeleteId,
  onConfirmDelete,
  onUpload,
  onRemove,
  onDownloadError,
}: {
  label: string
  file: Attachment | null
  canEdit: boolean
  busy: boolean
  confirmDeleteId: string | null
  onConfirmDelete: (id: string | null) => void
  onUpload: (file: File) => void
  onRemove: (id: string) => void
  onDownloadError: (msg: string) => void
}) {
  return (
    <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-xs font-semibold uppercase tracking-wide text-muted">{label}</h3>
        {canEdit ? (
          <UploadButton
            busy={busy}
            label={file ? 'Replace' : 'Attach'}
            onFile={onUpload}
          />
        ) : null}
      </div>
      {file ? (
        <div className="mt-2 flex flex-wrap items-center justify-between gap-2 rounded-md bg-white px-2.5 py-2 text-sm">
          <div className="min-w-0">
            <button
              type="button"
              className="truncate font-medium text-brand hover:underline"
              onClick={() =>
                void attachmentsApi.downloadAttachment(file.id, file.originalFilename).catch((err) =>
                  onDownloadError(err instanceof ApiError ? err.detail : 'Could not download the file.')
                )
              }
            >
              {file.originalFilename}
            </button>
            <p className="text-xs text-muted">{formatSize(file.sizeBytes)}</p>
          </div>
          {canEdit ? (
            <button
              type="button"
              className="text-xs text-danger"
              onClick={() => {
                if (confirmDeleteId === file.id) onRemove(file.id)
                else onConfirmDelete(file.id)
              }}
            >
              {confirmDeleteId === file.id ? 'Confirm remove' : 'Remove'}
            </button>
          ) : null}
        </div>
      ) : (
        <p className="mt-2 text-xs text-muted">Not attached</p>
      )}
    </div>
  )
}

function UploadButton({
  busy,
  onFile,
  label = 'Attach file',
}: {
  busy: boolean
  onFile: (file: File) => void
  label?: string
}) {
  const inputRef = useRef<HTMLInputElement>(null)
  return (
    <div>
      <input
        ref={inputRef}
        type="file"
        className="hidden"
        accept=".pdf,.txt,.csv,.json,.xml,.gml,.md,.png,.jpg,.jpeg"
        onChange={(e) => {
          const file = e.target.files?.[0]
          if (file) onFile(file)
          if (inputRef.current) inputRef.current.value = ''
        }}
      />
      <button
        type="button"
        className="rounded-md border border-slate-300 bg-white px-2.5 py-1 text-xs font-semibold text-ink disabled:opacity-60"
        disabled={busy}
        onClick={() => inputRef.current?.click()}
      >
        {busy ? 'Uploading…' : label}
      </button>
    </div>
  )
}

function FileList({
  rows,
  canEdit,
  confirmDeleteId,
  onConfirmDelete,
  onRemove,
  onDownloadError,
}: {
  rows: Attachment[]
  canEdit: boolean
  confirmDeleteId: string | null
  onConfirmDelete: (id: string | null) => void
  onRemove: (id: string) => void
  onDownloadError: (msg: string) => void
}) {
  return (
    <ul className="mt-2 space-y-2">
      {rows.map((file) => (
        <li key={file.id} className="flex flex-wrap items-center justify-between gap-2 rounded-md bg-white px-2.5 py-2 text-sm">
          <div className="min-w-0">
            <button
              type="button"
              className="truncate font-medium text-brand hover:underline"
              onClick={() =>
                void attachmentsApi.downloadAttachment(file.id, file.originalFilename).catch((err) =>
                  onDownloadError(err instanceof ApiError ? err.detail : 'Could not download the file.')
                )
              }
            >
              {file.originalFilename}
            </button>
            <p className="text-xs text-muted">
              {formatSize(file.sizeBytes)}
              {file.createdByAccount ? ` · ${file.createdByAccount}` : ''}
            </p>
          </div>
          {canEdit ? (
            <button
              type="button"
              className="text-xs text-danger"
              onClick={() => {
                if (confirmDeleteId === file.id) onRemove(file.id)
                else onConfirmDelete(file.id)
              }}
            >
              {confirmDeleteId === file.id ? 'Confirm remove' : 'Remove'}
            </button>
          ) : null}
        </li>
      ))}
    </ul>
  )
}

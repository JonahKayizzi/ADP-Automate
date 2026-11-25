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
  /**
   * When set with `slots`, only these slots show Attach/Replace.
   * Other slots are listed when a file is present (inventory view).
   */
  attachableSlots?: AttachmentSlot[]
  /** Hide empty non-attachable slots (list of attached production files). */
  hideEmpty?: boolean
  /** Slot layout: grid (default) or stacked rows. */
  layout?: 'grid' | 'rows'
  emptyHint?: string
}

export function AttachmentsPanel({
  ownerType,
  ownerId,
  canEdit,
  slots,
  attachableSlots,
  hideEmpty,
  layout = 'grid',
  emptyHint,
}: Props) {
  const qc = useQueryClient()
  const [error, setError] = useState('')
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)
  const queryKey = ['attachments', ownerType, ownerId] as const

  const historyKey = ['attachments-history', ownerType, ownerId] as const
  const [showHistory, setShowHistory] = useState(false)

  const list = useQuery({
    queryKey,
    queryFn: () => attachmentsApi.listAttachments(ownerType, ownerId),
  })

  const history = useQuery({
    queryKey: historyKey,
    queryFn: () => attachmentsApi.listAttachmentHistory(ownerType, ownerId),
    enabled: showHistory,
  })

  const upload = useMutation({
    mutationFn: ({ file, slot }: { file: File; slot?: AttachmentSlot }) =>
      attachmentsApi.uploadAttachment(ownerType, ownerId, file, slot),
    onSuccess: async () => {
      setError('')
      await qc.invalidateQueries({ queryKey })
      await qc.invalidateQueries({ queryKey: historyKey })
    },
    onError: (err) => setError(err instanceof ApiError ? err.detail : 'Could not upload the file.'),
  })

  const remove = useMutation({
    mutationFn: (id: string) => attachmentsApi.deleteAttachment(id),
    onSuccess: async () => {
      setConfirmDeleteId(null)
      await qc.invalidateQueries({ queryKey })
      await qc.invalidateQueries({ queryKey: historyKey })
    },
    onError: (err) => setError(err instanceof ApiError ? err.detail : 'Could not remove the file.'),
  })

  const rows = list.data ?? []
  const historyHint = (
    <div className="mt-2">
      <button
        type="button"
        className="text-[11px] font-semibold text-brand"
        onClick={() => setShowHistory((v) => !v)}
      >
        {showHistory ? 'Hide previous versions' : 'Show previous versions'}
      </button>
      {showHistory ? (
        history.isLoading ? (
          <p className="mt-1 text-xs text-muted">Loading history…</p>
        ) : (history.data ?? []).length === 0 ? (
          <p className="mt-1 text-xs text-muted">No previous versions.</p>
        ) : (
          <FileList
            rows={history.data ?? []}
            canEdit={false}
            confirmDeleteId={null}
            onConfirmDelete={() => undefined}
            onRemove={() => undefined}
            onDownloadError={setError}
          />
        )
      ) : null}
    </div>
  )

  if (slots && slots.length > 0) {
    const canAttach = (key: AttachmentSlot) =>
      attachableSlots == null ? true : attachableSlots.includes(key)
    const visibleSlots = slots.filter((slot) => {
      if (!hideEmpty) return true
      if (canAttach(slot.key)) return true
      return rows.some((r) => r.slot === slot.key)
    })
    const unslotted = rows.filter((r) => !r.slot || !slots.some((s) => s.key === r.slot))
    const anyAttached = visibleSlots.some((s) => rows.some((r) => r.slot === s.key)) || unslotted.length > 0
    return (
      <div className="space-y-2">
        {!list.isLoading && hideEmpty && !anyAttached && attachableSlots?.length === 0 ? (
          <p className="text-xs text-muted">{emptyHint ?? 'No production files attached yet.'}</p>
        ) : null}
        <div
          className={
            layout === 'rows'
              ? 'flex flex-col gap-2'
              : `grid gap-2 ${visibleSlots.length > 1 ? 'sm:grid-cols-2 lg:grid-cols-3' : ''}`
          }
        >
          {visibleSlots.map((slot) => {
            const file = rows.find((r) => r.slot === slot.key) ?? null
            return (
              <SlotCard
                key={slot.key}
                label={slot.label}
                file={file}
                canEdit={canEdit}
                canUpload={canAttach(slot.key)}
                busy={upload.isPending}
                confirmDeleteId={confirmDeleteId}
                onConfirmDelete={setConfirmDeleteId}
                onUpload={(f) => upload.mutate({ file: f, slot: slot.key })}
                onRemove={(id) => remove.mutate(id)}
                onDownloadError={setError}
                compact={layout === 'rows'}
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
        {historyHint}
        {error ? <p className="text-xs text-danger">{error}</p> : null}
        {attachableSlots == null || attachableSlots.length > 0 ? (
          <p className="text-[11px] text-muted">PDF, TXT, CSV, JSON, XML, GML, MD, PNG, JPEG · max 20 MB</p>
        ) : null}
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

      {historyHint}
      {error ? <p className="mt-2 text-xs text-danger">{error}</p> : null}
      <p className="mt-2 text-[11px] text-muted">PDF, TXT, CSV, JSON, XML, GML, MD, PNG, JPEG · max 20 MB</p>
    </div>
  )
}

function SlotCard({
  label,
  file,
  canEdit,
  canUpload = true,
  busy,
  confirmDeleteId,
  onConfirmDelete,
  onUpload,
  onRemove,
  onDownloadError,
  compact = false,
}: {
  label: string
  file: Attachment | null
  canEdit: boolean
  canUpload?: boolean
  busy: boolean
  confirmDeleteId: string | null
  onConfirmDelete: (id: string | null) => void
  onUpload: (file: File) => void
  onRemove: (id: string) => void
  onDownloadError: (msg: string) => void
  compact?: boolean
}) {
  if (compact) {
    return (
      <div className="flex flex-wrap items-center gap-3 rounded-md border border-slate-200 bg-slate-50 px-3 py-2">
        <p className="w-44 shrink-0 text-xs font-semibold uppercase tracking-wide text-muted">{label}</p>
        <div className="min-w-0 flex-1">
          {file ? (
            <button
              type="button"
              className="truncate text-sm font-medium text-brand hover:underline"
              onClick={() =>
                void attachmentsApi.downloadAttachment(file.id, file.originalFilename).catch((err) =>
                  onDownloadError(err instanceof ApiError ? err.detail : 'Could not download the file.')
                )
              }
            >
              {file.originalFilename}
              <span className="ml-1.5 text-xs font-normal text-muted">{formatSize(file.sizeBytes)}</span>
            </button>
          ) : (
            <p className="text-xs text-muted">Not attached</p>
          )}
        </div>
        <div className="flex shrink-0 flex-wrap items-center gap-2">
          {canEdit && canUpload ? (
            <UploadButton busy={busy} label={file ? 'Replace' : 'Attach'} onFile={onUpload} />
          ) : null}
          {canEdit && file ? (
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
      </div>
    )
  }

  return (
    <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-xs font-semibold uppercase tracking-wide text-muted">{label}</h3>
        {canEdit && canUpload ? (
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

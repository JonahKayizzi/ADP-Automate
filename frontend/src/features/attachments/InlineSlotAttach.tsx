import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { ApiError } from '@/services/api'
import * as attachmentsApi from '@/services/attachmentsApi'
import type { AttachmentOwnerType, AttachmentSlot } from '@/types/attachment'

type Props = {
  ownerType: AttachmentOwnerType
  ownerId: string
  slot: AttachmentSlot
  label: string
  canEdit: boolean
  /** Compact hint under the attach control */
  hint?: string
}

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

/** Compact attach control for a single production-file slot (used inside OF/82). */
export function InlineSlotAttach({ ownerType, ownerId, slot, label, canEdit, hint }: Props) {
  const qc = useQueryClient()
  const inputRef = useRef<HTMLInputElement>(null)
  const [error, setError] = useState('')
  const [confirmRemove, setConfirmRemove] = useState(false)
  const queryKey = ['attachments', ownerType, ownerId] as const

  const list = useQuery({
    queryKey,
    queryFn: () => attachmentsApi.listAttachments(ownerType, ownerId),
  })

  const file = (list.data ?? []).find((a) => a.slot === slot) ?? null

  const upload = useMutation({
    mutationFn: (f: File) => attachmentsApi.uploadAttachment(ownerType, ownerId, f, slot),
    onSuccess: async () => {
      setError('')
      setConfirmRemove(false)
      await qc.invalidateQueries({ queryKey })
      await qc.invalidateQueries({ queryKey: ['attachments-history', ownerType, ownerId] })
    },
    onError: (err) => setError(err instanceof ApiError ? err.detail : 'Could not upload the file.'),
  })

  const remove = useMutation({
    mutationFn: (id: string) => attachmentsApi.deleteAttachment(id),
    onSuccess: async () => {
      setConfirmRemove(false)
      await qc.invalidateQueries({ queryKey })
      await qc.invalidateQueries({ queryKey: ['attachments-history', ownerType, ownerId] })
    },
    onError: (err) => setError(err instanceof ApiError ? err.detail : 'Could not remove the file.'),
  })

  return (
    <div className="mt-2 rounded-md border border-slate-200 bg-slate-50/80 px-2.5 py-2">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="min-w-0">
          <p className="text-[11px] font-semibold uppercase tracking-wide text-muted">{label}</p>
          {file ? (
            <button
              type="button"
              className="mt-0.5 truncate text-sm font-medium text-brand hover:underline"
              onClick={() =>
                void attachmentsApi.downloadAttachment(file.id, file.originalFilename).catch((err) =>
                  setError(err instanceof ApiError ? err.detail : 'Could not download the file.')
                )
              }
            >
              {file.originalFilename}
              <span className="ml-1.5 text-xs font-normal text-muted">{formatSize(file.sizeBytes)}</span>
            </button>
          ) : (
            <p className="mt-0.5 text-xs text-muted">{hint ?? 'Not attached'}</p>
          )}
        </div>
        {canEdit ? (
          <div className="flex flex-wrap items-center gap-2">
            <input
              ref={inputRef}
              type="file"
              className="hidden"
              accept=".pdf,.txt,.csv,.json,.xml,.gml,.md,.png,.jpg,.jpeg"
              onChange={(e) => {
                const f = e.target.files?.[0]
                if (f) upload.mutate(f)
                if (inputRef.current) inputRef.current.value = ''
              }}
            />
            <button
              type="button"
              className="rounded-md border border-slate-200 bg-white px-2.5 py-1 text-xs font-semibold text-ink disabled:opacity-60"
              disabled={upload.isPending}
              onClick={() => inputRef.current?.click()}
            >
              {upload.isPending ? 'Uploading…' : file ? 'Replace' : 'Attach'}
            </button>
            {file ? (
              <button
                type="button"
                className="text-xs text-danger"
                disabled={remove.isPending}
                onClick={() => {
                  if (confirmRemove) remove.mutate(file.id)
                  else setConfirmRemove(true)
                }}
              >
                {confirmRemove ? 'Confirm remove' : 'Remove'}
              </button>
            ) : null}
          </div>
        ) : null}
      </div>
      {error ? <p className="mt-1 text-xs text-danger">{error}</p> : null}
    </div>
  )
}

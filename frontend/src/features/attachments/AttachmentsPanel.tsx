import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { ApiError } from '@/services/api'
import * as attachmentsApi from '@/services/attachmentsApi'
import type { AttachmentOwnerType } from '@/types/attachment'

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

type Props = {
  ownerType: AttachmentOwnerType
  ownerId: string
  canEdit: boolean
  emptyHint?: string
}

export function AttachmentsPanel({ ownerType, ownerId, canEdit, emptyHint }: Props) {
  const qc = useQueryClient()
  const inputRef = useRef<HTMLInputElement>(null)
  const [error, setError] = useState('')
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)
  const queryKey = ['attachments', ownerType, ownerId] as const

  const list = useQuery({
    queryKey,
    queryFn: () => attachmentsApi.listAttachments(ownerType, ownerId),
  })

  const upload = useMutation({
    mutationFn: (file: File) => attachmentsApi.uploadAttachment(ownerType, ownerId, file),
    onSuccess: async () => {
      setError('')
      if (inputRef.current) inputRef.current.value = ''
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

  return (
    <div className="mt-4 rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-xs font-semibold uppercase tracking-wide text-muted">Attachments</h3>
        {canEdit ? (
          <div>
            <input
              ref={inputRef}
              type="file"
              className="hidden"
              accept=".pdf,.txt,.csv,.json,.xml,.gml,.md,.png,.jpg,.jpeg"
              onChange={(e) => {
                const file = e.target.files?.[0]
                if (file) upload.mutate(file)
              }}
            />
            <button
              type="button"
              className="rounded-md border border-slate-300 bg-white px-2.5 py-1 text-xs font-semibold text-ink disabled:opacity-60"
              disabled={upload.isPending}
              onClick={() => inputRef.current?.click()}
            >
              {upload.isPending ? 'Uploading…' : 'Attach file'}
            </button>
          </div>
        ) : null}
      </div>

      {list.isLoading ? <p className="mt-2 text-xs text-muted">Loading files…</p> : null}

      {!list.isLoading && rows.length === 0 ? (
        <p className="mt-2 text-xs text-muted">{emptyHint ?? 'No file attached'}</p>
      ) : null}

      {rows.length > 0 ? (
        <ul className="mt-2 space-y-2">
          {rows.map((file) => (
            <li key={file.id} className="flex flex-wrap items-center justify-between gap-2 rounded-md bg-white px-2.5 py-2 text-sm">
              <div className="min-w-0">
                <button
                  type="button"
                  className="truncate font-medium text-brand hover:underline"
                  onClick={() =>
                    void attachmentsApi.downloadAttachment(file.id, file.originalFilename).catch((err) =>
                      setError(err instanceof ApiError ? err.detail : 'Could not download the file.')
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
                    if (confirmDeleteId === file.id) remove.mutate(file.id)
                    else setConfirmDeleteId(file.id)
                  }}
                >
                  {confirmDeleteId === file.id ? 'Confirm remove' : 'Remove'}
                </button>
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}

      {error ? <p className="mt-2 text-xs text-danger">{error}</p> : null}
      <p className="mt-2 text-[11px] text-muted">PDF, TXT, CSV, JSON, XML, GML, MD, PNG, JPEG · max 20 MB</p>
    </div>
  )
}

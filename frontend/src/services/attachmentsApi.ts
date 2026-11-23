import { api, ApiError } from './api'
import type { Attachment, AttachmentOwnerType } from '@/types/attachment'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api'

export function listAttachments(ownerType: AttachmentOwnerType, ownerId: string) {
  const q = new URLSearchParams({ ownerType, ownerId })
  return api<Attachment[]>(`/attachments?${q}`)
}

export async function uploadAttachment(
  ownerType: AttachmentOwnerType,
  ownerId: string,
  file: File
): Promise<Attachment> {
  const body = new FormData()
  body.append('ownerType', ownerType)
  body.append('ownerId', ownerId)
  body.append('file', file)

  const res = await fetch(`${API_BASE}/attachments`, {
    method: 'POST',
    body,
    credentials: 'include',
  })

  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (!res.ok) {
    const detail =
      (data && (data.detail || data.title || data.message)) || 'Could not upload the file.'
    throw new ApiError(res.status, String(detail))
  }
  return data as Attachment
}

export function deleteAttachment(id: string) {
  return api<void>(`/attachments/${id}`, { method: 'DELETE' })
}

export function downloadUrl(id: string) {
  return `${API_BASE}/attachments/${id}/download`
}

export async function downloadAttachment(id: string, filename: string) {
  const res = await fetch(downloadUrl(id), { credentials: 'include' })
  if (!res.ok) {
    const text = await res.text()
    let detail = 'Could not download the file.'
    try {
      const data = text ? JSON.parse(text) : null
      detail = (data && (data.detail || data.title || data.message)) || detail
    } catch {
      /* non-JSON error body */
    }
    throw new ApiError(res.status, String(detail))
  }
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

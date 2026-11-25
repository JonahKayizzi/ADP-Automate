import { api, ApiError } from './api'
import type { ProcessingFile, ProcessingFormDetail, ProcessingOwnerType } from '@/types/processing'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api'

export function getProcessingFile(ownerType: ProcessingOwnerType, ownerId: string) {
  const q = new URLSearchParams({ ownerType, ownerId })
  return api<ProcessingFile>(`/processing-files?${q}`)
}

export function getForm(fileId: string, formId: string) {
  return api<ProcessingFormDetail>(`/processing-files/${fileId}/forms/${formId}`)
}

export function saveForm(fileId: string, formId: string, payload: Record<string, unknown>) {
  return api<ProcessingFormDetail>(`/processing-files/${fileId}/forms/${formId}`, {
    method: 'PUT',
    body: JSON.stringify({ payload }),
  })
}

export function signForm(
  fileId: string,
  formId: string,
  slotKey: string,
  body: { signerName?: string; designation?: string } = {}
) {
  return api<ProcessingFormDetail>(`/processing-files/${fileId}/forms/${formId}/signatures/${slotKey}`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function reviseForm(fileId: string, formId: string) {
  return api<ProcessingFormDetail>(`/processing-files/${fileId}/forms/${formId}/revise`, {
    method: 'POST',
    body: JSON.stringify({}),
  })
}

export function clearFormSignatures(fileId: string, formId: string) {
  return api<ProcessingFormDetail>(`/processing-files/${fileId}/forms/${formId}/clear-signatures`, {
    method: 'POST',
    body: JSON.stringify({}),
  })
}

export type FormRevision = {
  id: string
  revisionNumber: number
  formCode: string
  payload: Record<string, unknown>
  signatures: Record<string, unknown>[]
  revisedAt: string
  revisedByAccount: string
}

export function listFormRevisions(fileId: string, formId: string) {
  return api<FormRevision[]>(`/processing-files/${fileId}/forms/${formId}/revisions`)
}

export async function exportOf82Pdf(fileId: string, formId: string, filename = 'OF82-raw-data-quality-check.pdf') {
  const res = await fetch(`${API_BASE}/processing-files/${fileId}/forms/${formId}/export.pdf`, {
    credentials: 'include',
  })
  if (!res.ok) {
    const text = await res.text()
    let detail = 'Could not export the PDF.'
    try {
      const data = text ? JSON.parse(text) : null
      detail = (data && (data.detail || data.title || data.message)) || detail
    } catch {
      /* non-JSON */
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

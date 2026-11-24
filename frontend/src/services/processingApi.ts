import { api } from './api'
import type { ProcessingFile, ProcessingFormDetail, ProcessingOwnerType } from '@/types/processing'

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

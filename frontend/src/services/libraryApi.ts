import { api } from './api'
import type { LibraryDocument, UpsertLibraryDocument } from '@/types/library'

export function listLibraryDocuments() {
  return api<LibraryDocument[]>('/library')
}

export function listLibraryTypes() {
  return api<string[]>('/library/types')
}

export function createLibraryDocument(body: UpsertLibraryDocument) {
  return api<LibraryDocument>('/library', { method: 'POST', body: JSON.stringify(body) })
}

export function updateLibraryDocument(id: string, body: UpsertLibraryDocument) {
  return api<LibraryDocument>(`/library/${id}`, { method: 'PUT', body: JSON.stringify(body) })
}

export function deleteLibraryDocument(id: string) {
  return api<void>(`/library/${id}`, { method: 'DELETE' })
}

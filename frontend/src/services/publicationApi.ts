import { api } from './api'
import type { AiracOption, Publication, UpsertPublication } from '@/types/publication'

export function listPublications() {
  return api<Publication[]>('/publications')
}

export function listPublicationTypes() {
  return api<string[]>('/publications/types')
}

export function listPublicationStatuses() {
  return api<string[]>('/publications/statuses')
}

export function listAiracOptions() {
  return api<AiracOption[]>('/publications/airac-options')
}

export function createPublication(body: UpsertPublication) {
  return api<Publication>('/publications', { method: 'POST', body: JSON.stringify(body) })
}

export function updatePublication(id: string, body: UpsertPublication) {
  return api<Publication>(`/publications/${id}`, { method: 'PUT', body: JSON.stringify(body) })
}

export function deletePublication(id: string) {
  return api<void>(`/publications/${id}`, { method: 'DELETE' })
}

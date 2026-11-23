import { api } from './api'
import type { DashboardSummary, Notam, UpsertNotam } from '@/types/notam'

export function listNotams() {
  return api<Notam[]>('/notams')
}

export function listOriginators() {
  return api<string[]>('/notams/originators')
}

export function createNotam(body: UpsertNotam) {
  return api<Notam>('/notams', { method: 'POST', body: JSON.stringify(body) })
}

export function updateNotam(id: string, body: UpsertNotam) {
  return api<Notam>(`/notams/${id}`, { method: 'PUT', body: JSON.stringify(body) })
}

export function deleteNotam(id: string) {
  return api<void>(`/notams/${id}`, { method: 'DELETE' })
}

export function fetchDashboardSummary() {
  return api<DashboardSummary>('/dashboard/summary')
}

export function fetchAiracCurrent() {
  return api<{ ident: string; today: string; dayWithinCycle: number; cycleDays: number }>('/airac/current')
}

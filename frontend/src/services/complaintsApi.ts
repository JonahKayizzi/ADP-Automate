import { api } from './api'
import type { Complaint, UpsertComplaint } from '@/types/complaint'

export function listComplaints(status: string) {
  const q = new URLSearchParams({ status })
  return api<Complaint[]>(`/complaints?${q}`)
}

export function listChannels() {
  return api<string[]>('/complaints/channels')
}

export function createComplaint(body: UpsertComplaint) {
  return api<Complaint>('/complaints', { method: 'POST', body: JSON.stringify(body) })
}

export function updateComplaint(id: string, body: UpsertComplaint) {
  return api<Complaint>(`/complaints/${id}`, { method: 'PUT', body: JSON.stringify(body) })
}

export function deleteComplaint(id: string) {
  return api<void>(`/complaints/${id}`, { method: 'DELETE' })
}

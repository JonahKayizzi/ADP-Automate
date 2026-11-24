import { api } from './api'

export type AuditEvent = {
  id: string
  occurredAt: string
  actorUsername?: string | null
  action: string
  entityType?: string | null
  entityId?: string | null
  detail: Record<string, unknown>
}

export function listAudit(params: { entityType?: string; entityId?: string; limit?: number } = {}) {
  const q = new URLSearchParams()
  if (params.entityType) q.set('entityType', params.entityType)
  if (params.entityId) q.set('entityId', params.entityId)
  q.set('limit', String(params.limit ?? 100))
  return api<AuditEvent[]>(`/audit?${q}`)
}

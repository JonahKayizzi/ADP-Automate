export type Notam = {
  id: string
  number: string
  issued: string
  title: string
  originator: string
  validTo?: string | null
  example: boolean
  createdAt: string
  createdByAccount?: string | null
  updatedAt: string
}

export type UpsertNotam = {
  number: string
  issued: string
  title: string
  originator: string
  validTo?: string
}

export type AiracCycle = {
  ident: string
  effective: string
  distributeBy: string
  inForce: boolean
  daysToDeadline: number
  dayWithinCycle: number
  publicationCount: number
}

export type DashboardSummary = {
  today: string
  notamsLogged: number
  processingFilesNotSignedOff: number
  publicationsNotYetEffective: number
  openComplaints: number
  overdueTasks: number
  airac: AiracCycle[]
}

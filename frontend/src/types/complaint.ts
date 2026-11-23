export type Complaint = {
  id: string
  title: string
  complainant: string
  received: string
  channel: string
  status: string
  description?: string | null
  actionTaken?: string | null
  example: boolean
  createdAt: string
  createdByAccount?: string | null
  updatedAt: string
}

export type UpsertComplaint = {
  title: string
  complainant: string
  received: string
  channel: string
  status: string
  description?: string
  actionTaken?: string
}

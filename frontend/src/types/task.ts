export type TaskItem = {
  id: string
  title: string
  dueDate: string
  status: string
  priority: boolean
  overdue: boolean
  daysLate?: number | null
  assigneeUsername?: string | null
  assigneeName?: string | null
  assigneeDesignation?: string | null
  assigneeEmail?: string | null
  assigneeInitials?: string | null
  assignedAt?: string | null
  assignedBy?: string | null
  assignedByName?: string | null
  newForYou: boolean
  seenAt?: string | null
  completedAt?: string | null
  notifyStatus: string
  notifyAt?: string | null
  notifyBy?: string | null
  example: boolean
  createdAt: string
  createdByAccount?: string | null
  updatedAt: string
}

export type UpsertTask = {
  title: string
  dueDate: string
  status: string
  priority: boolean
  assigneeUsername: string
}

export type AssigneeOption = {
  username: string
  displayName: string
  designation?: string | null
  email?: string | null
}

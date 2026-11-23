import { api } from './api'
import type { AssigneeOption, TaskItem, UpsertTask } from '@/types/task'

export function listTasks(filter: string) {
  const q = new URLSearchParams({ filter })
  return api<TaskItem[]>(`/tasks?${q}`)
}

export function listAssignees() {
  return api<AssigneeOption[]>('/tasks/assignees')
}

export function createTask(body: UpsertTask) {
  return api<TaskItem>('/tasks', { method: 'POST', body: JSON.stringify(body) })
}

export function updateTask(id: string, body: UpsertTask) {
  return api<TaskItem>(`/tasks/${id}`, { method: 'PUT', body: JSON.stringify(body) })
}

export function completeTask(id: string) {
  return api<TaskItem>(`/tasks/${id}/complete`, { method: 'POST', body: JSON.stringify({}) })
}

export function notifyTask(id: string, outcome: 'emailed' | 'skipped') {
  return api<TaskItem>(`/tasks/${id}/notify`, {
    method: 'POST',
    body: JSON.stringify({ outcome }),
  })
}

export function markTasksSeen() {
  return api<void>('/tasks/mark-seen', { method: 'POST', body: JSON.stringify({}) })
}

export function deleteTask(id: string) {
  return api<void>(`/tasks/${id}`, { method: 'DELETE' })
}

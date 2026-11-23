import { api } from './api'
import type { AuthStatus, AuthUser } from '@/types/auth'

export function fetchAuthStatus() {
  return api<AuthStatus>('/auth/status')
}

export function setupAdmin(body: {
  fullName: string
  designation?: string
  username: string
  password: string
  confirmPassword: string
}) {
  return api<AuthUser>('/auth/setup', { method: 'POST', body: JSON.stringify(body) })
}

export function login(body: { username: string; password: string }) {
  return api<{ user: AuthUser }>('/auth/login', { method: 'POST', body: JSON.stringify(body) })
}

export function logout() {
  return api<{ message: string }>('/auth/logout', { method: 'POST', body: '{}' })
}

export function changePassword(body: {
  currentPassword?: string
  newPassword: string
  confirmPassword: string
}) {
  return api<AuthUser>('/auth/change-password', { method: 'POST', body: JSON.stringify(body) })
}

export function unlock(body: { username: string; password: string }) {
  return api<{ message: string }>('/auth/unlock', { method: 'POST', body: JSON.stringify(body) })
}

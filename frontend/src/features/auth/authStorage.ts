const LAST_USERNAME_KEY = 'adp.lastUsername'
const ACTIVITY_KEY = 'adp.lastActivity'

export function getLastUsername() {
  return sessionStorage.getItem(LAST_USERNAME_KEY) ?? localStorage.getItem(LAST_USERNAME_KEY) ?? ''
}

export function setLastUsername(username: string) {
  localStorage.setItem(LAST_USERNAME_KEY, username)
  sessionStorage.setItem(LAST_USERNAME_KEY, username)
}

export function touchActivity() {
  sessionStorage.setItem(ACTIVITY_KEY, String(Date.now()))
}

export function getLastActivity(): number {
  const raw = sessionStorage.getItem(ACTIVITY_KEY)
  return raw ? Number(raw) : Date.now()
}

export const IDLE_LOCK_MS = 15 * 60 * 1000

const LAST_USERNAME_KEY = 'adp.lastUsername'
const ACTIVITY_KEY = 'adp.lastActivity'
const SIGNED_IN_AT_KEY = 'adp.signedInAt'

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
/** Matches server absolute session (8 hours). */
export const SESSION_MAX_MS = 8 * 60 * 60 * 1000

export function markSignedIn() {
  sessionStorage.setItem(SIGNED_IN_AT_KEY, String(Date.now()))
  touchActivity()
}

export function clearSignedIn() {
  sessionStorage.removeItem(SIGNED_IN_AT_KEY)
  sessionStorage.removeItem(ACTIVITY_KEY)
}

export function getSignedInAt(): number | null {
  const raw = sessionStorage.getItem(SIGNED_IN_AT_KEY)
  return raw ? Number(raw) : null
}

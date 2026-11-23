export type AccountRole = 'ADMINISTRATOR' | 'EDITOR' | 'VIEWER'

export type AuthUser = {
  id: string
  username: string
  displayName: string
  designation?: string | null
  email?: string | null
  role: AccountRole
  mustChange: boolean
}

export type AuthStatus = {
  setupRequired: boolean
  authenticated: boolean
  user: AuthUser | null
}

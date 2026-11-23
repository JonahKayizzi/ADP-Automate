import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import type { AuthUser } from '@/types/auth'
import * as authApi from '@/services/authApi'
import { ApiError } from '@/services/api'
import {
  getLastActivity,
  IDLE_LOCK_MS,
  setLastUsername,
  touchActivity,
} from './authStorage'

type AuthContextValue = {
  loading: boolean
  setupRequired: boolean
  user: AuthUser | null
  locked: boolean
  bootError: string | null
  refresh: () => Promise<void>
  completeSetup: (input: {
    fullName: string
    designation?: string
    username: string
    password: string
    confirmPassword: string
  }) => Promise<void>
  signIn: (username: string, password: string) => Promise<void>
  signOut: () => Promise<void>
  lock: () => void
  unlock: (password: string) => Promise<void>
  savePassword: (input: {
    currentPassword?: string
    newPassword: string
    confirmPassword: string
  }) => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [loading, setLoading] = useState(true)
  const [setupRequired, setSetupRequired] = useState(false)
  const [user, setUser] = useState<AuthUser | null>(null)
  const [locked, setLocked] = useState(false)
  const [bootError, setBootError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    const status = await authApi.fetchAuthStatus()
    setBootError(null)
    setSetupRequired(Boolean(status.setupRequired))
    setUser(status.user)
    if (!status.authenticated) {
      setLocked(false)
    } else {
      touchActivity()
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    refresh()
      .catch((err) => {
        if (cancelled) return
        setUser(null)
        setBootError(
          err instanceof ApiError
            ? err.detail
            : 'Could not reach the API. Is the backend running on port 8080?',
        )
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [refresh])

  useEffect(() => {
    if (!user || user.mustChange) return

    const onActivity = () => {
      if (!locked) touchActivity()
    }
    const events = ['click', 'keydown', 'scroll', 'touchstart'] as const
    events.forEach((event) => window.addEventListener(event, onActivity, { passive: true }))

    const timer = window.setInterval(() => {
      if (locked) return
      if (Date.now() - getLastActivity() >= IDLE_LOCK_MS) {
        setLocked(true)
      }
    }, 15_000)

    return () => {
      events.forEach((event) => window.removeEventListener(event, onActivity))
      window.clearInterval(timer)
    }
  }, [user, locked])

  const completeSetup = useCallback(async (input: {
    fullName: string
    designation?: string
    username: string
    password: string
    confirmPassword: string
  }) => {
    const created = await authApi.setupAdmin(input)
    setLastUsername(created.username)
    setUser(created)
    setSetupRequired(false)
    setLocked(false)
    touchActivity()
  }, [])

  const signIn = useCallback(async (username: string, password: string) => {
    const result = await authApi.login({ username, password })
    setLastUsername(result.user.username)
    setUser(result.user)
    setSetupRequired(false)
    setLocked(false)
    touchActivity()
  }, [])

  const signOut = useCallback(async () => {
    try {
      await authApi.logout()
    } catch {
      // Session may already be gone.
    }
    setUser(null)
    setLocked(false)
  }, [])

  const lock = useCallback(() => setLocked(true), [])

  const unlock = useCallback(async (password: string) => {
    if (!user) throw new ApiError(401, 'Not signed in.')
    await authApi.unlock({ username: user.username, password })
    setLocked(false)
    touchActivity()
  }, [user])

  const savePassword = useCallback(async (input: {
    currentPassword?: string
    newPassword: string
    confirmPassword: string
  }) => {
    const updated = await authApi.changePassword(input)
    setUser(updated)
    setLocked(false)
    touchActivity()
  }, [])

  const value = useMemo(
    () => ({
      loading,
      setupRequired,
      user,
      locked,
      bootError,
      refresh,
      completeSetup,
      signIn,
      signOut,
      lock,
      unlock,
      savePassword,
    }),
    [
      loading,
      setupRequired,
      user,
      locked,
      bootError,
      refresh,
      completeSetup,
      signIn,
      signOut,
      lock,
      unlock,
      savePassword,
    ],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}

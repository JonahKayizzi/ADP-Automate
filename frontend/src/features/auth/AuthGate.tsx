import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { ApiError } from '@/services/api'
import { getLastUsername } from './authStorage'
import { useAuth } from './AuthContext'
import { AuthCard } from './AuthCard'

function Field({
  id,
  label,
  type = 'text',
  value,
  onChange,
  placeholder,
  autoComplete,
}: {
  id: string
  label: string
  type?: string
  value: string
  onChange: (value: string) => void
  placeholder?: string
  autoComplete?: string
}) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="block text-sm font-semibold text-ink">
        {label}
      </label>
      <input
        id={id}
        type={type}
        value={value}
        placeholder={placeholder}
        autoComplete={autoComplete}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-brand focus:outline-none"
      />
    </div>
  )
}

function PrimaryButton({ children, disabled }: { children: ReactNode; disabled?: boolean }) {
  return (
    <button
      type="submit"
      disabled={disabled}
      className="mt-2 w-full rounded-md bg-brand px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-dark disabled:opacity-60"
    >
      {children}
    </button>
  )
}

function SetupForm() {
  const { completeSetup } = useAuth()
  const [fullName, setFullName] = useState('')
  const [designation, setDesignation] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await completeSetup({ fullName, designation, username, password, confirmPassword })
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Could not create the account.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthCard
      title="Create the administrator account"
      subtitle="No user accounts exist yet. This first account will be an Administrator and can add everyone else under Users."
      footer="Usernames are 3 to 32 characters: lower-case letters, digits, dot, dash or underscore."
    >
      <form className="space-y-4" onSubmit={onSubmit}>
        <Field id="fullName" label="Full name" value={fullName} onChange={setFullName} autoComplete="name" />
        <Field
          id="designation"
          label="Designation"
          value={designation}
          onChange={setDesignation}
          placeholder="e.g. SAIMO"
        />
        <Field
          id="username"
          label="Username"
          value={username}
          onChange={setUsername}
          placeholder="e.g. jblanch"
          autoComplete="username"
        />
        <Field
          id="password"
          label="Password"
          type="password"
          value={password}
          onChange={setPassword}
          autoComplete="new-password"
        />
        <Field
          id="confirmPassword"
          label="Confirm password"
          type="password"
          value={confirmPassword}
          onChange={setConfirmPassword}
          autoComplete="new-password"
        />
        <p className="text-xs text-muted">At least 8 characters, with at least one letter and one number.</p>
        {error ? <p className="text-sm text-danger">{error}</p> : null}
        <PrimaryButton disabled={busy}>{busy ? 'Creating…' : 'Create account and sign in'}</PrimaryButton>
      </form>
    </AuthCard>
  )
}

function SignInForm() {
  const { signIn, refresh } = useAuth()
  const [username, setUsername] = useState(getLastUsername())
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  // If the first status call failed earlier, re-check so first-run setup can appear.
  useEffect(() => {
    void refresh()
  }, [refresh])

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await signIn(username, password)
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Could not sign in.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthCard
      title="Sign in"
      footer="Forgotten your password? Ask an administrator to reset it from Users."
    >
      <form className="space-y-4" onSubmit={onSubmit}>
        <Field
          id="username"
          label="Username"
          value={username}
          onChange={setUsername}
          autoComplete="username"
        />
        <Field
          id="password"
          label="Password"
          type="password"
          value={password}
          onChange={setPassword}
          autoComplete="current-password"
        />
        {error ? <p className="text-sm text-danger">{error}</p> : null}
        <PrimaryButton disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</PrimaryButton>
      </form>
    </AuthCard>
  )
}

function TempPasswordForm() {
  const { user, savePassword, signOut } = useAuth()
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await savePassword({ newPassword, confirmPassword })
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Could not save password.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthCard
      title="Choose a new password"
      subtitle={
        <>
          Your administrator set a temporary password for <strong>{user?.displayName}</strong>. Choose your
          own before you continue.
        </>
      }
    >
      <form className="space-y-4" onSubmit={onSubmit}>
        <Field
          id="newPassword"
          label="New password"
          type="password"
          value={newPassword}
          onChange={setNewPassword}
          autoComplete="new-password"
        />
        <Field
          id="confirmPassword"
          label="Confirm new password"
          type="password"
          value={confirmPassword}
          onChange={setConfirmPassword}
          autoComplete="new-password"
        />
        <p className="text-xs text-muted">At least 8 characters, with at least one letter and one number.</p>
        {error ? <p className="text-sm text-danger">{error}</p> : null}
        <PrimaryButton disabled={busy}>{busy ? 'Saving…' : 'Save password and continue'}</PrimaryButton>
      </form>
      <button
        type="button"
        className="mt-4 w-full text-center text-sm font-semibold text-ink"
        onClick={() => void signOut()}
      >
        Sign out
      </button>
    </AuthCard>
  )
}

function LockForm() {
  const { user, unlock, signOut } = useAuth()
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await unlock(password)
      setPassword('')
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Could not unlock.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthCard
      title="Locked"
      subtitle={
        <>
          Signed in as <strong>{user?.displayName}</strong> ({user?.username}). Enter your password to
          continue.
        </>
      }
      footer="The page locks after 15 minutes without activity."
    >
      <form className="space-y-4" onSubmit={onSubmit}>
        <Field
          id="password"
          label="Password"
          type="password"
          value={password}
          onChange={setPassword}
          autoComplete="current-password"
        />
        {error ? <p className="text-sm text-danger">{error}</p> : null}
        <PrimaryButton disabled={busy}>{busy ? 'Unlocking…' : 'Unlock'}</PrimaryButton>
      </form>
      <button
        type="button"
        className="mt-4 w-full text-center text-sm font-semibold text-ink"
        onClick={() => void signOut()}
      >
        Not you? Sign out
      </button>
    </AuthCard>
  )
}

export function AuthGate({ children }: { children: ReactNode }) {
  const { loading, setupRequired, user, locked, bootError, refresh } = useAuth()
  const [retrying, setRetrying] = useState(false)

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-surface text-sm text-muted">
        Loading…
      </div>
    )
  }

  if (bootError) {
    return (
      <AuthCard title="Cannot reach AIS Doc Manager" subtitle={bootError}>
        <button
          type="button"
          className="w-full rounded-md bg-brand px-4 py-2.5 text-sm font-semibold text-white"
          disabled={retrying}
          onClick={() => {
            setRetrying(true)
            void refresh().finally(() => setRetrying(false))
          }}
        >
          {retrying ? 'Retrying…' : 'Retry'}
        </button>
      </AuthCard>
    )
  }

  if (setupRequired) return <SetupForm />
  if (!user) return <SignInForm />
  if (user.mustChange) return <TempPasswordForm />
  if (locked) return <LockForm />
  return children
}

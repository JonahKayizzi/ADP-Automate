import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '@/features/auth/AuthContext'
import { api, ApiError } from '@/services/api'
import type { AccountRole } from '@/types/auth'

type UserRow = {
  id: string
  username: string
  displayName: string
  designation?: string | null
  email?: string | null
  role: AccountRole
  active: boolean
  mustChange: boolean
  lockedUntil?: string | null
  lastLoginAt?: string | null
  you: boolean
}

const roleHelp: Record<AccountRole, string> = {
  ADMINISTRATOR: 'Manages user accounts, and adds, edits and signs every record.',
  EDITOR: 'Adds and edits records and signs processing file forms.',
  VIEWER: 'Reads records and forms. Cannot change or sign anything.',
}

function statusChip(row: UserRow) {
  if (!row.active) return { label: 'Deactivated', className: 'bg-slate-100 text-slate-700' }
  if (row.lockedUntil && new Date(row.lockedUntil) > new Date()) {
    return { label: 'Locked out', className: 'bg-red-50 text-danger' }
  }
  if (row.mustChange) return { label: 'Temporary password', className: 'bg-amber-50 text-warn' }
  return { label: 'Active', className: 'bg-emerald-50 text-ok' }
}

export function UsersPage() {
  const { user } = useAuth()
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({
    fullName: '',
    designation: '',
    email: '',
    username: '',
    role: 'EDITOR' as AccountRole,
    temporaryPassword: '',
    confirmPassword: '',
    askForNewPassword: true,
  })

  const users = useQuery({
    queryKey: ['users'],
    queryFn: () => api<UserRow[]>('/users'),
    enabled: user?.role === 'ADMINISTRATOR',
  })

  const createUser = useMutation({
    mutationFn: () =>
      api('/users', {
        method: 'POST',
        body: JSON.stringify(form),
      }),
    onSuccess: async () => {
      setOpen(false)
      setError('')
      setForm({
        fullName: '',
        designation: '',
        email: '',
        username: '',
        role: 'EDITOR',
        temporaryPassword: '',
        confirmPassword: '',
        askForNewPassword: true,
      })
      await qc.invalidateQueries({ queryKey: ['users'] })
    },
    onError: (err) => {
      setError(err instanceof ApiError ? err.detail : 'Could not add user.')
    },
  })

  if (user?.role !== 'ADMINISTRATOR') {
    return <Navigate to="/overview" replace />
  }

  const rows = users.data ?? []
  const counts = {
    ADMINISTRATOR: rows.filter((r) => r.active && r.role === 'ADMINISTRATOR').length,
    EDITOR: rows.filter((r) => r.active && r.role === 'EDITOR').length,
    VIEWER: rows.filter((r) => r.active && r.role === 'VIEWER').length,
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    createUser.mutate()
  }

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-semibold text-ink">Users</h1>
          <p className="mt-2 max-w-3xl text-muted">
            Everyone who signs in to AIS Doc Manager needs an account here. Accounts are deactivated rather
            than deleted, so signatures on processing files stay traceable.
          </p>
        </div>
        <button
          type="button"
          className="rounded-md bg-brand px-4 py-2 text-sm font-semibold text-white"
          onClick={() => setOpen(true)}
        >
          Add user
        </button>
      </div>

      <div className="grid gap-3 md:grid-cols-3">
        {(Object.keys(roleHelp) as AccountRole[]).map((role) => (
          <div key={role} className="rounded-xl border border-slate-200 bg-white p-4">
            <div className="font-display text-lg font-semibold">{role.charAt(0) + role.slice(1).toLowerCase()}</div>
            <p className="mt-2 text-sm text-muted">{roleHelp[role]}</p>
            <p className="mt-3 text-sm font-semibold">{counts[role]} active</p>
          </div>
        ))}
      </div>

      <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-slate-100 bg-slate-50 text-xs uppercase text-muted">
            <tr>
              <th className="px-4 py-3">Name</th>
              <th className="px-4 py-3">Designation</th>
              <th className="px-4 py-3">Role</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Last sign-in</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => {
              const status = statusChip(row)
              return (
                <tr key={row.id} className="border-b border-slate-100 last:border-0">
                  <td className="px-4 py-3">
                    <div className="font-semibold">
                      {row.displayName}
                      {row.you ? (
                        <span className="ml-2 rounded bg-slate-100 px-1.5 py-0.5 text-xs font-medium text-muted">
                          You
                        </span>
                      ) : null}
                    </div>
                    <div className="font-mono text-xs text-muted">{row.username}</div>
                  </td>
                  <td className="px-4 py-3">{row.designation || '—'}</td>
                  <td className="px-4 py-3">{row.role}</td>
                  <td className="px-4 py-3">
                    <span className={`rounded px-2 py-1 text-xs font-semibold ${status.className}`}>
                      {status.label}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-muted">
                    {row.lastLoginAt
                      ? new Date(row.lastLoginAt).toLocaleString('en-GB', { timeZone: 'Africa/Kampala' })
                      : '—'}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {open ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
          <form
            onSubmit={onSubmit}
            className="w-full max-w-2xl rounded-xl border border-slate-200 bg-white p-6 shadow-lg"
          >
            <div className="flex items-start justify-between">
              <h2 className="font-display text-xl font-semibold">Add user</h2>
              <button type="button" className="text-sm text-muted" onClick={() => setOpen(false)}>
                Close
              </button>
            </div>
            <div className="mt-4 grid gap-3 md:grid-cols-2">
              <label className="text-sm font-semibold">
                Full name
                <input
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                  value={form.fullName}
                  onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                  required
                />
              </label>
              <label className="text-sm font-semibold">
                Designation (optional)
                <input
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                  value={form.designation}
                  onChange={(e) => setForm({ ...form, designation: e.target.value })}
                />
              </label>
              <label className="text-sm font-semibold">
                Username
                <input
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                  value={form.username}
                  onChange={(e) => setForm({ ...form, username: e.target.value })}
                  required
                />
              </label>
              <label className="text-sm font-semibold">
                Role
                <select
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                  value={form.role}
                  onChange={(e) => setForm({ ...form, role: e.target.value as AccountRole })}
                >
                  <option value="EDITOR">Editor</option>
                  <option value="ADMINISTRATOR">Administrator</option>
                  <option value="VIEWER">Viewer</option>
                </select>
                <span className="mt-1 block text-xs font-normal text-muted">{roleHelp[form.role]}</span>
              </label>
              <label className="text-sm font-semibold">
                Temporary password
                <input
                  type="password"
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                  value={form.temporaryPassword}
                  onChange={(e) => setForm({ ...form, temporaryPassword: e.target.value })}
                  required
                />
              </label>
              <label className="text-sm font-semibold">
                Confirm temporary password
                <input
                  type="password"
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                  value={form.confirmPassword}
                  onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
                  required
                />
              </label>
            </div>
            <label className="mt-4 flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={form.askForNewPassword}
                onChange={(e) => setForm({ ...form, askForNewPassword: e.target.checked })}
              />
              Ask for a new password at first sign-in
            </label>
            {error ? <p className="mt-3 text-sm text-danger">{error}</p> : null}
            <div className="mt-4 flex justify-end gap-2">
              <button type="button" className="rounded-md border px-3 py-2 text-sm" onClick={() => setOpen(false)}>
                Cancel
              </button>
              <button
                type="submit"
                disabled={createUser.isPending}
                className="rounded-md bg-brand px-3 py-2 text-sm font-semibold text-white disabled:opacity-60"
              >
                {createUser.isPending ? 'Adding…' : 'Add user'}
              </button>
            </div>
          </form>
        </div>
      ) : null}
    </section>
  )
}

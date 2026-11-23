import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, type FormEvent } from 'react'
import { useAuth } from '@/features/auth/AuthContext'
import { ApiError } from '@/services/api'
import * as tasksApi from '@/services/tasksApi'
import type { TaskItem, UpsertTask } from '@/types/task'

function todayIso() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Africa/Kampala' }).format(new Date())
}

function formatDate(iso: string) {
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(iso + 'T00:00:00Z'))
}

function formatWhen(iso: string) {
  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(iso))
}

function buildEmail(task: TaskItem, assignerName: string, assignerDesignation?: string | null) {
  const to = task.assigneeEmail ?? ''
  const first = (task.assigneeName ?? 'colleague').split(/\s+/)[0]
  const subject = `Task assigned to you: ${task.title} (due ${formatDate(task.dueDate)})`
  const body = [
    `Dear ${first},`,
    '',
    `You have been assigned the following task in AIS Doc Manager:`,
    '',
    `Task: ${task.title}`,
    `Due: ${formatDate(task.dueDate)}`,
    `Priority: ${task.priority ? 'Priority' : 'Normal'}`,
    `Status: ${task.status}`,
    `Assigned by: ${assignerName}${assignerDesignation ? `, ${assignerDesignation}` : ''}`,
    '',
    `Open Tasks: ${window.location.origin}/tasks`,
    '',
    'Regards,',
    assignerName,
  ].join('\n')
  return { to, subject, body }
}

export function TasksPage() {
  const { user } = useAuth()
  const canEdit = user?.role !== 'VIEWER'
  const qc = useQueryClient()
  const [filter, setFilter] = useState('Open')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<TaskItem | null>(null)
  const [emailStep, setEmailStep] = useState<TaskItem | null>(null)
  const [form, setForm] = useState<UpsertTask>({
    title: '',
    dueDate: todayIso(),
    status: 'To do',
    priority: false,
    assigneeUsername: user?.username ?? '',
  })
  const [error, setError] = useState('')
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)

  const tasks = useQuery({
    queryKey: ['tasks', filter],
    queryFn: () => tasksApi.listTasks(filter),
  })
  const assignees = useQuery({ queryKey: ['task-assignees'], queryFn: tasksApi.listAssignees })

  useEffect(() => {
    void tasksApi.markTasksSeen().then(() => qc.invalidateQueries({ queryKey: ['tasks'] }))
  }, [qc])

  useEffect(() => {
    if (!confirmDeleteId) return
    const t = window.setTimeout(() => setConfirmDeleteId(null), 4000)
    return () => window.clearTimeout(t)
  }, [confirmDeleteId])

  const invalidate = async () => {
    await qc.invalidateQueries({ queryKey: ['tasks'] })
    await qc.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const save = useMutation({
    mutationFn: async () => {
      if (editing) return tasksApi.updateTask(editing.id, form)
      return tasksApi.createTask(form)
    },
    onSuccess: async (task) => {
      setOpen(false)
      setEditing(null)
      setError('')
      await invalidate()
      if (task.notifyStatus === 'pending') {
        setEmailStep(task)
      }
    },
    onError: (err) => setError(err instanceof ApiError ? err.detail : 'Could not save task.'),
  })

  const complete = useMutation({
    mutationFn: (id: string) => tasksApi.completeTask(id),
    onSuccess: invalidate,
  })

  const notify = useMutation({
    mutationFn: ({ id, outcome }: { id: string; outcome: 'emailed' | 'skipped' }) =>
      tasksApi.notifyTask(id, outcome),
    onSuccess: async () => {
      setEmailStep(null)
      await invalidate()
    },
  })

  const remove = useMutation({
    mutationFn: (id: string) => tasksApi.deleteTask(id),
    onSuccess: async () => {
      setConfirmDeleteId(null)
      await invalidate()
    },
  })

  function openCreate() {
    setEditing(null)
    setForm({
      title: '',
      dueDate: todayIso(),
      status: 'To do',
      priority: false,
      assigneeUsername: user?.username ?? '',
    })
    setError('')
    setOpen(true)
  }

  function openEdit(task: TaskItem) {
    setEditing(task)
    setForm({
      title: task.title,
      dueDate: task.dueDate,
      status: task.status,
      priority: task.priority,
      assigneeUsername: task.assigneeUsername ?? user?.username ?? '',
    })
    setError('')
    setOpen(true)
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    save.mutate()
  }

  const selectedAssignee = (assignees.data ?? []).find((a) => a.username === form.assigneeUsername)
  const rows = tasks.data ?? []

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-semibold text-ink">Task list</h1>
          <p className="mt-2 max-w-3xl text-muted">
            Assign work across AIM Entebbe, track due dates, and record whether the assignee was emailed.
          </p>
        </div>
        {canEdit ? (
          <button type="button" className="rounded-md bg-brand px-4 py-2 text-sm font-semibold text-white" onClick={openCreate}>
            Add task
          </button>
        ) : null}
      </div>

      <div className="flex flex-wrap gap-2">
        {['Open', 'Mine', 'Overdue', 'Done', 'All'].map((f) => (
          <button
            key={f}
            type="button"
            className={`rounded-md px-3 py-1.5 text-sm font-semibold ${
              filter === f ? 'bg-brand text-white' : 'border bg-white text-ink'
            }`}
            onClick={() => setFilter(f)}
          >
            {f}
          </button>
        ))}
      </div>

      {tasks.isLoading ? <p className="text-sm text-muted">Loading tasks…</p> : null}
      {!tasks.isLoading && rows.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-300 bg-white p-8 text-center text-muted">
          No tasks in this filter.
        </div>
      ) : null}

      <div className="space-y-3">
        {rows.map((task) => (
          <article key={task.id} className="rounded-xl border border-slate-200 bg-white p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="font-display text-lg font-semibold text-ink">{task.title}</h2>
                  {task.newForYou ? (
                    <span className="rounded bg-sky-50 px-2 py-0.5 text-xs font-semibold text-sky-800">New for you</span>
                  ) : null}
                  {task.overdue ? (
                    <span className="rounded bg-red-50 px-2 py-0.5 text-xs font-semibold text-danger">Overdue</span>
                  ) : (
                    <span className="rounded bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">{task.status}</span>
                  )}
                  {task.priority ? (
                    <span className="rounded bg-amber-50 px-2 py-0.5 text-xs font-semibold text-warn">Priority</span>
                  ) : null}
                </div>
                <div className="mt-2 flex flex-wrap items-center gap-2 text-sm text-muted">
                  <span className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-slate-200 text-xs font-bold text-ink">
                    {task.assigneeInitials}
                  </span>
                  <span>
                    {task.assigneeName}
                    {task.assigneeDesignation ? ` · ${task.assigneeDesignation}` : ''}
                  </span>
                  <span>· Due {formatDate(task.dueDate)}</span>
                  {task.daysLate != null ? <span className="font-semibold text-danger">· {task.daysLate} day{task.daysLate === 1 ? '' : 's'} late</span> : null}
                </div>
                {task.assignedByName && task.assignedAt ? (
                  <p className="mt-1 text-xs text-muted">
                    Assigned by {task.assignedByName} · {formatWhen(task.assignedAt)}
                  </p>
                ) : null}
                <p className={`mt-2 text-xs font-semibold ${task.notifyStatus === 'pending' ? 'text-warn' : 'text-muted'}`}>
                  {task.notifyStatus === 'pending'
                    ? 'Email not sent yet'
                    : task.notifyStatus === 'emailed'
                      ? `Emailed${task.notifyBy ? ` by ${task.notifyBy}` : ''}${task.notifyAt ? ` · ${formatWhen(task.notifyAt)}` : ''}`
                      : task.notifyStatus === 'skipped'
                        ? 'No email'
                        : 'No email needed'}
                  {canEdit && task.notifyStatus === 'pending' ? (
                    <>
                      {' · '}
                      <button type="button" className="underline" onClick={() => setEmailStep(task)}>
                        Email assignee
                      </button>
                    </>
                  ) : null}
                </p>
              </div>
              <div className="flex flex-wrap gap-2">
                {canEdit && task.status !== 'Done' ? (
                  <button
                    type="button"
                    className="rounded-md bg-emerald-600 px-3 py-1.5 text-sm font-semibold text-white"
                    onClick={() => complete.mutate(task.id)}
                  >
                    Mark done
                  </button>
                ) : null}
                {canEdit ? (
                  <>
                    <button type="button" className="rounded-md border px-3 py-1.5 text-sm" onClick={() => openEdit(task)}>
                      Edit
                    </button>
                    <button
                      type="button"
                      className="rounded-md border border-red-200 px-3 py-1.5 text-sm text-danger"
                      onClick={() => {
                        if (confirmDeleteId === task.id) remove.mutate(task.id)
                        else setConfirmDeleteId(task.id)
                      }}
                    >
                      {confirmDeleteId === task.id ? 'Confirm delete' : 'Delete'}
                    </button>
                  </>
                ) : null}
              </div>
            </div>
          </article>
        ))}
      </div>

      {open ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
          <form onSubmit={onSubmit} className="w-full max-w-lg rounded-xl border bg-white p-6 shadow-lg">
            <div className="flex items-start justify-between">
              <h2 className="font-display text-xl font-semibold">{editing ? 'Edit task' : 'Add task'}</h2>
              <button type="button" className="text-sm text-muted" onClick={() => setOpen(false)}>
                Close
              </button>
            </div>
            <div className="mt-4 space-y-3">
              <label className="block text-sm font-semibold">
                Title
                <input className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
              </label>
              <label className="block text-sm font-semibold">
                Due date
                <input type="date" className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal" value={form.dueDate} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} required />
              </label>
              <label className="block text-sm font-semibold">
                Status
                <select className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                  <option>To do</option>
                  <option>In progress</option>
                  <option>Done</option>
                </select>
              </label>
              <label className="flex items-center gap-2 text-sm font-semibold">
                <input type="checkbox" checked={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.checked })} />
                Priority
              </label>
              <label className="block text-sm font-semibold">
                Assigned to
                <select
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm font-normal"
                  value={form.assigneeUsername}
                  onChange={(e) => setForm({ ...form, assigneeUsername: e.target.value })}
                  required
                >
                  {(assignees.data ?? []).map((a) => (
                    <option key={a.username} value={a.username}>
                      {a.displayName}
                      {!a.email ? ' (no email)' : ''}
                    </option>
                  ))}
                </select>
              </label>
              <p className="text-xs text-muted">
                {form.assigneeUsername && form.assigneeUsername !== user?.username
                  ? selectedAssignee?.email
                    ? 'An email step will open after save so you can notify the assignee.'
                    : 'This account has no email — add one under Users before notifying.'
                  : 'No assignment email is needed when you assign the task to yourself.'}
              </p>
            </div>
            {error ? <p className="mt-3 text-sm text-danger">{error}</p> : null}
            <div className="mt-4 flex justify-end gap-2">
              <button type="button" className="rounded-md border px-3 py-2 text-sm" onClick={() => setOpen(false)}>
                Cancel
              </button>
              <button type="submit" disabled={save.isPending} className="rounded-md bg-brand px-3 py-2 text-sm font-semibold text-white disabled:opacity-60">
                {save.isPending ? 'Saving…' : editing ? 'Save changes' : 'Add task'}
              </button>
            </div>
          </form>
        </div>
      ) : null}

      {emailStep ? (
        <EmailStep
          task={emailStep}
          assignerName={user?.displayName ?? 'Colleague'}
          assignerDesignation={user?.designation}
          busy={notify.isPending}
          onEmailed={() => notify.mutate({ id: emailStep.id, outcome: 'emailed' })}
          onSkip={() => notify.mutate({ id: emailStep.id, outcome: 'skipped' })}
          onClose={() => setEmailStep(null)}
        />
      ) : null}
    </section>
  )
}

function EmailStep({
  task,
  assignerName,
  assignerDesignation,
  busy,
  onEmailed,
  onSkip,
  onClose,
}: {
  task: TaskItem
  assignerName: string
  assignerDesignation?: string | null
  busy: boolean
  onEmailed: () => void
  onSkip: () => void
  onClose: () => void
}) {
  const mail = buildEmail(task, assignerName, assignerDesignation)
  const outlook = `https://outlook.office.com/mail/deeplink/compose?to=${encodeURIComponent(mail.to)}&subject=${encodeURIComponent(mail.subject)}&body=${encodeURIComponent(mail.body)}`

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
      <div className="w-full max-w-lg rounded-xl border bg-white p-6 shadow-lg">
        <div className="flex items-start justify-between">
          <h2 className="font-display text-xl font-semibold">Assignment email</h2>
          <button type="button" className="text-sm text-muted" onClick={onClose}>
            Close
          </button>
        </div>
        {!mail.to ? (
          <p className="mt-4 text-sm text-muted">
            {task.assigneeName} has no email address on their account. Add one under Users, then return here.
          </p>
        ) : (
          <div className="mt-4 space-y-2 text-sm">
            <p>
              <span className="font-semibold">To:</span> {task.assigneeName} &lt;{mail.to}&gt;
            </p>
            <p>
              <span className="font-semibold">Subject:</span> {mail.subject}
            </p>
            <pre className="max-h-48 overflow-auto whitespace-pre-wrap rounded-md border bg-slate-50 p-3 text-xs">{mail.body}</pre>
          </div>
        )}
        <div className="mt-4 flex flex-wrap justify-end gap-2">
          {mail.to ? (
            <>
              <a className="rounded-md border px-3 py-2 text-sm" href={outlook} target="_blank" rel="noreferrer">
                Open in Outlook
              </a>
              <button
                type="button"
                className="rounded-md border px-3 py-2 text-sm"
                onClick={() => void navigator.clipboard.writeText(`To: ${mail.to}\nSubject: ${mail.subject}\n\n${mail.body}`)}
              >
                Copy message
              </button>
              <button type="button" disabled={busy} className="rounded-md bg-brand px-3 py-2 text-sm font-semibold text-white" onClick={onEmailed}>
                I&apos;ve sent it
              </button>
            </>
          ) : null}
          <button type="button" disabled={busy} className="rounded-md border px-3 py-2 text-sm" onClick={onSkip}>
            Don&apos;t email
          </button>
        </div>
      </div>
    </div>
  )
}

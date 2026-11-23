import type { ReactNode } from 'react'

type Props = {
  title: string
  subtitle?: ReactNode
  children: ReactNode
  footer?: ReactNode
}

function todayLabel() {
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    timeZone: 'Africa/Kampala',
  }).format(new Date())
}

export function AuthCard({ title, subtitle, children, footer }: Props) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-surface px-4 py-8">
      <div className="w-full max-w-md rounded-xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 px-6 py-5">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-brand text-white" aria-hidden>
              ✈
            </div>
            <div>
              <div className="font-display text-sm font-bold uppercase tracking-wide text-ink">
                AIS Doc Manager
              </div>
              <div className="text-xs text-muted">UCAA · AIM Entebbe</div>
            </div>
          </div>
          <div className="mt-4 flex items-center justify-between text-xs text-muted">
            <span>{todayLabel()}</span>
            <span>
              AIRAC <span className="font-mono font-semibold text-airac">setup</span>
            </span>
          </div>
        </div>

        <div className="px-6 py-6">
          <h1 className="font-display text-2xl font-semibold text-ink">{title}</h1>
          {subtitle ? <div className="mt-2 text-sm text-muted">{subtitle}</div> : null}
          <div className="mt-6">{children}</div>
        </div>

        {footer ? (
          <div className="border-t border-slate-100 bg-slate-50 px-6 py-3 text-xs text-muted">
            {footer}
          </div>
        ) : null}
      </div>
    </div>
  )
}

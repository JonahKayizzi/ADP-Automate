import type { ReactNode } from 'react'

type PillProps = {
  value: string
  options: { value: string; label: string; tone?: 'ok' | 'bad' | 'neutral' }[]
  disabled?: boolean
  onChange: (value: string) => void
}

export function PillGroup({ value, options, disabled, onChange }: PillProps) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {options.map((opt) => {
        const selected = value === opt.value
        const tone =
          opt.tone === 'ok'
            ? selected
              ? 'bg-emerald-600 text-white border-emerald-600'
              : 'border-emerald-200 text-emerald-800'
            : opt.tone === 'bad'
              ? selected
                ? 'bg-red-600 text-white border-red-600'
                : 'border-red-200 text-red-700'
              : selected
                ? 'bg-slate-800 text-white border-slate-800'
                : 'border-slate-300 text-ink'
        return (
          <button
            key={opt.value}
            type="button"
            disabled={disabled}
            className={`rounded-full border px-2.5 py-1 text-xs font-semibold disabled:opacity-60 ${tone}`}
            onClick={() => onChange(opt.value)}
          >
            {opt.label}
          </button>
        )
      })}
    </div>
  )
}

export const pctOptions = [
  { value: '100%', label: '100%', tone: 'ok' as const },
  { value: '0%', label: '0%', tone: 'bad' as const },
]

export const pctNaOptions = [
  ...pctOptions,
  { value: 'N/A', label: 'N/A', tone: 'neutral' as const },
]

export const yesNoOptions = [
  { value: 'Yes', label: 'Yes', tone: 'ok' as const },
  { value: 'No', label: 'No', tone: 'bad' as const },
]

export const integrityOptions = [
  { value: 'Routine', label: 'Routine', tone: 'neutral' as const },
  { value: 'Essential', label: 'Essential', tone: 'neutral' as const },
  { value: 'Critical', label: 'Critical', tone: 'bad' as const },
]

type FieldProps = {
  label: string
  children: ReactNode
}

export function Field({ label, children }: FieldProps) {
  return (
    <label className="block text-sm font-semibold text-ink">
      {label}
      <div className="mt-1 font-normal">{children}</div>
    </label>
  )
}

export function TextArea({
  value,
  disabled,
  onChange,
  rows = 3,
}: {
  value: string
  disabled?: boolean
  onChange: (v: string) => void
  rows?: number
}) {
  return (
    <textarea
      className="w-full rounded-md border px-3 py-2 text-sm disabled:bg-slate-50"
      rows={rows}
      value={value}
      disabled={disabled}
      onChange={(e) => onChange(e.target.value)}
    />
  )
}

export function TextInput({
  value,
  disabled,
  onChange,
  type = 'text',
}: {
  value: string
  disabled?: boolean
  onChange: (v: string) => void
  type?: string
}) {
  return (
    <input
      type={type}
      className="w-full rounded-md border px-3 py-2 text-sm disabled:bg-slate-50"
      value={value}
      disabled={disabled}
      onChange={(e) => onChange(e.target.value)}
    />
  )
}

export function asString(v: unknown) {
  return typeof v === 'string' ? v : ''
}

export function asBool(v: unknown) {
  return v === true
}

export function asArray<T>(v: unknown): T[] {
  return Array.isArray(v) ? (v as T[]) : []
}

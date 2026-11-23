import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import * as processingApi from '@/services/processingApi'
import type { ProcessingOwnerType } from '@/types/processing'
import { ProcessingModal } from './ProcessingModal'

type Props = {
  ownerType: ProcessingOwnerType
  ownerId: string
  canEdit: boolean
  label?: string
}

function statusClass(status: string) {
  if (status === 'Signed off') return 'text-ok'
  if (status === 'In progress') return 'text-warn'
  return 'text-muted'
}

export function ProcessingFilePanel({ ownerType, ownerId, canEdit, label }: Props) {
  const [openStep, setOpenStep] = useState<number | null>(null)
  const file = useQuery({
    queryKey: ['processing-file', ownerType, ownerId],
    queryFn: () => processingApi.getProcessingFile(ownerType, ownerId),
  })

  const data = file.data
  const chip = data ? `${data.signedOffCount} of ${data.totalForms} signed off` : '…'

  return (
    <>
      <div className="mt-3 rounded-lg border border-slate-200 bg-white px-3 py-3">
        <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-muted">
            Processing file{label ? ` · ${label}` : ''}
          </h3>
          <span
            className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${
              data && data.signedOffCount === data.totalForms
                ? 'bg-emerald-50 text-ok'
                : 'bg-amber-50 text-warn'
            }`}
          >
            {chip}
          </span>
        </div>

        {file.isLoading ? <p className="text-xs text-muted">Loading processing file…</p> : null}
        {file.isError ? <p className="text-xs text-danger">Could not load processing file.</p> : null}

        {data ? (
          <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
            {data.forms.map((form) => (
              <button
                key={form.id}
                type="button"
                className="rounded-md border border-slate-200 bg-slate-50 px-2.5 py-2 text-left hover:border-brand/40"
                onClick={() => setOpenStep(form.stepNumber)}
              >
                <div className="flex items-center gap-2">
                  <span
                    className={`flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold ${
                      form.status === 'Signed off' ? 'bg-emerald-600 text-white' : 'bg-slate-200 text-ink'
                    }`}
                  >
                    {form.status === 'Signed off' ? '✓' : form.stepNumber}
                  </span>
                  <span className="text-xs font-semibold text-ink">{form.formTitle.split('(')[0].trim()}</span>
                </div>
                <p className="mt-1 font-mono text-[10px] text-muted">{form.formCode.split('/').slice(-1)[0]}</p>
                <p className={`mt-1 text-[11px] font-semibold ${statusClass(form.status)}`}>{form.status}</p>
              </button>
            ))}
          </div>
        ) : null}
      </div>

      {openStep != null && data ? (
        <ProcessingModal
          file={data}
          initialStep={openStep}
          canEdit={canEdit}
          onClose={() => setOpenStep(null)}
        />
      ) : null}
    </>
  )
}

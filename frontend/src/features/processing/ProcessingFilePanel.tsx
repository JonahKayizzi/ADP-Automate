import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { ApiError } from '@/services/api'
import * as processingApi from '@/services/processingApi'
import type { FormSummary, ProcessingOwnerType } from '@/types/processing'
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

function cardLabel(form: FormSummary) {
  if (form.formCode.endsWith('/OF/82')) return 'Raw data quality check'
  return form.formTitle.split('(')[0].trim()
}

export function ProcessingFilePanel({ ownerType, ownerId, canEdit, label }: Props) {
  const [openStep, setOpenStep] = useState<number | null>(null)
  const [exportError, setExportError] = useState('')
  const [exportingId, setExportingId] = useState<string | null>(null)
  const file = useQuery({
    queryKey: ['processing-file', ownerType, ownerId],
    queryFn: () => processingApi.getProcessingFile(ownerType, ownerId),
  })

  const data = file.data
  const chip = data ? `${data.signedOffCount} of ${data.totalForms} signed off` : '…'

  async function onExport(form: FormSummary) {
    if (!data || form.status !== 'Signed off') return
    setExportError('')
    setExportingId(form.id)
    try {
      await processingApi.exportOf82Pdf(
        data.id,
        form.id,
        `OF82-${ownerType}-${ownerId.slice(0, 8)}.pdf`
      )
    } catch (err) {
      setExportError(err instanceof ApiError ? err.detail : 'Could not export the PDF.')
    } finally {
      setExportingId(null)
    }
  }

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
            {data.forms.map((form) => {
              const of82 = form.formCode.endsWith('/OF/82')
              const canExport = of82 && form.status === 'Signed off'
              return (
                <div
                  key={form.id}
                  className="rounded-md border border-slate-200 bg-slate-50 px-2.5 py-2"
                >
                  <button
                    type="button"
                    className="w-full text-left hover:opacity-90"
                    onClick={() => setOpenStep(form.stepNumber)}
                  >
                    <div className="flex items-center gap-2">
                      <span
                        className={`flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold ${
                          form.status === 'Signed off'
                            ? 'bg-emerald-600 text-white'
                            : 'bg-slate-200 text-ink'
                        }`}
                      >
                        {form.status === 'Signed off' ? '✓' : form.stepNumber}
                      </span>
                      <span className="text-xs font-semibold text-ink">{cardLabel(form)}</span>
                    </div>
                    <p className="mt-1 font-mono text-[10px] text-muted">
                      {form.formCode.split('/').slice(-1)[0]}
                    </p>
                    <p className={`mt-1 text-[11px] font-semibold ${statusClass(form.status)}`}>
                      {form.status}
                    </p>
                  </button>
                  {of82 ? (
                    <button
                      type="button"
                      className="mt-2 w-full rounded border border-slate-300 bg-white px-2 py-1 text-[11px] font-semibold text-ink disabled:cursor-not-allowed disabled:opacity-50"
                      disabled={!canExport || exportingId === form.id}
                      title={
                        canExport
                          ? 'Export signed OF/82 as PDF'
                          : 'Available after this step is signed off'
                      }
                      onClick={() => void onExport(form)}
                    >
                      {exportingId === form.id ? 'Exporting…' : 'Export PDF'}
                    </button>
                  ) : null}
                </div>
              )
            })}
          </div>
        ) : null}
        {exportError ? <p className="mt-2 text-xs text-danger">{exportError}</p> : null}
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

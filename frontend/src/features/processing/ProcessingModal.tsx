import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useAuth } from '@/features/auth/AuthContext'
import { ApiError } from '@/services/api'
import * as processingApi from '@/services/processingApi'
import type { FormSummary, ProcessingFile } from '@/types/processing'
import { FormEditor } from './FormEditors'
import { Field, TextInput } from './formControls'

type Props = {
  file: ProcessingFile
  initialStep: number
  canEdit: boolean
  onClose: () => void
}

function shortStepLabel(formCode: string, formTitle: string) {
  if (formCode.endsWith('/OF/82')) return 'Raw data QC'
  if (formCode.endsWith('/OF/83a')) return 'AIP metadata'
  if (formCode.endsWith('/OF/83b')) return 'Metadata'
  if (formCode.endsWith('/OF/83c')) return 'Raw data processing'
  if (formCode.endsWith('/OF/83d')) return 'Product QC'
  return formTitle.split('(')[0].trim()
}

export function ProcessingModal({ file, initialStep, canEdit, onClose }: Props) {
  const { user } = useAuth()
  const qc = useQueryClient()
  const [step, setStep] = useState(initialStep)
  const formSummary = file.forms.find((f) => f.stepNumber === step) as FormSummary
  const [payload, setPayload] = useState<Record<string, unknown>>({})
  const [toast, setToast] = useState('')
  const [error, setError] = useState('')
  const [confirmRevise, setConfirmRevise] = useState(false)
  const [signerName, setSignerName] = useState(user?.displayName ?? '')
  const [designation, setDesignation] = useState(user?.designation ?? '')

  const detail = useQuery({
    queryKey: ['processing-form', file.id, formSummary.id],
    queryFn: () => processingApi.getForm(file.id, formSummary.id),
  })

  useEffect(() => {
    if (detail.data) {
      setPayload(detail.data.payload)
      setError('')
      setConfirmRevise(false)
    }
  }, [detail.data])

  useEffect(() => {
    if (!toast) return
    const t = window.setTimeout(() => setToast(''), 3500)
    return () => window.clearTimeout(t)
  }, [toast])

  const invalidate = async () => {
    await qc.invalidateQueries({ queryKey: ['processing-file', file.ownerType, file.ownerId] })
    await qc.invalidateQueries({ queryKey: ['processing-form', file.id, formSummary.id] })
    await qc.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const save = useMutation({
    mutationFn: () => processingApi.saveForm(file.id, formSummary.id, payload),
    onSuccess: async (data) => {
      const prevSigned = detail.data?.signatures.filter((s) => s.signed).length ?? 0
      if (prevSigned > 0) {
        setToast('Saving cleared existing signatures on this form.')
      }
      setPayload(data.payload)
      await invalidate()
      setError('')
    },
    onError: (err) => setError(err instanceof ApiError ? err.detail : 'Could not save the form.'),
  })

  const sign = useMutation({
    mutationFn: (slotKey: string) =>
      processingApi.signForm(file.id, formSummary.id, slotKey, { signerName, designation }),
    onSuccess: async () => {
      await invalidate()
      setError('')
    },
    onError: (err) => setError(err instanceof ApiError ? err.detail : 'Could not sign.'),
  })

  const revise = useMutation({
    mutationFn: () => processingApi.reviseForm(file.id, formSummary.id),
    onSuccess: async () => {
      setConfirmRevise(false)
      await invalidate()
      setError('')
    },
    onError: (err) => setError(err instanceof ApiError ? err.detail : 'Could not revise.'),
  })

  const locked = detail.data?.locked ?? false
  const signatures = detail.data?.signatures ?? []

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-3 sm:p-6">
      <div className="flex max-h-[92vh] w-full max-w-4xl flex-col overflow-hidden rounded-xl border bg-white shadow-xl">
        <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-200 px-5 py-5">
          <div className="min-w-0 pr-4">
            <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-muted">
              Processing file · Step {step} of {file.forms.length}
            </p>
            <h2 className="mt-1.5 font-display text-xl font-semibold leading-snug text-ink">
              {formSummary.formTitle}
              <span className="ml-2 font-sans text-sm font-normal text-muted">· {file.recordKind}</span>
            </h2>
            <p className="mt-1.5 font-mono text-xs text-muted">{formSummary.formCode}</p>
          </div>
          <button type="button" className="shrink-0 pt-0.5 text-sm text-muted hover:text-ink" onClick={onClose}>
            Close
          </button>
        </div>

        <nav className="border-b border-slate-200 px-5" aria-label="Processing form steps">
          <ol className="-mb-px flex gap-1 overflow-x-auto pb-0">
            {file.forms.map((f) => {
              const active = f.stepNumber === step
              const signed = f.status === 'Signed off'
              const inProgress = f.status === 'In progress'
              return (
                <li key={f.id} className="min-w-[10.5rem] flex-1">
                  <button
                    type="button"
                    aria-current={active ? 'step' : undefined}
                    className={`flex h-full w-full items-start gap-2.5 border-b-2 px-2 py-3.5 text-left transition-colors ${
                      active
                        ? 'border-brand bg-sky-50/60'
                        : 'border-transparent hover:bg-slate-50'
                    }`}
                    onClick={() => setStep(f.stepNumber)}
                  >
                    <span
                      className={`mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                        signed
                          ? 'bg-emerald-600 text-white'
                          : active
                            ? 'bg-brand text-white'
                            : inProgress
                              ? 'bg-amber-500 text-white'
                              : 'bg-slate-200 text-slate-700'
                      }`}
                    >
                      {signed ? '✓' : f.stepNumber}
                    </span>
                    <span className="min-w-0 pt-0.5">
                      <span
                        className={`block whitespace-normal text-sm font-semibold leading-5 ${
                          active ? 'text-brand' : 'text-ink'
                        }`}
                      >
                        {shortStepLabel(f.formCode, f.formTitle)}
                      </span>
                      <span
                        className={`mt-1 block text-[11px] font-medium leading-4 ${
                          signed ? 'text-ok' : inProgress ? 'text-warn' : 'text-muted'
                        }`}
                      >
                        {f.status}
                      </span>
                    </span>
                  </button>
                </li>
              )
            })}
          </ol>
        </nav>

        <div className="flex-1 overflow-y-auto px-5 py-4">
          {toast ? (
            <div className="mb-3 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
              {toast}
            </div>
          ) : null}
          {locked ? (
            <div className="mb-3 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm font-semibold text-ok">
              Signed off — this form is locked.
            </div>
          ) : null}
          {detail.isLoading ? <p className="text-sm text-muted">Loading form…</p> : null}
          {detail.data ? (
            <FormEditor
              formCode={detail.data.formCode}
              payload={payload}
              locked={locked || !canEdit}
              onChange={setPayload}
            />
          ) : null}

          <div className="mt-6 space-y-3 border-t pt-4">
            <h3 className="font-display text-base font-semibold">Sign-offs</h3>
            {canEdit && !locked ? (
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label="Name">
                  <TextInput value={signerName} onChange={setSignerName} />
                </Field>
                <Field label="Designation">
                  <TextInput value={designation} onChange={setDesignation} />
                </Field>
              </div>
            ) : null}
            {signatures.map((slot) => (
              <div
                key={slot.slotKey}
                className="flex flex-wrap items-center justify-between gap-2 rounded-md border bg-slate-50 px-3 py-2"
              >
                <div>
                  <p className="text-sm font-semibold">{slot.slotLabel}</p>
                  {slot.signed ? (
                    <p className="text-xs text-muted">
                      {slot.signerName}
                      {slot.designation ? ` · ${slot.designation}` : ''}
                      {slot.signedAt
                        ? ` · ${new Intl.DateTimeFormat('en-GB', {
                            dateStyle: 'medium',
                            timeStyle: 'short',
                          }).format(new Date(slot.signedAt))}`
                        : ''}
                    </p>
                  ) : (
                    <p className="text-xs text-muted">Not signed</p>
                  )}
                </div>
                {canEdit && !locked && !slot.signed ? (
                  <button
                    type="button"
                    className="rounded-md bg-brand px-3 py-1.5 text-xs font-semibold text-white"
                    disabled={sign.isPending}
                    onClick={() => sign.mutate(slot.slotKey)}
                  >
                    Sign
                  </button>
                ) : null}
              </div>
            ))}
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3 border-t px-5 py-3">
          <p className="max-w-xl text-[11px] text-muted">{detail.data?.controlledNotice}</p>
          <div className="flex flex-wrap gap-2">
            {canEdit && locked ? (
              <button
                type="button"
                className="rounded-md border border-red-200 px-3 py-2 text-sm text-danger"
                onClick={() => {
                  if (confirmRevise) revise.mutate()
                  else setConfirmRevise(true)
                }}
              >
                {confirmRevise ? 'Confirm revise' : 'Revise form'}
              </button>
            ) : null}
            {canEdit && !locked ? (
              <button
                type="button"
                className="rounded-md bg-brand px-3 py-2 text-sm font-semibold text-white disabled:opacity-60"
                disabled={save.isPending}
                onClick={() => save.mutate()}
              >
                {save.isPending ? 'Saving…' : 'Save form'}
              </button>
            ) : null}
            {step < file.forms.length ? (
              <button
                type="button"
                className="rounded-md border px-3 py-2 text-sm"
                onClick={() => setStep(step + 1)}
              >
                Next
              </button>
            ) : null}
          </div>
        </div>
        {error ? <p className="border-t px-5 py-2 text-sm text-danger">{error}</p> : null}
      </div>
    </div>
  )
}

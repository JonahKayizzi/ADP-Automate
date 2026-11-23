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
        <div className="flex flex-wrap items-start justify-between gap-3 border-b px-5 py-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-muted">
              Processing file · {file.recordKind}
            </p>
            <h2 className="font-display text-xl font-semibold text-ink">
              Step {step}: {formSummary.formTitle}
            </h2>
            <p className="mt-1 font-mono text-xs text-muted">{formSummary.formCode}</p>
          </div>
          <button type="button" className="text-sm text-muted" onClick={onClose}>
            Close
          </button>
        </div>

        <div className="flex gap-1 overflow-x-auto border-b px-3 py-2">
          {file.forms.map((f) => (
            <button
              key={f.id}
              type="button"
              className={`rounded-md px-3 py-1.5 text-xs font-semibold ${
                f.stepNumber === step ? 'bg-brand text-white' : 'bg-slate-100 text-ink'
              }`}
              onClick={() => setStep(f.stepNumber)}
            >
              {f.stepNumber}. {f.formTitle.split('(')[0].trim()}
            </button>
          ))}
        </div>

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

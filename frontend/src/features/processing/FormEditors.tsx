import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { InlineSlotAttach } from '@/features/attachments/InlineSlotAttach'
import * as attachmentsApi from '@/services/attachmentsApi'
import type { AttachmentSlot } from '@/types/attachment'
import type { ProcessingOwnerType } from '@/types/processing'
import { calculateConfidence } from './confidence'
import {
  Field,
  PillGroup,
  TextArea,
  TextInput,
  asArray,
  asBool,
  asString,
  integrityOptions,
  pctNaOptions,
  pctOptions,
  yesNoOptions,
} from './formControls'

export type FormEditorContext = {
  ownerType: ProcessingOwnerType
  ownerId: string
  recordKind: string
  canEdit: boolean
}

const AIP_SLOT_LABELS: Record<string, string> = {
  promulgation: 'Signed promulgation form',
  data_collection: 'Data collection form',
  coordinate_verification: 'Coordinate verification file',
  metadata_form: 'Meta data form',
  safety_assessment: 'Safety assessment (if exists)',
  regulator_approval: 'Regulator approval (if required)',
}

const REMOVED_AIP_ITEMS = new Set([
  'Meta data for AIP',
  'Approval from the regulator if required',
])

type Props = {
  formCode: string
  payload: Record<string, unknown>
  locked: boolean
  onChange: (next: Record<string, unknown>) => void
  context?: FormEditorContext
}

function isAipRecord(recordKind: string) {
  const upper = recordKind.toUpperCase()
  return upper.includes('AMDT') || (upper.includes('AIP') && !upper.includes('SUP'))
}

type CompletenessRow = { item: string; column?: string; value: string }
type AixmRow = { item: string; value: string }
type SystemGroup = { name: string; steps: { label: string; done: string }[] }
type MetaField =
  | { kind: 'section'; title: string }
  | {
      kind: 'field'
      key: string
      item: string
      description?: string
      input?: 'text' | 'textarea' | 'date' | 'adq' | 'operation'
    }
type Part1Row = {
  label: string
  description: string
  completed: string
  remarks: string
  date: string
  by: string
}

export function FormEditor({ formCode, payload, locked, onChange, context }: Props) {
  if (formCode.endsWith('/OF/82')) {
    return <Of82 payload={payload} locked={locked} onChange={onChange} context={context} />
  }
  if (formCode.endsWith('/OF/83a')) {
    return <Of83a payload={payload} locked={locked} onChange={onChange} />
  }
  if (formCode.endsWith('/OF/83b')) {
    return <Of83b payload={payload} locked={locked} onChange={onChange} />
  }
  if (formCode.endsWith('/OF/83c')) {
    return <Of83c payload={payload} locked={locked} onChange={onChange} />
  }
  return <Of83d payload={payload} locked={locked} onChange={onChange} />
}

function set(payload: Record<string, unknown>, key: string, value: unknown, onChange: Props['onChange']) {
  onChange({ ...payload, [key]: value })
}

function ConfidenceBanner({ payload }: { payload: Record<string, unknown> }) {
  const result = calculateConfidence(payload)
  return (
    <div className="rounded-md border border-slate-200 bg-slate-50 px-4 py-3">
      <p className="text-sm font-semibold text-ink">
        Calculated from checks {result.percent}%
      </p>
      <p className="mt-1 text-xs text-muted">
        {result.scored100} of {result.scoredTotal} checks scored at 100%
        {result.naOrBlank > 0 ? ` · ${result.naOrBlank} N/A or blank excluded` : ''}
        {' '}· share of scored checks at 100%
      </p>
    </div>
  )
}

const AIP_AUTO_ITEMS: Record<string, string> = {
  'All source data received': 'data_collection',
  'Coordinates verified': 'coordinate_verification',
  'Effective dates confirmed': 'metadata_form',
  'Layout / format reviewed': 'metadata_form',
  'Signed promulgation form': 'promulgation',
  'Safety assessment if required': 'safety_assessment',
}

function parseConfidencePercent(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw)) return Math.min(100, Math.max(0, Math.round(raw)))
  const s = asString(raw).replace('%', '').trim()
  const n = Number(s)
  if (!Number.isFinite(n)) return 0
  return Math.min(100, Math.max(0, Math.round(n)))
}

function Of82({
  payload,
  locked,
  onChange,
  context,
}: Omit<Props, 'formCode'>) {
  const completeness = asArray<CompletenessRow>(payload.completeness).filter(
    (row) =>
      !/^cross[-\s]?references?\s+checked$/i.test(row.item) && !REMOVED_AIP_ITEMS.has(row.item)
  )
  const aixm = asArray<AixmRow>(payload.aixmChecklist)
  const aixmIncluded = asBool(payload.aixmIncluded)
  const column = completeness[0]?.column
  const aip = context ? isAipRecord(context.recordKind) : false

  const attachments = useQuery({
    queryKey: ['attachments', context?.ownerType, context?.ownerId],
    queryFn: () => attachmentsApi.listAttachments(context!.ownerType, context!.ownerId),
    enabled: Boolean(aip && context?.ownerType && context?.ownerId),
  })

  const attachedSlots = new Set(
    (attachments.data ?? []).map((a) => a.slot).filter((s): s is string => Boolean(s))
  )
  const hasPromulgation = !aip || attachedSlots.has('promulgation')
  const fieldsLocked = locked || (aip && !hasPromulgation)
  const canAttach = Boolean(context?.canEdit) && !locked

  function slotAttach(slot: AttachmentSlot) {
    if (!aip || !context) return null
    return (
      <InlineSlotAttach
        ownerType={context.ownerType}
        ownerId={context.ownerId}
        slot={slot}
        label={AIP_SLOT_LABELS[slot] ?? slot}
        canEdit={canAttach}
        hint={
          slot === 'safety_assessment'
            ? 'Attach if a safety assessment exists'
            : slot === 'regulator_approval'
              ? 'Attach if regulator approval is required'
              : slot === 'metadata_form'
                ? 'Marks effective dates and layout/format at 100%'
                : 'Not attached'
        }
      />
    )
  }

  useEffect(() => {
    if (!aip || locked) return
    const next = completeness.map((row) => {
      const slot = AIP_AUTO_ITEMS[row.item]
      if (slot && attachedSlots.has(slot)) {
        return row.value === '100%' ? row : { ...row, value: '100%' }
      }
      return row
    })
    const changed = next.some((row, i) => row.value !== completeness[i]?.value)
    if (changed) {
      onChange({ ...payload, completeness: next, confidenceManual: true })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- sync from attachment signals only
  }, [aip, locked, attachedSlots.size, attachments.dataUpdatedAt])

  const confidence = parseConfidencePercent(payload.confidenceLevel)

  function completenessDisabled(item: string) {
    if (fieldsLocked) return true
    if (!aip) return false
    const slot = AIP_AUTO_ITEMS[item]
    if (slot && attachedSlots.has(slot)) return true
    return false
  }

  return (
    <div className="space-y-5">
      {aip && !hasPromulgation ? (
        <div className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-950">
          Attach a <strong>signed promulgation form</strong> under Completeness before you can update
          this raw data quality check.
        </div>
      ) : null}

      <section className="space-y-3">
        <h3 className="font-display text-base font-semibold">Analysis of raw data</h3>
        <Field label="Findings">
          <TextArea
            value={asString(payload.findings)}
            disabled={fieldsLocked}
            onChange={(v) => set(payload, 'findings', v, onChange)}
          />
        </Field>
      </section>

      <section className="grid gap-3 compact:grid-cols-1 nav:grid-cols-2">
        <h3 className="font-display text-base font-semibold nav:col-span-2">Approval</h3>
        <Field label="Approver">
          <TextInput
            value={asString(payload.approver)}
            disabled={fieldsLocked}
            onChange={(v) => set(payload, 'approver', v, onChange)}
          />
        </Field>
        <Field label="Date of approval">
          <TextInput
            type="date"
            value={asString(payload.approvalDate)}
            disabled={fieldsLocked}
            onChange={(v) => set(payload, 'approvalDate', v, onChange)}
          />
        </Field>
      </section>

      <section className="space-y-3">
        <h3 className="font-display text-base font-semibold">Quality check</h3>
        {(
          [
            ['accuracy', 'accuracyComment', 'Accuracy'],
            ['resolution', 'resolutionComment', 'Resolution'],
            ['traceability', 'traceabilityComment', 'Traceability'],
            ['timeliness', 'timelinessComment', 'Timeliness'],
            ['format', 'formatComment', 'Format'],
          ] as const
        ).map(([key, commentKey, label]) => (
          <div
            key={key}
            className="flex flex-wrap items-center gap-3 rounded-md border border-slate-200 bg-white px-3 py-2"
          >
            <p className="w-28 shrink-0 text-sm font-semibold">{label}</p>
            <PillGroup
              value={asString(payload[key])}
              options={pctOptions}
              disabled={fieldsLocked}
              onChange={(v) => set(payload, key, v, onChange)}
            />
            <div className="min-w-[12rem] flex-1">
              <TextInput
                value={asString(payload[commentKey])}
                disabled={fieldsLocked}
                onChange={(v) => set(payload, commentKey, v, onChange)}
                placeholder="Comment"
              />
            </div>
          </div>
        ))}
        <div className="flex flex-wrap items-center gap-3 rounded-md border border-slate-200 bg-white px-3 py-2">
          <p className="w-28 shrink-0 text-sm font-semibold">Integrity</p>
          <PillGroup
            value={asString(payload.integrity)}
            options={integrityOptions}
            disabled={fieldsLocked}
            onChange={(v) => set(payload, 'integrity', v, onChange)}
          />
        </div>
        <div className="grid gap-3 sm:grid-cols-3">
          <Field label="Date received">
            <TextInput
              type="date"
              value={asString(payload.dateReceived)}
              disabled={fieldsLocked}
              onChange={(v) => set(payload, 'dateReceived', v, onChange)}
            />
          </Field>
          <Field label="Publication date">
            <TextInput
              type="date"
              value={asString(payload.publicationDate)}
              disabled={fieldsLocked}
              onChange={(v) => set(payload, 'publicationDate', v, onChange)}
            />
          </Field>
          <Field label="File type">
            <TextInput
              value={asString(payload.fileType)}
              disabled={fieldsLocked}
              onChange={(v) => set(payload, 'fileType', v, onChange)}
            />
          </Field>
        </div>
      </section>

      <section className="space-y-3">
        <h3 className="font-display text-base font-semibold">
          Completeness{column ? ` (${column})` : ''}
        </h3>
        {aip ? (
          <p className="text-xs text-muted">
            Attach the related form beside each check. One Meta data form marks both effective dates
            and layout/format. Attached files appear under Production files on the publication.
          </p>
        ) : null}
        {completeness.map((row, idx) => {
          const slot = AIP_AUTO_ITEMS[row.item] as AttachmentSlot | undefined
          // Meta data form attach shown once on Effective dates (also covers layout/format).
          const showInlineAttach =
            aip && slot && row.item !== 'Layout / format reviewed'
          return (
            <div key={row.item} className="rounded-md border border-slate-200 bg-white px-3 py-2">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="min-w-0">
                  <p className="text-sm font-medium">{row.item}</p>
                  {aip && row.item === 'Layout / format reviewed' ? (
                    <p className="text-[11px] text-muted">Uses the Meta data form attachment above</p>
                  ) : null}
                  {aip && completenessDisabled(row.item) && !fieldsLocked ? (
                    <p className="text-[11px] text-muted">Set automatically from attachment</p>
                  ) : null}
                </div>
                <PillGroup
                  value={row.value}
                  options={pctNaOptions}
                  disabled={completenessDisabled(row.item)}
                  onChange={(v) => {
                    const next = completeness.map((r, i) => (i === idx ? { ...r, value: v } : r))
                    set(payload, 'completeness', next, onChange)
                  }}
                />
              </div>
              {showInlineAttach && slot ? slotAttach(slot) : null}
            </div>
          )
        })}
      </section>

      {aip ? (
        <section className="space-y-2">
          <h3 className="font-display text-base font-semibold">Regulator approval</h3>
          <p className="text-xs text-muted">Attach only if regulator approval is required.</p>
          {slotAttach('regulator_approval')}
        </section>
      ) : null}

      <section className="space-y-3">
        <label className="flex items-center gap-2 text-sm font-semibold">
          <input
            type="checkbox"
            checked={aixmIncluded}
            disabled={fieldsLocked}
            onChange={(e) => set(payload, 'aixmIncluded', e.target.checked, onChange)}
          />
          An AIXM file is part of this raw data
        </label>
        {aixmIncluded ? (
          <div className="space-y-2">
            {aixm.map((row, idx) => (
              <div
                key={row.item}
                className="flex flex-wrap items-center justify-between gap-2 rounded-md border bg-white px-3 py-2"
              >
                <p className="text-sm">{row.item}</p>
                <PillGroup
                  value={row.value}
                  options={pctNaOptions}
                  disabled={fieldsLocked}
                  onChange={(v) => {
                    const next = aixm.map((r, i) => (i === idx ? { ...r, value: v } : r))
                    set(payload, 'aixmChecklist', next, onChange)
                  }}
                />
              </div>
            ))}
          </div>
        ) : null}
      </section>

      <section className="space-y-3 rounded-md border border-slate-200 bg-white px-4 py-3">
        <h3 className="font-display text-base font-semibold">Confidence level</h3>
        <div className="flex flex-wrap items-center gap-4">
          <input
            type="range"
            min={0}
            max={100}
            step={1}
            value={confidence}
            disabled={fieldsLocked}
            className="min-w-[12rem] flex-1 accent-brand"
            onChange={(e) =>
              onChange({
                ...payload,
                confidenceManual: true,
                confidenceLevel: `${e.target.value}%`,
              })
            }
          />
          <span className="font-mono text-sm font-semibold tabular-nums">{confidence}%</span>
        </div>
        <ConfidenceBanner payload={payload} />
      </section>
    </div>
  )
}

const of83bFields: MetaField[] = [
  { kind: 'field', key: 'personResponsible', item: 'Person responsible', input: 'text' },
  { kind: 'field', key: 'role', item: 'Role', input: 'text' },
  { kind: 'field', key: 'datePerformed', item: 'Date performed', input: 'date' },
  {
    kind: 'field',
    key: 'standardsApplied',
    item: 'Standards applied',
    description: 'e.g. ICAO Annex 15; Civil Aviation (ATS and AIS) Regulations',
    input: 'textarea',
  },
  {
    kind: 'field',
    key: 'effectiveDateTime',
    item: 'Effective date/time',
    description: 'As written, e.g. 2611260000',
    input: 'text',
  },
  {
    kind: 'field',
    key: 'endDateTime',
    item: 'End date/time',
    description: 'As written, e.g. 2612050000 or PERM',
    input: 'text',
  },
  { kind: 'field', key: 'schedule', item: 'Schedule', input: 'text' },
  { kind: 'section', title: 'For coordinate, distance, bearing and elevation data' },
  { kind: 'field', key: 'referenceSystem', item: 'Reference system', input: 'text' },
  { kind: 'field', key: 'methodUsed', item: 'Method used', input: 'text' },
  { kind: 'field', key: 'qualityAccuracy', item: 'Quality attributes — accuracy', input: 'text' },
  { kind: 'field', key: 'qualityResolution', item: 'Quality attributes — resolution', input: 'text' },
  { kind: 'section', title: 'Converted / transformed data' },
  { kind: 'field', key: 'conversionUsed', item: 'Conversion used', input: 'text' },
  { kind: 'section', title: 'Tools used' },
  { kind: 'field', key: 'tool', item: 'Tool', input: 'text' },
  { kind: 'field', key: 'toolVersion', item: 'Version', input: 'text' },
]

const of83aFields: MetaField[] = [
  {
    kind: 'field',
    key: 'adqCompliance',
    item: 'ADQ compliance',
    description: 'ADQ-IR regulation compliance',
    input: 'adq',
  },
  { kind: 'field', key: 'reviewDate', item: 'Review date', input: 'date' },
  { kind: 'field', key: 'personResponsible', item: 'Person responsible', input: 'text' },
  { kind: 'field', key: 'role', item: 'Role', input: 'text' },
  {
    kind: 'field',
    key: 'operation',
    item: 'Operation performed',
    description: 'New data, amended data, or withdrawn data',
    input: 'operation',
  },
  { kind: 'field', key: 'datePerformed', item: 'Date performed', input: 'date' },
  {
    kind: 'field',
    key: 'standardsApplied',
    item: 'Standards applied',
    input: 'textarea',
  },
  { kind: 'field', key: 'effectiveDate', item: 'Effective date', input: 'date' },
  {
    kind: 'field',
    key: 'effectiveTime',
    item: 'Effective time',
    description: 'e.g. 0000 UTC',
    input: 'text',
  },
  { kind: 'section', title: 'For coordinate, distance, bearing and elevation data' },
  { kind: 'field', key: 'referenceSystem', item: 'Reference system', input: 'textarea' },
  { kind: 'field', key: 'methodUsed', item: 'Method used', input: 'textarea' },
  { kind: 'field', key: 'qualityAccuracy', item: 'Quality attributes — accuracy', input: 'textarea' },
  { kind: 'field', key: 'qualityResolution', item: 'Quality attributes — resolution', input: 'textarea' },
  { kind: 'section', title: 'Converted / transformed data' },
  { kind: 'field', key: 'conversionUsed', item: 'Conversion used', input: 'textarea' },
  { kind: 'section', title: 'Tools used' },
  { kind: 'field', key: 'tool', item: 'Tool', input: 'textarea' },
  { kind: 'field', key: 'toolVersion', item: 'Version', input: 'textarea' },
]

function Of83b({ payload, locked, onChange }: Omit<Props, 'formCode'>) {
  return <MetaDataTable fields={of83bFields} payload={payload} locked={locked} onChange={onChange} />
}

function Of83a({ payload, locked, onChange }: Omit<Props, 'formCode'>) {
  return <MetaDataTable fields={of83aFields} payload={payload} locked={locked} onChange={onChange} />
}

function MetaDataTable({
  fields,
  payload,
  locked,
  onChange,
}: {
  fields: MetaField[]
  payload: Record<string, unknown>
  locked: boolean
  onChange: Props['onChange']
}) {
  return (
    <div className="overflow-x-auto rounded-md border border-slate-200">
      <table className="w-full min-w-[36rem] border-collapse text-sm">
        <thead>
          <tr className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-muted">
            <th className="border-b px-3 py-2 w-[28%]">Item</th>
            <th className="border-b px-3 py-2 w-[32%]">Description</th>
            <th className="border-b px-3 py-2">Value</th>
          </tr>
        </thead>
        <tbody>
          {fields.map((row) => {
            if (row.kind === 'section') {
              return (
                <tr key={row.title} className="bg-sky-50">
                  <td colSpan={3} className="px-3 py-2 text-xs font-semibold uppercase tracking-wide text-brand">
                    {row.title}
                  </td>
                </tr>
              )
            }
            return (
              <tr key={row.key} className="align-top border-b border-slate-100">
                <td className="px-3 py-2 font-semibold text-ink">{row.item}</td>
                <td className="px-3 py-2 text-xs text-muted">{row.description ?? ''}</td>
                <td className="px-3 py-2">
                  <MetaValueInput
                    input={row.input ?? 'text'}
                    value={asString(payload[row.key])}
                    locked={locked}
                    onChange={(v) => set(payload, row.key, v, onChange)}
                  />
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

function MetaValueInput({
  input,
  value,
  locked,
  onChange,
}: {
  input: NonNullable<Extract<MetaField, { kind: 'field' }>['input']>
  value: string
  locked: boolean
  onChange: (v: string) => void
}) {
  if (input === 'adq') {
    return <PillGroup value={value} options={yesNoOptions} disabled={locked} onChange={onChange} />
  }
  if (input === 'operation') {
    return (
      <PillGroup
        value={value}
        options={[
          { value: 'New', label: 'New', tone: 'ok' },
          { value: 'Amended', label: 'Amended', tone: 'neutral' },
          { value: 'Withdrawn', label: 'Withdrawn', tone: 'bad' },
        ]}
        disabled={locked}
        onChange={onChange}
      />
    )
  }
  if (input === 'textarea') {
    return <TextArea rows={2} value={value} disabled={locked} onChange={onChange} />
  }
  return <TextInput type={input === 'date' ? 'date' : 'text'} value={value} disabled={locked} onChange={onChange} />
}

function Of83c({ payload, locked, onChange }: Omit<Props, 'formCode'>) {
  const systems = asArray<SystemGroup>(payload.systems)
  const part1 = asArray<Part1Row>(payload.part1)

  return (
    <div className="space-y-6">
      <section className="space-y-3">
        <h3 className="font-display text-base font-semibold">Processing steps by system</h3>
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {systems.map((system, sIdx) => (
            <div key={system.name} className="rounded-md border bg-white p-3">
              <p className="text-sm font-semibold text-ink">{system.name}</p>
              <ul className="mt-2 space-y-1.5">
                {system.steps.map((step, stepIdx) => {
                  const checked = step.done === 'Yes'
                  return (
                    <li key={step.label}>
                      <label className="flex items-start gap-2 text-sm">
                        <input
                          type="checkbox"
                          className="mt-0.5"
                          checked={checked}
                          disabled={locked}
                          onChange={(e) => {
                            const next = systems.map((s, i) =>
                              i === sIdx
                                ? {
                                    ...s,
                                    steps: s.steps.map((st, j) =>
                                      j === stepIdx ? { ...st, done: e.target.checked ? 'Yes' : '' } : st
                                    ),
                                  }
                                : s
                            )
                            set(payload, 'systems', next, onChange)
                          }}
                        />
                        <span>{step.label}</span>
                      </label>
                    </li>
                  )
                })}
              </ul>
            </div>
          ))}
        </div>
      </section>

      <section className="space-y-3">
        <h3 className="font-display text-base font-semibold">Part I</h3>
        {part1.map((row, idx) => (
          <div key={row.label} className="space-y-2 rounded-md border bg-white p-3">
            <p className="text-sm font-semibold">{row.label}</p>
            <Field label="Description / AIP page and section">
              <TextInput
                value={row.description}
                disabled={locked}
                onChange={(v) => {
                  const next = part1.map((r, i) => (i === idx ? { ...r, description: v } : r))
                  set(payload, 'part1', next, onChange)
                }}
              />
            </Field>
            <Field label="Successfully completed">
              <PillGroup
                value={row.completed}
                options={yesNoOptions}
                disabled={locked}
                onChange={(v) => {
                  const next = part1.map((r, i) => (i === idx ? { ...r, completed: v } : r))
                  set(payload, 'part1', next, onChange)
                }}
              />
            </Field>
            <div className="grid gap-3 sm:grid-cols-3">
              <Field label="Remarks">
                <TextInput
                  value={row.remarks}
                  disabled={locked}
                  onChange={(v) => {
                    const next = part1.map((r, i) => (i === idx ? { ...r, remarks: v } : r))
                    set(payload, 'part1', next, onChange)
                  }}
                />
              </Field>
              <Field label="Date">
                <TextInput
                  type="date"
                  value={row.date}
                  disabled={locked}
                  onChange={(v) => {
                    const next = part1.map((r, i) => (i === idx ? { ...r, date: v } : r))
                    set(payload, 'part1', next, onChange)
                  }}
                />
              </Field>
              <Field label="By">
                <TextInput
                  value={row.by}
                  disabled={locked}
                  onChange={(v) => {
                    const next = part1.map((r, i) => (i === idx ? { ...r, by: v } : r))
                    set(payload, 'part1', next, onChange)
                  }}
                />
              </Field>
            </div>
          </div>
        ))}
      </section>
    </div>
  )
}

function Of83d({ payload, locked, onChange }: Omit<Props, 'formCode'>) {
  const completeness = asArray<CompletenessRow>(payload.completeness)
  const column = completeness[0]?.column
  return (
    <div className="space-y-5">
      <section className="grid gap-3 sm:grid-cols-3">
        <h3 className="font-display text-base font-semibold sm:col-span-3">Approval</h3>
        <Field label="Affected section">
          <TextInput value={asString(payload.affectedSection)} disabled={locked} onChange={(v) => set(payload, 'affectedSection', v, onChange)} />
        </Field>
        <Field label="Approver">
          <TextInput value={asString(payload.approver)} disabled={locked} onChange={(v) => set(payload, 'approver', v, onChange)} />
        </Field>
        <Field label="Date of approval">
          <TextInput type="date" value={asString(payload.approvalDate)} disabled={locked} onChange={(v) => set(payload, 'approvalDate', v, onChange)} />
        </Field>
      </section>

      <section className="space-y-3">
        <h3 className="font-display text-base font-semibold">Quality check</h3>
        {(
          [
            ['accuracy', 'Accuracy'],
            ['resolution', 'Resolution'],
            ['traceability', 'Traceability'],
            ['timeliness', 'Timeliness'],
            ['format', 'Format'],
          ] as const
        ).map(([key, label]) => (
          <Field key={key} label={label}>
            <PillGroup
              value={asString(payload[key])}
              options={pctOptions}
              disabled={locked}
              onChange={(v) => set(payload, key, v, onChange)}
            />
          </Field>
        ))}
        <Field label="Integrity">
          <PillGroup
            value={asString(payload.integrity)}
            options={integrityOptions}
            disabled={locked}
            onChange={(v) => set(payload, 'integrity', v, onChange)}
          />
        </Field>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <Field label="Final draft review date">
            <TextInput type="date" value={asString(payload.finalDraftReviewDate)} disabled={locked} onChange={(v) => set(payload, 'finalDraftReviewDate', v, onChange)} />
          </Field>
          <Field label="Publication date">
            <TextInput type="date" value={asString(payload.publicationDate)} disabled={locked} onChange={(v) => set(payload, 'publicationDate', v, onChange)} />
          </Field>
          <Field label="File type">
            <TextInput value={asString(payload.fileType)} disabled={locked} onChange={(v) => set(payload, 'fileType', v, onChange)} />
          </Field>
          <Field label="Applicable regulation(s)">
            <TextInput value={asString(payload.applicableRegulations)} disabled={locked} onChange={(v) => set(payload, 'applicableRegulations', v, onChange)} />
          </Field>
        </div>
      </section>

      <section className="space-y-3">
        <h3 className="font-display text-base font-semibold">
          Completeness{column ? ` (${column})` : ''}
        </h3>
        {completeness.map((row, idx) => (
          <div key={row.item} className="flex flex-wrap items-center justify-between gap-2 rounded-md border bg-white px-3 py-2">
            <p className="text-sm font-medium">{row.item}</p>
            <PillGroup
              value={row.value}
              options={pctNaOptions}
              disabled={locked}
              onChange={(v) => {
                const next = completeness.map((r, i) => (i === idx ? { ...r, value: v } : r))
                set(payload, 'completeness', next, onChange)
              }}
            />
          </div>
        ))}
      </section>

      <ConfidenceBanner payload={payload} />
    </div>
  )
}

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

type Props = {
  formCode: string
  payload: Record<string, unknown>
  locked: boolean
  onChange: (next: Record<string, unknown>) => void
}

type CompletenessRow = { item: string; column?: string; value: string }
type AixmRow = { item: string; value: string }
type MetaRow = { label: string; value: string }
type SystemGroup = { name: string; steps: { label: string; done: string }[] }
type Part1Row = {
  label: string
  description: string
  completed: string
  remarks: string
  date: string
  by: string
}

export function FormEditor({ formCode, payload, locked, onChange }: Props) {
  if (formCode.endsWith('/OF/82')) {
    return <Of82 payload={payload} locked={locked} onChange={onChange} />
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

function Of82({ payload, locked, onChange }: Omit<Props, 'formCode'>) {
  const completeness = asArray<CompletenessRow>(payload.completeness)
  const aixm = asArray<AixmRow>(payload.aixmChecklist)
  const aixmIncluded = asBool(payload.aixmIncluded)

  return (
    <div className="space-y-5">
      <section className="space-y-3">
        <h3 className="font-display text-base font-semibold">Analysis of raw data</h3>
        <Field label="Findings">
          <TextArea value={asString(payload.findings)} disabled={locked} onChange={(v) => set(payload, 'findings', v, onChange)} />
        </Field>
      </section>

      <section className="grid gap-3 sm:grid-cols-2">
        <h3 className="font-display text-base font-semibold sm:col-span-2">Approval</h3>
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
        <div className="grid gap-3 sm:grid-cols-3">
          <Field label="Date received">
            <TextInput type="date" value={asString(payload.dateReceived)} disabled={locked} onChange={(v) => set(payload, 'dateReceived', v, onChange)} />
          </Field>
          <Field label="Publication date">
            <TextInput type="date" value={asString(payload.publicationDate)} disabled={locked} onChange={(v) => set(payload, 'publicationDate', v, onChange)} />
          </Field>
          <Field label="File type">
            <TextInput value={asString(payload.fileType)} disabled={locked} onChange={(v) => set(payload, 'fileType', v, onChange)} />
          </Field>
        </div>
      </section>

      <section className="space-y-3">
        <h3 className="font-display text-base font-semibold">Completeness</h3>
        {completeness.map((row, idx) => (
          <div key={row.item} className="flex flex-wrap items-center justify-between gap-2 rounded-md border bg-white px-3 py-2">
            <div>
              <p className="text-sm font-medium">{row.item}</p>
              <p className="text-xs text-muted">{row.column}</p>
            </div>
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

      <section className="space-y-3">
        <label className="flex items-center gap-2 text-sm font-semibold">
          <input
            type="checkbox"
            checked={aixmIncluded}
            disabled={locked}
            onChange={(e) => set(payload, 'aixmIncluded', e.target.checked, onChange)}
          />
          An AIXM file is part of this raw data
        </label>
        {aixmIncluded ? (
          <div className="space-y-2">
            {aixm.map((row, idx) => (
              <div key={row.item} className="flex flex-wrap items-center justify-between gap-2 rounded-md border bg-white px-3 py-2">
                <p className="text-sm">{row.item}</p>
                <PillGroup
                  value={row.value}
                  options={yesNoOptions}
                  disabled={locked}
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
    </div>
  )
}

function Of83b({ payload, locked, onChange }: Omit<Props, 'formCode'>) {
  const rows = asArray<MetaRow>(payload.rows)
  return (
    <div className="space-y-3">
      {rows.map((row, idx) => (
        <Field key={row.label} label={row.label}>
          <TextArea
            rows={2}
            value={row.value}
            disabled={locked}
            onChange={(v) => {
              const next = rows.map((r, i) => (i === idx ? { ...r, value: v } : r))
              set(payload, 'rows', next, onChange)
            }}
          />
        </Field>
      ))}
    </div>
  )
}

function Of83a({ payload, locked, onChange }: Omit<Props, 'formCode'>) {
  return (
    <div className="space-y-4">
      <Field label="ADQ compliance">
        <PillGroup
          value={asString(payload.adqCompliance)}
          options={yesNoOptions}
          disabled={locked}
          onChange={(v) => set(payload, 'adqCompliance', v, onChange)}
        />
      </Field>
      <Field label="Review date">
        <TextInput type="date" value={asString(payload.reviewDate)} disabled={locked} onChange={(v) => set(payload, 'reviewDate', v, onChange)} />
      </Field>
      <Field label="Operation performed">
        <PillGroup
          value={asString(payload.operation)}
          options={[
            { value: 'New', label: 'New', tone: 'ok' },
            { value: 'Amended', label: 'Amended', tone: 'neutral' },
            { value: 'Withdrawn data', label: 'Withdrawn data', tone: 'bad' },
          ]}
          disabled={locked}
          onChange={(v) => set(payload, 'operation', v, onChange)}
        />
      </Field>
      <div className="grid gap-3 sm:grid-cols-2">
        <Field label="Effective date">
          <TextInput type="date" value={asString(payload.effectiveDate)} disabled={locked} onChange={(v) => set(payload, 'effectiveDate', v, onChange)} />
        </Field>
        <Field label="Effective time">
          <TextInput value={asString(payload.effectiveTime)} disabled={locked} onChange={(v) => set(payload, 'effectiveTime', v, onChange)} />
        </Field>
      </div>
      <Field label="Notes">
        <TextArea value={asString(payload.notes)} disabled={locked} onChange={(v) => set(payload, 'notes', v, onChange)} />
      </Field>
    </div>
  )
}

function Of83c({ payload, locked, onChange }: Omit<Props, 'formCode'>) {
  const systems = asArray<SystemGroup>(payload.systems)
  const part1 = asArray<Part1Row>(payload.part1)

  return (
    <div className="space-y-6">
      <section className="space-y-3">
        <h3 className="font-display text-base font-semibold">Systems</h3>
        {systems.map((system, sIdx) => (
          <div key={system.name} className="rounded-md border bg-white p-3">
            <p className="text-sm font-semibold">{system.name}</p>
            <div className="mt-2 space-y-2">
              {system.steps.map((step, stepIdx) => (
                <div key={step.label} className="flex flex-wrap items-center justify-between gap-2">
                  <span className="text-sm">{step.label}</span>
                  <PillGroup
                    value={step.done}
                    options={yesNoOptions}
                    disabled={locked}
                    onChange={(v) => {
                      const next = systems.map((s, i) =>
                        i === sIdx
                          ? {
                              ...s,
                              steps: s.steps.map((st, j) => (j === stepIdx ? { ...st, done: v } : st)),
                            }
                          : s
                      )
                      set(payload, 'systems', next, onChange)
                    }}
                  />
                </div>
              ))}
            </div>
          </div>
        ))}
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
        <div className="grid gap-3 sm:grid-cols-3">
          <Field label="Final draft review date">
            <TextInput type="date" value={asString(payload.finalDraftReviewDate)} disabled={locked} onChange={(v) => set(payload, 'finalDraftReviewDate', v, onChange)} />
          </Field>
          <Field label="Publication date">
            <TextInput type="date" value={asString(payload.publicationDate)} disabled={locked} onChange={(v) => set(payload, 'publicationDate', v, onChange)} />
          </Field>
          <Field label="Applicable regulation(s)">
            <TextInput value={asString(payload.applicableRegulations)} disabled={locked} onChange={(v) => set(payload, 'applicableRegulations', v, onChange)} />
          </Field>
        </div>
      </section>

      <section className="space-y-3">
        <h3 className="font-display text-base font-semibold">Completeness</h3>
        {completeness.map((row, idx) => (
          <div key={row.item} className="flex flex-wrap items-center justify-between gap-2 rounded-md border bg-white px-3 py-2">
            <div>
              <p className="text-sm font-medium">{row.item}</p>
              <p className="text-xs text-muted">{row.column}</p>
            </div>
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

      <Field label="Confidence level">
        <TextInput value={asString(payload.confidenceLevel)} disabled={locked} onChange={(v) => set(payload, 'confidenceLevel', v, onChange)} />
      </Field>
    </div>
  )
}

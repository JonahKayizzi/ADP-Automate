export type ConfidenceResult = {
  percent: number
  scored100: number
  scoredTotal: number
  naOrBlank: number
}

function normalizeScore(value: unknown): '100%' | '0%' | null {
  if (typeof value !== 'string' || !value.trim()) return null
  const trimmed = value.trim()
  if (/^n\/?a$/i.test(trimmed)) return null
  if (trimmed === '100%' || /^yes$/i.test(trimmed)) return '100%'
  if (trimmed === '0%' || /^no$/i.test(trimmed)) return '0%'
  return null
}

/** Live confidence for OF/82 and OF/83d payloads. */
export function calculateConfidence(payload: Record<string, unknown>): ConfidenceResult {
  const scores: unknown[] = [
    payload.accuracy,
    payload.resolution,
    payload.traceability,
    payload.timeliness,
    payload.format,
  ]

  if (Array.isArray(payload.completeness)) {
    for (const row of payload.completeness) {
      if (row && typeof row === 'object' && 'value' in row) {
        scores.push((row as { value: unknown }).value)
      }
    }
  }

  if (payload.aixmIncluded === true && Array.isArray(payload.aixmChecklist)) {
    for (const row of payload.aixmChecklist) {
      if (row && typeof row === 'object' && 'value' in row) {
        scores.push((row as { value: unknown }).value)
      }
    }
  }

  let scored100 = 0
  let scoredTotal = 0
  let naOrBlank = 0
  for (const raw of scores) {
    const normalized = normalizeScore(raw)
    if (normalized == null) {
      naOrBlank++
      continue
    }
    scoredTotal++
    if (normalized === '100%') scored100++
  }

  const percent = scoredTotal === 0 ? 0 : Math.round((scored100 * 100) / scoredTotal)
  return { percent, scored100, scoredTotal, naOrBlank }
}

export type Publication = {
  id: string
  type: string
  number: string
  title: string
  effective: string
  status: string
  airac: boolean
  airacIdent?: string | null
  distributeBy?: string | null
  daysToDeadline?: number | null
  effectiveIsAiracDate: boolean
  example: boolean
  createdAt: string
  createdByAccount?: string | null
  updatedAt: string
}

export type UpsertPublication = {
  type: string
  number: string
  title: string
  effective: string
  status: string
}

export type AiracOption = {
  ident: string
  effective: string
  distributeBy: string
  daysToDeadline: number
  nextDefault: boolean
}

export type PublicationFilter = 'ALL' | 'AIRAC' | 'NON_AIRAC' | 'AIC'

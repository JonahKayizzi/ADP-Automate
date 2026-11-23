export type ProcessingOwnerType = 'notam' | 'publication'

export type FormSummary = {
  id: string
  stepNumber: number
  formCode: string
  formTitle: string
  status: string
  signedCount: number
  requiredSignatures: number
}

export type ProcessingFile = {
  id: string
  ownerType: ProcessingOwnerType
  ownerId: string
  recordKind: string
  signedOffCount: number
  totalForms: number
  forms: FormSummary[]
}

export type SignatureSlot = {
  slotKey: string
  slotLabel: string
  signerName?: string | null
  designation?: string | null
  signedAt?: string | null
  signedByAccount?: string | null
  signed: boolean
}

export type ProcessingFormDetail = {
  id: string
  stepNumber: number
  formCode: string
  formTitle: string
  controlledNotice: string
  status: string
  locked: boolean
  payload: Record<string, unknown>
  signatures: SignatureSlot[]
  updatedAt: string
}

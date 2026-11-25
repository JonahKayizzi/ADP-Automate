export type AttachmentOwnerType = 'notam' | 'publication' | 'library'

export type AttachmentSlot =
  | 'supporting'
  | 'product'
  | 'publication'
  | 'promulgation'
  | 'data_collection'
  | 'coordinate_verification'
  | 'metadata_form'
  | 'safety_assessment'
  | 'regulator_approval'

export type Attachment = {
  id: string
  ownerType: AttachmentOwnerType
  ownerId: string
  slot?: AttachmentSlot | string | null
  originalFilename: string
  contentType: string
  sizeBytes: number
  sha256: string
  createdAt: string
  createdByAccount?: string | null
}

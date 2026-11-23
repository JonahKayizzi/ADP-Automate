export type AttachmentOwnerType = 'notam' | 'publication' | 'library'

export type Attachment = {
  id: string
  ownerType: AttachmentOwnerType
  ownerId: string
  originalFilename: string
  contentType: string
  sizeBytes: number
  sha256: string
  createdAt: string
  createdByAccount?: string | null
}

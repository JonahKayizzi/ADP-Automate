export type LibraryDocument = {
  id: string
  docType: string
  title: string
  edition?: string | null
  example: boolean
  createdAt: string
  createdByAccount?: string | null
  updatedAt: string
}

export type UpsertLibraryDocument = {
  docType: string
  title: string
  edition?: string
}

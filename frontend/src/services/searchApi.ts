import { api } from './api'

export type SearchHit = {
  id: string
  type: string
  title: string
  subtitle?: string | null
  href: string
}

export type SearchResponse = {
  query: string
  groups: {
    notams: SearchHit[]
    publications: SearchHit[]
    library: SearchHit[]
    complaints: SearchHit[]
    tasks: SearchHit[]
  }
}

export function searchAll(q: string) {
  const params = new URLSearchParams({ q })
  return api<SearchResponse>(`/search?${params}`)
}

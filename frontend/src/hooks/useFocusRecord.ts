import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'

/** Reads ?focus=<id>, highlights that card briefly, then clears the query param. */
export function useFocusRecord() {
  const [params, setParams] = useSearchParams()
  const focusId = params.get('focus')
  const [activeId, setActiveId] = useState<string | null>(focusId)

  useEffect(() => {
    if (!focusId) {
      setActiveId(null)
      return
    }
    setActiveId(focusId)
    const el = document.getElementById(`record-${focusId}`)
    el?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    const clearParam = window.setTimeout(() => {
      const next = new URLSearchParams(window.location.search)
      next.delete('focus')
      setParams(next, { replace: true })
    }, 500)
    const clearHighlight = window.setTimeout(() => setActiveId(null), 4000)
    return () => {
      window.clearTimeout(clearParam)
      window.clearTimeout(clearHighlight)
    }
  }, [focusId, setParams])

  return activeId
}

export function focusClass(activeId: string | null, id: string) {
  return activeId === id ? 'ring-2 ring-brand ring-offset-2' : ''
}

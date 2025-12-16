import { useState, useEffect } from 'react'
import { QueryDefinition } from '../types'

export function useQueries() {
  const [queries, setQueries] = useState<QueryDefinition[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetch('/api/patch/queries')
      .then(res => {
        if (!res.ok) {
          throw new Error(`HTTP error! status: ${res.status}`)
        }
        return res.json()
      })
      .then(data => {
        setQueries(data)
        setLoading(false)
      })
      .catch(err => {
        console.error('Error loading queries:', err)
        setError(err.message)
        setLoading(false)
      })
  }, [])

  return { queries, loading, error }
}


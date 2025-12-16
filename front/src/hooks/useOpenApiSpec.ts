import { useState, useEffect } from 'react'
import { OpenApiSpec } from '../types'

export function useOpenApiSpec(group: string = 'sql-generator') {
  const [spec, setSpec] = useState<OpenApiSpec | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const url = group === 'default' 
      ? '/v3/api-docs' 
      : `/v3/api-docs/${group}`
    
    fetch(url)
      .then(res => {
        if (!res.ok) {
          throw new Error(`HTTP error! status: ${res.status}`)
        }
        return res.json()
      })
      .then(data => {
        setSpec(data)
        setLoading(false)
      })
      .catch(err => {
        console.error('Error loading OpenAPI spec:', err)
        setError(err.message)
        setLoading(false)
      })
  }, [group])

  return { spec, loading, error }
}


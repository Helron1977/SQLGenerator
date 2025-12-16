import { useState, FormEvent } from 'react'
import { QueryDefinition, OpenApiSpec } from '../types'
import './PatchForm.css'

interface PatchFormProps {
  query: QueryDefinition
  openApiSpec: OpenApiSpec | null
}

export default function PatchForm({ query, openApiSpec }: PatchFormProps) {
  const [formData, setFormData] = useState<Record<string, string | File>>({})
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [mode, setMode] = useState<'unitaire' | 'masse'>('unitaire')

  // Récupérer le schéma OpenAPI pour ce endpoint
  const endpointPath = `/api/patch/${query.id}`
  const masseEndpointPath = `/api/patch/${query.id}/masse`
  const operation = openApiSpec?.paths?.[endpointPath]?.post
  const masseOperation = openApiSpec?.paths?.[masseEndpointPath]?.post
  
  const schema = operation?.requestBody?.content?.['application/x-www-form-urlencoded']?.schema ||
                 operation?.requestBody?.content?.['multipart/form-data']?.schema

  const hasInParameter = query.parameters?.some(p => p.isFile) || false
  const canUseMasseMode = !hasInParameter

  const handleInputChange = (name: string, value: string | File) => {
    setFormData(prev => ({ ...prev, [name]: value }))
    setError(null)
  }

  const handleFileChange = (name: string, file: File | null) => {
    if (file) {
      handleInputChange(name, file)
    }
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)

    try {
      const formDataToSend = new FormData()
      
      // Ajouter les données du formulaire
      Object.entries(formData).forEach(([key, value]) => {
        if (value instanceof File) {
          formDataToSend.append(key, value)
        } else {
          formDataToSend.append(key, value || '')
        }
      })

      // Ajouter le ticket si manquant
      if (!formDataToSend.has('ticket')) {
        formDataToSend.append('ticket', `ticket-${Date.now()}`)
      }

      // Choisir l'endpoint selon le mode
      const endpoint = mode === 'masse' ? masseEndpointPath : endpointPath
      
      const response = await fetch(endpoint, {
        method: 'POST',
        body: formDataToSend
      })

      if (!response.ok) {
        throw new Error(`Erreur ${response.status}: ${response.statusText}`)
      }

      // Télécharger le fichier
      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${query.id}_${Date.now()}.sql`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      window.URL.revokeObjectURL(url)

      // Réinitialiser le formulaire
      setFormData({})
      setMode('unitaire')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Une erreur est survenue')
    } finally {
      setLoading(false)
    }
  }

  const renderField = (name: string, prop: any) => {
    const isRequired = schema?.required?.includes(name) || false
    const value = formData[name] as string || ''
    const fieldType = prop.type || 'string'
    const format = prop.format

    if (format === 'binary' || name === 'masseFile') {
      return (
        <div key={name} className="form-field">
          <label htmlFor={name}>
            {prop.description || name}
            {isRequired && <span className="required">*</span>}
          </label>
          <input
            type="file"
            id={name}
            name={name}
            required={isRequired}
            onChange={(e) => handleFileChange(name, e.target.files?.[0] || null)}
            accept={name === 'masseFile' ? '.csv' : undefined}
          />
          {name === 'masseFile' && (
            <small className="field-help">
              Fichier CSV : une ligne par requête, valeurs séparées par virgule
            </small>
          )}
        </div>
      )
    }

    if (prop.enum && prop.enum.length > 0) {
      return (
        <div key={name} className="form-field">
          <label htmlFor={name}>
            {prop.description || name}
            {isRequired && <span className="required">*</span>}
          </label>
          <select
            id={name}
            name={name}
            value={value}
            required={isRequired}
            onChange={(e) => handleInputChange(name, e.target.value)}
          >
            {prop.enum.map((val: string) => (
              <option key={val} value={val}>{val}</option>
            ))}
          </select>
        </div>
      )
    }

    return (
      <div key={name} className="form-field">
        <label htmlFor={name}>
          {prop.description || name}
          {isRequired && <span className="required">*</span>}
        </label>
        <input
          type={fieldType === 'integer' || fieldType === 'number' ? 'number' : 'text'}
          id={name}
          name={name}
          value={value}
          required={isRequired}
          onChange={(e) => handleInputChange(name, e.target.value)}
          placeholder={prop.example || ''}
        />
        {prop.example && (
          <small className="field-help">Exemple : {prop.example}</small>
        )}
      </div>
    )
  }

  return (
    <div className="patch-form-container">
      <div className="form-header">
        <h2>{query.name || query.id}</h2>
        {query.description && (
          <p className="form-description">{query.description}</p>
        )}
      </div>

      {canUseMasseMode && (
        <div className="mode-selector">
          <label>
            <input
              type="radio"
              name="mode"
              value="unitaire"
              checked={mode === 'unitaire'}
              onChange={() => setMode('unitaire')}
            />
            Mode Unitaire
          </label>
          <label>
            <input
              type="radio"
              name="mode"
              value="masse"
              checked={mode === 'masse'}
              onChange={() => setMode('masse')}
            />
            Mode Masse
          </label>
        </div>
      )}

      <form onSubmit={handleSubmit} className="patch-form">
        {mode === 'masse' ? (
          // Mode masse : seulement ticket + fichier CSV
          schema?.properties && (
            <>
              {renderField('ticket', schema.properties.ticket || {})}
              {renderField('masseFile', schema.properties.masseFile || {})}
            </>
          )
        ) : (
          // Mode unitaire : tous les champs
          schema?.properties && Object.entries(schema.properties)
            .filter(([key]) => key !== 'masseFile') // Exclure masseFile en mode unitaire
            .map(([name, prop]) => renderField(name, prop))
        )}

        {error && (
          <div className="error-message">
            ❌ {error}
          </div>
        )}

        <div className="form-actions">
          <button type="submit" disabled={loading} className="submit-button">
            {loading ? '⏳ Génération...' : '🚀 Générer le patch SQL'}
          </button>
        </div>
      </form>
    </div>
  )
}


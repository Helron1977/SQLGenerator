import { useState, useEffect } from 'react'
import { QueryDefinition } from './types'
import { useQueries, useOpenApiSpec } from './hooks'
import QueryList from './components/QueryList'
import PatchForm from './components/PatchForm'
import './App.css'

function App() {
  const [selectedQuery, setSelectedQuery] = useState<QueryDefinition | null>(null)
  const { queries, loading: queriesLoading } = useQueries()
  const { spec, loading: specLoading } = useOpenApiSpec('sql-generator')

  if (queriesLoading || specLoading) {
    return (
      <div className="app-container">
        <div className="loading">Chargement...</div>
      </div>
    )
  }

  return (
    <div className="app-container">
      <header className="app-header">
        <h1>🔧 SQL Patch Generator</h1>
        <p>Générez des patches SQL personnalisés à partir de templates</p>
      </header>

      <div className="app-content">
        <aside className="sidebar">
          <QueryList
            queries={queries}
            selectedQuery={selectedQuery}
            onSelectQuery={setSelectedQuery}
          />
        </aside>

        <main className="main-content">
          {selectedQuery ? (
            <PatchForm
              query={selectedQuery}
              openApiSpec={spec}
            />
          ) : (
            <div className="empty-state">
              <p>👈 Sélectionnez une requête dans la liste pour commencer</p>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}

export default App


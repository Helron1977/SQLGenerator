import { QueryDefinition } from '../types'
import './QueryList.css'

interface QueryListProps {
  queries: QueryDefinition[]
  selectedQuery: QueryDefinition | null
  onSelectQuery: (query: QueryDefinition) => void
}

export default function QueryList({ queries, selectedQuery, onSelectQuery }: QueryListProps) {
  if (queries.length === 0) {
    return (
      <div className="query-list">
        <h2>Requêtes disponibles</h2>
        <p className="empty-message">Aucune requête disponible</p>
      </div>
    )
  }

  return (
    <div className="query-list">
      <h2>Requêtes disponibles ({queries.length})</h2>
      <div className="query-items">
        {queries.map(query => (
          <div
            key={query.id}
            className={`query-item ${selectedQuery?.id === query.id ? 'selected' : ''}`}
            onClick={() => onSelectQuery(query)}
          >
            <div className="query-item-header">
              <h3>{query.name || query.id}</h3>
              {query.tags && query.tags.length > 0 && (
                <div className="query-tags">
                  {query.tags.map(tag => (
                    <span key={tag} className="tag">{tag}</span>
                  ))}
                </div>
              )}
            </div>
            {query.description && (
              <p className="query-description">{query.description}</p>
            )}
            <div className="query-meta">
              <span className="query-params-count">
                {query.parameters?.length || 0} paramètre(s)
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}


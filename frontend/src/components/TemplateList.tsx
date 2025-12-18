import { useEffect, useState } from 'react';
import { fetchAllFormSchemas, type FormSchema } from '../services/api';
import './TemplateList.css';

interface TemplateListProps {
  selectedTemplateId: string | null;
  onTemplateSelect: (templateId: string) => void;
}

/**
 * Composant de liste des templates (colonne gauche 1/3)
 * 
 * Affiche la liste des formulaires disponibles avec recherche et filtres.
 */
export function TemplateList({ selectedTemplateId, onTemplateSelect }: TemplateListProps) {
  const [templates, setTemplates] = useState<FormSchema[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    loadTemplates();
  }, []);

  const loadTemplates = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await fetchAllFormSchemas();
      setTemplates(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur inconnue');
    } finally {
      setLoading(false);
    }
  };

  const filteredTemplates = templates.filter(template =>
    template.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    template.templateId.toLowerCase().includes(searchTerm.toLowerCase()) ||
    template.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    template.tags?.some(tag => tag.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  if (loading) {
    return (
      <div className="template-list">
        <div className="template-list-loading">Chargement...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="template-list">
        <div className="template-list-error">
          <p>Erreur: {error}</p>
          <button onClick={loadTemplates}>Réessayer</button>
        </div>
      </div>
    );
  }

  return (
    <div className="template-list">
      <div className="template-list-header">
        <h2>Templates SQL</h2>
        <input
          type="text"
          placeholder="Rechercher..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="template-list-search"
        />
      </div>
      <div className="template-list-content">
        {filteredTemplates.length === 0 ? (
          <div className="template-list-empty">
            {searchTerm ? 'Aucun template trouvé' : 'Aucun template disponible'}
          </div>
        ) : (
          filteredTemplates.map((template) => (
            <div
              key={template.templateId}
              className={`template-item ${selectedTemplateId === template.templateId ? 'selected' : ''}`}
              onClick={() => onTemplateSelect(template.templateId)}
            >
              <div className="template-item-name">{template.name}</div>
              {template.description && (
                <div className="template-item-description">{template.description}</div>
              )}
              <div className="template-item-meta">
                <span className="template-item-id">{template.templateId}</span>
                {Array.isArray(template.modes) && template.modes.includes('masse') && (
                  <span className="template-item-badge">Masse</span>
                )}
                {template.hasInParameter && (
                  <span className="template-item-badge">IN</span>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}


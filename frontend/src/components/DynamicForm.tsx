import { useState, useEffect } from 'react';
import { fetchFormSchema, generateScriptUnitaire, generateScriptMasse, type FormSchema } from '../services/api';
import './DynamicForm.css';

interface DynamicFormProps {
  templateId: string | null;
}

/**
 * Composant de formulaire dynamique (zone droite 2/3)
 * 
 * Génère automatiquement un formulaire basé sur le FormSchema du backend.
 * Gère les modes unitaire et masse.
 */
export function DynamicForm({ templateId }: DynamicFormProps) {
  const [schema, setSchema] = useState<FormSchema | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formData, setFormData] = useState<Record<string, string | File>>({});
  const [mode, setMode] = useState<'unitaire' | 'masse'>('unitaire');
  const [masseFile, setMasseFile] = useState<File | null>(null);
  const [generating, setGenerating] = useState(false);

  useEffect(() => {
    if (templateId) {
      loadSchema(templateId);
      // Réinitialiser le formulaire
      setFormData({});
      setMasseFile(null);
      setMode('unitaire');
    } else {
      setSchema(null);
    }
  }, [templateId]);

  const loadSchema = async (id: string) => {
    try {
      setLoading(true);
      setError(null);
      const data = await fetchFormSchema(id);
      setSchema(data);
      // Initialiser les valeurs par défaut à partir des champs unitaires (hors champs techniques)
      const initialData: Record<string, string> = {};
      (data.unitFields || []).forEach(field => {
        if (field.technical) {
          return; // Champs techniques : non affichés, gérés par le composant
        }
        if (field.example) {
          initialData[field.name] = field.example;
        }
      });
      setFormData(initialData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur inconnue');
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (name: string, value: string) => {
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleFileChange = (name: string, file: File | null) => {
    if (file) {
      setFormData(prev => ({ ...prev, [name]: file }));
    } else {
      const newData = { ...formData };
      delete newData[name];
      setFormData(newData);
    }
  };

  const handleMasseFileChange = (file: File | null) => {
    setMasseFile(file);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!templateId || !schema) return;

    try {
      setGenerating(true);
      setError(null);

      let blob: Blob;
      if (mode === 'masse') {
        if (!masseFile) {
          setError('Veuillez sélectionner un fichier CSV pour le mode masse');
          return;
        }
        blob = await generateScriptMasse(templateId, masseFile);
      } else {
        const params = { ...formData };
        // Forcer le paramètre technique executionType en fonction du mode choisi
        params['executionType'] = mode;
        blob = await generateScriptUnitaire(templateId, params);
      }

      // Télécharger le fichier généré
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${templateId}_${mode}_${new Date().getTime()}.sql`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur lors de la génération');
    } finally {
      setGenerating(false);
    }
  };

  if (!templateId) {
    return (
      <div className="dynamic-form">
        <div className="dynamic-form-empty">
          Sélectionnez un template dans la liste de gauche
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="dynamic-form">
        <div className="dynamic-form-loading">Chargement du formulaire...</div>
      </div>
    );
  }

  if (error && !schema) {
    return (
      <div className="dynamic-form">
        <div className="dynamic-form-error">
          <p>Erreur: {error}</p>
          <button onClick={() => loadSchema(templateId)}>Réessayer</button>
        </div>
      </div>
    );
  }

  if (!schema) {
    return null;
  }

  return (
    <div className="dynamic-form">
      <div className="dynamic-form-header">
        <h2>{schema.name}</h2>
        {schema.description && <p className="dynamic-form-description">{schema.description}</p>}
      </div>

      {Array.isArray(schema.modes) && schema.modes.includes('masse') && (
        <div className="dynamic-form-mode-selector">
          <label>
            <input
              type="radio"
              name="mode"
              value="unitaire"
              checked={mode === 'unitaire'}
              onChange={(e) => setMode(e.target.value as 'unitaire' | 'masse')}
            />
            Mode Unitaire
          </label>
          <label>
            <input
              type="radio"
              name="mode"
              value="masse"
              checked={mode === 'masse'}
              onChange={(e) => setMode(e.target.value as 'unitaire' | 'masse')}
            />
            Mode Masse
          </label>
        </div>
      )}

      <form onSubmit={handleSubmit} className="dynamic-form-content">
        {mode === 'masse' ? (
          <div className="dynamic-form-field">
            <label htmlFor="masseFile">
              Fichier CSV <span className="required">*</span>
            </label>
            <input
              type="file"
              id="masseFile"
              accept=".csv"
              onChange={(e) => handleMasseFileChange(e.target.files?.[0] || null)}
              required
            />
            <small>Fichier CSV contenant les valeurs pour chaque ligne (une ligne = un script SQL)</small>
          </div>
        ) : (
          (schema.unitFields || [])
            // Ne pas afficher les champs techniques dans le formulaire
            .filter(field => !field.technical)
            .map((field) => (
            <div key={field.name} className="dynamic-form-field">
              <label htmlFor={field.name}>
                {field.label}
                {field.required && <span className="required">*</span>}
              </label>
              {field.description && (
                <small className="field-description">{field.description}</small>
              )}
              {field.type === 'file' ? (
                <input
                  type="file"
                  id={field.name}
                  onChange={(e) => handleFileChange(field.name, e.target.files?.[0] || null)}
                  required={field.required}
                />
              ) : field.type === 'date' ? (
                <input
                  type="date"
                  id={field.name}
                  value={(formData[field.name] as string) || ''}
                  onChange={(e) => handleInputChange(field.name, e.target.value)}
                  required={field.required}
                />
              ) : field.type === 'number' ? (
                <input
                  type="number"
                  id={field.name}
                  value={(formData[field.name] as string) || ''}
                  onChange={(e) => handleInputChange(field.name, e.target.value)}
                  required={field.required}
                />
              ) : (
                <input
                  type="text"
                  id={field.name}
                  value={(formData[field.name] as string) || ''}
                  onChange={(e) => handleInputChange(field.name, e.target.value)}
                  placeholder={field.example}
                  required={field.required}
                />
              )}
            </div>
          ))
        )}

        {error && <div className="dynamic-form-error-message">{error}</div>}

        <div className="dynamic-form-actions">
          <button type="submit" disabled={generating} className="submit-button">
            {generating ? 'Génération...' : 'Générer le script SQL'}
          </button>
        </div>
      </form>
    </div>
  );
}


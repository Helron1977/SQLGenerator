import { useState } from 'react';
import { TemplateList } from './components/TemplateList';
import { DynamicForm } from './components/DynamicForm';
import './SqlGeneratorApp.css';

/**
 * Composant principal SQL Generator
 * 
 * Ce composant peut être intégré dans une application plus grande.
 * Il ne contient pas d'en-tête ni de navbar (gérés par l'application parente).
 * 
 * Layout :
 * - Colonne gauche (1/3) : Liste des templates
 * - Zone droite (2/3) : Formulaire dynamique
 */
export function SqlGeneratorApp() {
  const [selectedTemplateId, setSelectedTemplateId] = useState<string | null>(null);

  return (
    <div className="sql-generator-app">
      <div className="sql-generator-layout">
        <div className="sql-generator-sidebar">
          <TemplateList
            selectedTemplateId={selectedTemplateId}
            onTemplateSelect={setSelectedTemplateId}
          />
        </div>
        <div className="sql-generator-main">
          <DynamicForm templateId={selectedTemplateId} />
        </div>
      </div>
    </div>
  );
}


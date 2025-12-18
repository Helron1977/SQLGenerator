# 🎨 SQL Generator - Frontend

Frontend React modulaire pour SQL Generator, prêt pour intégration dans une application plus grande.

## 📋 Caractéristiques

- **Modulaire** : Composant isolé sans en-tête ni navbar
- **Layout flexible** : Colonne gauche (1/3) + Zone formulaire (2/3)
- **Formulaires dynamiques** : Génération automatique depuis les schémas backend
- **Modes unitaire et masse** : Support complet des deux modes
- **Upload de fichiers** : Gestion des fichiers CSV et paramètres de type file
- **Téléchargement automatique** : Scripts SQL générés téléchargés automatiquement

## 🚀 Installation

```bash
cd frontend
npm install
```

## 🛠️ Développement

```bash
npm run dev
```

Le frontend sera accessible sur `http://localhost:5173` (Vite par défaut).

## 📦 Build

```bash
npm run build
```

Génère les fichiers dans `dist/` prêts pour la production.

## 🔌 Intégration dans un projet parent

### Option 1 : Import comme module

```tsx
import { SqlGeneratorApp } from '@sqlgenerator/frontend';

function App() {
  return (
    <div>
      <Header />
      <Sidebar />
      <main>
        <SqlGeneratorApp />
      </main>
    </div>
  );
}
```

### Option 2 : Iframe (simple mais moins flexible)

```html
<iframe src="http://localhost:5173" style="width: 100%; height: 100vh; border: none;"></iframe>
```

### Option 3 : Intégration complète (copier les composants)

Copier les fichiers `src/` dans le projet parent et adapter les imports.

## ⚙️ Configuration

### Variable d'environnement

Définir `VITE_API_BASE_URL` pour pointer vers le backend :

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

Par défaut, utilise `/api` (URL relative).

### Proxy de développement

Le `vite.config.ts` inclut un proxy pour `/api` vers `http://localhost:8080`.

## 📁 Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── TemplateList.tsx      # Liste des templates (colonne gauche)
│   │   ├── TemplateList.css
│   │   ├── DynamicForm.tsx        # Formulaire dynamique (zone droite)
│   │   └── DynamicForm.css
│   ├── services/
│   │   └── api.ts                 # Service API pour communiquer avec le backend
│   ├── types/
│   │   └── api.ts                 # Types TypeScript
│   ├── SqlGeneratorApp.tsx        # Composant principal
│   ├── SqlGeneratorApp.css
│   ├── main.tsx                   # Point d'entrée
│   └── index.css                  # Styles globaux
├── package.json
├── vite.config.ts
└── tsconfig.json
```

## 🎨 Styles

Les styles utilisent des préfixes de classe (`sql-generator-`, `template-list-`, `dynamic-form-`) pour éviter les conflits avec l'application parente.

## 📝 Notes d'intégration

- Le composant ne gère **pas** l'en-tête ni la navbar (à gérer par le parent)
- Le layout est flexible et s'adapte à la taille du conteneur parent
- Les appels API utilisent des URLs relatives (configurables via `VITE_API_BASE_URL`)
- Le CORS doit être configuré côté backend (ou via proxy)


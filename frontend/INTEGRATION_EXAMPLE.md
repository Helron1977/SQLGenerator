# 📝 Exemple d'Intégration Frontend

Ce document montre comment intégrer le composant `SqlGeneratorApp` dans une application React parente.

## 🎯 Scénario

Vous avez une application React avec :
- Un en-tête (`<Header />`)
- Une sidebar de navigation (`<Sidebar />`)
- Une zone de contenu principale où vous voulez intégrer SQL Generator

## 📦 Option 1 : Import comme module (si publié sur npm)

```tsx
// App.tsx du projet parent
import { SqlGeneratorApp } from '@sqlgenerator/frontend';

function App() {
  return (
    <div className="app-layout">
      <Header />
      <div className="app-content">
        <Sidebar />
        <main className="app-main">
          <SqlGeneratorApp />
        </main>
      </div>
    </div>
  );
}
```

## 📦 Option 2 : Copier les composants (recommandé pour l'instant)

### Étape 1 : Copier les fichiers

Copier le contenu de `frontend/src/` dans votre projet parent, par exemple dans `src/modules/sql-generator/`.

### Étape 2 : Adapter les imports

```tsx
// src/modules/sql-generator/SqlGeneratorApp.tsx
import { TemplateList } from './components/TemplateList';
import { DynamicForm } from './components/DynamicForm';
// ... autres imports
```

### Étape 3 : Utiliser dans l'application parente

```tsx
// App.tsx du projet parent
import { SqlGeneratorApp } from './modules/sql-generator/SqlGeneratorApp';

function App() {
  return (
    <div className="app-layout">
      <Header />
      <div className="app-content">
        <Sidebar />
        <main className="app-main">
          <SqlGeneratorApp />
        </main>
      </div>
    </div>
  );
}
```

### Étape 4 : Configurer l'API

Créer un fichier `.env` dans le projet parent :

```env
VITE_API_BASE_URL=/api
```

Ou si le backend est sur un autre serveur :

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

## 🎨 Styles

Les styles utilisent des préfixes de classe pour éviter les conflits :
- `sql-generator-*`
- `template-list-*`
- `dynamic-form-*`

Si vous utilisez un framework CSS global (Bootstrap, Material-UI, etc.), vous pouvez :
1. **Laisser les styles isolés** : Les préfixes garantissent l'isolation
2. **Adapter les styles** : Modifier les fichiers CSS pour correspondre au thème du parent
3. **Utiliser CSS Modules** : Convertir les fichiers CSS en modules si nécessaire

## 🔧 Personnalisation

### Changer la largeur de la colonne

Modifier `SqlGeneratorApp.css` :

```css
.sql-generator-sidebar {
  flex: 0 0 30%; /* Au lieu de 33.333% */
}

.sql-generator-main {
  flex: 1; /* Prend le reste */
}
```

### Adapter les couleurs

Modifier les variables CSS ou les fichiers CSS directement :

```css
/* TemplateList.css */
.template-item.selected {
  background-color: #votre-couleur;
  border-color: #votre-couleur;
}
```

## 📱 Responsive

Le layout est responsive par défaut :
- Sur écrans < 768px : Passage en colonne (liste au-dessus, formulaire en dessous)
- Sur écrans >= 768px : Layout horizontal (1/3 + 2/3)

Vous pouvez adapter les breakpoints dans `SqlGeneratorApp.css`.

## 🔌 Communication avec le backend

Le service API (`services/api.ts`) utilise `fetch` avec l'URL de base configurée.

Si votre application parente utilise un client HTTP différent (axios, etc.), vous pouvez :
1. **Adapter le service API** : Remplacer `fetch` par votre client
2. **Créer un wrapper** : Encapsuler les appels dans votre système de requêtes

## ✅ Checklist d'intégration

- [ ] Fichiers copiés dans le projet parent
- [ ] Imports adaptés
- [ ] Variable `VITE_API_BASE_URL` configurée
- [ ] Styles vérifiés (pas de conflits)
- [ ] CORS configuré côté backend
- [ ] Test de chargement des templates
- [ ] Test de génération de script
- [ ] Test du mode masse
- [ ] Test responsive


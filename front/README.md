# SQL Generator Front - React

Front-end React pour le générateur de patches SQL.

## 🚀 Démarrage

### Prérequis

- Node.js 18+ et npm/yarn
- Backend Spring Boot démarré sur `http://localhost:8080`

### Installation

```bash
cd front
npm install
```

### Développement

```bash
npm run dev
```

L'application sera accessible sur `http://localhost:3000`

### Build

```bash
npm run build
```

## 📋 Fonctionnalités

- ✅ Liste automatique des queries disponibles
- ✅ Génération dynamique de formulaires depuis OpenAPI
- ✅ Support des modes unitaire et masse
- ✅ Upload de fichiers (pour clauses IN et mode masse)
- ✅ Téléchargement automatique des patches SQL générés
- ✅ Interface moderne et responsive

## 🏗️ Architecture

```
front/
├── src/
│   ├── components/        # Composants React
│   │   ├── QueryList.tsx  # Liste des queries
│   │   └── PatchForm.tsx  # Formulaire dynamique
│   ├── hooks/            # Hooks personnalisés
│   │   ├── useQueries.ts  # Charge les queries
│   │   └── useOpenApiSpec.ts # Charge la spec OpenAPI
│   ├── types.ts          # Types TypeScript
│   ├── App.tsx           # Composant principal
│   └── main.tsx          # Point d'entrée
```

## 🔌 API

Le front utilise les endpoints suivants :

- `GET /api/patch/queries` - Liste des queries disponibles
- `GET /v3/api-docs/sql-generator` - Spec OpenAPI
- `POST /api/patch/{id}` - Génération unitaire
- `POST /api/patch/{id}/masse` - Génération masse


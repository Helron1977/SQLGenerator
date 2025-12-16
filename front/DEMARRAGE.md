# 🚀 Guide de Démarrage - Front React

## Installation

```bash
cd front
npm install
```

## Démarrage

1. **Démarrer le backend Spring Boot** (dans le dossier parent) :
```bash
mvn spring-boot:run
```

2. **Démarrer le front React** (dans le dossier `front/`) :
```bash
npm run dev
```

3. **Ouvrir le navigateur** :
```
http://localhost:3000
```

## Fonctionnalités

### ✅ Ce qui fonctionne automatiquement

1. **Liste des queries** : Charge automatiquement toutes les queries disponibles via `/api/patch/queries`

2. **Formulaires dynamiques** : Génère les formulaires depuis la spec OpenAPI (`/v3/api-docs/sql-generator`)

3. **Modes d'exécution** :
   - **Mode Unitaire** : Formulaire avec tous les paramètres
   - **Mode Masse** : Formulaire simplifié (ticket + fichier CSV) - uniquement pour les queries sans paramètre IN

4. **Upload de fichiers** :
   - Pour les paramètres `@param-file:` (clauses IN)
   - Pour le mode masse (fichier CSV)

5. **Téléchargement** : Télécharge automatiquement le fichier SQL généré

## Structure

```
front/
├── src/
│   ├── components/
│   │   ├── QueryList.tsx    # Liste des queries (sidebar)
│   │   └── PatchForm.tsx     # Formulaire dynamique généré depuis OpenAPI
│   ├── hooks/
│   │   ├── useQueries.ts    # Hook pour charger les queries
│   │   └── useOpenApiSpec.ts # Hook pour charger la spec OpenAPI
│   ├── types.ts             # Types TypeScript
│   ├── App.tsx              # Composant principal
│   └── main.tsx             # Point d'entrée
```

## Personnalisation

### Modifier les styles

Les styles sont dans :
- `src/App.css` - Styles globaux et layout
- `src/components/QueryList.css` - Styles de la liste
- `src/components/PatchForm.css` - Styles du formulaire

### Ajouter des fonctionnalités

Le formulaire est généré dynamiquement depuis OpenAPI, donc :
- ✅ Ajouter une query dans le backend → Apparaît automatiquement dans le front
- ✅ Modifier les paramètres d'une query → Le formulaire se met à jour automatiquement
- ✅ Pas besoin de régénérer le code front

## Dépannage

### Le front ne charge pas les queries

Vérifiez que :
1. Le backend est démarré sur `http://localhost:8080`
2. L'endpoint `/api/patch/queries` est accessible
3. Le proxy dans `vite.config.ts` est correctement configuré

### Les formulaires ne s'affichent pas

Vérifiez que :
1. La spec OpenAPI est accessible sur `/v3/api-docs/sql-generator`
2. Les queries ont bien des paramètres définis dans les métadonnées SQL

### Erreur CORS

Le backend a déjà `@CrossOrigin(origins = "*")` configuré, donc normalement pas de problème. Si erreur, vérifiez la configuration CORS dans `PatchController.java`.


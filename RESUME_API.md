# API SQL Generator - Résumé

## 🎯 Qu'est-ce que c'est ?

API REST qui génère dynamiquement des fichiers de patch SQL à partir de templates paramétrés. Chaque requête SQL avec métadonnées génère automatiquement un endpoint documenté dans Swagger.

## 🔗 Accès

- **Swagger UI** : `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON** : `http://localhost:8080/v3/api-docs` (pour exporter)

## 📥 Exporter le schéma

**Méthode simple** :
1. Ouvrir `http://localhost:8080/v3/api-docs` dans le navigateur
2. Ctrl+A (tout sélectionner) puis Ctrl+C (copier)
3. Coller dans un fichier `.json`

**Via PowerShell** :
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs" -OutFile "openapi-schema.json"
```

## 🔌 Endpoints principaux

### Queries SQL
- `GET /api/queries` → Liste toutes les queries
- `GET /api/queries/{id}` → Schéma complet (pour frontend)
- `GET /api/queries/{id}/request-body` → JSON à copier-coller (pour tests)

### Patches SQL
- `POST /api/patches/{id}?mode=unitaire` → Génère un patch SQL
- `POST /api/patches/{id}?mode=masse` → Génère N patches SQL (mode masse)

## 🔄 Workflow de test

1. `GET /api/queries/{id}/request-body` → Copier le JSON
2. Modifier les valeurs d'exemple
3. `POST /api/patches/{id}` → Coller le JSON dans le body
4. Télécharger le fichier SQL généré

## ⚠️ Boutons clipboard Swagger

Si les boutons ne fonctionnent pas : utiliser directement `http://localhost:8080/v3/api-docs` et copier le JSON.

## 📦 Utilisation du schéma

Le fichier `openapi-schema.json` peut être importé dans :
- Postman (Import > Link)
- Insomnia (Import > From URL)
- OpenAPI Generator (génération de clients)


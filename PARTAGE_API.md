# API SQL Generator - Guide de partage

## 🚀 Accès rapide

- **Swagger UI** : `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON** : `http://localhost:8080/v3/api-docs`
- **OpenAPI YAML** : `http://localhost:8080/v3/api-docs.yaml`

---

## 📥 Exporter le schéma OpenAPI

### Méthode 1 : Via navigateur (le plus simple)
1. Ouvrir `http://localhost:8080/v3/api-docs` dans le navigateur
2. Sélectionner tout (Ctrl+A) et copier (Ctrl+C)
3. Coller dans un fichier `.json`

### Méthode 2 : Via PowerShell
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs" -OutFile "openapi-schema.json"
```

### Méthode 3 : Via curl
```bash
curl http://localhost:8080/v3/api-docs > openapi-schema.json
```

---

## 📋 Endpoints principaux

### Queries SQL (`/api/queries`)

| Endpoint | Description | Usage |
|----------|-------------|-------|
| `GET /api/queries` | Liste toutes les queries disponibles | Frontend : construire la liste |
| `GET /api/queries/{id}` | Schéma complet d'une query | Frontend : construire le formulaire |
| `GET /api/queries/{id}/request-body?mode=unitaire` | JSON à copier-coller | Tests manuels (Swagger, Postman) |

### Patches SQL (`/api/patches`)

| Endpoint | Description | Usage |
|----------|-------------|-------|
| `POST /api/patches/{id}?mode=unitaire` | Génère un patch SQL (mode unitaire) | Génération d'un fichier SQL |
| `POST /api/patches/{id}?mode=masse` | Génère un patch SQL (mode masse) | Génération de N fichiers SQL |

---

## 🔄 Workflow pour tester dans Swagger

1. **Obtenir le JSON du body** :
   ```
   GET /api/queries/{id}/request-body
   ```
   Exemple : `GET /api/queries/update-person-name/request-body`

2. **Copier le JSON retourné** :
   ```json
   {
     "ticket": "dc905fff-27a6-452f-aa0d-360c6c37b94a",
     "executionType": "unitaire",
     "person_id": "example-value",
     "name": "example-value"
   }
   ```

3. **Modifier les valeurs d'exemple** par tes vraies valeurs

4. **Coller dans le body** de `POST /api/patches/{id}`

5. **Exécuter** → Le fichier SQL est téléchargé

---

## ⚠️ Problème des boutons clipboard dans Swagger

Si les boutons clipboard ne fonctionnent pas dans Swagger UI :

**Solution** : Utiliser directement l'endpoint OpenAPI
- Ouvrir `http://localhost:8080/v3/api-docs` dans le navigateur
- Copier le JSON affiché
- Ou utiliser PowerShell/curl (voir ci-dessus)

---

## 📦 Utilisation du schéma exporté

Le fichier `openapi-schema.json` peut être utilisé pour :

- ✅ **Importer dans Postman** : Import > Link > Coller l'URL `http://localhost:8080/v3/api-docs`
- ✅ **Importer dans Insomnia** : Import > From URL
- ✅ **Générer un client** : OpenAPI Generator, Swagger Codegen
- ✅ **Documentation externe** : Redoc, Swagger UI standalone
- ✅ **Tests automatisés** : Schemathesis, Dredd

---

## 🎯 Exemple complet

### 1. Lister les queries disponibles
```bash
GET http://localhost:8080/api/queries
```

### 2. Obtenir le JSON pour une query
```bash
GET http://localhost:8080/api/queries/update-person-name/request-body
```

Réponse :
```json
{
  "ticket": "dc905fff-27a6-452f-aa0d-360c6c37b94a",
  "executionType": "unitaire",
  "person_id": "example-value",
  "name": "example-value"
}
```

### 3. Générer le patch
```bash
POST http://localhost:8080/api/patches/update-person-name
Content-Type: application/json

{
  "ticket": "dc905fff-27a6-452f-aa0d-360c6c37b94a",
  "executionType": "unitaire",
  "person_id": "001",
  "name": "John Doe"
}
```

Réponse : Fichier SQL téléchargeable

---

## 📝 Notes importantes

- **Mode unitaire** : Génère une seule requête SQL
- **Mode masse** : Génère N requêtes SQL (une par ligne du CSV). Disponible uniquement pour les queries SANS paramètre IN
- **Clauses IN** : Si > 999 valeurs, lotissement automatique (limite Oracle)
- **Format de date** : `DD/MM/YY` (ex: `30/11/25`)
- **CORS** : Activé pour toutes les origines

---

## 🔗 Documentation complète

- **Description détaillée** : Voir `DESCRIPTION_API.md`
- **Guide d'ajout de requête** : Voir `GUIDE_AJOUT_REQUETE.md`
- **Analyse REST** : Voir `REST_API_ANALYSIS.md`


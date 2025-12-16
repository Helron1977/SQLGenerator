# Export du schéma OpenAPI / Swagger

## 📋 Endpoints pour exporter le schéma

### 1. JSON (recommandé)
```
GET http://localhost:8080/v3/api-docs
```

### 2. YAML
```
GET http://localhost:8080/v3/api-docs.yaml
```

### 3. Par groupe (si configuré)
```
GET http://localhost:8080/v3/api-docs/{group-name}
```

## 🔧 Méthodes d'export

### Méthode 1 : Via navigateur
1. Ouvrir `http://localhost:8080/v3/api-docs` dans le navigateur
2. Copier tout le JSON affiché
3. Coller dans un fichier `.json`

### Méthode 2 : Via curl (ligne de commande)
```bash
# JSON
curl http://localhost:8080/v3/api-docs > openapi-schema.json

# YAML
curl http://localhost:8080/v3/api-docs.yaml > openapi-schema.yaml
```

### Méthode 3 : Via PowerShell (Windows)
```powershell
# JSON
Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs" -OutFile "openapi-schema.json"

# YAML
Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs.yaml" -OutFile "openapi-schema.yaml"
```

### Méthode 4 : Via Swagger UI (si les boutons fonctionnent)
1. Aller sur `http://localhost:8080/swagger-ui.html`
2. Cliquer sur le lien "v3/api-docs" en haut
3. Copier le JSON

## ⚠️ Problème des boutons clipboard dans Swagger UI

Si les boutons clipboard ne fonctionnent pas, c'est souvent dû à :
- Problème de permissions JavaScript dans le navigateur
- Extension de navigateur qui bloque
- Version de SpringDoc/Swagger UI

**Solution** : Utiliser directement les endpoints `/v3/api-docs` ou `/v3/api-docs.yaml`

## 📦 Utilisation du schéma exporté

Le schéma OpenAPI peut être utilisé pour :
- **Générer un client** (OpenAPI Generator, Swagger Codegen)
- **Importer dans Postman** (Import > Link > Coller l'URL)
- **Importer dans Insomnia** (Import > From URL)
- **Documentation externe** (Redoc, Swagger UI standalone)
- **Tests automatisés** (Schemathesis, Dredd)

## 🔗 URLs utiles

- **Swagger UI** : `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON** : `http://localhost:8080/v3/api-docs`
- **OpenAPI YAML** : `http://localhost:8080/v3/api-docs.yaml`


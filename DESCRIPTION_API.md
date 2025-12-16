# Description de l'API SQL Generator

## 🎯 Vue d'ensemble

L'API **SQL Generator** permet de générer dynamiquement des fichiers de patch SQL à partir de templates paramétrés. Chaque requête SQL avec métadonnées génère automatiquement un endpoint REST documenté dans Swagger.

## 📚 Architecture REST

L'API suit les principes REST et expose deux ressources principales :

### 1. **Queries SQL** (`/api/queries`)
Ressource représentant les requêtes SQL disponibles et leurs schémas.

### 2. **Patches SQL** (`/api/patches`)
Ressource représentant la génération de fichiers de patch SQL.

---

## 🔌 Endpoints disponibles

### Groupe : SQL Queries

#### `GET /api/queries`
**Description** : Liste toutes les queries SQL disponibles avec leurs schémas complets.

**Réponse** : Tableau de `FormSchema` contenant :
- Identifiant, nom, description
- Tags de catégorisation
- Champs pour chaque mode (unitaire/masse)
- Types, labels, validations

**Usage** : Frontend pour construire dynamiquement les formulaires.

---

#### `GET /api/queries/{id}`
**Description** : Retourne le schéma complet d'une query spécifique.

**Paramètres** :
- `{id}` : Identifiant de la query (ex: `update-person-name`)

**Réponse** : `FormSchema` complet avec tous les détails (nom, description, tags, champs, types, labels, validations).

**Usage** : Frontend pour construire le formulaire d'une query spécifique.

---

#### `GET /api/queries/{id}/request-body`
**Description** : ⚠️ **Usage : Tests manuels uniquement** (Swagger, Postman, etc.)

Retourne uniquement le JSON avec les paramètres et leurs valeurs d'exemple, prêt à être copié-collé dans le body de `POST /api/patches/{id}`.

**Paramètres** :
- `{id}` : Identifiant de la query
- `mode` (query param, optionnel) : `unitaire` (défaut) ou `masse`

**Exemple** :
```
GET /api/queries/update-person-name/request-body?mode=unitaire
```

**Réponse** :
```json
{
  "ticket": "dc905fff-27a6-452f-aa0d-360c6c37b94a",
  "executionType": "unitaire",
  "person_id": "example-value",
  "name": "example-value"
}
```

**Usage** : Tests manuels dans Swagger ou clients HTTP. Pour le frontend, utiliser plutôt `GET /api/queries/{id}`.

---

### Groupe : SQL Patches

#### `POST /api/patches/{id}`
**Description** : Génère un fichier de patch SQL.

**Paramètres** :
- `{id}` : Identifiant de la query
- `mode` (query param, optionnel) : `unitaire` (défaut) ou `masse`

**Body** (JSON, form-urlencoded ou multipart) :
- Mode **unitaire** : Paramètres de la query + `executionType` (optionnel, défaut: "unitaire")
- Mode **masse** : `ticket` + fichier CSV (`masseFile` via multipart)

**Modes d'exécution** :
- **unitaire** : Génère une seule requête SQL. Peut gérer les clauses IN (avec fichier).
- **masse** : Génère N requêtes SQL (une par ligne du fichier CSV). Disponible uniquement pour les queries SANS paramètre IN.

**Réponse** : Fichier SQL téléchargeable (Content-Type: `application/sql`)

**Exemples** :

1. **Mode unitaire avec JSON** :
```bash
POST /api/patches/update-person-name
Content-Type: application/json

{
  "ticket": "dc905fff-27a6-452f-aa0d-360c6c37b94a",
  "executionType": "unitaire",
  "person_id": "001",
  "name": "John Doe"
}
```

2. **Mode masse avec query param** :
```bash
POST /api/patches/update-person-name?mode=masse
Content-Type: multipart/form-data

ticket=dc905fff-27a6-452f-aa0d-360c6c37b94a
masseFile=<fichier.csv>
```

**Fichier CSV (mode masse)** : Une ligne = une requête. Les valeurs doivent être dans l'ordre des paramètres de la query.

---

## 🔄 Workflow d'utilisation

### Pour le frontend

1. **Lister les queries disponibles** :
   ```
   GET /api/queries
   ```

2. **Obtenir le schéma d'une query** :
   ```
   GET /api/queries/{id}
   ```

3. **Construire le formulaire** dynamiquement à partir du `FormSchema`

4. **Générer le patch** :
   ```
   POST /api/patches/{id}
   Body: { paramètres de la query }
   ```

### Pour les tests manuels (Swagger, Postman)

1. **Obtenir le JSON à copier-coller** :
   ```
   GET /api/queries/{id}/request-body?mode=unitaire
   ```

2. **Copier le JSON retourné**

3. **Modifier les valeurs d'exemple** par les vraies valeurs

4. **Coller dans le body** de `POST /api/patches/{id}`

---

## 📝 Format des métadonnées SQL

Chaque fichier SQL doit commencer par des métadonnées en commentaires :

```sql
-- @id: update-person-name              # Identifiant (optionnel, dérivé du nom de fichier si absent)
-- @name: Mise à jour du nom            # Nom affiché
-- @description: Description détaillée  # Description
-- @tags: person,update                 # Tags séparés par virgules
-- @param: person_id|text|ID Personne|true
-- @param: name|text|Nom|true
-- @param-file: contrat_ids|Liste des IDs|true  # Pour clauses IN

UPDATE PERSON SET NAME = {{name}} WHERE PERSON_ID = {{person_id}};
```

---

## 🎯 Cas d'usage

### 1. Requête simple (mode unitaire)
- Un seul appel → Un fichier SQL généré
- Paramètres passés directement dans le body

### 2. Clause IN (mode unitaire avec fichier)
- Paramètre de type `file` (`@param-file`)
- Fichier uploadé via multipart
- Si > 999 valeurs → Lotissement automatique (limite Oracle)

### 3. Mode masse
- Fichier CSV avec N lignes → N requêtes SQL dans un seul fichier
- Disponible uniquement pour les queries SANS paramètre IN

---

## 🔗 URLs importantes

- **Swagger UI** : `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON** : `http://localhost:8080/v3/api-docs`
- **OpenAPI YAML** : `http://localhost:8080/v3/api-docs.yaml`

---

## 📦 Export du schéma OpenAPI

Pour exporter le schéma complet :

```bash
# JSON
curl http://localhost:8080/v3/api-docs > openapi-schema.json

# YAML
curl http://localhost:8080/v3/api-docs.yaml > openapi-schema.yaml
```

Ou via navigateur : Ouvrir `http://localhost:8080/v3/api-docs` et copier le JSON.

---

## ⚙️ Configuration

- **Port par défaut** : `8080`
- **CORS** : Activé pour toutes les origines (`*`)
- **Format de date** : `DD/MM/YY` (ex: `30/11/25`)
- **Limite clause IN** : 999 valeurs (Oracle)

---

## 🚀 Démarrage

```bash
mvn spring-boot:run
```

Puis accéder à : `http://localhost:8080/swagger-ui.html`


# Analyse REST des Endpoints - SQL Generator

## Endpoints actuels

### FormController (`/api/forms`) ⚠️ **À RENOMMER**
- `GET /api/forms` - Liste des schémas de formulaires
- `GET /api/forms/{id}` - Schéma d'un formulaire spécifique
- `GET /api/forms/{id}/request-schema` - Structure JSON pour mode unitaire/masse

**Problème** : "forms" n'est pas la ressource métier réelle. Ce contrôleur expose des **queries SQL** et leurs schémas.

**Suggestion REST** : Renommer en `QueryController` avec `/api/queries`

### PatchController (`/api/patches`)
- `POST /api/patch/{id}` - Générer un patch SQL (mode unitaire)
- `POST /api/patch/{id}/masse` - Générer un patch SQL (mode masse)

---

## Analyse selon les principes REST (Nicolas Hachet)

### ✅ Points conformes

1. **Utilisation des verbes HTTP** : GET pour lecture, POST pour création/génération
2. **Noms au pluriel** : `/api/forms` (collection)
3. **Identifiants dans l'URI** : `/{id}` pour identifier une ressource spécifique
4. **Séparation des ressources** : `/api/forms` et `/api/patch` sont distincts

### ⚠️ Points à améliorer

#### 1. **"patch" est un verbe, pas une ressource**
   - **Actuel** : `POST /api/patch/{id}`
   - **Problème** : "patch" est une action, pas une ressource
   - **REST pur** : Les URIs doivent représenter des ressources (noms), pas des actions
   - **Suggestion** : `POST /api/patches/{id}` ou `POST /api/sql-patches/{id}`

#### 2. **"masse" comme qualificatif dans l'URI**
   - **Actuel** : `POST /api/patch/{id}/masse`
   - **Problème** : "masse" est un qualificatif/adjectif, pas une sous-ressource
   - **Alternatives REST** :
     - **Option A** : Query parameter : `POST /api/patches/{id}?mode=masse`
     - **Option B** : Sous-ressource : `POST /api/patches/{id}/executions/masse`
     - **Option C** : Header ou body : Mode dans le body JSON

#### 3. **"body-structure" est une action/description**
   - **Actuel** : `GET /api/forms/{id}/body-structure`
   - **Problème** : "body-structure" décrit une action (obtenir la structure), pas une ressource
   - **Alternatives REST** :
     - **Option A** : Sous-ressource : `GET /api/forms/{id}/request-schema` ou `/request-template`
     - **Option B** : Query parameter : `GET /api/forms/{id}?include=request-schema`
     - **Option C** : Header Accept : `GET /api/forms/{id}` avec `Accept: application/vnd.request-schema+json`

#### 4. **Incohérence dans le chemin "body-structure/masse"**
   - **Actuel** : `GET /api/forms/{id}/body-structure/masse`
   - **Problème** : Mélange action + qualificatif
   - **Suggestion** : `GET /api/forms/{id}/request-schema?mode=masse`

---

## Suggestions d'amélioration (REST pur)

### Option 1 : Approche ressources + query parameters (recommandée)

```
GET  /api/forms                    → Liste des schémas
GET  /api/forms/{id}               → Schéma complet
GET  /api/forms/{id}/request-schema?mode=unitaire  → Template JSON unitaire
GET  /api/forms/{id}/request-schema?mode=masse      → Template JSON masse

POST /api/patches/{id}?mode=unitaire  → Générer patch unitaire
POST /api/patches/{id}?mode=masse     → Générer patch masse
```

**Avantages** :
- Ressources claires (`patches`, `forms`)
- Mode comme paramètre (plus flexible)
- URIs plus courtes et cohérentes

### Option 2 : Approche sous-ressources

```
GET  /api/forms/{id}
GET  /api/forms/{id}/request-schemas/unitaire
GET  /api/forms/{id}/request-schemas/masse

POST /api/patches/{id}/executions/unitaire
POST /api/patches/{id}/executions/masse
```

**Avantages** :
- Hiérarchie claire des ressources
- Mode comme sous-ressource

**Inconvénients** :
- URIs plus longues
- "executions" peut être confus

### Option 3 : Mode dans le body (actuel, mais amélioré)

```
GET  /api/forms/{id}
GET  /api/forms/{id}/request-schema?mode=unitaire
GET  /api/forms/{id}/request-schema?mode=masse

POST /api/patches/{id}  → Body contient {"executionType": "unitaire" ou "masse"}
```

**Avantages** :
- URI unique pour la génération
- Mode dans le body (déjà implémenté partiellement)

---

## Recommandation finale

**Approche hybride** (équilibre REST / praticité) :

```
# Schémas de formulaires
GET  /api/forms                          → Liste
GET  /api/forms/{id}                     → Schéma complet
GET  /api/forms/{id}/request-schema      → Template JSON (mode par défaut: unitaire)
GET  /api/forms/{id}/request-schema?mode=masse  → Template JSON masse

# Génération de patches
POST /api/patches/{id}                   → Générer (mode dans body: executionType)
POST /api/patches/{id}?mode=masse        → Alternative avec query param
```

**Changements nécessaires** :
1. Renommer `/api/patch` → `/api/patches` (pluriel, ressource)
2. Renommer `/body-structure` → `/request-schema` (ressource, pas action)
3. Utiliser query param `?mode=masse` au lieu de `/masse` dans l'URI
4. Garder la compatibilité avec le body JSON (executionType)

---

## Question : POST pour récupérer un fichier est-il conforme REST ?

### Analyse du problème

**Situation actuelle** :
```
POST /api/patch/{id}  → Génère un fichier SQL ET le retourne en téléchargement
```

**Problème REST** :
- **POST** est censé **créer** une ressource, pas la récupérer
- En REST pur, **GET** récupère, **POST** crée
- Mélange création + récupération dans une seule opération

### Options REST

#### Option 1 : POST pour créer, GET pour récupérer (REST pur)

```
POST /api/patches/{id}              → Génère le patch, retourne { "patchId": "xxx", "location": "/api/patches/{id}/files/xxx" }
GET  /api/patches/{id}/files/{patchId}  → Télécharge le fichier généré
```

**Avantages** :
- ✅ Sémantique REST pure
- ✅ Séparation création / récupération
- ✅ Cacheable (GET est cacheable)

**Inconvénients** :
- ❌ Plus complexe (2 appels)
- ❌ Nécessite stockage temporaire
- ❌ Moins pratique pour un usage simple

#### Option 2 : POST avec effet de bord (actuel, acceptable)

```
POST /api/patches/{id}  → Génère et retourne directement le fichier
```

**Justification** :
- POST génère une ressource (le fichier SQL) → **effet de bord** ✅
- Le retour du fichier est une **représentation** de la ressource créée
- C'est une **génération à la demande**, pas une ressource persistante

**Conforme REST si** :
- On considère que POST **crée** une ressource temporaire
- Le fichier retourné est la **représentation** de cette ressource
- C'est une **action** (générer), pas juste une récupération

#### Option 3 : GET avec query params (non recommandé)

```
GET /api/patches/{id}?ticket=xxx&param1=value1  → Génère et retourne
```

**Problèmes** :
- ❌ GET doit être **idempotent** (sans effet de bord)
- ❌ Génération = effet de bord (création de fichier)
- ❌ Violation des principes REST

### Recommandation

**Option 2 (actuel) est acceptable** pour ce cas d'usage car :

1. **POST génère une ressource** (le fichier SQL) → effet de bord légitime
2. **Le fichier retourné** est la représentation de la ressource créée
3. **Génération à la demande** : pas de persistance nécessaire
4. **Pratique** : un seul appel, réponse immédiate

**Amélioration possible** (hybride) :

```
POST /api/patches/{id}  → Génère et retourne directement (actuel)
                        → OU retourne { "patchId": "xxx", "downloadUrl": "/api/patches/{id}/files/xxx" }
GET  /api/patches/{id}/files/{patchId}  → Télécharge (optionnel, si on veut persister)
```

**Conclusion** : POST pour générer + retourner est **acceptable en REST** pour une génération à la demande, même si ce n'est pas le "REST pur" idéal.

---

## Question : `{id}` devrait-il être un query parameter ?

### Analyse REST

**Règle REST** :
- **Path variable** (`/{id}`) : Pour identifier une **ressource spécifique**
- **Query parameter** (`?id=xxx`) : Pour **filtrer/rechercher** dans une collection

### Cas d'usage actuels

#### ✅ `{id}` en path variable (actuel) - **CORRECT**

```
GET /api/forms/{id}              → "Obtenir la ressource 'form' identifiée par {id}"
POST /api/patches/{id}           → "Créer un patch pour la query identifiée par {id}"
```

**Justification** :
- `{id}` identifie une **ressource spécifique** (un formulaire, une query)
- C'est un **identifiant de ressource**, pas un filtre
- Conforme à REST : `/collection/{identifier}`

#### ❌ `{id}` en query parameter - **NON RECOMMANDÉ**

```
GET /api/forms?id={id}           → "Filtrer les forms par id" (moins clair)
POST /api/patches?id={id}        → Ambigu (quel patch ?)
```

**Problèmes** :
- Moins explicite sur l'identité de la ressource
- Peut être confondu avec un filtre
- Moins standard en REST

### Exception : Quand utiliser un query parameter ?

**Query parameters** sont appropriés pour :
- **Filtres** : `GET /api/forms?tag=update&status=active`
- **Pagination** : `GET /api/forms?page=1&size=10`
- **Tri** : `GET /api/forms?sort=name&order=asc`
- **Recherche** : `GET /api/forms?search=person`

**Path variables** sont appropriés pour :
- **Identifiants de ressources** : `GET /api/forms/{id}`
- **Sous-ressources** : `GET /api/forms/{id}/request-schema`
- **Relations** : `GET /api/users/{userId}/orders/{orderId}`

### Conclusion sur `{id}`

✅ **`{id}` en path variable est CORRECT** selon REST

- `{id}` identifie une ressource spécifique
- C'est la pratique standard : `/collection/{identifier}`
- Plus explicite et lisible
- Conforme aux principes REST de Nicolas Hachet

**Recommandation** : **Garder `{id}` en path variable**

---

## Conclusion

**Score REST actuel : 6/10**

**Points forts** :
- Utilisation correcte des verbes HTTP
- Collections au pluriel (`/forms`)
- Identifiants dans l'URI (`/{id}`) ✅ **CORRECT**

**Points à améliorer** :
- "patch" devrait être "patches" (ressource)
- "masse" devrait être un paramètre, pas dans l'URI
- "body-structure" devrait être une ressource nommée

**Impact** :
- Les changements sont **mineurs** et **rétrocompatibles** si on garde les anciens endpoints
- Améliorerait la **clarté** et la **maintenabilité**
- Respecterait mieux les **standards REST**


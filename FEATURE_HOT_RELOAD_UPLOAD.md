# Feature : Upload et Hot-Reload de Fichiers SQL

## 🎯 Objectif

Permettre l'upload de nouveaux fichiers SQL via un endpoint REST et recharger automatiquement les queries sans redémarrer l'application.

## 📋 Spécifications

### Endpoint Principal

**POST `/api/admin/upload-sql`**

- **Description** : Upload un fichier SQL, le valide, le sauvegarde et recharge les queries
- **Content-Type** : `multipart/form-data`
- **Paramètres** :
  - `file` (MultipartFile, requis) : Le fichier SQL à uploader
    - **IMPORTANT** : Le nom du fichier (sans extension `.sql`) sera utilisé comme ID
    - Si `-- @id:` est présent dans les métadonnées, il doit correspondre au nom du fichier
    - Exemple : Fichier `update-person.sql` → ID = `update-person` (automatique)
    - **Simplification** : Plus besoin de spécifier `-- @id:` si le nom du fichier correspond à l'ID souhaité
  - `overwrite` (boolean, optionnel, défaut: false) : Si true, écrase un fichier existant

- **Réponses** :
  - `200 OK` : Fichier uploadé et queries rechargées avec succès
  - `400 Bad Request` : Erreur de validation (métadonnées manquantes, placeholders invalides, etc.)
  - `409 Conflict` : Fichier déjà existant (si overwrite=false)
  - `500 Internal Server Error` : Erreur serveur

### Endpoint de Reload

**POST `/api/admin/reload-queries`**

- **Description** : Recharge toutes les queries depuis les fichiers SQL (classpath + uploads)
- **Réponses** :
  - `200 OK` : Queries rechargées avec succès
  - `500 Internal Server Error` : Erreur lors du rechargement

## 🏗️ Architecture Technique

### Problème Identifié

Les fichiers dans `src/main/resources/sql/` sont dans le classpath :
- ✅ Accessibles via `ClassPathResource` au runtime
- ❌ En lecture seule dans un JAR déployé
- ❌ Modifiables seulement au build time

### Solution Proposée

**Double source de fichiers SQL :**

1. **Fichiers statiques** (classpath) : `src/main/resources/sql/*.sql`
   - Chargés au démarrage
   - Non modifiables en runtime

2. **Fichiers uploadés** (répertoire externe) : `./sql_uploads/*.sql`
   - Créés via l'endpoint d'upload
   - Modifiables en runtime
   - Priorité sur les fichiers classpath (en cas de conflit de nom)

### Modifications Nécessaires

#### 1. QueryService - Scan Multi-Source

```java
private List<String> scanSqlFiles() throws IOException {
    List<String> filenames = new ArrayList<>();
    
    // 1. Scanner le classpath (fichiers statiques)
    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] classpathResources = resolver.getResources("classpath:sql/*.sql");
    for (Resource resource : classpathResources) {
        String filename = resource.getFilename();
        if (filename != null && filename.endsWith(".sql")) {
            filenames.add(filename);
        }
    }
    
    // 2. Scanner le répertoire d'uploads (fichiers dynamiques)
    Path uploadsDir = Paths.get("./sql_uploads/");
    if (Files.exists(uploadsDir)) {
        try (Stream<Path> paths = Files.walk(uploadsDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".sql"))
                 .forEach(p -> {
                     String filename = p.getFileName().toString();
                     // Priorité aux uploads : remplacer si déjà dans la liste
                     filenames.remove(filename);
                     filenames.add(filename);
                 });
        }
    }
    
    return filenames;
}
```

#### 2. QueryMetadataParser - Support Multi-Source

```java
public QueryDefinition parseSqlFile(String filename) throws IOException {
    // Essayer d'abord le répertoire d'uploads
    Path uploadPath = Paths.get("./sql_uploads/", filename);
    if (Files.exists(uploadPath)) {
        String sqlContent = Files.readString(uploadPath, StandardCharsets.UTF_8);
        return parseSqlContent(sqlContent, filename);
    }
    
    // Sinon, utiliser le classpath
    ClassPathResource resource = new ClassPathResource("sql/" + filename);
    String sqlContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    return parseSqlContent(sqlContent, filename);
}
```

#### 3. Nouveau Controller Admin

```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @Autowired
    private QueryService queryService;
    
    @PostMapping("/upload-sql")
    public ResponseEntity<UploadResult> uploadSqlFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "overwrite", defaultValue = "false") boolean overwrite) {
        
        // Validation
        // Sauvegarde
        // Reload
    }
    
    @PostMapping("/reload-queries")
    public ResponseEntity<ReloadResult> reloadQueries() {
        // Recharger toutes les queries
    }
}
```

## 📝 Documentation des Métadonnées et Placeholders

### Format des Métadonnées

Les métadonnées doivent être placées **en haut du fichier SQL**, avant le code SQL.

#### Métadonnées Obligatoires

```sql
-- @id: identifiant-unique
```
- **Obligatoire** : Non (optionnel)
- **Format** : Lettres, chiffres, tirets et underscores uniquement
- **Unicité** : Doit être unique parmi tous les fichiers SQL
- **Exemple** : `update-person-name`, `activate-contrats`
- **Note** : Si `-- @id:` n'est pas présent, l'ID est automatiquement extrait du nom du fichier (sans extension `.sql`)
  - Fichier `update-person.sql` → ID = `update-person` (automatique)
  - Fichier `update-person.sql` avec `-- @id: update-user` → ID = `update-user` (mais doit correspondre au nom du fichier)

#### Métadonnées Optionnelles

```sql
-- @name: Nom descriptif de la requête
```
- **Obligatoire** : Non
- **Description** : Nom affiché dans Swagger UI
- **Exemple** : `Mise à jour du nom d'une personne`

```sql
-- @description: Description détaillée de ce que fait la requête
```
- **Obligatoire** : Non
- **Description** : Description affichée dans Swagger UI
- **Exemple** : `Met à jour le nom d'une personne dans la table PERSON.`

```sql
-- @tags: tag1,tag2,tag3
```
- **Obligatoire** : Non
- **Format** : Tags séparés par des virgules (espaces optionnels)
- **Description** : Permet de filtrer les endpoints dans Swagger
- **Exemple** : `person,update,unitaire`

### Format des Paramètres

#### Paramètre Normal

```sql
-- @param: nom_param|type|label|required
```

- **`nom_param`** : Nom du paramètre (utilisé dans le placeholder `{{nom_param}}`)
- **`type`** : Type du paramètre (`text`, `number`, `integer`, `date`)
- **`label`** : Libellé affiché dans Swagger UI
- **`required`** : `true` ou `false` (optionnel, défaut: `false`)

**Exemple :**
```sql
-- @param: person_id|text|ID Personne|true
-- @param: name|text|Nom|true
-- @param: age|number|Âge|false
```

#### Paramètre Fichier (pour clause IN)

```sql
-- @param-file: nom_param|type|label|required
```

- Même format que `@param:` mais indique un upload de fichier
- Le fichier doit contenir **1 valeur par ligne**
- Si > 999 valeurs, lotissement automatique en plusieurs clauses IN

**Exemple :**
```sql
-- @param-file: contrat_iide|text|Fichier contenant les IDs contrats (1 par ligne)|true
```

### Format des Placeholders

Les placeholders dans le SQL utilisent la syntaxe **double accolades** :

```sql
{{nom_param}}
```

#### Règles Importantes

1. **Nom du placeholder = Nom du paramètre**
   ```sql
   -- @param: person_id|text|ID Personne|true
   UPDATE PERSON SET NAME = {{person_id}};  ✅ Correct
   UPDATE PERSON SET NAME = {{personId}};   ❌ Erreur : nom différent
   ```

2. **Tous les placeholders doivent avoir un paramètre défini**
   ```sql
   -- Si vous utilisez {{name}} dans le SQL, vous DEVEZ avoir :
   -- @param: name|text|Nom|true
   ```

3. **Les placeholders sont remplacés par les valeurs fournies**
   - Type `text` : `'valeur'` (avec guillemets simples, échappement des apostrophes)
   - Type `number` : `123` (sans guillemets)
   - Type `date` : `'30/11/25'` (format DD/MM/YY)
   - Valeur NULL/vide : `NULL` (sans guillemets)

4. **Les placeholders de fichiers (IN) sont remplacés par des listes**
   ```sql
   -- @param-file: ids|text|Liste IDs|true
   WHERE id IN ({{ids}})
   -- Devient : WHERE id IN ('id1', 'id2', 'id3')
   ```

### Exemple Complet

**Avec `-- @id:` explicite (optionnel) :**
```sql
-- @id: update-person-name
-- @name: Mise à jour du nom d'une personne
-- @description: Met à jour le nom d'une personne dans la table PERSON.
-- @tags: person,update,unitaire
-- @param: person_id|text|ID Personne|true
-- @param: name|text|Nom|true

UPDATE PERSON 
SET NAME = {{name}} 
WHERE PERSON_ID = {{person_id}};
```

**Sans `-- @id:` (ID extrait du nom du fichier) :**
```sql
-- @name: Mise à jour du nom d'une personne
-- @description: Met à jour le nom d'une personne dans la table PERSON.
-- @tags: person,update,unitaire
-- @param: person_id|text|ID Personne|true
-- @param: name|text|Nom|true

UPDATE PERSON 
SET NAME = {{name}} 
WHERE PERSON_ID = {{person_id}};
```
*(Si le fichier s'appelle `update-person-name.sql`, l'ID sera automatiquement `update-person-name`)*

## ✅ Validation

Lors de l'upload, le système valide :

1. ✅ **Format du fichier** : Extension `.sql`
2. ✅ **Correspondance nom/ID** : Le nom du fichier (sans `.sql`) doit correspondre à l'ID dans `-- @id:`
   - Exemple : Fichier `update-person.sql` → `-- @id: update-person` ✅
   - Exemple : Fichier `update-person.sql` → `-- @id: update-user` ❌ Erreur
3. ✅ **Métadonnées obligatoires** : `@id` présent et non vide
4. ✅ **Format des paramètres** : Syntaxe correcte (`nom|type|label|required`)
5. ✅ **Placeholders vs Paramètres** : Tous les `{{param}}` ont un `@param:` ou `@param-file:` correspondant
6. ✅ **Unicité de l'ID** : L'ID n'existe pas déjà (sauf si `overwrite=true`)
7. ✅ **Syntaxe SQL** : Le SQL est valide (validation basique)

## 🔒 Sécurité (Optionnel)

Pour la production, considérer :

- Authentification : `@PreAuthorize("hasRole('ADMIN')")`
- Validation du nom de fichier (éviter les paths relatifs)
- Limite de taille de fichier
- Rate limiting sur l'endpoint

## 📊 Réponse de l'Endpoint

```json
{
  "success": true,
  "message": "Fichier uploadé et queries rechargées avec succès",
  "filename": "new-query.sql",
  "queryId": "new-query",
  "queriesLoaded": 5,
  "warnings": []
}
```

En cas d'erreur :

```json
{
  "success": false,
  "message": "Erreur de validation",
  "errors": [
    "Placeholders non définis : param1, param2",
    "ID déjà existant : update-person-name"
  ]
}
```


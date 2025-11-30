# Guide : Ajouter une nouvelle requête SQL

Ce guide explique comment ajouter une nouvelle requête SQL qui générera automatiquement un endpoint dans l'API et dans la documentation Swagger.

## Principe

Le système scanne automatiquement les fichiers SQL dans `src/main/resources/sql/` au démarrage de l'application. Chaque fichier SQL avec des métadonnées valides génère automatiquement :
- Un endpoint REST : `POST /api/patch/{id}`
- Une documentation Swagger avec formulaire interactif
- Un système de filtrage par tags

**Aucune modification de code Java n'est nécessaire !**

---

## Étape 1 : Créer le fichier SQL

### 1.1 Emplacement
Créez un nouveau fichier dans : `src/main/resources/sql/`

**Nom du fichier** : `{identifiant-requete}.sql`

Exemple : `update-person-name.sql`, `delete-contrat.sql`

### 1.2 Structure du fichier

Le fichier SQL doit commencer par des **métadonnées en commentaires**, suivies de votre requête SQL.

#### Format des métadonnées

```sql
-- @id: identifiant-unique
-- @name: Nom descriptif de la requête
-- @description: Description détaillée de ce que fait la requête
-- @tags: tag1,tag2,tag3
-- @param: nom_parametre|type|Libellé affiché|required
-- @param: autre_param|type|Autre libellé|required

-- Votre requête SQL ici
UPDATE table SET colonne = {{nom_parametre}} WHERE id = {{autre_param}};
```

### 1.3 Détails des métadonnées

#### `-- @id:` (obligatoire)
Identifiant unique utilisé pour :
- L'URL de l'endpoint : `/api/patch/{id}`
- Le nom du fichier généré : `{id}_{executionType}_{timestamp}.sql`

**Exemple** : `-- @id: update-person-name`

#### `-- @name:`
Nom affiché dans Swagger UI comme titre de l'endpoint.

**Exemple** : `-- @name: Mise à jour du nom d'une personne`

#### `-- @description:`
Description détaillée affichée dans Swagger UI.

**Exemple** : `-- @description: Met à jour le nom d'une personne dans la table PERSON.`

#### `-- @tags:`
Tags séparés par des virgules pour le filtrage dans Swagger UI.

**Exemple** : `-- @tags: person,update,unitaire`

#### `-- @param:`
Définition d'un paramètre. Format : `nom|type|label|required`

- **nom** : Nom du paramètre (utilisé dans les placeholders `{{nom}}`)
- **type** : `text`, `number`, ou `date`
- **label** : Libellé affiché dans le formulaire Swagger
- **required** : `true` ou `false`

**Exemple** : `-- @param: person_id|text|ID Personne|true`

### 1.4 Placeholders dans le SQL

Utilisez `{{nom_parametre}}` pour les valeurs dynamiques. Le système remplace automatiquement :
- **Type `text` ou `date`** : `{{param}}` → `'valeur'` (avec guillemets)
- **Type `number`** : `{{param}}` → `123` (sans guillemets)

---

## Étape 2 : Exemples complets

### Exemple 1 : Requête SQL simple

**Fichier** : `src/main/resources/sql/update-person-name.sql`

```sql
-- @id: update-person-name
-- @name: Mise à jour du nom d'une personne
-- @description: Met à jour le nom d'une personne dans la table PERSON.
-- @tags: person,update,unitaire
-- @param: person_id|text|ID Personne|true
-- @param: name|text|Nom|true

UPDATE PERSON SET NAME = {{name}} WHERE PERSON_ID = {{person_id}};
```

### Exemple 2 : Bloc PL/SQL avec logs et transaction

**Fichier** : `src/main/resources/sql/update-cedem-role.sql`

```sql
-- @id: update-cedem-role
-- @name: Mise à jour du rôle CEDEM avec logs
-- @description: Met à jour le rôle dans CEDEM et PEPERROL avec logs avant/après et commit conditionnel.
-- @tags: cedem,peperrol,update,transaction,unitaire
-- @param: dem_iide|text|ID Demande (CEDEM)|true
-- @param: num_person|text|Numéro de personne|true

DECLARE
  v_dem_iide VARCHAR2(50) := {{dem_iide}};
  v_num_person VARCHAR2(50) := {{num_person}};
  v_prl_iide_before VARCHAR2(50);
  v_prl_iide_after VARCHAR2(50);
BEGIN
  -- Log AVANT : Récupération de la valeur actuelle
  SELECT dem_r_pcl INTO v_prl_iide_before FROM cedem WHERE dem_iide = v_dem_iide;
  DBMS_OUTPUT.PUT_LINE('=== LOG AVANT ===');
  DBMS_OUTPUT.PUT_LINE('dem_iide: ' || v_dem_iide);
  DBMS_OUTPUT.PUT_LINE('dem_r_pcl (avant): ' || NVL(v_prl_iide_before, 'NULL'));
  
  -- UPDATE 1 : Mise à jour de CEDEM
  UPDATE cedem
  SET dem_r_pcl = (SELECT prl_iide FROM peperrol WHERE prl_r_per_jourol = v_num_person AND prl_r_rol_possed = 12)
  WHERE dem_iide = v_dem_iide;
  
  -- UPDATE 2 : Mise à jour de PEPERROL
  UPDATE peperrol
  SET prlddatfin = NULL
  WHERE prl_iide = (SELECT prl_iide FROM peperrol WHERE prl_r_per_jourol = v_num_person AND prl_r_rol_possed = 12);
  
  -- Log APRÈS : Vérification de la nouvelle valeur
  SELECT dem_r_pcl INTO v_prl_iide_after FROM cedem WHERE dem_iide = v_dem_iide;
  DBMS_OUTPUT.PUT_LINE('=== LOG APRÈS ===');
  DBMS_OUTPUT.PUT_LINE('dem_r_pcl (après): ' || NVL(v_prl_iide_after, 'NULL'));
  
  -- Commit conditionnel (seulement si tout s'est bien passé)
  COMMIT;
  DBMS_OUTPUT.PUT_LINE('=== SUCCÈS ===');
  DBMS_OUTPUT.PUT_LINE('Transaction réussie - COMMIT effectué');
EXCEPTION
  WHEN OTHERS THEN
    ROLLBACK;
    DBMS_OUTPUT.PUT_LINE('=== ERREUR ===');
    DBMS_OUTPUT.PUT_LINE('Code erreur: ' || SQLCODE);
    DBMS_OUTPUT.PUT_LINE('Message: ' || SQLERRM);
    DBMS_OUTPUT.PUT_LINE('ROLLBACK effectué - Transaction annulée');
    RAISE;
END;
```

---

## Étape 3 : Redémarrer l'application

Après avoir créé votre fichier SQL :

1. **Redémarrez l'application Spring Boot**
   - L'application scanne automatiquement les fichiers SQL au démarrage
   - Les métadonnées sont parsées et les endpoints sont générés

2. **Vérifiez les logs**
   - Si une erreur de parsing survient, elle sera affichée dans les logs
   - Exemple : `Erreur parsing update-example.sql: Le fichier update-example.sql doit contenir un -- @id: dans ses métadonnées`

---

## Étape 4 : Tester dans Swagger UI

### 4.1 Accéder à Swagger UI
Ouvrez : `http://localhost:8080/swagger-ui.html`

### 4.2 Vérifier l'endpoint
- Votre nouvel endpoint doit apparaître : `POST /api/patch/{votre-id}`
- Le titre correspond au `-- @name:`
- La description correspond au `-- @description:`

### 4.3 Filtrer par tags
- Utilisez la barre de recherche en haut de Swagger UI
- Tapez un tag (ex: `person`, `update`) pour filtrer les endpoints

### 4.4 Tester l'endpoint
1. Cliquez sur l'endpoint
2. Cliquez sur **"Try it out"**
3. Remplissez le formulaire :
   - **ticket** : Numéro de ticket (obligatoire)
   - **executionType** : Type d'exécution (optionnel, défaut: `unitaire`)
   - Vos paramètres personnalisés
4. Cliquez sur **"Execute"**
5. Le fichier SQL généré sera téléchargé

---

## Étape 5 : Vérifier le fichier généré

Le fichier SQL généré est sauvegardé dans : `./svn_repo_mock/`

**Format du nom** : `{id}_{executionType}_{timestamp}.sql`

**Exemple** : `update-person-name_unitaire_20241130144019.sql`

### Structure du fichier généré

```sql
-- Fichier de Patch Généré le 2024-11-30T14:40:19
-- Requête: Mise à jour du nom d'une personne
-- ID: update-person-name
-- Ticket: dc905fff-27a6-452f-aa0d-360c6c37b94a
-- Type: unitaire

UPDATE PERSON SET NAME = 'roland' WHERE PERSON_ID = '001';
```

---

## Règles et bonnes pratiques

### Obligatoire
- ✅ Le fichier doit contenir `-- @id:` (sinon erreur au démarrage)
- ✅ L'ID doit être unique (sinon conflit d'endpoints)
- ✅ Les noms de paramètres dans `-- @param:` doivent correspondre aux placeholders `{{nom}}`

### Recommandé
- ✅ Utilisez des noms de fichiers descriptifs : `update-person-name.sql` plutôt que `query1.sql`
- ✅ Ajoutez des tags pertinents pour faciliter le filtrage
- ✅ Documentez bien la description pour les utilisateurs de l'API
- ✅ Testez toujours votre requête SQL avant de l'ajouter

### Types de paramètres

| Type | Remplacement | Exemple |
|------|--------------|---------|
| `text` | `'valeur'` (avec guillemets) | `{{name}}` → `'roland'` |
| `number` | `123` (sans guillemets) | `{{count}}` → `10` |
| `date` | `'2024-01-01'` (avec guillemets) | `{{date}}` → `'2024-11-30'` |

### Placeholders dans différents contextes

```sql
-- Dans une clause WHERE
WHERE person_id = {{person_id}};  -- Devient: WHERE person_id = '001';

-- Dans une variable PL/SQL
v_name VARCHAR2(50) := {{name}};  -- Devient: v_name VARCHAR2(50) := 'roland';

-- Dans une expression
SET count = {{count}};  -- Devient: SET count = 10; (si type=number)
```

---

## Dépannage

### L'endpoint n'apparaît pas dans Swagger
- Vérifiez que le fichier SQL est dans `src/main/resources/sql/`
- Vérifiez que le fichier contient `-- @id:` valide
- Redémarrez l'application
- Consultez les logs pour les erreurs de parsing

### Erreur "Query not found"
- Vérifiez que l'ID dans l'URL correspond au `-- @id:` dans le fichier SQL
- Vérifiez que l'application a bien démarré et chargé le fichier

### Les paramètres ne s'affichent pas correctement
- Vérifiez le format de `-- @param:` : `nom|type|label|required`
- Vérifiez que les noms de paramètres correspondent aux placeholders `{{nom}}`

### Le fichier généré est vide ou incorrect
- Vérifiez que les placeholders utilisent la syntaxe `{{nom_parametre}}` (double accolades)
- Vérifiez que les noms de paramètres correspondent exactement (sensible à la casse)

---

## Résumé rapide

1. ✅ Créer `src/main/resources/sql/mon-requete.sql`
2. ✅ Ajouter les métadonnées (`-- @id:`, `-- @name:`, `-- @param:`, etc.)
3. ✅ Écrire la requête SQL avec `{{placeholders}}`
4. ✅ Redémarrer l'application
5. ✅ Tester dans Swagger UI : `http://localhost:8080/swagger-ui.html`

**C'est tout ! Aucune modification de code Java nécessaire.**

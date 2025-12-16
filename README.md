# SQL Patch Generator

Application Spring Boot pour générer dynamiquement des fichiers de patch SQL à partir de templates paramétrés, avec documentation Swagger automatique.

## 🎯 Fonctionnalités

- ✅ **Génération dynamique de patches SQL** à partir de templates
- ✅ **Documentation Swagger automatique** : chaque requête SQL génère un endpoint avec formulaire interactif
- ✅ **Système de tags** : filtrage des endpoints par catégories
- ✅ **Support PL/SQL complet** : blocs DECLARE/BEGIN/END, transactions, logs
- ✅ **Aucune modification de code** : ajoutez simplement un fichier SQL avec métadonnées
- ✅ **Interface utilisateur intuitive** : formulaire Swagger pour chaque endpoint

## 🚀 Démarrage rapide

### Prérequis

- Java 17 ou supérieur
- Maven 3.6+

### Installation

1. **Cloner le repository**
```bash
git clone <url-du-repo>
cd SQLGenerator
```

2. **Compiler le projet**
```bash
mvn clean install
```

3. **Lancer l'application**
```bash
mvn spring-boot:run
```

4. **Accéder à la documentation Swagger**
```
http://localhost:8080/swagger-ui.html
```

## 📖 Utilisation

### Ajouter une nouvelle requête SQL

Le système scanne automatiquement les fichiers SQL dans `src/main/resources/sql/` au démarrage.

**Exemple minimal** : `src/main/resources/sql/update-person-name.sql`

```sql
-- @id: update-person-name
-- @name: Mise à jour du nom d'une personne
-- @description: Met à jour le nom d'une personne dans la table PERSON.
-- @tags: person,update,unitaire
-- @param: person_id|text|ID Personne|true
-- @param: name|text|Nom|true

UPDATE PERSON SET NAME = {{name}} WHERE PERSON_ID = {{person_id}};
```

**Résultat** :
- ✅ Endpoint automatique : `POST /api/patch/update-person-name`
- ✅ Formulaire Swagger avec champs `ticket`, `executionType`, `person_id`, `name`
- ✅ Fichier SQL généré dans `./svn_repo_mock/`

📚 **Guide complet** : Voir [GUIDE_AJOUT_REQUETE.md](GUIDE_AJOUT_REQUETE.md)

### Utiliser l'API

#### Via Swagger UI (recommandé)

1. Ouvrez `http://localhost:8080/swagger-ui.html`
2. Trouvez votre endpoint (filtrez par tags si nécessaire)
3. Cliquez sur **"Try it out"**
4. Remplissez le formulaire
5. Cliquez sur **"Execute"**
6. Le fichier SQL est téléchargé

#### Via requête HTTP directe

```bash
curl -X POST "http://localhost:8080/api/patch/update-person-name" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "ticket=dc905fff-27a6-452f-aa0d-360c6c37b94a&person_id=001&name=roland&executionType=unitaire"
```

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Application Start                     │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              QueryService.init()                         │
│  • Scan fichiers SQL (resources/sql/*.sql)              │
│  • Parse métadonnées via QueryMetadataParser            │
│  • Charge QueryDefinition en mémoire                    │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│         PatchOpenApiCustomizer                           │
│  • Génère endpoints Swagger dynamiquement               │
│  • Crée formulaires pour chaque requête                 │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              Swagger UI disponible                       │
│         http://localhost:8080/swagger-ui.html            │
└─────────────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│         POST /api/patch/{id}                            │
│  • PatchController reçoit les paramètres                │
│  • QueryService génère le fichier SQL                   │
│  • Retourne le fichier en téléchargement                │
└─────────────────────────────────────────────────────────┘
```

## 📁 Structure du projet

```
SQLGenerator/
├── src/main/
│   ├── java/com/sqlgenerator/backend/
│   │   ├── config/
│   │   │   ├── OpenApiConfig.java              # Configuration Swagger
│   │   │   └── PatchOpenApiCustomizer.java     # Génération dynamique endpoints
│   │   ├── controller/
│   │   │   └── PatchController.java            # Endpoint REST générique
│   │   ├── model/
│   │   │   ├── QueryDefinition.java            # Modèle requête
│   │   │   └── ParameterDefinition.java       # Modèle paramètre
│   │   ├── service/
│   │   │   ├── QueryService.java               # Service principal
│   │   │   └── QueryMetadataParser.java       # Parser métadonnées SQL
│   │   └── SqlGeneratorApplication.java        # Point d'entrée
│   └── resources/
│       ├── sql/                                # Templates SQL
│       │   ├── update-person-name.sql
│       │   └── update-cedem-role.sql
│       └── application.properties              # Configuration Spring
├── svn_repo_mock/                              # Fichiers générés
├── GUIDE_AJOUT_REQUETE.md                      # Guide d'utilisation
└── README.md                                   # Ce fichier
```

## 🔧 Configuration

### Port de l'application

Modifier dans `src/main/resources/application.properties` :

```properties
server.port=8080
```

### Configuration Swagger

Les options Swagger sont dans `application.properties` :

```properties
springdoc.swagger-ui.operations-sorter=method
springdoc.swagger-ui.tags-sorter=alpha
springdoc.swagger-ui.filter=true
```

## 📝 Format des métadonnées SQL

Chaque fichier SQL doit commencer par des métadonnées en commentaires :

```sql
-- @name: Nom descriptif                 # Affiché dans Swagger
-- @description: Description détaillée   # Affiché dans Swagger
-- @tags: tag1,tag2,tag3                 # Pour le filtrage
-- @param: nom|type|label|required      # Définition paramètre
```

### Types de paramètres

- `text` : Chaîne de caractères → `'valeur'`
- `number` : Nombre → `123`
- `date` : Date → `'2024-01-01'`

### Placeholders

Utilisez `{{nom_parametre}}` dans votre SQL :

```sql
UPDATE table SET colonne = {{name}} WHERE id = {{id}};
```

## 🧪 Exemples

### Exemple 1 : Requête simple

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

### Exemple 2 : Bloc PL/SQL avec transaction

Voir `src/main/resources/sql/update-cedem-role.sql` pour un exemple complet avec :
- Bloc DECLARE/BEGIN/END
- Logs avant/après
- Gestion d'erreurs avec ROLLBACK
- Commit conditionnel

## 🐛 Dépannage

### L'endpoint n'apparaît pas dans Swagger

- ✅ Vérifiez que le fichier SQL est dans `src/main/resources/sql/`
- ✅ Vérifiez que le nom du fichier se termine par `.sql` (l'ID sera extrait automatiquement)
- ✅ Redémarrez l'application
- ✅ Consultez les logs pour les erreurs de parsing

### Erreur 404 "Query not found"

- ✅ Vérifiez que l'ID dans l'URL correspond au `-- @id:` (si présent) ou au nom du fichier sans `.sql`
- ✅ Vérifiez que l'application a bien démarré

### Erreur 415 "Unsupported Media Type"

- ✅ Utilisez `Content-Type: application/x-www-form-urlencoded`
- ✅ Envoyez les paramètres en format formulaire, pas en JSON

## 📊 Statistiques du projet

- **Fichiers Java** : 8 fichiers (~518 lignes)
- **Fichiers SQL** : 2 templates (exemples)
- **Configuration** : Minimaliste et claire

## 🤝 Contribution

1. Créez un fichier SQL dans `src/main/resources/sql/`
2. Ajoutez les métadonnées requises
3. Testez dans Swagger UI
4. Le système génère automatiquement l'endpoint !

## 📄 Licence

GeoInc.com

## 👥 Auteurs

Équipe SQL Generator

---

**Documentation complète** : Voir [GUIDE_AJOUT_REQUETE.md](GUIDE_AJOUT_REQUETE.md)


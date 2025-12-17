# 🔧 SQL Generator

**Générateur de scripts SQL paramétrés avec API REST et documentation Swagger automatique.**

---

## 📋 Description

SQL Generator est un outil qui transforme vos fichiers SQL paramétrés en API REST documentée automatiquement. Ajoutez simplement un fichier `.sql` avec des métadonnées, et obtenez instantanément un endpoint Swagger prêt à l'emploi.

### ✨ Fonctionnalités

- **📝 Définition simple** : Écrivez vos requêtes SQL avec des métadonnées dans les commentaires
- **🚀 API REST automatique** : Un endpoint par fichier SQL
- **📖 Documentation Swagger** : Interface de test intégrée
- **🔄 Modes d'exécution** :
  - **Unitaire** : 1 requête → 1 script SQL
  - **Masse** : 1 fichier CSV → N scripts SQL (1 par ligne)
- **📊 Gestion des clauses IN** : Lotissement automatique (batches de 999 pour Oracle)
- **💾 Scripts horodatés** : Sortie dans `Cf fichier properties` avec timestamp
- **🔍 Validation** : Vérification des placeholders au démarrage

---

## 🚀 Démarrage Rapide

### 1️⃣ Prérequis

- Java 17+
- Maven 3.8+

### 2️⃣ Lancement

```bash
mvn spring-boot:run
```

L'application démarre sur **http://localhost:8080** < cf properties>

### 3️⃣ Accès Swagger

Ouvrez votre navigateur : **http://localhost:8080/swagger-ui/index.html**

**Note** : Avec SpringDoc OpenAPI 2.2.0, l'URL est `/swagger-ui/index.html` (et non `/swagger-ui.html`)

---

## 🏗️ Architecture du Domaine SQLGENERATOR

Le domaine SQLGENERATOR est organisé en **trois sous-domaines distincts**, chacun ayant une responsabilité claire et des clients spécifiques :

### 📋 Sous-domaine "Form Schema" (`FormSchemaController`)

**Responsabilité** : Exposer les schémas de formulaires construits à partir des templates SQL.

- **Ressources** : `FormSchema` (schémas JSON pour le frontend, dérivés des `TemplateDefinition`)
- **Opérations** : GET uniquement (lecture des schémas)
- **Clients** : Frontend (construction de formulaires dynamiques)
- **Endpoints** :
  - `GET /api/forms` : Liste tous les schémas de formulaires disponibles
  - `GET /api/forms/{id}` : Schéma de formulaire complet pour un template
  - `GET /api/forms/{id}/request-body` : JSON prêt à copier-coller pour tests manuels

**Justification** : Séparation claire entre l'**exposition des schémas de formulaires** (métadonnées pour construire les formulaires) et la **génération de ressources** (scripts SQL). Le frontend a besoin uniquement des schémas pour construire les formulaires, sans déclencher de génération.

### ⚙️ Sous-domaine "Script Generation" (`ScriptController`)

**Responsabilité** : Générer des scripts SQL exécutables à partir de templates et de paramètres.

- **Ressources** : Script (fichier SQL généré, téléchargeable)
- **Opérations** : POST (création de ressource)
- **Clients** : Frontend, outils externes, intégrations
- **Endpoints** :
  - `POST /api/scripts/{id}` : Génération mode unitaire (ou avec clause IN)
  - `POST /api/scripts/{id}/masse` : Génération mode masse (CSV → N scripts)

**Justification** : Conforme aux principes REST : **POST = création de ressource**. Le script SQL généré est une ressource nouvelle, créée à la demande. Cette séparation permet d'évoluer indépendamment la logique de génération (batching, formatage, etc.) sans impacter le catalogue.

### 🔧 Sous-domaine "Administration" (`AdminController`)

**Responsabilité** : Opérations d'administration et tests d'intégration système.

- **Opérations** : POST (opérations d'administration)
- **Clients** : DevOps, administrateurs, pipelines CI/CD
- **Endpoints** :
  - `POST /api/admin/integration-test` : Tests d'intégration complets

**Justification** : Séparation des **opérations d'administration** (tests, validations système) des opérations métier (exposition de schémas, génération de scripts). Les clients sont différents (opérateurs vs utilisateurs finaux), les permissions peuvent être distinctes, et l'évolution est indépendante.

### 🎯 Avantages de cette Architecture

1. **Séparation des responsabilités** : Chaque contrôleur a un contrat clair et isolé
2. **Séparation des clients** : Frontend (Templates/Scripts) vs DevOps (Admin)
3. **Séparation des contrats** : JSON (Templates) vs Fichiers binaires (Scripts) vs Opérations (Admin)
4. **Évolutivité** : Chaque sous-domaine peut évoluer indépendamment
5. **Testabilité** : Tests unitaires isolés par responsabilité
6. **Alignement REST** : Templates = ressources de catalogue (GET), Scripts = création de ressources (POST)

---

## 📂 Structure du Projet

```
SQLGenerator/
├── src/main/
│   ├── java/com/sqlgenerator/backend/
│   │   ├── controller/
│   │   │   ├── FormSchemaController.java  # Sous-domaine "Form Schema"
│   │   │   ├── ScriptController.java       # Sous-domaine "Script Generation"
│   │   │   └── AdminController.java        # Sous-domaine "Administration"
│   │   ├── service/
│   │   │   ├── TemplateService.java       # Gestion des templates
│   │   │   ├── TemplateMetadataParser.java # Parsing des métadonnées SQL
│   │   │   ├── FormSchemaService.java     # Génération des schémas pour le front
│   │   │   ├── SqlFileBuilder.java        # Construction des fichiers SQL
│   │   │   └── TemplateConstants.java     # Constantes centralisées
│   │   └── model/
│   │       ├── TemplateDefinition.java    # Modèle d'un template SQL
│   │       ├── ParameterDefinition.java   # Modèle d'un paramètre
│   │       ├── FormSchema.java            # Schéma pour le frontend
│   │       └── FormField.java             # Champ de formulaire
│   └── resources/
│       └── templates/                     # ← Vos fichiers SQL ici
│           ├── update-person-name.sql
│           └── activate-contrats.sql
└── sql_uploads/                           # Upload de nouveaux templates (futur)
```

---

## 📝 Créer un Template SQL

### Exemple Minimal

**Fichier** : `src/main/resources/templates/update-person-name.sql`

```sql
-- @id: update-person-name
-- @name: Mise à jour du nom d'une personne
-- @description: Met à jour le nom d'une personne dans la table PERSON.
-- @tags: person, update
-- @param: person_id | ID de la personne | number | required
-- @param: name | Nouveau nom | text | required

UPDATE PERSON
SET NAME = '{{name}}'
WHERE PERSON_ID = {{person_id}};
```

### Métadonnées Disponibles

| Métadonnée | Description | Obligatoire |
|------------|-------------|-------------|
| `@id` | Identifiant unique (déduit du nom de fichier si absent) | ❌ |
| `@name` | Nom lisible du template | ✅ |
| `@description` | Description fonctionnelle | ❌ |
| `@tags` | Tags séparés par des virgules | ❌ |
| `@param` | Définition d'un paramètre : `nom \| label \| type \| required/optional` | ✅ (si placeholders) |
| `@param-file` | Paramètre fichier pour clause IN : `nom \| label \| required` | ❌ |

### Types de Paramètres

- `text` : Texte libre
- `number` : Nombre (integer/decimal)
- `date` : Date (format `DD/MM/YY`)

### Placeholders SQL

Utilisez `{{nom_parametre}}` dans votre SQL. Ils seront remplacés automatiquement par les valeurs fournies.

---

## 🔌 Utilisation de l'API

### 1️⃣ Lister les Schémas de Formulaires Disponibles

```http
GET /api/forms
```

**Réponse** : Liste des schémas de formulaires avec leurs métadonnées (modes unitaire/masse, champs, etc.)

### 2️⃣ Obtenir un Schéma de Formulaire Spécifique

```http
GET /api/forms/{id}
```

**Réponse** : Schéma complet du formulaire (pour construire un formulaire dynamique)

### 3️⃣ Obtenir le JSON de Test

```http
GET /api/forms/{id}/request-body?mode=unitaire
GET /api/forms/{id}/request-body?mode=masse
```

**Réponse** : JSON prêt à copier-coller dans Swagger pour tester

### 4️⃣ Générer un Script SQL (Mode Unitaire)

```http
POST /api/scripts/{id}
Content-Type: application/json

{
  "ticket": "JIRA-123",
  "executionType": "unitaire",
  "person_id": "42",
  "name": "Jean Dupont"
}
```

**Réponse** : Téléchargement du fichier SQL généré

### 5️⃣ Générer un Script SQL (Mode Masse)

```http
POST /api/scripts/{id}/masse
Content-Type: multipart/form-data

ticket=JIRA-123
masseFile=<fichier.csv>
```

**Fichier CSV** (1 ligne = 1 requête) :

```
42,Jean Dupont
43,Marie Martin
44,Pierre Durand
```

---

## 🧪 Tests

Exécuter les tests unitaires :

```bash
mvn test
```

Exécuter un test spécifique :

```bash
mvn test -Dtest=TemplateMetadataParserTest
```

---

## 📚 Documentation Complémentaire

### Diagramme de Contexte (Domain-Driven Design)

```
┌──────────────────────────────────────────────────────────────┐
│                    Domaine SQLGENERATOR                      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐  ┌──────────────────┐   ┌───────────┐  │
│  │ Template Catalog │  │ Script Generation│   │ Template  │  │
│  │  (TemplateCtrl)  │  │  (ScriptCtrl)    │   │Management │  │
│  │                  │  │                  │   │(AdminCtrl)│  │
│  │ GET /templates   │  │ POST /scripts    │   │POST /admin│  │
│  │                  │  │                  │   │           │  │
│  │ Ressources:      │  │ Ressources:      │   │ Opérations│  │
│  │ - FormSchema     │  │ - Script (file)  │   │ - Upload  │  │
│  │ - TemplateDef    │  │                  │   │ - Tests   │  │
│  └────────┬─────────┘  └────────┬─────────┘   └─────┬─────┘  │
│           │                     │                   │        │
└───────────┼─────────────────────┼───────────────────┼────────┘
            │                     │                   │
    ┌───────▼───────┐    ┌────────▼────────┐  ┌───────▼──────┐
    │   Frontend    │    │ Outils externes │  │   DevOps     │
    │ (Formulaires) │    │ (Intégrations)  │  │ (Maintenance)│
    └───────────────┘    └─────────────────┘  └──────────────┘
```

**Légende** :
- **Form Schema** : Fournit les schémas de formulaires pour construire les interfaces
- **Script Generation** : Crée les ressources Script (fichiers SQL générés)
- **Administration** : Opérations d'administration et tests d'intégration système

---

## 🛠️ Configuration

### Changer le Port

Modifier `src/main/resources/application.properties` :

```properties
server.port=8081
```

### Changer le Répertoire de Sortie

Modifier `TemplateConstants.OUTPUT_SCRIPTS_PATH` :

```java
public static final String OUTPUT_SCRIPTS_PATH = "./mon_repertoire/";
```

---

## 🤝 Contribution

1. Créer une branche feature : `git checkout -b feature/ma-fonctionnalite`
2. Commit : `git commit -m "feat: ajout de ma fonctionnalité"`
3. Push : `git push origin feature/ma-fonctionnalite`
4. Créer une Merge Request

---

## 📄 Licence

Ce projet est sous licence 4

---

## 🆘 Dépannage

### Le serveur ne démarre pas

```bash
# Vérifier si le port 8080 est déjà utilisé
Get-NetTCPConnection -LocalPort <PORT du fichier propertie>

# Tuer le processus
Stop-Process -Id <PID> -Force
```

### Les templates ne sont pas chargés

Vérifier que vos fichiers `.sql` sont dans `src/main/resources/templates/` et respectent le format des métadonnées.

### Erreur "Placeholder not defined"

Tous les `{{placeholders}}` dans le SQL doivent avoir une ligne `@param` ou `@param-file` correspondante.

---

**Développé avec amnégation pour simplifier la génération de scripts SQL**

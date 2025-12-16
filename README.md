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
- **💾 Scripts horodatés** : Sortie dans `./svn_repo_mock/` avec timestamp
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

L'application démarre sur **http://localhost:8080**

### 3️⃣ Accès Swagger

Ouvrez votre navigateur : **http://localhost:8080/swagger-ui/index.html**

**Note** : Avec SpringDoc OpenAPI 2.2.0, l'URL est `/swagger-ui/index.html` (et non `/swagger-ui.html`)

---

## 📂 Structure du Projet

```
SQLGenerator/
├── src/main/
│   ├── java/com/sqlgenerator/backend/
│   │   ├── controller/
│   │   │   ├── TemplateController.java    # API des templates (GET /api/templates)
│   │   │   └── ScriptController.java      # API de génération (POST /api/scripts/{id})
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

### 1️⃣ Lister les Templates Disponibles

```http
GET /api/templates
```

**Réponse** : Liste des templates avec leurs schémas (modes unitaire/masse, champs, etc.)

### 2️⃣ Obtenir un Template Spécifique

```http
GET /api/templates/{id}
```

**Réponse** : Schéma complet du template (pour construire un formulaire dynamique)

### 3️⃣ Obtenir le JSON de Test

```http
GET /api/templates/{id}/request-body?mode=unitaire
GET /api/templates/{id}/request-body?mode=masse
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

- **[GUIDE_AJOUT_REQUETE.md](GUIDE_AJOUT_REQUETE.md)** : Guide détaillé pour ajouter un nouveau template
- **[FLOWCHART.md](FLOWCHART.md)** : Diagramme du flux de traitement
- **[EXPORT_OPENAPI_SCHEMA.md](EXPORT_OPENAPI_SCHEMA.md)** : Exporter le schéma OpenAPI
- **[COMMANDES_PORT_8080.md](COMMANDES_PORT_8080.md)** : Gérer les processus sur le port 8080

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
4. Créer une Pull Request

---

## 📄 Licence

Ce projet est sous licence MIT.

---

## 🆘 Dépannage

### Le serveur ne démarre pas

```bash
# Vérifier si le port 8080 est déjà utilisé
Get-NetTCPConnection -LocalPort 8080

# Tuer le processus
Stop-Process -Id <PID> -Force
```

Voir [COMMANDES_PORT_8080.md](COMMANDES_PORT_8080.md) pour plus de détails.

### Les templates ne sont pas chargés

Vérifier que vos fichiers `.sql` sont dans `src/main/resources/templates/` et respectent le format des métadonnées.

### Erreur "Placeholder not defined"

Tous les `{{placeholders}}` dans le SQL doivent avoir une ligne `@param` ou `@param-file` correspondante.

---

**Développé avec ❤️ pour simplifier la génération de scripts SQL**

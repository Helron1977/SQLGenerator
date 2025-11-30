# Configuration Git et GitHub

## Étape 1 : Vérifier les fichiers à committer

```bash
git status
```

Vérifiez que les fichiers suivants sont bien ignorés (ne doivent PAS apparaître) :
- `target/` (compilation Maven)
- `svn_repo_mock/` (fichiers générés)
- `*.class` (fichiers compilés)
- `src.7z` (archive)

## Étape 2 : Ajouter les fichiers au staging

```bash
git add .
```

Ou sélectivement :
```bash
git add .gitignore
git add README.md
git add GUIDE_AJOUT_REQUETE.md
git add FLOWCHART.md
git add pom.xml
git add src/
```

## Étape 3 : Faire le premier commit

```bash
git commit -m "Initial commit: SQL Patch Generator avec support mode masse et IN"
```

Ou avec un message plus détaillé :
```bash
git commit -m "Initial commit: SQL Patch Generator

- Génération dynamique de patches SQL à partir de templates
- Support mode unitaire et masse
- Support clauses IN avec lotissement automatique (>999 valeurs)
- Documentation Swagger automatique
- Upload de fichiers CSV pour mode masse
- Format de date DD/MM/YY
- Gestion des valeurs NULL"
```

## Étape 4 : Créer le repository sur GitHub

1. Aller sur https://github.com
2. Cliquer sur le bouton **"+"** en haut à droite → **"New repository"**
3. Remplir :
   - **Repository name** : `SQLGenerator` (ou le nom de votre choix)
   - **Description** : "Générateur de patches SQL dynamique avec documentation Swagger automatique"
   - **Visibility** : Public ou Private (selon votre choix)
   - **NE PAS** cocher "Initialize with README" (vous avez déjà un README)
4. Cliquer sur **"Create repository"**

## Étape 5 : Lier le repository local à GitHub

GitHub vous donnera des commandes. Utilisez celles pour un repository existant :

```bash
git remote add origin https://github.com/VOTRE_USERNAME/SQLGenerator.git
git branch -M main
git push -u origin main
```

**Note** : Remplacez `VOTRE_USERNAME` par votre nom d'utilisateur GitHub.

## Étape 6 : Vérifier

Allez sur votre repository GitHub et vérifiez que tous les fichiers sont bien présents.

## Commandes utiles pour la suite

### Voir les changements
```bash
git status
```

### Ajouter des fichiers modifiés
```bash
git add .
```

### Faire un commit
```bash
git commit -m "Description des changements"
```

### Pousser vers GitHub
```bash
git push
```

### Voir l'historique
```bash
git log --oneline
```

## Structure du repository

```
SQLGenerator/
├── .gitignore          # Fichiers à ignorer
├── README.md           # Documentation principale
├── GUIDE_AJOUT_REQUETE.md  # Guide d'utilisation
├── FLOWCHART.md        # Diagramme de flux
├── pom.xml             # Configuration Maven
└── src/                # Code source
    └── main/
        ├── java/       # Code Java
        └── resources/  # Ressources (SQL, config)
```

## Fichiers exclus du repository

Les fichiers suivants sont automatiquement ignorés grâce au `.gitignore` :
- `target/` : Fichiers compilés Maven
- `svn_repo_mock/` : Fichiers SQL générés
- `*.class` : Fichiers compilés Java
- `*.7z`, `*.zip` : Archives
- Fichiers de test temporaires


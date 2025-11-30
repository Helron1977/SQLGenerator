# Flowchart - SQL Patch Generator

## Architecture fonctionnelle principale

```mermaid
flowchart TD
    Start([Démarrage Application]) --> Init[QueryService.init]
    
    Init --> Scan[Scanner fichiers SQL<br/>resources/sql/*.sql]
    Scan --> Parse[QueryMetadataParser<br/>Parser métadonnées]
    Parse --> Load[Charger QueryDefinition<br/>id, name, params, tags]
    Load --> Store[Stocker en mémoire]
    
    Store --> Swagger[PatchOpenApiCustomizer<br/>Générer endpoints Swagger]
    Swagger --> Doc[Documentation Swagger UI<br/>disponible]
    
    Doc --> Request{Requête HTTP<br/>POST /api/patch/{id}}
    
    Request --> Controller[PatchController<br/>Recevoir paramètres]
    Controller --> Validate[Valider query ID]
    Validate -->|Non trouvé| Error404[404 Not Found]
    Validate -->|Trouvé| Extract[Extraire paramètres<br/>ticket, executionType, params]
    
    Extract --> Generate[QueryService<br/>generatePatchFile]
    
    Generate --> LoadSQL[Charger fichier SQL]
    LoadSQL --> RemoveMeta[Retirer métadonnées<br/>-- @...]
    RemoveMeta --> Replace[Remplacer placeholders<br/>{{param}}]
    Replace --> BuildHeader[Construire en-tête<br/>date, ticket, type]
    BuildHeader --> Write[Écrire fichier<br/>svn_repo_mock/]
    
    Write --> Return[Retourner fichier SQL<br/>ResponseEntity]
    Return --> End([Fin])
    Error404 --> End
    
    style Start fill:#90EE90
    style End fill:#FFB6C1
    style Init fill:#87CEEB
    style Generate fill:#FFD700
    style Swagger fill:#DDA0DD
    style Controller fill:#F0E68C
```

## Briques fonctionnelles

### 1. **Initialisation** (au démarrage)
- **QueryService.init()** : Scanne et charge les fichiers SQL
- **QueryMetadataParser** : Parse les métadonnées (`-- @id`, `-- @param`, etc.)
- **PatchOpenApiCustomizer** : Génère dynamiquement la documentation Swagger

### 2. **Traitement des requêtes**
- **PatchController** : Point d'entrée REST (`/api/patch/{id}`)
- **QueryService.generatePatchFile()** : Génère le fichier SQL final
  - Charge le template SQL
  - Retire les métadonnées
  - Remplace les placeholders
  - Ajoute l'en-tête
  - Écrit le fichier

### 3. **Modèles de données**
- **QueryDefinition** : Définition d'une requête (id, name, params, tags)
- **ParameterDefinition** : Définition d'un paramètre (name, type, label, required)


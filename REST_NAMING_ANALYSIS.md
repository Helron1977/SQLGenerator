# Analyse du nommage : FormController vs QueryController

## Problème identifié

Le contrôleur s'appelle `FormController` avec le path `/api/forms`, mais il expose en réalité :

1. **Des queries SQL** (QueryDefinition) avec leurs métadonnées
2. **Des schémas de requêtes** (FormSchema) pour construire les formulaires
3. **Des templates de body** (request-schema) pour les requêtes

## Pourquoi "forms" est problématique ?

### ❌ "forms" est une abstraction UI, pas une ressource métier

- "Form" = concept de présentation (frontend)
- Ce n'est pas ce que l'API expose réellement
- En REST, les ressources doivent représenter le **domaine métier**, pas l'implémentation

### ✅ Ce que l'API expose vraiment

- **Queries** : Les requêtes SQL disponibles
- **Query schemas** : Les schémas pour construire les requêtes
- **Request templates** : Les templates JSON pour les body

## Alternatives REST

### Option 1 : `/api/queries` (recommandée)

```
GET /api/queries                    → Liste des queries disponibles
GET /api/queries/{id}               → Schéma d'une query spécifique
GET /api/queries/{id}/request-schema → Template JSON pour le body
```

**Avantages** :
- ✅ Représente la ressource métier réelle (les queries SQL)
- ✅ Cohérent avec le domaine métier
- ✅ Clair et explicite
- ✅ Aligné avec `QueryDefinition`, `QueryService`, etc.

**Changements nécessaires** :
- `FormController` → `QueryController`
- `/api/forms` → `/api/queries`
- `FormSchema` → `QuerySchema` (optionnel, peut rester pour le front)
- `FormSchemaService` → `QuerySchemaService` (optionnel)

### Option 2 : `/api/query-schemas`

```
GET /api/query-schemas              → Liste des schémas
GET /api/query-schemas/{id}         → Schéma d'une query
GET /api/query-schemas/{id}/request-schema
```

**Avantages** :
- ✅ Plus précis sur ce qui est exposé (les schémas)
- ✅ Séparation claire : queries vs schémas

**Inconvénients** :
- ❌ Plus long
- ❌ Moins naturel

### Option 3 : Garder `/api/forms` mais renommer le contrôleur

**Justification** : Si on considère que l'API expose des "formulaires" comme ressource métier (même si c'est une abstraction).

**Problème** : Pas vraiment REST-compliant car "form" n'est pas une ressource métier.

## Recommandation

**Option 1 : `/api/queries`** est la meilleure option car :

1. **Ressource métier claire** : Les queries SQL sont la ressource réelle
2. **Cohérence** : Aligné avec `QueryDefinition`, `QueryService`, `QueryMetadataParser`
3. **Simplicité** : Plus court et naturel
4. **REST-compliant** : Représente le domaine métier, pas l'implémentation

## Impact des changements

### Fichiers à modifier

1. **FormController.java** → `QueryController.java`
   - Renommer la classe
   - Changer `@RequestMapping("/api/forms")` → `@RequestMapping("/api/queries")`

2. **FormSchemaService.java** → Optionnel, peut rester (c'est un service interne)

3. **FormSchema.java** → Optionnel, peut rester (c'est un DTO pour le front)

4. **Documentation** : Mettre à jour les références dans Swagger, README, etc.

### Rétrocompatibilité

Si nécessaire, on peut garder `/api/forms` en redirection vers `/api/queries` pendant une période de transition.

## Conclusion

**"forms" est un mauvais nom** car il représente une abstraction UI, pas la ressource métier.

**`/api/queries` est le nom REST-compliant** car il représente la ressource métier réelle : les requêtes SQL.


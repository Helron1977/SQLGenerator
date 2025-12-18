# 🔗 Guide d'Intégration SQL Generator

Ce document décrit comment intégrer le module SQL Generator dans un projet Spring Boot plus grand.

## 📋 Prérequis

- Spring Boot 3.2.0+
- Java 17+
- Le projet parent doit gérer :
  - La configuration Swagger/OpenAPI (optionnel)
  - La configuration CORS (optionnel)
  - La sécurité/authentification (optionnel)

## 🔧 Configuration

### 1. Propriétés

Le module utilise le préfixe `sql.generator` pour isoler ses propriétés :

```properties
# Répertoire de sortie des scripts SQL générés
sql.generator.output.path=./svn_repo_mock/

# Désactiver Swagger si le projet parent le gère
sql.generator.swagger.enabled=false
```

**Note** : Si le projet parent a sa propre gestion de properties, ces valeurs peuvent être surchargées dans le `application.properties` du parent.

### 2. Swagger/OpenAPI

#### Si le projet parent gère Swagger

1. Désactiver Swagger du module :
```properties
sql.generator.swagger.enabled=false
```

2. Les annotations `@Tag` dans les contrôleurs seront toujours présentes mais n'affecteront pas la configuration du parent.

3. Le projet parent peut grouper les endpoints SQL Generator dans sa propre configuration Swagger.

#### Si le projet parent ne gère pas Swagger

Le module utilise `springdoc-openapi-starter-webmvc-ui` par défaut. Les endpoints seront documentés automatiquement.

**Dépendance Maven** (si le parent gère Swagger, cette dépendance peut être exclue) :
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

### 3. CORS

Les contrôleurs n'ont **pas** de configuration CORS hardcodée. Le projet parent doit gérer CORS globalement via :

- `@CrossOrigin` sur un `@Configuration` global
- `CorsConfiguration` dans Spring Security
- Configuration dans un `WebMvcConfigurer`

### 4. Sécurité/Authentification

Le module **n'inclut aucune configuration de sécurité**. Le projet parent doit :

- Configurer Spring Security
- Ajouter des filtres d'authentification
- Protéger les endpoints `/api/admin/*` (opérations d'administration)
- Laisser `/api/forms/*` et `/api/scripts/*` accessibles selon les besoins

**Recommandation** : Protéger `/api/admin/*` avec des rôles administrateur.

## 📦 Structure des Packages

Le module est isolé dans le package `com.sqlgenerator.backend` :

```
com.sqlgenerator.backend/
├── config/          # Configuration (AppProperties, AutoConfiguration)
├── controller/     # Contrôleurs REST
├── service/        # Services métier
└── model/          # Modèles de données
```

Aucun conflit de package avec le projet parent si celui-ci utilise un package différent.

## 🔌 Endpoints Exposés

### Form Schema
- `GET /api/forms` : Liste des schémas de formulaires
- `GET /api/forms/{id}` : Schéma d'un formulaire
- `GET /api/forms/{id}/request-body` : JSON de test

### Script Generation
- `POST /api/scripts/{id}` : Génération mode unitaire
- `POST /api/scripts/{id}/masse` : Génération mode masse

### Administration
- `POST /api/admin/integration-test` : Tests d'intégration
- `POST /api/admin/upload-template` : Upload de template SQL

## 🎯 Points d'Attention

1. **Templates SQL** : Les fichiers SQL doivent être dans `src/main/resources/templates/` ou dans un répertoire accessible via classpath.

2. **Répertoire de sortie** : Le répertoire `sql.generator.output.path` doit être accessible en écriture.

3. **Logging** : Le module utilise SLF4J. Le projet parent peut configurer le logging via `logging.level.com.sqlgenerator.backend`.

4. **Dépendances** : Le module nécessite :
   - `spring-boot-starter-web`
   - `jackson-databind` (pour la sérialisation JSON)
   - `springdoc-openapi-starter-webmvc-ui` (optionnel si Swagger désactivé)

## 📝 Exemple d'Intégration

### Dans le projet parent

1. **Ajouter la dépendance** :
```xml
<dependency>
    <groupId>com.sqlgenerator</groupId>
    <artifactId>backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

2. **Configurer les propriétés** :
```properties
# application.properties du parent
sql.generator.output.path=/var/app/sql-scripts/
sql.generator.swagger.enabled=false
```

3. **Configurer CORS globalement** :
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://frontend.example.com")
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
    }
}
```

4. **Protéger les endpoints admin** :
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/**").permitAll()
        );
        return http.build();
    }
}
```

## ✅ Checklist d'Intégration

- [ ] Propriétés configurées avec préfixe `sql.generator`
- [ ] Swagger désactivé si le parent le gère (`sql.generator.swagger.enabled=false`)
- [ ] CORS configuré globalement dans le parent
- [ ] Sécurité configurée pour protéger `/api/admin/*`
- [ ] Répertoire de sortie accessible en écriture
- [ ] Templates SQL disponibles dans classpath ou répertoire configuré
- [ ] Tests d'intégration vérifiés via `POST /api/admin/integration-test`

## 🎨 Frontend

Le module inclut un frontend React modulaire prêt pour l'intégration.

### Structure

Le frontend est dans le répertoire `frontend/` :
- **Composant principal** : `SqlGeneratorApp` (sans en-tête ni navbar)
- **Layout** : Colonne gauche (1/3) pour la liste des templates + Zone droite (2/3) pour le formulaire
- **Formulaires dynamiques** : Génération automatique depuis les schémas backend
- **Modes supportés** : Unitaire et Masse

### Installation

```bash
cd frontend
npm install
npm run dev  # Développement
npm run build # Production
```

### Intégration dans le projet parent

#### Option 1 : Import comme module (recommandé)

```tsx
import { SqlGeneratorApp } from '@sqlgenerator/frontend';

function App() {
  return (
    <div>
      <Header /> {/* Géré par le parent */}
      <Sidebar /> {/* Géré par le parent */}
      <main>
        <SqlGeneratorApp /> {/* Module SQL Generator */}
      </main>
    </div>
  );
}
```

#### Option 2 : Copier les composants

Copier le contenu de `frontend/src/` dans le projet parent et adapter les imports.

#### Option 3 : Iframe (simple mais moins flexible)

```html
<iframe 
  src="http://localhost:5173" 
  style="width: 100%; height: 100vh; border: none;"
></iframe>
```

### Configuration

#### Variable d'environnement

Définir `VITE_API_BASE_URL` pour pointer vers le backend :

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

Par défaut, utilise `/api` (URL relative).

#### Proxy de développement

Le `vite.config.ts` inclut un proxy pour `/api` vers `http://localhost:8080`.

### Points d'attention Frontend

1. **Pas d'en-tête/navbar** : Le composant `SqlGeneratorApp` ne contient que le contenu principal
2. **Styles isolés** : Préfixes de classe (`sql-generator-`, `template-list-`, `dynamic-form-`) pour éviter les conflits
3. **Layout flexible** : S'adapte à la taille du conteneur parent
4. **CORS** : Doit être configuré côté backend (ou via proxy)

### Documentation Frontend

Consultez `frontend/README.md` pour plus de détails.

## 🐛 Dépannage

### Conflit Swagger
Si le projet parent a sa propre configuration Swagger et qu'il y a des conflits :
- Mettre `sql.generator.swagger.enabled=false`
- Vérifier que les annotations `@Tag` n'interfèrent pas avec la config du parent

### CORS non fonctionnel
- Vérifier que le parent configure CORS globalement
- Retirer tout `@CrossOrigin` des contrôleurs (déjà fait)
- Vérifier que le frontend peut accéder aux endpoints `/api/*`

### Properties non chargées
- Vérifier que `@ConfigurationProperties` est activé dans le parent
- Vérifier le préfixe `sql.generator` dans application.properties

### Frontend ne charge pas les données
- Vérifier que `VITE_API_BASE_URL` est correctement configuré
- Vérifier que le backend est accessible depuis le frontend
- Vérifier les logs du navigateur (Console) pour les erreurs CORS ou réseau


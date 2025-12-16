# Configuration Spring Security pour SQL Generator

## Problème

Vous obtenez une erreur **403 Forbidden** lors des appels API depuis votre front React.

## Solution

### Option 1 : Si vous avez déjà Spring Security dans votre BFF

Ajoutez ces patterns dans votre `SecurityConfig` existante :

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // ✅ Ajouter ces lignes pour autoriser les endpoints SQL Generator
                .requestMatchers("/api/patch/**").permitAll()
                .requestMatchers("/api/admin/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                
                // Vos autres règles...
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
}
```

### Option 2 : Si vous n'avez pas Spring Security

1. **Ajouter la dépendance** dans `pom.xml` :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

2. **Utiliser la classe `SecurityConfig.java`** que j'ai créée dans `src/main/java/com/sqlgenerator/backend/config/`

### Option 3 : Configuration CORS plus stricte (recommandé pour production)

Si vous voulez limiter les origines autorisées :

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",  // Front React en dev
            "https://votre-domaine.com"  // Front en production
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

Et dans `SecurityConfig` :

```java
http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
```

## Vérification

Après configuration, testez :

```bash
# Test de l'endpoint queries
curl http://localhost:8080/api/patch/queries

# Devrait retourner un JSON avec la liste des queries
```

## Notes importantes

- **CSRF** : J'ai désactivé CSRF dans la config par défaut car c'est une API REST. Si vous avez besoin de CSRF, configurez-le selon vos besoins.

- **Authentification** : Les endpoints sont en `permitAll()` par défaut. Si vous voulez les protéger, remplacez par `.authenticated()` ou `.hasRole("ADMIN")`.

- **CORS** : Le `@CrossOrigin(origins = "*")` sur les controllers fonctionne, mais Spring Security peut bloquer avant. La config Security doit aussi autoriser CORS.


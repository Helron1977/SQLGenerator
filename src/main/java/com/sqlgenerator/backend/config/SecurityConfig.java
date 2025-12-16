package com.sqlgenerator.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration Spring Security pour le SQL Generator.
 * 
 * Cette configuration autorise l'accès public aux endpoints du SQL Generator
 * pour permettre l'utilisation depuis le front React.
 * 
 * Si vous intégrez ce code dans un BFF avec sa propre configuration Security,
 * vous pouvez soit :
 * 1. Supprimer cette classe et ajouter les patterns dans votre SecurityConfig existante
 * 2. Ou garder cette classe et fusionner les configurations
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configure les règles de sécurité HTTP.
     * 
     * Autorise l'accès public à :
     * - /api/patch/** : Tous les endpoints de génération de patches
     * - /api/admin/** : Endpoints d'administration (upload, reload)
     * - /v3/api-docs/** : Documentation OpenAPI
     * - /swagger-ui/** : Interface Swagger UI
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Désactiver CSRF pour les API REST (ou configurer selon vos besoins)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Autoriser les requêtes CORS (utilise la config @CrossOrigin des controllers)
            .cors(cors -> {})
            
            // Configuration des autorisations
            .authorizeHttpRequests(auth -> auth
                // Endpoints SQL Generator - accès public
                .requestMatchers("/api/patch/**").permitAll()
                .requestMatchers("/api/admin/**").permitAll()
                
                // Documentation OpenAPI - accès public
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                
                // Tous les autres endpoints nécessitent une authentification
                // (à adapter selon vos besoins)
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
}


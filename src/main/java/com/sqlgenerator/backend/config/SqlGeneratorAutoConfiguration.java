package com.sqlgenerator.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration automatique du module SQL Generator.
 * 
 * <p>Cette configuration permet de désactiver certaines fonctionnalités
 * si le projet parent les gère déjà.</p>
 * 
 * <p>Propriétés de contrôle :</p>
 * <ul>
 *   <li>{@code sql.generator.swagger.enabled} : Active/désactive Swagger (défaut: true)
 *       Si le projet parent gère Swagger, mettre à false</li>
 * </ul>
 */
@Configuration
public class SqlGeneratorAutoConfiguration {

    /**
     * Configuration conditionnelle pour Swagger.
     * 
     * Si le projet parent gère Swagger/OpenAPI, cette configuration peut être désactivée
     * en mettant {@code sql.generator.swagger.enabled=false} dans application.properties.
     */
    @Configuration
    @ConditionalOnProperty(name = "sql.generator.swagger.enabled", havingValue = "true", matchIfMissing = true)
    public static class SwaggerConfiguration {
        // La configuration Swagger est gérée automatiquement par springdoc-openapi
        // Si le projet parent configure Swagger différemment, désactiver avec :
        // sql.generator.swagger.enabled=false
    }
}


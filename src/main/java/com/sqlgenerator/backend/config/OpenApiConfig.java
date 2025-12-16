package com.sqlgenerator.backend.config;

import com.sqlgenerator.backend.service.QueryService;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class OpenApiConfig {

    @Autowired
    private QueryService queryService;

    /**
     * Configuration OpenAPI pour le SQL Generator.
     * Ce bean est utilisé uniquement par le groupe "sql-generator".
     */
    @Bean
    public OpenAPI sqlGeneratorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SQL Patch Generator API")
                        .version("1.0.0")
                        .description("API pour générer des patches SQL personnalisés. " +
                                "Cette API permet de créer des fichiers de patch SQL avec des paramètres dynamiques.")
                        .contact(new Contact()
                                .name("Équipe SQL Generator")
                                .email("support@sqlgenerator.com"))
                        .license(new License()
                                .name("GeoInc.com")))
                .servers(Collections.emptyList());
    }

    /**
     * Groupe d'API pour le SQL Generator.
     * Inclut uniquement les endpoints /api/patch/* et /api/admin/*
     * Le customizer est ajouté uniquement à ce groupe pour éviter qu'il s'applique au BFF.
     */
    @Bean
    public GroupedOpenApi sqlGeneratorGroup() {
        PatchOpenApiCustomizer customizer = new PatchOpenApiCustomizer();
        customizer.setQueryService(queryService);
        
        return GroupedOpenApi.builder()
                .group("sql-generator")
                .displayName("SQL Generator")
                .pathsToMatch("/api/patch/**", "/api/admin/**")
                .addOpenApiCustomizer(customizer)
                .build();
    }

    /**
     * Groupe par défaut pour le BFF (Backend For Frontend).
     * Inclut tous les autres endpoints (exclut ceux du SQL Generator).
     * 
     * Si votre BFF a déjà sa propre configuration de groupe, vous pouvez supprimer ce bean.
     */
    @Bean
    public GroupedOpenApi bffGroup() {
        return GroupedOpenApi.builder()
                .group("default")
                .displayName("BFF API")
                .pathsToExclude("/api/patch/**", "/api/admin/**")
                .build();
    }
}
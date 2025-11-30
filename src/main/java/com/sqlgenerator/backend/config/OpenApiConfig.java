package com.sqlgenerator.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
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
        // Pas de .servers() - le CSS/JS le masquera
    }

}
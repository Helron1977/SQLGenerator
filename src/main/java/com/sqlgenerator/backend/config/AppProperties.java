package com.sqlgenerator.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Propriétés de l'application chargées depuis application.properties.
 * 
 * Centralise toutes les propriétés configurables pour faciliter la maintenance.
 */
@Component
public class AppProperties {

    /**
     * Répertoire où sont stockés les scripts SQL générés.
     * Configurable via application.properties : sql.generator.output.path
     * Valeur par défaut : ./svn_repo_mock/
     */
    @Value("${sql.generator.output.path:./svn_repo_mock/}")
    private String outputScriptsPath;

    public String getOutputScriptsPath() {
        return outputScriptsPath;
    }
}


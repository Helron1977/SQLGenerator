package com.sqlgenerator.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propriétés de configuration du module SQL Generator.
 * 
 * <p>Ces propriétés sont isolées avec le préfixe {@code sql.generator} pour éviter
 * les conflits lors de l'intégration dans un projet plus grand.</p>
 * 
 * <p>Configuration via application.properties :</p>
 * <pre>
 * sql.generator.output.path=./svn_repo_mock/
 * </pre>
 * 
 * <p>En cas d'intégration dans un projet parent, ces propriétés peuvent être
 * surchargées dans le application.properties du projet parent.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "sql.generator")
public class AppProperties {

    /**
     * Répertoire où sont stockés les scripts SQL générés.
     * Valeur par défaut : ./svn_repo_mock/
     */
    private String outputPath = "./svn_repo_mock/";

    public String getOutputScriptsPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }
}


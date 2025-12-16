package com.sqlgenerator.backend.service;

import com.sqlgenerator.backend.config.AppProperties;
import com.sqlgenerator.backend.model.TemplateDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Service dédié à la construction et à l'écriture des fichiers SQL générés.
 * 
 * Séparation des responsabilités :
 * - TemplateService : logique métier (parsing, remplacement placeholders)
 * - SqlFileBuilder : génération de fichiers (en-tête, nommage, écriture)
 * 
 * Pourquoi cette séparation ?
 * - Facilite la modification du format d'en-tête sans toucher à la logique métier
 * - Permet de changer facilement le format de nommage des fichiers
 * - Code plus testable et maintenable
 */
@Service
public class SqlFileBuilder {

    @Autowired
    private AppProperties appProperties;


    /**
     * Construit le contenu complet du fichier SQL (en-tête + SQL).
     * 
     * Structure du fichier généré :
     * 1. En-tête avec métadonnées (date, ticket, type, etc.)
     * 2. Ligne vide
     * 3. SQL traité (avec placeholders remplacés)
     */
    public String buildCompleteFile(TemplateDefinition template, String executionType, 
                                    Map<String, Object> params, String sql) {
        StringBuilder content = new StringBuilder();
        content.append(buildHeader(template, executionType, params));
        content.append("\n");
        content.append(sql);
        return content.toString();
    }

    /**
     * Construit l'en-tête du fichier SQL.
     * 
     * Pourquoi un en-tête dans chaque fichier généré ?
     * - Traçabilité : savoir quand et pour quel ticket le fichier a été généré
     * - Identification : retrouver facilement le template source
     * - Audit : historique des générations
     * 
     * Format modifiable ici sans impact sur la logique métier.
     */
    public String buildHeader(TemplateDefinition template, String executionType, Map<String, Object> params) {
        StringBuilder header = new StringBuilder();
        header.append("-- Fichier de Script Généré le ").append(LocalDateTime.now()).append("\n");
        header.append("-- Template: ").append(template.getName()).append("\n");
        header.append("-- ID: ").append(template.getId()).append("\n");
        header.append("-- Ticket: ").append(params.get("ticket")).append("\n");
        header.append("-- Type: ").append(executionType).append("\n");
        return header.toString();
    }

    /**
     * Génère le nom de fichier avec timestamp.
     * 
     * Format : {templateId}_{executionType}_{timestamp}.sql
     * 
     * Pourquoi inclure le timestamp ?
     * - Évite les collisions si plusieurs fichiers sont générés rapidement
     * - Permet de retrouver facilement un fichier par date
     * - Facilite le tri chronologique
     */
    public String generateFileName(String templateId, String executionType) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("%s_%s_%s.sql", templateId, executionType, timestamp);
    }

    /**
     * Écrit le fichier SQL sur le disque.
     */
    public void writeFile(String fileName, String content) throws IOException {
        Path path = Paths.get(appProperties.getOutputScriptsPath(), fileName);
        Files.write(path, content.getBytes());
    }

    /**
     * Construit et écrit le fichier SQL complet.
     * Retourne le nom du fichier généré.
     */
    public String buildAndWriteFile(TemplateDefinition template, String executionType, 
                                    Map<String, Object> params, String sql) throws IOException {
        String fileContent = buildCompleteFile(template, executionType, params, sql);
        String fileName = generateFileName(template.getId(), executionType);
        writeFile(fileName, fileContent);
        return fileName;
    }
}


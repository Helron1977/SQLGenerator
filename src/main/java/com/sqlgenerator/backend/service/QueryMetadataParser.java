package com.sqlgenerator.backend.service;

import com.sqlgenerator.backend.model.QueryDefinition;
import com.sqlgenerator.backend.model.ParameterDefinition;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parse les métadonnées des fichiers SQL pour créer des QueryDefinition.
 * 
 * Format des métadonnées supporté :
 * - -- @id: identifiant unique
 * - -- @name: nom descriptif
 * - -- @description: description détaillée
 * - -- @tags: tags séparés par virgules
 * - -- @param: nom|type|label|required (paramètre normal)
 * - -- @param-file: nom|type|label|required (paramètre fichier pour IN)
 */
@Service
public class QueryMetadataParser {

    private static final Logger logger = LoggerFactory.getLogger(QueryMetadataParser.class);

    /**
     * Parse un fichier SQL et extrait les métadonnées pour créer une QueryDefinition.
     * 
     * Pourquoi parser les métadonnées dans les commentaires SQL ?
     * - Permet de définir les requêtes directement dans les fichiers SQL
     * - Pas besoin de fichier JSON séparé
     * - Syntaxe SQL native avec coloration dans l'IDE
     * - Facilite la maintenance : tout est au même endroit
     */
    /**
     * Parse un fichier SQL depuis le classpath ou le répertoire d'uploads.
     * 
     * Ordre de recherche :
     * 1. Répertoire d'uploads (./sql_uploads/) - priorité
     * 2. Classpath (src/main/resources/sql/)
     */
    public QueryDefinition parseSqlFile(String filename) throws IOException {
        logger.debug("Parsing du fichier SQL : {}", filename);
        
        String sqlContent = loadSqlContent(filename);
        return parseSqlContent(sqlContent, filename);
    }

    /**
     * Charge le contenu SQL depuis le répertoire d'uploads ou le classpath.
     */
    public String loadSqlContent(String filename) throws IOException {
        // Essayer d'abord le répertoire d'uploads
        Path uploadPath = Paths.get("./sql_uploads/", filename);
        if (Files.exists(uploadPath)) {
            logger.debug("Chargement depuis uploads : {}", filename);
            return Files.readString(uploadPath, StandardCharsets.UTF_8);
        }
        
        // Sinon, utiliser le classpath
        logger.debug("Chargement depuis classpath : {}", filename);
        ClassPathResource resource = new ClassPathResource("sql/" + filename);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Parse le contenu SQL et extrait les métadonnées.
     * Méthode publique pour permettre la validation avant sauvegarde lors d'un upload.
     * 
     * L'ID peut être fourni de deux manières :
     * 1. Via -- @id: dans les métadonnées (priorité)
     * 2. Via le nom du fichier (sans extension .sql) si -- @id: n'est pas présent
     */
    public QueryDefinition parseSqlContent(String sqlContent, String filename) {
        Map<String, String> metadata = extractMetadata(sqlContent);
        List<ParameterDefinition> parameters = extractParameters(sqlContent);
        
        logger.debug("Fichier '{}' : {} métadonnée(s) et {} paramètre(s) trouvé(s)", 
                filename, metadata.size(), parameters.size());
        
        QueryDefinition query = new QueryDefinition();
        
        // Extraire l'ID : depuis les métadonnées ou depuis le nom du fichier
        String id = metadata.get("id");
        if (id == null || id.trim().isEmpty()) {
            // Si pas d'ID dans les métadonnées, l'extraire du nom du fichier
            if (filename != null && filename.endsWith(".sql")) {
                id = filename.substring(0, filename.length() - 4); // Retirer ".sql"
                logger.debug("ID extrait du nom de fichier : {}", id);
            } else {
                throw new IllegalArgumentException(
                    String.format(
                        "❌ Fichier '%s' : Impossible de déterminer l'ID.\n" +
                        "   Solution 1 : Ajoutez -- @id: votre-identifiant dans les métadonnées\n" +
                        "   Solution 2 : Le nom du fichier doit se terminer par .sql (ex: votre-id.sql)",
                        filename
                    )
                );
            }
        }
        
        query.setId(id);
        query.setName(metadata.get("name"));
        query.setDescription(metadata.get("description"));
        
        String tagsStr = metadata.get("tags");
        if (tagsStr != null && !tagsStr.trim().isEmpty()) {
            List<String> tags = Arrays.asList(tagsStr.split(","));
            query.setTags(tags.stream().map(String::trim).collect(Collectors.toList()));
        }
        
        query.setSqlFile(filename);
        query.setParameters(parameters != null ? parameters : new ArrayList<>());
        
        return query;
    }

    private Map<String, String> extractMetadata(String sqlContent) {
        Map<String, String> metadata = new HashMap<>();
        String[] lines = sqlContent.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("-- @")) {
                String[] parts = line.substring(4).split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    metadata.put(key, value);
                }
            }
        }
        
        return metadata;
    }

    /**
     * Extrait les paramètres depuis les métadonnées SQL.
     * 
     * Supporte deux types de paramètres :
     * - @param: paramètre normal (saisie manuelle dans Swagger)
     * - @param-file: paramètre fichier (upload pour clauses IN)
     */
    private List<ParameterDefinition> extractParameters(String sqlContent) {
        List<ParameterDefinition> parameters = new ArrayList<>();
        String[] lines = sqlContent.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            ParameterDefinition param = null;
            
            if (line.startsWith("-- @param:")) {
                param = parseParameterLine(line.substring(10).trim(), false);
            } else if (line.startsWith("-- @param-file:")) {
                param = parseParameterLine(line.substring(15).trim(), true);
            }
            
            if (param != null) {
                parameters.add(param);
            }
        }
        
        return parameters;
    }

    /**
     * Parse une ligne de paramètre au format : nom|type|label|required
     * 
     * Pourquoi utiliser le pipe (|) comme séparateur ?
     * Le pipe est rare dans les libellés et évite les conflits avec les virgules
     * utilisées dans les tags ou autres métadonnées.
     */
    private ParameterDefinition parseParameterLine(String paramLine, boolean isFile) {
        String[] parts = paramLine.split("\\|");
        
        if (parts.length >= 3) {
            ParameterDefinition param = new ParameterDefinition();
            param.setName(parts[0].trim());
            param.setType(parts[1].trim());
            param.setLabel(parts[2].trim());
            param.setRequired(parts.length >= 4 && "true".equalsIgnoreCase(parts[3].trim()));
            param.setFile(isFile);
            return param;
        }
        
        return null;
    }
}


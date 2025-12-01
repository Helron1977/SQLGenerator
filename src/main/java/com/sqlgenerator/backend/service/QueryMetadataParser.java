package com.sqlgenerator.backend.service;

import com.sqlgenerator.backend.model.QueryDefinition;
import com.sqlgenerator.backend.model.ParameterDefinition;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    public QueryDefinition parseSqlFile(String filename) throws IOException {
        logger.debug("Parsing du fichier SQL : {}", filename);
        
        ClassPathResource resource = new ClassPathResource("sql/" + filename);
        String sqlContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        
        Map<String, String> metadata = extractMetadata(sqlContent);
        List<ParameterDefinition> parameters = extractParameters(sqlContent);
        
        logger.debug("Fichier '{}' : {} métadonnée(s) et {} paramètre(s) trouvé(s)", 
                filename, metadata.size(), parameters.size());
        
        QueryDefinition query = new QueryDefinition();
        
        // Valider que l'ID est présent
        String id = metadata.get("id");
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(
                String.format(
                    "❌ Fichier '%s' : L'ID est obligatoire.\n" +
                    "   Solution : Ajoutez une ligne au début du fichier :\n" +
                    "   -- @id: votre-identifiant-unique\n" +
                    "   \n" +
                    "   L'ID doit être unique et ne contenir que des lettres, chiffres et tirets.",
                    filename
                )
            );
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


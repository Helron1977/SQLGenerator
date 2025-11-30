package com.sqlgenerator.backend.service;

import com.sqlgenerator.backend.model.QueryDefinition;
import com.sqlgenerator.backend.model.ParameterDefinition;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QueryMetadataParser {

    public QueryDefinition parseSqlFile(String filename) throws IOException {
        ClassPathResource resource = new ClassPathResource("sql/" + filename);
        String sqlContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        
        Map<String, String> metadata = extractMetadata(sqlContent);
        List<ParameterDefinition> parameters = extractParameters(sqlContent);
        
        QueryDefinition query = new QueryDefinition();
        
        // Valider que l'ID est présent
        String id = metadata.get("id");
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Le fichier " + filename + " doit contenir un -- @id: dans ses métadonnées");
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

    private List<ParameterDefinition> extractParameters(String sqlContent) {
        List<ParameterDefinition> parameters = new ArrayList<>();
        String[] lines = sqlContent.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("-- @param:")) {
                String paramLine = line.substring(10).trim();
                String[] parts = paramLine.split("\\|");
                
                if (parts.length >= 3) {
                    ParameterDefinition param = new ParameterDefinition();
                    param.setName(parts[0].trim());
                    param.setType(parts[1].trim());
                    param.setLabel(parts[2].trim());
                    param.setRequired(parts.length >= 4 && "true".equalsIgnoreCase(parts[3].trim()));
                    param.setFile(false);
                    parameters.add(param);
                }
            } else if (line.startsWith("-- @param-file:")) {
                String paramLine = line.substring(15).trim();
                String[] parts = paramLine.split("\\|");
                
                if (parts.length >= 3) {
                    ParameterDefinition param = new ParameterDefinition();
                    param.setName(parts[0].trim());
                    param.setType(parts[1].trim());
                    param.setLabel(parts[2].trim());
                    param.setRequired(parts.length >= 4 && "true".equalsIgnoreCase(parts[3].trim()));
                    param.setFile(true);
                    parameters.add(param);
                }
            }
        }
        
        return parameters;
    }
}


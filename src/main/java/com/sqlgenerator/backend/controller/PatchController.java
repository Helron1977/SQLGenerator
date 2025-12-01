package com.sqlgenerator.backend.controller;

import com.sqlgenerator.backend.service.QueryConstants;
import com.sqlgenerator.backend.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contrôleur REST pour la génération de patches SQL.
 * 
 * Endpoints générés dynamiquement par PatchOpenApiCustomizer :
 * - POST /api/patch/{id} : mode unitaire (ou avec IN)
 * - POST /api/patch/{id}/masse : mode masse (uniquement pour requêtes sans IN)
 * 
 * Pourquoi deux endpoints séparés ?
 * - Interface Swagger plus claire : pas de paramètres inutiles selon le mode
 * - Validation plus simple : fichier CSV requis uniquement en mode masse
 * - Meilleure expérience utilisateur dans la documentation
 */
@RestController
@RequestMapping("/api/patch")
@CrossOrigin(origins = "*")
public class PatchController {

    private static final Logger logger = LoggerFactory.getLogger(PatchController.class);

    @Autowired
    private QueryService queryService;

    @PostMapping(value = "/{id}", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    @io.swagger.v3.oas.annotations.Hidden
    public ResponseEntity<Resource> generatePatch(
            @PathVariable String id,
            @RequestParam Map<String, String> formParams,
            @RequestParam(required = false) Map<String, MultipartFile> fileParams) {
        
        var query = queryService.getQueryById(id);
        if (query == null) {
            logger.warn("Tentative d'accès à une query inexistante : {}", id);
            return ResponseEntity.notFound().build();
        }

        String executionType = formParams.getOrDefault("executionType", QueryConstants.EXECUTION_TYPE_UNITAIRE);
        Map<String, Object> params = extractParameters(query, formParams, fileParams);
        
        try {
            String fileName = queryService.generatePatchFile(id, executionType, params);
            return buildFileResponse(fileName);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur de validation pour query '{}' : {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du patch pour query '{}' : {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/{id}/masse", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    @io.swagger.v3.oas.annotations.Hidden
    public ResponseEntity<Resource> generatePatchMasse(
            @PathVariable String id,
            @RequestParam Map<String, String> formParams,
            @RequestParam(required = false) Map<String, MultipartFile> fileParams) {
        
        var query = queryService.getQueryById(id);
        if (query == null) {
            logger.warn("Tentative d'accès à une query inexistante (masse) : {}", id);
            return ResponseEntity.notFound().build();
        }

        // Vérifier que le fichier CSV est présent
        if (fileParams == null || !fileParams.containsKey("masseFile")) {
            logger.warn("Fichier CSV manquant pour query '{}' en mode masse", id);
            return ResponseEntity.badRequest().build();
        }

        MultipartFile masseFile = fileParams.get("masseFile");
        if (masseFile == null || masseFile.isEmpty()) {
            logger.warn("Fichier CSV vide pour query '{}' en mode masse", id);
            return ResponseEntity.badRequest().build();
        }

        try {
            // Parser le fichier CSV
            List<String> csvLines = parseFileContent(masseFile);
            logger.debug("Fichier CSV parsé : {} ligne(s) pour query '{}'", csvLines.size(), id);
            
            Map<String, Object> params = new HashMap<>();
            String ticket = formParams.get("ticket");
            if (ticket != null) {
                params.put("ticket", ticket);
            }
            params.put("masseFile", csvLines);
            
            String fileName = queryService.generatePatchFile(id, QueryConstants.EXECUTION_TYPE_MASSE, params);
            return buildFileResponse(fileName);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur de validation pour query '{}' (masse) : {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du patch (masse) pour query '{}' : {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private Map<String, Object> extractParameters(com.sqlgenerator.backend.model.QueryDefinition query,
                                                   Map<String, String> formParams,
                                                   Map<String, MultipartFile> fileParams) {
        Map<String, Object> params = new HashMap<>();
        
        if (query.getParameters() != null) {
            for (var paramDef : query.getParameters()) {
                if (paramDef != null && paramDef.getName() != null) {
                    Object value = extractParameterValue(paramDef, formParams, fileParams);
                    if (value != null) {
                        params.put(paramDef.getName(), value);
                    }
                }
            }
        }

        String ticket = formParams.get("ticket");
        if (ticket != null) {
            params.put("ticket", ticket);
        }

        return params;
    }

    private Object extractParameterValue(com.sqlgenerator.backend.model.ParameterDefinition paramDef,
                                        Map<String, String> formParams,
                                        Map<String, MultipartFile> fileParams) {
        if (paramDef.isFile() && fileParams != null) {
            return extractFileParameter(paramDef.getName(), fileParams);
        }
        return extractFormParameter(paramDef.getName(), formParams);
    }

    private Object extractFileParameter(String paramName, Map<String, MultipartFile> fileParams) {
        MultipartFile file = fileParams.get(paramName);
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            return parseFileContent(file);
        } catch (Exception e) {
            throw new IllegalArgumentException("Erreur lors du parsing du fichier " + paramName, e);
        }
    }

    private String extractFormParameter(String paramName, Map<String, String> formParams) {
        String value = formParams.get(paramName);
        return (value != null && !value.isEmpty()) ? value : null;
    }

    private ResponseEntity<Resource> buildFileResponse(String fileName) {
        try {
            Path path = Paths.get("./svn_repo_mock/" + fileName);
            Resource resource = new UrlResource(Objects.requireNonNull(path.toUri()));

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/sql"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de la ressource pour " + fileName, e);
        }
    }

    /**
     * Parse le contenu d'un fichier uploadé (1 valeur par ligne).
     */
    private List<String> parseFileContent(MultipartFile file) throws Exception {
        List<String> values = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    values.add(line);
                }
            }
        }
        return values;
    }
}


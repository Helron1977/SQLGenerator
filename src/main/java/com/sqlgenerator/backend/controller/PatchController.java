package com.sqlgenerator.backend.controller;

import com.sqlgenerator.backend.service.QueryConstants;
import com.sqlgenerator.backend.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
 * Endpoints disponibles :
 * - POST /api/patches/{id} : mode unitaire (ou avec IN)
 * - POST /api/patches/{id}?mode=masse : mode masse (uniquement pour requêtes sans IN)
 * 
 * Pour connaître les paramètres exacts d'une query, utilisez GET /api/queries/{id}
 */
@RestController
@RequestMapping("/api/patches")
@CrossOrigin(origins = "*")
@io.swagger.v3.oas.annotations.tags.Tag(name = "SQL Patches", description = "Génération de patches SQL à partir de templates paramétrés")
public class PatchController {

    private static final Logger logger = LoggerFactory.getLogger(PatchController.class);

    @Autowired
    private QueryService queryService;

    @PostMapping(value = "/{id}", consumes = {
            MediaType.APPLICATION_FORM_URLENCODED_VALUE, 
            MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.APPLICATION_JSON_VALUE
    })
    @Operation(
            summary = "Générer un patch SQL (mode unitaire)",
            description = "Génère un fichier de patch SQL en mode unitaire. " +
                    "Ce mode génère une seule requête SQL avec les paramètres fournis. " +
                    "Peut gérer les clauses IN (avec fichier) et les requêtes simples. " +
                    "\n\n" +
                    "Vous pouvez utiliser du JSON (copiez-collez depuis GET /api/queries/{id}/request-body) " +
                    "ou des paramètres form-urlencoded. " +
                    "Pour les paramètres IN, le fichier doit être uploadé via multipart (même en mode JSON)."
    )
    @RequestBody(
            description = "Body JSON (optionnel). " +
                    "⚠️ IMPORTANT : Pour obtenir le JSON correct avec les paramètres de cette query, " +
                    "appelez d'abord GET /api/queries/{id}/request-body (ou ?mode=masse pour le mode masse) " +
                    "et copiez-collez directement le JSON retourné. " +
                    "Chaque query a ses propres paramètres, donc l'exemple varie selon la query.",
            required = false,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            type = "object",
                            description = "JSON avec les paramètres spécifiques à cette query. " +
                                    "Pour connaître la structure exacte, appelez GET /api/queries/{id}/request-body " +
                                    "(ou ?mode=masse pour le mode masse) et copiez-collez le JSON retourné."
                    )
            )
    )
    public ResponseEntity<Resource> generatePatch(
            @Parameter(description = "Identifiant de la query", required = true, example = "update-person-name")
            @PathVariable String id,
            @Parameter(description = "Paramètres en form-urlencoded (alternative au JSON body)", required = false, hidden = true)
            @RequestParam(required = false) Map<String, String> formParams,
            @Parameter(description = "Fichiers uploadés", required = false, hidden = true)
            @RequestParam(required = false) Map<String, MultipartFile> fileParams,
            @org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, Object> jsonBody) {
        
        var query = queryService.getQueryById(id);
        if (query == null) {
            logger.warn("Tentative d'accès à une query inexistante : {}", id);
            return ResponseEntity.notFound().build();
        }

        // Si JSON body est fourni, l'utiliser (copié depuis /api/queries/{id}/request-body -> unitBodyStructure)
        // Sinon, utiliser formParams (pour Swagger ou form-urlencoded)
        Map<String, Object> params;
        String executionType;
        
        if (jsonBody != null && !jsonBody.isEmpty()) {
            // Mode JSON : convertir en Map pour le traitement
            params = new HashMap<>();
            for (Map.Entry<String, Object> entry : jsonBody.entrySet()) {
                if (entry.getValue() != null) {
                    params.put(entry.getKey(), entry.getValue().toString());
                }
            }
            // Les fichiers doivent toujours être passés via fileParams même en mode JSON
            if (fileParams != null && !fileParams.isEmpty()) {
                for (var paramDef : query.getParameters()) {
                    if (paramDef != null && paramDef.isFile() && fileParams.containsKey(paramDef.getName())) {
                        Object fileValue = extractFileParameter(paramDef.getName(), fileParams);
                        if (fileValue != null) {
                            params.put(paramDef.getName(), fileValue);
                        }
                    }
                }
            }
            executionType = params.getOrDefault("executionType", QueryConstants.EXECUTION_TYPE_UNITAIRE).toString();
        } else {
            // Mode form-urlencoded/multipart (comportement existant)
            executionType = formParams != null ? formParams.getOrDefault("executionType", QueryConstants.EXECUTION_TYPE_UNITAIRE) : QueryConstants.EXECUTION_TYPE_UNITAIRE;
            params = extractParameters(query, formParams != null ? formParams : new HashMap<>(), fileParams != null ? fileParams : new HashMap<>());
        }
        
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

    @PostMapping(value = "/{id}/masse", consumes = {
            MediaType.APPLICATION_FORM_URLENCODED_VALUE, 
            MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.APPLICATION_JSON_VALUE
    })
    @Operation(
            summary = "Générer un patch SQL (mode masse)",
            description = "Génère un fichier de patch SQL en mode masse. " +
                    "Ce mode génère N requêtes SQL (une par ligne du fichier CSV). " +
                    "⚠️ Disponible uniquement pour les queries SANS paramètre IN. " +
                    "\n\n" +
                    "Vous pouvez utiliser du JSON (copiez-collez depuis GET /api/forms/{id}/body-structure/masse) " +
                    "ou des paramètres form-urlencoded. " +
                    "Le fichier CSV doit toujours être fourni via multipart (même en mode JSON)."
    )
    @RequestBody(
            description = "Body JSON (optionnel). " +
                    "⚠️ IMPORTANT : Pour obtenir le JSON correct avec les paramètres de cette query, " +
                    "appelez d'abord GET /api/forms/{id}/body-structure/masse et copiez-collez directement le JSON retourné. " +
                    "Note : Le fichier CSV (masseFile) doit toujours être uploadé via multipart, même en mode JSON.",
            required = false,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            type = "object",
                            description = "JSON avec les paramètres spécifiques à cette query. " +
                                    "Pour connaître la structure exacte, appelez GET /api/forms/{id}/body-structure/masse " +
                                    "et copiez-collez le JSON retourné. " +
                                    "Le fichier CSV doit être uploadé séparément via multipart/form-data."
                    )
            )
    )
    public ResponseEntity<Resource> generatePatchMasse(
            @Parameter(description = "Identifiant de la query", required = true, example = "update-person-name")
            @PathVariable String id,
            @Parameter(description = "Ticket (optionnel si fourni dans JSON body)", required = false, hidden = true)
            @RequestParam(required = false) String ticket,
            @Parameter(description = "Fichier CSV contenant les données (une ligne = une requête). Obligatoire.", required = true)
            @RequestParam(value = "masseFile", required = false) MultipartFile masseFile,
            @org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, Object> jsonBody) {
        
        var query = queryService.getQueryById(id);
        if (query == null) {
            logger.warn("Tentative d'accès à une query inexistante (masse) : {}", id);
            return ResponseEntity.notFound().build();
        }

        // Vérifier que le mode masse est disponible (pas de paramètre IN)
        boolean hasInParameter = query.getParameters() != null &&
                query.getParameters().stream().anyMatch(p -> p != null && p.isFile());
        if (hasInParameter) {
            logger.warn("Tentative d'utilisation du mode masse sur une query avec IN : {}", id);
            return ResponseEntity.badRequest().build();
        }

        // Vérifier que le fichier CSV est présent
        if (masseFile == null || masseFile.isEmpty()) {
            logger.warn("Fichier CSV manquant ou vide pour query '{}' en mode masse", id);
            return ResponseEntity.badRequest().build();
        }

        try {
            // Parser le fichier CSV
            List<String> csvLines = parseFileContent(masseFile);
            logger.debug("Fichier CSV parsé : {} ligne(s) pour query '{}'", csvLines.size(), id);
            
            Map<String, Object> params = new HashMap<>();
            
            // Si JSON body est fourni, l'utiliser (copié depuis /api/forms/{id}/body-structure -> massBodyStructure)
            if (jsonBody != null && !jsonBody.isEmpty()) {
                // Mode JSON : convertir en Map pour le traitement
                for (Map.Entry<String, Object> entry : jsonBody.entrySet()) {
                    if (entry.getValue() != null && !"masseFile".equals(entry.getKey())) {
                        // masseFile doit toujours être passé via multipart
                        params.put(entry.getKey(), entry.getValue().toString());
                    }
                }
            } else {
                // Mode form-urlencoded (comportement existant)
                if (ticket != null && !ticket.isEmpty()) {
                    params.put("ticket", ticket);
                }
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


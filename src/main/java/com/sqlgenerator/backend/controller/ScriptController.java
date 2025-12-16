package com.sqlgenerator.backend.controller;

import com.sqlgenerator.backend.config.AppProperties;
import com.sqlgenerator.backend.service.TemplateConstants;
import com.sqlgenerator.backend.service.TemplateService;
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
 * Contrôleur REST pour la génération de scripts SQL.
 * 
 * Endpoints disponibles :
 * - POST /api/scripts/{id} : mode unitaire (ou avec IN)
 * - POST /api/scripts/{id}/masse : mode masse (uniquement pour templates sans IN)
 * 
 * Pour connaître les paramètres exacts d'un template, utilisez GET /api/templates/{id}
 */
@RestController
@RequestMapping("/api/scripts")
@CrossOrigin(origins = "*")
@io.swagger.v3.oas.annotations.tags.Tag(name = "SQL Scripts", description = "Génération de scripts SQL à partir de templates paramétrés")
public class ScriptController {

    private static final Logger logger = LoggerFactory.getLogger(ScriptController.class);

    @Autowired
    private TemplateService templateService;

    @Autowired
    private AppProperties appProperties;

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
                    "Vous pouvez utiliser du JSON (copiez-collez depuis GET /api/templates/{id}/request-body) " +
                    "ou des paramètres form-urlencoded. " +
                    "Pour les paramètres IN, le fichier doit être uploadé via multipart (même en mode JSON)."
    )
    @RequestBody(
            description = "Body JSON (optionnel). " +
                    "⚠️ IMPORTANT : Pour obtenir le JSON correct avec les paramètres de ce template, " +
                    "appelez d'abord GET /api/templates/{id}/request-body (ou ?mode=masse pour le mode masse) " +
                    "et copiez-collez directement le JSON retourné. " +
                    "Chaque template a ses propres paramètres, donc l'exemple varie selon le template.",
            required = false,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            type = "object",
                            description = "JSON avec les paramètres spécifiques à ce template. " +
                                    "Pour connaître la structure exacte, appelez GET /api/templates/{id}/request-body " +
                                    "(ou ?mode=masse pour le mode masse) et copiez-collez le JSON retourné."
                    )
            )
    )
    public ResponseEntity<Resource> generatePatch(
            @Parameter(description = "Identifiant du template", required = true, example = "update-person-name")
            @PathVariable String id,
            @Parameter(description = "Paramètres en form-urlencoded (alternative au JSON body)", required = false, hidden = true)
            @RequestParam(required = false) Map<String, String> formParams,
            @Parameter(description = "Fichiers uploadés", required = false, hidden = true)
            @RequestParam(required = false) Map<String, MultipartFile> fileParams,
            @org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, Object> jsonBody) {
        
        var template = templateService.getTemplateById(id);
        if (template == null) {
            logger.warn("Tentative d'accès à un template inexistant : {}", id);
            return ResponseEntity.notFound().build();
        }

        // Si JSON body est fourni, l'utiliser (copié depuis /api/templates/{id}/request-body -> unitBodyStructure)
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
                for (var paramDef : template.getParameters()) {
                    if (paramDef != null && paramDef.isFile() && fileParams.containsKey(paramDef.getName())) {
                        Object fileValue = extractFileParameter(paramDef.getName(), fileParams);
                        if (fileValue != null) {
                            params.put(paramDef.getName(), fileValue);
                        }
                    }
                }
            }
            executionType = params.getOrDefault("executionType", TemplateConstants.EXECUTION_TYPE_UNITAIRE).toString();
        } else {
            // Mode form-urlencoded/multipart (comportement existant)
            executionType = formParams != null ? formParams.getOrDefault("executionType", TemplateConstants.EXECUTION_TYPE_UNITAIRE) : TemplateConstants.EXECUTION_TYPE_UNITAIRE;
            params = extractParameters(template, formParams != null ? formParams : new HashMap<>(), fileParams != null ? fileParams : new HashMap<>());
        }
        
        try {
            String fileName = templateService.generateScriptFile(id, executionType, params);
            return buildFileResponse(fileName);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur de validation pour template '{}' : {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du script pour template '{}' : {}", id, e.getMessage(), e);
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
                    "⚠️ Disponible uniquement pour les templates SANS paramètre IN. " +
                    "\n\n" +
                    "Vous pouvez utiliser du JSON (copiez-collez depuis GET /api/templates/{id}/request-body?mode=masse) " +
                    "ou des paramètres form-urlencoded. " +
                    "Le fichier CSV doit toujours être fourni via multipart (même en mode JSON)."
    )
    @RequestBody(
            description = "Body JSON (optionnel). " +
                    "⚠️ IMPORTANT : Pour obtenir le JSON correct avec les paramètres de ce template, " +
                    "appelez d'abord GET /api/templates/{id}/request-body?mode=masse et copiez-collez directement le JSON retourné. " +
                    "Note : Le fichier CSV (masseFile) doit toujours être uploadé via multipart, même en mode JSON.",
            required = false,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            type = "object",
                            description = "JSON avec les paramètres spécifiques à ce template. " +
                                    "Pour connaître la structure exacte, appelez GET /api/templates/{id}/request-body?mode=masse " +
                                    "et copiez-collez le JSON retourné. " +
                                    "Le fichier CSV doit être uploadé séparément via multipart/form-data."
                    )
            )
    )
    public ResponseEntity<Resource> generatePatchMasse(
            @Parameter(description = "Identifiant du template", required = true, example = "update-person-name")
            @PathVariable String id,
            @Parameter(description = "Ticket (optionnel si fourni dans JSON body)", required = false, hidden = true)
            @RequestParam(required = false) String ticket,
            @Parameter(description = "Fichier CSV contenant les données (une ligne = une requête). Obligatoire.", required = true)
            @RequestParam(value = "masseFile", required = false) MultipartFile masseFile,
            @org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, Object> jsonBody) {
        
        var template = templateService.getTemplateById(id);
        if (template == null) {
            logger.warn("Tentative d'accès à un template inexistant (masse) : {}", id);
            return ResponseEntity.notFound().build();
        }

        // Vérifier que le mode masse est disponible (pas de paramètre IN)
        boolean hasInParameter = template.getParameters() != null &&
                template.getParameters().stream().anyMatch(p -> p != null && p.isFile());
        if (hasInParameter) {
            logger.warn("Tentative d'utilisation du mode masse sur un template avec IN : {}", id);
            return ResponseEntity.badRequest().build();
        }

        // Vérifier que le fichier CSV est présent
        if (masseFile == null || masseFile.isEmpty()) {
            logger.warn("Fichier CSV manquant ou vide pour template '{}' en mode masse", id);
            return ResponseEntity.badRequest().build();
        }

        try {
            // Parser le fichier CSV
            List<String> csvLines = parseFileContent(masseFile);
            logger.debug("Fichier CSV parsé : {} ligne(s) pour template '{}'", csvLines.size(), id);
            
            Map<String, Object> params = new HashMap<>();
            
            // Si JSON body est fourni, l'utiliser (copié depuis /api/templates/{id}/request-body?mode=masse -> massBodyStructure)
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
            
            String fileName = templateService.generateScriptFile(id, TemplateConstants.EXECUTION_TYPE_MASSE, params);
            return buildFileResponse(fileName);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur de validation pour template '{}' (masse) : {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du script (masse) pour template '{}' : {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private Map<String, Object> extractParameters(com.sqlgenerator.backend.model.TemplateDefinition template,
                                                   Map<String, String> formParams,
                                                   Map<String, MultipartFile> fileParams) {
        Map<String, Object> params = new HashMap<>();
        
        if (template.getParameters() != null) {
            for (var paramDef : template.getParameters()) {
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
            Path path = Paths.get(appProperties.getOutputScriptsPath() + fileName);
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


package com.sqlgenerator.backend.controller;

import com.sqlgenerator.backend.config.AppProperties;
import com.sqlgenerator.backend.model.TemplateDefinition;
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
 * Sous-domaine "Script Generation" - Contrôleur REST pour la génération de scripts SQL exécutables.
 * 
 * <p><b>Responsabilité</b> : Générer des scripts SQL exécutables à partir de templates et de paramètres.</p>
 * 
 * <p><b>Ressources</b> : Script (fichier SQL généré, téléchargeable)</p>
 * <p><b>Opérations</b> : POST (création de ressource)</p>
 * <p><b>Clients</b> : Frontend, outils externes, intégrations</p>
 * 
 * <p>Conforme aux principes REST : POST = création de ressource. Le script SQL généré
 * est une ressource nouvelle, créée à la demande.</p>
 * 
 * <p><b>Endpoints disponibles</b> :</p>
 * <ul>
 *   <li>POST /api/scripts/{id} : mode unitaire (ou avec clause IN)</li>
 *   <li>POST /api/scripts/{id}/masse : mode masse (uniquement pour templates sans IN)</li>
 * </ul>
 * 
 * <p>Pour connaître les paramètres exacts d'un template, utilisez {@link FormSchemaController#getTemplate(String)}</p>
 */
@RestController
@RequestMapping("/api/scripts")
@CrossOrigin(origins = "*")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Script Generation", description = "Génération de scripts SQL - Création de ressources Script à partir de templates paramétrés")
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
            description = """
                    Génère un fichier de patch SQL en mode unitaire.
                    Ce mode génère une seule requête SQL avec les paramètres fournis.
                    Peut gérer les clauses IN (avec fichier) et les requêtes simples.
                    
                    Vous pouvez utiliser du JSON (copiez-collez depuis GET /api/forms/{id}/request-body)
                    ou des paramètres form-urlencoded.
                    Pour les paramètres IN, le fichier doit être uploadé via multipart (même en mode JSON).
                    """
    )
    @RequestBody(
            description = """
                    Body JSON (optionnel).
                    ⚠️ IMPORTANT : Pour obtenir le JSON correct avec les paramètres de ce template,
                    appelez d'abord GET /api/forms/{id}/request-body (ou ?mode=masse pour le mode masse)
                    et copiez-collez directement le JSON retourné.
                    Chaque template a ses propres paramètres, donc l'exemple varie selon le template.
                    """,
            required = false,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            type = "object",
                            description = """
                                    JSON avec les paramètres spécifiques à ce template.
                                    Pour connaître la structure exacte, appelez GET /api/forms/{id}/request-body
                                    (ou ?mode=masse pour le mode masse) et copiez-collez le JSON retourné.
                                    """
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
        
        var template = validateTemplateExists(id);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            RequestParams requestParams = extractRequestParams(template, formParams, fileParams, jsonBody);
            String fileName = templateService.generateScriptFile(id, requestParams.executionType(), requestParams.params());
            return buildFileResponse(fileName);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur de validation pour template '{}' : {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du script pour template '{}' : {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Extrait les paramètres de la requête (JSON ou form-urlencoded).
     * 
     * @param template Template concerné
     * @param formParams Paramètres form-urlencoded (peut être null)
     * @param fileParams Fichiers uploadés (peut être null)
     * @param jsonBody Body JSON (peut être null)
     * @return RequestParams contenant les paramètres et le type d'exécution
     */
    private RequestParams extractRequestParams(com.sqlgenerator.backend.model.TemplateDefinition template,
                                               Map<String, String> formParams,
                                               Map<String, MultipartFile> fileParams,
                                               Map<String, Object> jsonBody) {
        if (hasJsonBody(jsonBody)) {
            return extractParamsFromJson(template, jsonBody, fileParams);
        } else {
            return extractParamsFromForm(template, formParams, fileParams);
        }
    }

    /**
     * Extrait les paramètres depuis un body JSON.
     * 
     * @param template Template concerné
     * @param jsonBody Body JSON
     * @param fileParams Fichiers uploadés (peut être null)
     * @return RequestParams contenant les paramètres et le type d'exécution
     */
    private RequestParams extractParamsFromJson(com.sqlgenerator.backend.model.TemplateDefinition template,
                                                Map<String, Object> jsonBody,
                                                Map<String, MultipartFile> fileParams) {
        Map<String, Object> params = new HashMap<>();
        
        // Convertir les valeurs JSON en String
        for (Map.Entry<String, Object> entry : jsonBody.entrySet()) {
            if (entry.getValue() != null) {
                params.put(entry.getKey(), entry.getValue().toString());
            }
        }
        
        // Ajouter les fichiers uploadés (doivent toujours être passés via fileParams même en mode JSON)
        addFileParameters(template, params, fileParams);
        
        String executionType = params.getOrDefault(TemplateConstants.EXECUTION_TYPE_PARAM, 
                TemplateConstants.EXECUTION_TYPE_UNITAIRE).toString();
        
        return new RequestParams(params, executionType);
    }

    /**
     * Extrait les paramètres depuis form-urlencoded/multipart.
     * 
     * @param template Template concerné
     * @param formParams Paramètres form-urlencoded (peut être null)
     * @param fileParams Fichiers uploadés (peut être null)
     * @return RequestParams contenant les paramètres et le type d'exécution
     */
    private RequestParams extractParamsFromForm(com.sqlgenerator.backend.model.TemplateDefinition template,
                                               Map<String, String> formParams,
                                               Map<String, MultipartFile> fileParams) {
        Map<String, String> safeFormParams = formParams != null ? formParams : new HashMap<>();
        Map<String, MultipartFile> safeFileParams = fileParams != null ? fileParams : new HashMap<>();
        
        Map<String, Object> params = extractParameters(template, safeFormParams, safeFileParams);
        String executionType = safeFormParams.getOrDefault(TemplateConstants.EXECUTION_TYPE_PARAM, 
                TemplateConstants.EXECUTION_TYPE_UNITAIRE);
        
        return new RequestParams(params, executionType);
    }

    /**
     * Ajoute les paramètres de type fichier depuis fileParams.
     * 
     * @param template Template concerné
     * @param params Map des paramètres (modifiée en place)
     * @param fileParams Fichiers uploadés (peut être null)
     */
    private void addFileParameters(com.sqlgenerator.backend.model.TemplateDefinition template,
                                   Map<String, Object> params,
                                   Map<String, MultipartFile> fileParams) {
        if (fileParams == null || fileParams.isEmpty()) {
            return;
        }
        
        for (var paramDef : template.getParameters()) {
            if (paramDef != null && paramDef.isFile() && fileParams.containsKey(paramDef.getName())) {
                Object fileValue = extractFileParameter(paramDef.getName(), fileParams);
                if (fileValue != null) {
                    params.put(paramDef.getName(), fileValue);
                }
            }
        }
    }

    /**
     * Regroupe les paramètres de requête et le type d'exécution.
     */
    private record RequestParams(Map<String, Object> params, String executionType) {}

    @PostMapping(value = "/{id}/masse", consumes = {
            MediaType.APPLICATION_FORM_URLENCODED_VALUE, 
            MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.APPLICATION_JSON_VALUE
    })
    @Operation(
            summary = "Générer un patch SQL (mode masse)",
            description = """
                    Génère un fichier de patch SQL en mode masse.
                    Ce mode génère N requêtes SQL (une par ligne du fichier CSV).
                    ⚠️ Disponible uniquement pour les templates SANS paramètre IN.
                    
                    Vous pouvez utiliser du JSON (copiez-collez depuis GET /api/templates/{id}/request-body?mode=masse)
                    ou des paramètres form-urlencoded.
                    Le fichier CSV doit toujours être fourni via multipart (même en mode JSON).
                    """
    )
    @RequestBody(
            description = """
                    Body JSON (optionnel).
                    ⚠️ IMPORTANT : Pour obtenir le JSON correct avec les paramètres de ce template,
                    appelez d'abord GET /api/forms/{id}/request-body?mode=masse et copiez-collez directement le JSON retourné.
                    Note : Le fichier CSV (masseFile) doit toujours être uploadé via multipart, même en mode JSON.
                    """,
            required = false,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            type = "object",
                            description = """
                                    JSON avec les paramètres spécifiques à ce template.
                                    Pour connaître la structure exacte, appelez GET /api/forms/{id}/request-body?mode=masse
                                    et copiez-collez le JSON retourné.
                                    Le fichier CSV doit être uploadé séparément via multipart/form-data.
                                    """
                    )
            )
    )
    public ResponseEntity<Resource> generatePatchMasse(
            @Parameter(description = "Identifiant du template", required = true, example = "update-person-name")
            @PathVariable String id,
            @Parameter(description = "Ticket (optionnel si fourni dans JSON body)", required = false, hidden = true)
            @RequestParam(required = false) String ticket,
            @Parameter(description = "Fichier CSV contenant les données (une ligne = une requête). Obligatoire.", required = true)
            @RequestParam(value = TemplateConstants.MASSE_FILE_PARAM, required = false) MultipartFile masseFile,
            @org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, Object> jsonBody) {
        
        var template = validateTemplateExists(id, "masse");
        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        // Vérifier que le mode masse est disponible (pas de paramètre IN)
        if (hasInParameter(template)) {
            logger.warn("Tentative d'utilisation du mode masse sur un template avec IN : {}", id);
            return ResponseEntity.badRequest().build();
        }

        // Vérifier que le fichier CSV est présent
        if (isMasseFileEmpty(masseFile)) {
            logger.warn("Fichier CSV manquant ou vide pour template '{}' en mode masse", id);
            return ResponseEntity.badRequest().build();
        }

        try {
            // Parser le fichier CSV
            List<String> csvLines = parseFileContent(masseFile);
            logger.debug("Fichier CSV parsé : {} ligne(s) pour template '{}'", csvLines.size(), id);
            
            Map<String, Object> params = new HashMap<>();
            
            // Si JSON body est fourni, l'utiliser (copié depuis /api/forms/{id}/request-body?mode=masse -> massBodyStructure)
            if (hasJsonBody(jsonBody)) {
                extractParamsFromJsonForMasse(jsonBody, params);
            } else {
                // Mode form-urlencoded (comportement existant)
                addTicketIfNotEmpty(ticket, params);
            }
            
            params.put(TemplateConstants.MASSE_FILE_PARAM, csvLines);
            
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

    /**
     * Extrait les paramètres depuis form-urlencoded/multipart pour le mode masse.
     * 
     * @param jsonBody Body JSON
     * @param params Map des paramètres (modifiée en place)
     */
    private void extractParamsFromJsonForMasse(Map<String, Object> jsonBody, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : jsonBody.entrySet()) {
            if (entry.getValue() != null && !TemplateConstants.MASSE_FILE_PARAM.equals(entry.getKey())) {
                // masseFile doit toujours être passé via multipart
                params.put(entry.getKey(), entry.getValue().toString());
            }
        }
    }

    /**
     * Extrait les paramètres depuis form-urlencoded/multipart.
     * 
     * @param template Template concerné
     * @param formParams Paramètres form-urlencoded
     * @param fileParams Fichiers uploadés
     * @return Map des paramètres extraits
     */
    private Map<String, Object> extractParameters(com.sqlgenerator.backend.model.TemplateDefinition template,
                                                   Map<String, String> formParams,
                                                   Map<String, MultipartFile> fileParams) {
        Map<String, Object> params = new HashMap<>();
        
        // Extraire les paramètres du template
        extractTemplateParameters(template, formParams, fileParams, params);

        // Ajouter le ticket si présent
        addTicketIfPresent(formParams, params);

        return params;
    }

    /**
     * Extrait les paramètres du template et les ajoute à la map des paramètres.
     * 
     * @param template Template contenant les définitions de paramètres
     * @param formParams Paramètres form-urlencoded
     * @param fileParams Fichiers uploadés
     * @param params Map des paramètres (modifiée en place)
     */
    private void extractTemplateParameters(TemplateDefinition template,
                                           Map<String, String> formParams,
                                           Map<String, MultipartFile> fileParams,
                                           Map<String, Object> params) {
        if (template.getParameters() == null) {
            return;
        }
        
        for (var paramDef : template.getParameters()) {
            if (isParameterDefinitionValid(paramDef)) {
                Object value = extractParameterValue(paramDef, formParams, fileParams);
                if (value != null) {
                    params.put(paramDef.getName(), value);
                }
            }
        }
    }

    /**
     * Vérifie si une définition de paramètre est valide (non null et avec un nom).
     * 
     * @param paramDef Définition de paramètre à vérifier
     * @return true si le paramètre est valide, false sinon
     */
    private boolean isParameterDefinitionValid(com.sqlgenerator.backend.model.ParameterDefinition paramDef) {
        return paramDef != null && paramDef.getName() != null;
    }

    /**
     * Ajoute le paramètre ticket s'il est présent dans formParams.
     * 
     * @param formParams Paramètres form-urlencoded
     * @param params Map des paramètres (modifiée en place)
     */
    private void addTicketIfPresent(Map<String, String> formParams, Map<String, Object> params) {
        String ticket = formParams.get(TemplateConstants.TICKET_PARAM);
        if (ticket != null) {
            params.put(TemplateConstants.TICKET_PARAM, ticket);
        }
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
        if (isFileEmpty(file)) {
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
     * Vérifie si un template contient un paramètre de type fichier (IN).
     * 
     * @param template Template à vérifier
     * @return true si le template contient au moins un paramètre fichier, false sinon
     */
    private boolean hasInParameter(com.sqlgenerator.backend.model.TemplateDefinition template) {
        if (template.getParameters() == null) {
            return false;
        }
        return template.getParameters().stream()
                .anyMatch(p -> p != null && p.isFile());
    }

    /**
     * Parse le contenu d'un fichier uploadé (1 valeur par ligne).
     */
    /**
     * Vérifie si un template existe et log un warning si absent.
     * 
     * @param id Identifiant du template
     * @return Template trouvé, ou null si absent
     */
    private com.sqlgenerator.backend.model.TemplateDefinition validateTemplateExists(String id) {
        return validateTemplateExists(id, null);
    }

    /**
     * Vérifie si un template existe et log un warning si absent.
     * 
     * @param id Identifiant du template
     * @param context Contexte supplémentaire pour le log (ex: "masse")
     * @return Template trouvé, ou null si absent
     */
    private com.sqlgenerator.backend.model.TemplateDefinition validateTemplateExists(String id, String context) {
        var template = templateService.getTemplateById(id);
        if (template == null) {
            String message = context != null 
                    ? "Tentative d'accès à un template inexistant (" + context + ") : " + id
                    : "Tentative d'accès à un template inexistant : " + id;
            logger.warn(message);
        }
        return template;
    }

    /**
     * Vérifie si le fichier CSV pour le mode masse est vide ou null.
     * 
     * @param masseFile Fichier CSV à vérifier
     * @return true si le fichier est null ou vide, false sinon
     */
    private boolean isMasseFileEmpty(MultipartFile masseFile) {
        return masseFile == null || masseFile.isEmpty();
    }

    /**
     * Vérifie si un body JSON est présent et non vide.
     * 
     * @param jsonBody Body JSON à vérifier
     * @return true si le body JSON est présent et non vide, false sinon
     */
    private boolean hasJsonBody(Map<String, Object> jsonBody) {
        return jsonBody != null && !jsonBody.isEmpty();
    }

    /**
     * Ajoute le paramètre ticket s'il n'est pas vide.
     * 
     * @param ticket Valeur du ticket (peut être null)
     * @param params Map des paramètres (modifiée en place)
     */
    private void addTicketIfNotEmpty(String ticket, Map<String, Object> params) {
        if (isTicketNotEmpty(ticket)) {
            params.put(TemplateConstants.TICKET_PARAM, ticket);
        }
    }

    /**
     * Vérifie si le ticket n'est pas null et non vide.
     * 
     * @param ticket Valeur du ticket à vérifier
     * @return true si le ticket est non null et non vide, false sinon
     */
    private boolean isTicketNotEmpty(String ticket) {
        return ticket != null && !ticket.isEmpty();
    }

    /**
     * Vérifie si un fichier est null ou vide.
     * 
     * @param file Fichier à vérifier
     * @return true si le fichier est null ou vide, false sinon
     */
    private boolean isFileEmpty(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    /**
     * Vérifie si une ligne n'est pas vide après trim.
     * 
     * @param line Ligne à vérifier
     * @return true si la ligne n'est pas vide après trim, false sinon
     */
    private boolean isLineNotEmpty(String line) {
        return line != null && !line.trim().isEmpty();
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
                if (isLineNotEmpty(line)) {
                    values.add(line);
                }
            }
        }
        return values;
    }
}


package com.sqlgenerator.backend.service;

import com.sqlgenerator.backend.model.TemplateDefinition;
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
 * Parse les métadonnées des fichiers SQL pour créer des TemplateDefinition.
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
public class TemplateMetadataParser {

    private static final Logger logger = LoggerFactory.getLogger(TemplateMetadataParser.class);

    /**
     * Parse un fichier SQL et extrait les métadonnées pour créer une TemplateDefinition.
     * 
     * Pourquoi parser les métadonnées dans les commentaires SQL ?
     * - Permet de définir les requêtes directement dans les fichiers SQL
     * - Pas besoin de fichier JSON séparé
     * - Syntaxe SQL native avec coloration dans l'IDE
     * - Facilite la maintenance : tout est au même endroit
     */
    public TemplateDefinition parseSqlFile(String filename) throws IOException {
        logger.debug("Parsing du fichier SQL : {}", filename);
        
        String sqlContent = loadSqlFileContent(filename);
        Map<String, String> metadata = extractMetadata(sqlContent);
        List<ParameterDefinition> parameters = extractParameters(sqlContent);
        
        logger.debug("Fichier '{}' : {} métadonnée(s) et {} paramètre(s) trouvé(s)", 
                filename, metadata.size(), parameters.size());
        
        return buildTemplateDefinition(filename, metadata, parameters);
    }

    /**
     * Charge le contenu d'un fichier SQL depuis les ressources.
     * 
     * @param filename Nom du fichier SQL
     * @return Contenu du fichier en UTF-8
     * @throws IOException Si le fichier ne peut pas être lu
     */
    private String loadSqlFileContent(String filename) throws IOException {
        ClassPathResource resource = new ClassPathResource(TemplateConstants.TEMPLATES_DIR + filename);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Construit un TemplateDefinition à partir des métadonnées et paramètres extraits.
     * 
     * @param filename Nom du fichier SQL source
     * @param metadata Map des métadonnées extraites
     * @param parameters Liste des paramètres extraits
     * @return TemplateDefinition construit et validé
     * @throws IllegalArgumentException Si l'ID est manquant
     */
    private TemplateDefinition buildTemplateDefinition(String filename, Map<String, String> metadata, 
                                                      List<ParameterDefinition> parameters) {
        TemplateDefinition template = new TemplateDefinition();
        
        String id = validateAndGetId(metadata, filename);
        template.setId(id);
        template.setName(metadata.get("name"));
        template.setDescription(metadata.get("description"));
        template.setTags(extractTags(metadata));
        template.setSqlFilename(filename);
        template.setParameters(parameters != null ? parameters : new ArrayList<>());
        
        return template;
    }

    /**
     * Valide et récupère l'ID depuis les métadonnées.
     * 
     * @param metadata Map des métadonnées
     * @param filename Nom du fichier (pour le message d'erreur)
     * @return ID validé
     * @throws IllegalArgumentException Si l'ID est manquant ou vide
     */
    private String validateAndGetId(Map<String, String> metadata, String filename) {
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
        return id;
    }

    /**
     * Extrait et parse les tags depuis les métadonnées.
     * 
     * @param metadata Map des métadonnées
     * @return Liste des tags (trimés), ou null si aucun tag
     */
    private List<String> extractTags(Map<String, String> metadata) {
        String tagsStr = metadata.get("tags");
        if (tagsStr == null || tagsStr.trim().isEmpty()) {
            return null;
        }
        
        return Arrays.stream(tagsStr.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * Extrait les métadonnées depuis le contenu SQL (lignes -- @id:, -- @name:, etc.).
     * 
     * @param sqlContent Contenu SQL brut avec métadonnées
     * @return Map associant chaque clé de métadonnée à sa valeur
     */
    private Map<String, String> extractMetadata(String sqlContent) {
        Map<String, String> metadata = new HashMap<>();
        String[] lines = sqlContent.split("\n");
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (isMetadataLine(trimmedLine)) {
                parseMetadataLine(trimmedLine, metadata);
            }
        }
        
        return metadata;
    }

    /**
     * Vérifie si une ligne est une ligne de métadonnée (commence par "-- @").
     * 
     * @param line Ligne à vérifier (déjà trimée)
     * @return true si la ligne est une métadonnée, false sinon
     */
    private boolean isMetadataLine(String line) {
        return line != null && line.startsWith(TemplateConstants.METADATA_PREFIX);
    }

    /**
     * Parse une ligne de métadonnée et l'ajoute à la map.
     * 
     * Format attendu : "-- @clé: valeur"
     * 
     * Si la ligne est mal formée (pas de ":" après "-- @"), elle est ignorée silencieusement.
     * 
     * @param line Ligne de métadonnée (déjà trimée et validée par isMetadataLine())
     * @param metadata Map où ajouter la métadonnée parsée (non null)
     */
    private void parseMetadataLine(String line, Map<String, String> metadata) {
        if (line == null || metadata == null) {
            return;
        }
        
        Optional<String> content = removeMetadataPrefix(line);
        if (content.isEmpty()) {
            return;
        }
        
        Optional<KeyValue> keyValue = parseKeyValue(content.get());
        if (keyValue.isEmpty()) {
            return;
        }
        
        addKeyValueToMetadata(keyValue.get(), metadata, line);
    }

    /**
     * Ajoute une paire clé-valeur à la map de métadonnées si la clé est valide.
     * 
     * @param keyValue Paire clé-valeur à ajouter
     * @param metadata Map où ajouter la métadonnée
     * @param originalLine Ligne originale pour les logs d'erreur
     */
    private void addKeyValueToMetadata(KeyValue keyValue, Map<String, String> metadata, String originalLine) {
        if (isKeyValid(keyValue.key())) {
            metadata.put(keyValue.key(), keyValue.value());
        } else {
            logInvalidKeyError(originalLine);
        }
    }

    /**
     * Log un warning pour une métadonnée avec clé vide.
     * 
     * @param originalLine Ligne originale qui a causé l'erreur
     */
    private void logInvalidKeyError(String originalLine) {
        logger.warn("Métadonnée avec clé vide, ignorée: {}", originalLine);
    }

    /**
     * Enlève le préfixe "-- @" d'une ligne de métadonnée.
     * 
     * Vérifie que la ligne est assez longue avant d'enlever le préfixe.
     * 
     * @param line Ligne avec préfixe "-- @" (déjà validée par isMetadataLine())
     * @return Optional contenant le contenu sans préfixe, ou Optional.empty() si ligne trop courte ou null
     */
    private Optional<String> removeMetadataPrefix(String line) {
        if (line == null) {
            return Optional.empty();
        }
        
        if (!validateLengthAndLogIfError(line, TemplateConstants.METADATA_PREFIX_LENGTH)) {
            return Optional.empty();
        }
        
        return Optional.of(line.substring(TemplateConstants.METADATA_PREFIX_LENGTH));
    }

    /**
     * Valide la longueur d'une ligne et log un warning si elle est trop courte.
     * 
     * @param line Ligne à valider
     * @param minLength Longueur minimale requise
     * @return true si la longueur est suffisante, false sinon (avec log si insuffisant)
     */
    private boolean validateLengthAndLogIfError(String line, int minLength) {
        if (!isLongEnough(line, minLength)) {
            logger.warn("Ligne de métadonnée trop courte, ignorée: {}", line);
            return false;
        }
        return true;
    }

    /**
     * Vérifie si une chaîne a une longueur minimale.
     * 
     * @param line Chaîne à vérifier
     * @param minLength Longueur minimale requise
     * @return true si la longueur est suffisante, false sinon
     */
    private boolean isLongEnough(String line, int minLength) {
        return line != null && line.length() >= minLength;
    }

    /**
     * Parse une chaîne au format "clé: valeur" et retourne un KeyValue.
     * 
     * @param content Contenu à parser (sans le préfixe "-- @")
     * @return Optional contenant KeyValue si le format est valide, Optional.empty() sinon
     */
    private Optional<KeyValue> parseKeyValue(String content) {
        if (content == null) {
            return Optional.empty();
        }
        
        String[] parts = content.split(":", 2);
        if (!isValidKeyValueFormat(parts)) {
            return Optional.empty();
        }
        
        String key = parts[0].trim();
        String value = parts[1].trim();
        return Optional.of(new KeyValue(key, value));
    }

    /**
     * Vérifie si le format clé:valeur est valide (2 parties après split).
     * 
     * @param parts Tableau résultant du split sur ":"
     * @return true si le format est valide, false sinon
     */
    private boolean isValidKeyValueFormat(String[] parts) {
        return parts.length == 2;
    }

    /**
     * Vérifie si une clé de métadonnée est valide (non vide).
     * 
     * @param key Clé à vérifier
     * @return true si la clé est valide, false sinon
     */
    private boolean isKeyValid(String key) {
        return key != null && !key.isEmpty();
    }

    /**
     * Record pour représenter une paire clé-valeur.
     * Utilise le record Java moderne (depuis Java 14) au lieu d'une classe interne.
     */
    private record KeyValue(String key, String value) {
    }

    /**
     * Extrait les définitions de paramètres depuis le contenu SQL (lignes -- @param: et -- @param-file:).
     * 
     * @param sqlContent Contenu SQL brut avec métadonnées
     * @return Liste des paramètres parsés
     */
    private List<ParameterDefinition> extractParameters(String sqlContent) {
        if (sqlContent == null) {
            return new ArrayList<>();
        }
        
        List<ParameterDefinition> parameters = new ArrayList<>();
        String[] lines = sqlContent.split("\n");
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            Optional<ParameterDefinition> param = parseParameterLineIfValid(trimmedLine);
            
            if (param.isPresent()) {
                parameters.add(param.get());
            }
        }
        
        return parameters;
    }

    /**
     * Parse une ligne de paramètre si elle est valide (commence par -- @param: ou -- @param-file:).
     * 
     * @param line Ligne à parser (déjà trimée)
     * @return Optional contenant ParameterDefinition si la ligne est valide, Optional.empty() sinon
     */
    private Optional<ParameterDefinition> parseParameterLineIfValid(String line) {
        if (line == null) {
            return Optional.empty();
        }
        
        if (line.startsWith(TemplateConstants.PARAM_PREFIX)) {
            String content = extractParameterContent(line, TemplateConstants.PARAM_PREFIX_LENGTH);
            return parseParameterLine(content, false);
        } else if (line.startsWith(TemplateConstants.PARAM_FILE_PREFIX)) {
            String content = extractParameterContent(line, TemplateConstants.PARAM_FILE_PREFIX_LENGTH);
            return parseParameterLine(content, true);
        }
        
        return Optional.empty();
    }

    /**
     * Extrait le contenu d'une ligne de paramètre après le préfixe.
     * 
     * @param line Ligne complète avec préfixe
     * @param prefixLength Longueur du préfixe à enlever
     * @return Contenu sans le préfixe (trimé)
     */
    private String extractParameterContent(String line, int prefixLength) {
        if (!isLongEnough(line, prefixLength)) {
            return "";
        }
        return line.substring(prefixLength).trim();
    }

    /**
     * Parse une ligne de paramètre au format : nom|type|label|required
     * 
     * Pourquoi utiliser le pipe (|) comme séparateur ?
     * Le pipe est rare dans les libellés et évite les conflits avec les virgules
     * utilisées dans les tags ou autres métadonnées.
     * 
     * @param paramLine Ligne de paramètre à parser (sans préfixe -- @param:)
     * @param isFile true si c'est un paramètre fichier, false sinon
     * @return Optional contenant ParameterDefinition si le format est valide, Optional.empty() sinon
     */
    private Optional<ParameterDefinition> parseParameterLine(String paramLine, boolean isFile) {
        if (paramLine == null || paramLine.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String[] parts = paramLine.split("\\|");
        if (!hasMinimumParameterPartsAndLogIfError(parts, paramLine)) {
            return Optional.empty();
        }
        
        return Optional.of(buildParameterDefinition(parts, isFile));
    }

    /**
     * Vérifie si le tableau de parties contient au moins 3 éléments et log un warning si insuffisant.
     * 
     * @param parts Tableau résultant du split sur "|"
     * @param paramLine Ligne originale pour les logs d'erreur
     * @return true si au moins 3 parties, false sinon (avec log si insuffisant)
     */
    private boolean hasMinimumParameterPartsAndLogIfError(String[] parts, String paramLine) {
        if (!hasMinimumParameterParts(parts)) {
            logInvalidParameterFormat(paramLine);
            return false;
        }
        return true;
    }

    /**
     * Vérifie si le tableau de parties contient au moins 3 éléments (nom, type, label minimum).
     * 
     * @param parts Tableau résultant du split sur "|"
     * @return true si au moins 3 parties, false sinon
     */
    private boolean hasMinimumParameterParts(String[] parts) {
        return parts.length >= TemplateConstants.MINIMUM_PARAMETER_PARTS;
    }

    /**
     * Construit un ParameterDefinition à partir des parties parsées.
     * 
     * @param parts Tableau contenant [nom, type, label, required?]
     * @param isFile true si c'est un paramètre fichier, false sinon
     * @return ParameterDefinition construit
     */
    private ParameterDefinition buildParameterDefinition(String[] parts, boolean isFile) {
        ParameterDefinition param = new ParameterDefinition();
        param.setName(parts[0].trim());
        param.setType(parts[1].trim());
        param.setLabel(parts[2].trim());
        param.setRequired(parts.length >= 4 && parseBooleanValue(parts[3].trim()));
        param.setFile(isFile);
        return param;
    }

    /**
     * Parse une valeur booléenne depuis une chaîne.
     * 
     * @param value Chaîne à parser ("true" = true, tout le reste = false)
     * @return true si la valeur est "true" (insensible à la casse), false sinon
     */
    private boolean parseBooleanValue(String value) {
        return "true".equalsIgnoreCase(value);
    }

    /**
     * Log un warning pour un format de paramètre invalide.
     * 
     * @param paramLine Ligne qui a causé l'erreur
     */
    private void logInvalidParameterFormat(String paramLine) {
        logger.debug("Format de paramètre invalide (moins de 3 parties), ignoré: {}", paramLine);
    }
}


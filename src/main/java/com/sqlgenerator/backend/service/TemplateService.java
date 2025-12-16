package com.sqlgenerator.backend.service;

import com.sqlgenerator.backend.model.TemplateDefinition;
import com.sqlgenerator.backend.model.ParameterDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service principal pour la gestion des templates SQL.
 * 
 * Responsabilités :
 * - Chargement et parsing des fichiers SQL au démarrage
 * - Traitement des templates SQL (remplacement de placeholders, lotissement, mode masse)
 * - Délégation de la génération de fichiers à SqlFileBuilder
 */
@Service
public class TemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateService.class);

    private List<TemplateDefinition> templates; // Templates SQL chargés

    @Autowired
    private TemplateMetadataParser metadataParser;

    @Autowired
    private SqlFileBuilder sqlFileBuilder;

    @Autowired
    private com.sqlgenerator.backend.config.AppProperties appProperties;

    /**
     * Initialise le service au démarrage de l'application.
     * 
     * Pourquoi cette méthode existe :
     * - Scanne automatiquement tous les fichiers SQL dans resources/templates/
     * - Parse les métadonnées pour créer les TemplateDefinition
     * - Crée le répertoire de sortie si nécessaire
     * 
     * Les erreurs de parsing sont loggées mais n'empêchent pas le démarrage
     * pour permettre à l'application de démarrer même si un fichier SQL est mal formé.
     */
    @PostConstruct
    public void init() throws IOException {
        templates = new ArrayList<>();
        
        List<String> sqlFiles = scanSqlFiles();
        logger.info("Démarrage : {} fichier(s) SQL trouvé(s)", sqlFiles.size());
        
        for (String filename : sqlFiles) {
            try {
                TemplateDefinition template = loadTemplateFromFile(filename);
                
                // Valider les placeholders vs paramètres définis
                String sqlContent = loadSqlFromFile(template);
                validatePlaceholders(template, sqlContent, filename);
                
                templates.add(template);
                logger.debug("Template chargé : {} ({})", template.getId(), template.getName());
            } catch (Exception e) {
                // Log mais ne bloque pas le démarrage : un fichier mal formé ne doit pas empêcher l'app
                logger.error("❌ Erreur lors du parsing du fichier '{}' : {}", filename, e.getMessage(), e);
            }
        }
        
        logger.info("Initialisation terminée : {} template(s) chargé(s) avec succès", templates.size());
        
        if (templates.isEmpty()) {
            logger.warn("⚠️  Aucun template chargé. Vérifiez que les fichiers SQL sont dans src/main/resources/templates/");
        }
        
        // Créer le répertoire de sortie au démarrage
        try {
            Files.createDirectories(Paths.get(appProperties.getOutputScriptsPath()));
            logger.debug("Répertoire de sortie créé/vérifié: {}", appProperties.getOutputScriptsPath());
        } catch (Exception e) {
            logger.warn("Impossible de créer le répertoire de sortie: {}", e.getMessage());
        }
    }

    private List<String> scanSqlFiles() throws IOException {
        List<String> filenames = new ArrayList<>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(TemplateConstants.TEMPLATES_CLASSPATH_PATTERN);
        
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename != null && filename.endsWith(TemplateConstants.SQL_FILE_EXTENSION)) {
                filenames.add(filename);
            }
        }
        
        return filenames;
    }

    private TemplateDefinition loadTemplateFromFile(String filename) throws IOException {
        return metadataParser.parseSqlFile(filename);
    }

    public TemplateDefinition getTemplateById(String id) {
        return templates.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<TemplateDefinition> getAllTemplates() {
        return templates != null ? templates : Collections.emptyList();
    }

    public String generateScriptFile(String templateId, String executionType, Map<String, Object> params)
            throws IOException {
        TemplateDefinition template = validateAndGetTemplate(templateId);
        String baseSql = loadSqlFromFile(template);
        String sql = processSqlWithParams(template, baseSql, params, executionType);
        return sqlFileBuilder.buildAndWriteFile(template, executionType, params, sql);
    }

    private TemplateDefinition validateAndGetTemplate(String templateId) {
        TemplateDefinition template = getTemplateById(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateId);
        }
        if (template.getSqlFilename() == null || template.getSqlFilename().isEmpty()) {
            throw new IllegalArgumentException("Template must have sqlFilename defined: " + templateId);
        }
        return template;
    }

    private String loadSqlFromFile(TemplateDefinition template) throws IOException {
        ClassPathResource sqlResource = new ClassPathResource(TemplateConstants.TEMPLATES_DIR + template.getSqlFilename());
        String sqlContent = new String(sqlResource.getInputStream().readAllBytes(), 
                java.nio.charset.StandardCharsets.UTF_8);
        return removeMetadataComments(sqlContent);
    }

    /**
     * Valide que tous les placeholders {{param}} dans le SQL ont un paramètre défini.
     * 
     * Pourquoi cette validation ?
     * - Détecte les erreurs de configuration au démarrage
     * - Évite les bugs en production (placeholders non remplacés)
     * - Améliore la qualité du code SQL
     * 
     * @param template La définition du template avec ses paramètres
     * @param sqlContent Le contenu SQL (sans métadonnées)
     * @param filename Le nom du fichier pour les messages d'erreur
     * @throws IllegalArgumentException Si des placeholders ne sont pas définis
     */
    private void validatePlaceholders(TemplateDefinition template, String sqlContent, String filename) {
        // Extraire tous les placeholders du format {{nom_param}}
        Pattern placeholderPattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher matcher = placeholderPattern.matcher(sqlContent);
        Set<String> placeholders = new HashSet<>();
        
        while (matcher.find()) {
            placeholders.add(matcher.group(1).trim());
        }
        
        // Si aucun placeholder, pas de validation nécessaire
        if (placeholders.isEmpty()) {
            return;
        }
        
        // Récupérer les noms des paramètres définis
        Set<String> definedParams = new HashSet<>();
        if (template.getParameters() != null) {
            definedParams = template.getParameters().stream()
                    .map(ParameterDefinition::getName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
        
        // Trouver les placeholders non définis
        Set<String> missing = new HashSet<>(placeholders);
        missing.removeAll(definedParams);
        
        if (!missing.isEmpty()) {
            String missingList = String.join(", ", missing);
            throw new IllegalArgumentException(
                String.format(
                    "❌ Fichier '%s' : Placeholders non définis dans les paramètres : %s\n" +
                    "   Solution : Ajoutez les paramètres manquants avec -- @param: ou -- @param-file:\n" +
                    "   Exemple : -- @param: %s|text|Description|true",
                    filename, missingList, missing.iterator().next()
                )
            );
        }
        
        // Vérifier aussi les paramètres définis mais non utilisés (warning seulement)
        Set<String> unused = new HashSet<>(definedParams);
        unused.removeAll(placeholders);
        if (!unused.isEmpty()) {
            logger.warn("Fichier '{}' : Paramètres définis mais non utilisés dans le SQL : {}", 
                    filename, String.join(", ", unused));
        }
    }

    /**
     * Traite le SQL selon le type d'exécution et les paramètres fournis.
     * 
     * Ordre de traitement (important) :
     * 1. Mode masse : priorité si fichier CSV fourni (génère n requêtes)
     * 2. Lotissement : si clause IN > 999 valeurs (limite Oracle)
     * 3. Mode unitaire : traitement standard avec remplacement simple
     */
    private String processSqlWithParams(TemplateDefinition template, String baseSql, Map<String, Object> params, String executionType) {
        // Mode masse : générer n requêtes (une par ligne du fichier CSV)
        if (TemplateConstants.EXECUTION_TYPE_MASSE.equals(executionType) && params.containsKey("masseFile")) {
            return generateMasseSql(template, baseSql, params);
        }
        
        // Lotissement pour clauses IN > 999 valeurs (limite Oracle)
        if (requiresBatching(template, params)) {
            return generateBatchedSql(template, baseSql, params);
        }
        
        // Mode unitaire standard : remplacement simple des placeholders
        return replacePlaceholders(template, baseSql, params);
    }

    /**
     * Vérifie si un lotissement est nécessaire pour une clause IN.
     * 
     * Pourquoi 999 et pas 1000 ?
     * Oracle limite les clauses IN à 1000 éléments. On utilise 999 pour éviter
     * les erreurs de dépassement et laisser une marge de sécurité.
     */
    private boolean requiresBatching(TemplateDefinition query, Map<String, Object> params) {
        return query.getParameters().stream()
                .anyMatch(p -> p.isFile() 
                        && params.get(p.getName()) instanceof List 
                        && ((List<?>) params.get(p.getName())).size() > TemplateConstants.ORACLE_IN_MAX_SIZE);
    }

    private String replacePlaceholders(TemplateDefinition template, String sql, Map<String, Object> params) {
        String result = sql;
        for (ParameterDefinition paramDef : template.getParameters()) {
            String replacement = buildParameterReplacement(paramDef, params.get(paramDef.getName()));
            result = result.replace("{{" + paramDef.getName() + "}}", replacement);
        }
        return result;
    }

    private String buildParameterReplacement(ParameterDefinition paramDef, Object value) {
        if (value == null || isNullValue(value)) {
            return "NULL";
        }

        if (paramDef.isFile()) {
            return buildFileParameterReplacement(value);
        }

        return buildSimpleParameterReplacement(paramDef.getType(), value);
    }

    /**
     * Vérifie si la valeur représente NULL.
     * 
     * Pourquoi cette méthode existe :
     * Les utilisateurs peuvent fournir NULL de différentes manières :
     * - Chaîne vide ""
     * - Mot-clé "null" ou "NULL"
     * - Valeur null réelle
     * 
     * Cette méthode unifie la détection pour éviter les incohérences.
     */
    private boolean isNullValue(Object value) {
        if (value == null) {
            return true;
        }
        String str = value.toString().trim();
        return str.isEmpty() || "null".equalsIgnoreCase(str) || "NULL".equalsIgnoreCase(str);
    }

    private String buildFileParameterReplacement(Object value) {
        if (value == null || isNullValue(value)) {
            return "NULL";
        }
        
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> values = (List<String>) value;
            // Filtrer les valeurs NULL/vides
            List<String> filteredValues = values.stream()
                    .filter(v -> v != null && !v.trim().isEmpty() && !isNullValue(v))
                    .collect(java.util.stream.Collectors.toList());
            
            if (filteredValues.isEmpty()) {
                return "NULL";
            }
            return formatSingleInClause(filteredValues);
        }
        
        if (value instanceof String) {
            String strValue = ((String) value).trim();
            if (strValue.isEmpty() || isNullValue(strValue)) {
                return "NULL";
            }
            return formatSingleInClause(Collections.singletonList(strValue));
        }
        
        return "NULL";
    }

    private String buildSimpleParameterReplacement(String type, Object value) {
        if (value == null || isNullValue(value)) {
            return "NULL";
        }

        String valueStr = value.toString().trim();
        
        if ("date".equals(type)) {
            return formatDate(valueStr);
        }
        
        if ("text".equals(type)) {
            return "'" + escapeSqlString(valueStr) + "'";
        }
        
        return valueStr;
    }

    /**
     * Formate une date au format DD/MM/YY (ex: 30/11/25).
     * 
     * Pourquoi ce format spécifique ?
     * Les dates sont souvent stockées en CHAR dans Oracle, pas en DATE.
     * Le format DD/MM/YY est le format standard utilisé dans ce contexte métier.
     * 
     * Si la date est déjà au bon format, la retourne telle quelle.
     * Sinon, tente de convertir depuis d'autres formats (ex: YYYY-MM-DD).
     * Si aucun format reconnu, retourne tel quel (responsabilité du dev SQL).
     */
    private String formatDate(String dateValue) {
        if (dateValue == null || dateValue.trim().isEmpty() || isNullValue(dateValue)) {
            return "NULL";
        }
        
        String trimmed = dateValue.trim();
        
        // Si déjà au format DD/MM/YY, retourner tel quel
        if (trimmed.matches("\\d{2}/\\d{2}/\\d{2}")) {
            return "'" + trimmed + "'";
        }
        
        // Si format YYYY-MM-DD, convertir en DD/MM/YY
        if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] parts = trimmed.split("-");
            String year = parts[0];
            String month = parts[1];
            String day = parts[2];
            // Prendre les 2 derniers chiffres de l'année
            String shortYear = year.length() >= 2 ? year.substring(year.length() - 2) : year;
            return "'" + day + "/" + month + "/" + shortYear + "'";
        }
        
        // Sinon, retourner tel quel (responsabilité du dev SQL)
        return "'" + escapeSqlString(trimmed) + "'";
    }

    /**
     * Échappe les apostrophes dans les chaînes SQL.
     */
    private String escapeSqlString(String value) {
        return value.replace("'", "''");
    }


    private String removeMetadataComments(String sqlContent) {
        StringBuilder sql = new StringBuilder();
        String[] lines = sqlContent.split("\n");
        boolean metadataSection = true;
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-- @")) {
                // Ignorer les lignes de métadonnées
                continue;
            }
            if (trimmed.isEmpty() && metadataSection) {
                // Ignorer les lignes vides dans la section métadonnées
                continue;
            }
            // Première ligne non-métadonnée : on commence le SQL
            metadataSection = false;
            sql.append(line).append("\n");
        }
        
        return sql.toString().trim();
    }

    /**
     * Génère du SQL avec lotissement si nécessaire (> 999 valeurs dans un IN).
     */
    private String generateBatchedSql(TemplateDefinition template, String baseSql, Map<String, Object> params) {
        ParameterDefinition fileParam = findFileParameterForBatching(template, params);
        if (fileParam == null) {
            return baseSql;
        }

        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) params.get(fileParam.getName());
        if (values == null || values.isEmpty()) {
            return baseSql;
        }

        String sqlTemplate = replaceNonFileParameters(template, baseSql, params, fileParam);
        return generateBatches(fileParam, values, sqlTemplate);
    }

    private ParameterDefinition findFileParameterForBatching(TemplateDefinition template, Map<String, Object> params) {
        return template.getParameters().stream()
                .filter(p -> p.isFile() && params.get(p.getName()) instanceof List)
                .findFirst()
                .orElse(null);
    }

    private String replaceNonFileParameters(TemplateDefinition template, String sql, 
                                           Map<String, Object> params, ParameterDefinition fileParam) {
        String result = sql;
        for (ParameterDefinition paramDef : template.getParameters()) {
            if (!paramDef.isFile() && !paramDef.getName().equals(fileParam.getName())) {
                String replacement = buildSimpleParameterReplacement(paramDef.getType(), params.get(paramDef.getName()));
                result = result.replace("{{" + paramDef.getName() + "}}", replacement);
            }
        }
        return result;
    }

    /**
     * Génère plusieurs lots de SQL pour gérer les clauses IN > 999 valeurs.
     * 
     * Pourquoi diviser en lots ?
     * Oracle ne supporte pas plus de 1000 éléments dans une clause IN.
     * On génère donc plusieurs requêtes SQL séparées avec des commentaires
     * pour identifier chaque lot.
     */
    private String generateBatches(ParameterDefinition fileParam, List<String> values, String sqlTemplate) {
        StringBuilder result = new StringBuilder();
        int batchSize = TemplateConstants.ORACLE_IN_MAX_SIZE;
        int totalBatches = (int) Math.ceil((double) values.size() / batchSize);

        for (int i = 0; i < totalBatches; i++) {
            List<String> batch = extractBatch(values, i, batchSize);
            appendBatch(result, i, totalBatches, batch, fileParam, sqlTemplate);
        }

        return result.toString();
    }

    private List<String> extractBatch(List<String> values, int batchIndex, int batchSize) {
        int start = batchIndex * batchSize;
        int end = Math.min(start + batchSize, values.size());
        return values.subList(start, end);
    }

    private void appendBatch(StringBuilder result, int batchIndex, int totalBatches, 
                             List<String> batch, ParameterDefinition fileParam, String sqlTemplate) {
        if (batchIndex > 0) {
            result.append("\n\n");
        }

        result.append("-- Lot ").append(batchIndex + 1).append("/").append(totalBatches)
               .append(" (").append(batch.size()).append(" valeurs)\n");

        String inClause = formatSingleInClause(batch);
        String sqlForBatch = sqlTemplate.replace("{{" + fileParam.getName() + "}}", inClause);
        result.append(sqlForBatch);
    }

    /**
     * Formate une liste de valeurs pour une clause IN simple (<= 999 valeurs).
     */
    private String formatSingleInClause(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "NULL";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            String value = values.get(i).trim();
            // Filtrer les valeurs NULL/vides
            if (value.isEmpty() || isNullValue(value)) {
                sb.append("NULL");
            } else {
                // Entourer de guillemets simples pour les valeurs texte
                sb.append("'").append(escapeSqlString(value)).append("'");
            }
        }
        return sb.toString();
    }


    /**
     * Génère du SQL en mode masse : n lignes dans le fichier CSV = n requêtes SQL dans un seul fichier.
     * Format du fichier : CSV avec une ligne par requête, valeurs séparées par virgule dans l'ordre des paramètres.
     */
    private String generateMasseSql(TemplateDefinition template, String baseSql, Map<String, Object> params) {
        // Récupérer les lignes du fichier CSV uploadé
        @SuppressWarnings("unchecked")
        List<String> fileLines = (List<String>) params.get("masseFile");
        if (fileLines == null || fileLines.isEmpty()) {
            return baseSql;
        }

        // Extraire l'ordre des paramètres (tous les paramètres non-fichier dans l'ordre)
        List<ParameterDefinition> orderedParams = template.getParameters().stream()
                .filter(p -> !p.isFile())
                .collect(java.util.stream.Collectors.toList());

        // Générer une requête par ligne
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < fileLines.size(); i++) {
            String line = fileLines.get(i);
            Map<String, Object> lineParams = parseCsvLine(line, orderedParams);
            
            if (i > 0) {
                result.append("\n\n");
            }
            
            result.append("-- Requête ").append(i + 1).append("/").append(fileLines.size()).append("\n");
            
            // Remplacer les placeholders avec les valeurs de la ligne
            String sqlForLine = replacePlaceholdersForLine(template, baseSql, lineParams, params);
            result.append(sqlForLine);
        }

        return result.toString();
    }


    /**
     * Parse une ligne CSV et crée un Map avec les valeurs dans l'ordre des paramètres.
     * Gère les valeurs NULL/vides.
     */
    private Map<String, Object> parseCsvLine(String line, List<ParameterDefinition> orderedParams) {
        Map<String, Object> lineParams = new HashMap<>();
        String[] values = line.split(",");
        
        for (int i = 0; i < Math.min(values.length, orderedParams.size()); i++) {
            ParameterDefinition param = orderedParams.get(i);
            String value = values[i].trim();
            // Stocker même si vide (sera géré comme NULL dans buildSimpleParameterReplacement)
            lineParams.put(param.getName(), value.isEmpty() ? null : value);
        }
        
        return lineParams;
    }

    /**
     * Remplace les placeholders pour une ligne spécifique (mode masse).
     */
    private String replacePlaceholdersForLine(TemplateDefinition template, String sql, 
                                              Map<String, Object> lineParams, Map<String, Object> globalParams) {
        String result = sql;
        
        // Remplacer avec les paramètres de la ligne
        for (ParameterDefinition paramDef : template.getParameters()) {
            if (!paramDef.isFile()) {
                Object value = lineParams.get(paramDef.getName());
                if (value == null) {
                    // Si pas dans la ligne, chercher dans les paramètres globaux (ex: ticket)
                    value = globalParams.get(paramDef.getName());
                }
                
                // Gérer NULL/absence de valeur
                String replacement = buildSimpleParameterReplacement(paramDef.getType(), value);
                result = result.replace("{{" + paramDef.getName() + "}}", replacement);
            }
        }
        
        return result;
    }

}

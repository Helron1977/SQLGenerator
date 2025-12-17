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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        
        loadAllTemplates(sqlFiles);
        
        logInitializationSummary();
        createOutputDirectoryIfNeeded();
    }

    /**
     * Charge tous les templates depuis les fichiers SQL trouvés.
     * 
     * @param sqlFiles Liste des noms de fichiers SQL à charger
     */
    private void loadAllTemplates(List<String> sqlFiles) {
        for (String filename : sqlFiles) {
            loadTemplateSafely(filename);
        }
    }

    /**
     * Charge un template de manière sécurisée (ne bloque pas le démarrage en cas d'erreur).
     * 
     * @param filename Nom du fichier SQL à charger
     */
    private void loadTemplateSafely(String filename) {
        try {
            TemplateDefinition template = loadTemplateFromFile(filename);
            String sqlContent = loadSqlFromFile(template);
            validatePlaceholders(template, sqlContent, filename);
            
            templates.add(template);
            logger.debug("Template chargé : {} ({})", template.getId(), template.getName());
        } catch (Exception e) {
            logTemplateLoadError(filename, e);
        }
    }

    /**
     * Log une erreur lors du chargement d'un template.
     * 
     * @param filename Nom du fichier qui a causé l'erreur
     * @param e Exception levée
     */
    private void logTemplateLoadError(String filename, Exception e) {
        logger.error("❌ Erreur lors du parsing du fichier '{}' : {}", filename, e.getMessage(), e);
    }

    /**
     * Log le résumé de l'initialisation.
     */
    private void logInitializationSummary() {
        logger.info("Initialisation terminée : {} template(s) chargé(s) avec succès", templates.size());
        
        if (templates.isEmpty()) {
            logger.warn("⚠️  Aucun template chargé. Vérifiez que les fichiers SQL sont dans src/main/resources/templates/");
        }
    }

    /**
     * Crée le répertoire de sortie s'il n'existe pas.
     */
    private void createOutputDirectoryIfNeeded() {
        try {
            Files.createDirectories(Paths.get(appProperties.getOutputScriptsPath()));
            logger.debug("Répertoire de sortie créé/vérifié: {}", appProperties.getOutputScriptsPath());
        } catch (Exception e) {
            logOutputDirectoryCreationError(e);
        }
    }

    /**
     * Log une erreur lors de la création du répertoire de sortie.
     * 
     * @param e Exception levée
     */
    private void logOutputDirectoryCreationError(Exception e) {
        logger.warn("Impossible de créer le répertoire de sortie: {}", e.getMessage());
    }

    /**
     * Scanne le répertoire des templates pour trouver tous les fichiers SQL.
     * 
     * @return Liste des noms de fichiers SQL trouvés
     * @throws IOException Si le scan échoue
     */
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

    /**
     * Charge un template depuis un fichier SQL en parsant ses métadonnées.
     * 
     * @param filename Nom du fichier SQL à charger
     * @return Template parsé avec ses métadonnées
     * @throws IOException Si le fichier ne peut pas être lu ou parsé
     */
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

    /**
     * Valide et récupère un template par son ID.
     * 
     * @param templateId Identifiant du template
     * @return Template validé
     * @throws IllegalArgumentException Si le template n'existe pas ou n'a pas de sqlFilename
     */
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

    /**
     * Charge le contenu SQL pur (sans métadonnées) depuis le fichier du template.
     * 
     * @param template Template dont on veut charger le SQL
     * @return Contenu SQL sans métadonnées
     * @throws IOException Si le fichier ne peut pas être lu
     */
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
        Set<String> placeholders = extractPlaceholders(sqlContent);
        
        if (placeholders.isEmpty()) {
            return;
        }
        
        Set<String> definedParams = extractDefinedParameters(template);
        validateAllPlaceholdersDefined(placeholders, definedParams, filename);
        logUnusedParameters(placeholders, definedParams, filename);
    }

    /**
     * Extrait tous les placeholders du format {{nom_param}} depuis le SQL.
     * 
     * @param sqlContent Contenu SQL à analyser
     * @return Set des noms de placeholders trouvés
     */
    private Set<String> extractPlaceholders(String sqlContent) {
        Pattern placeholderPattern = Pattern.compile(TemplateConstants.PLACEHOLDER_PATTERN);
        Matcher matcher = placeholderPattern.matcher(sqlContent);
        Set<String> placeholders = new HashSet<>();
        
        while (matcher.find()) {
            placeholders.add(matcher.group(1).trim());
        }
        
        return placeholders;
    }

    /**
     * Extrait les noms des paramètres définis dans le template.
     * 
     * @param template Template à analyser
     * @return Set des noms de paramètres définis
     */
    private Set<String> extractDefinedParameters(TemplateDefinition template) {
        if (template.getParameters() == null) {
            return new HashSet<>();
        }
        
        return template.getParameters().stream()
                .map(ParameterDefinition::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Valide que tous les placeholders ont un paramètre défini.
     * 
     * @param placeholders Placeholders trouvés dans le SQL
     * @param definedParams Paramètres définis dans le template
     * @param filename Nom du fichier pour les messages d'erreur
     * @throws IllegalArgumentException Si des placeholders ne sont pas définis
     */
    private void validateAllPlaceholdersDefined(Set<String> placeholders, Set<String> definedParams, String filename) {
        Set<String> missing = findMissingPlaceholders(placeholders, definedParams);
        
        if (!missing.isEmpty()) {
            throw createMissingPlaceholdersException(missing, filename);
        }
    }

    /**
     * Trouve les placeholders qui n'ont pas de paramètre défini.
     * 
     * @param placeholders Placeholders trouvés dans le SQL
     * @param definedParams Paramètres définis dans le template
     * @return Set des placeholders manquants
     */
    private Set<String> findMissingPlaceholders(Set<String> placeholders, Set<String> definedParams) {
        Set<String> missing = new HashSet<>(placeholders);
        missing.removeAll(definedParams);
        return missing;
    }

    /**
     * Crée une exception pour les placeholders manquants.
     * 
     * @param missing Placeholders manquants
     * @param filename Nom du fichier
     * @return IllegalArgumentException avec message détaillé
     */
    private IllegalArgumentException createMissingPlaceholdersException(Set<String> missing, String filename) {
        String missingList = String.join(", ", missing);
        return new IllegalArgumentException(
            String.format(
                """
                ❌ Fichier '%s' : Placeholders non définis dans les paramètres : %s
                   Solution : Ajoutez les paramètres manquants avec -- @param: ou -- @param-file:
                   Exemple : -- @param: %s|text|Description|true
                """,
                filename, missingList, missing.iterator().next()
            )
        );
    }

    /**
     * Log les paramètres définis mais non utilisés dans le SQL.
     * 
     * @param placeholders Placeholders trouvés dans le SQL
     * @param definedParams Paramètres définis dans le template
     * @param filename Nom du fichier pour les logs
     */
    private void logUnusedParameters(Set<String> placeholders, Set<String> definedParams, String filename) {
        Set<String> unused = findUnusedParameters(placeholders, definedParams);
        
        if (!unused.isEmpty()) {
            logUnusedParametersWarning(unused, filename);
        }
    }

    /**
     * Trouve les paramètres définis mais non utilisés dans le SQL.
     * 
     * @param placeholders Placeholders trouvés dans le SQL
     * @param definedParams Paramètres définis dans le template
     * @return Set des paramètres non utilisés
     */
    private Set<String> findUnusedParameters(Set<String> placeholders, Set<String> definedParams) {
        Set<String> unused = new HashSet<>(definedParams);
        unused.removeAll(placeholders);
        return unused;
    }

    /**
     * Log un warning pour les paramètres non utilisés.
     * 
     * @param unused Paramètres non utilisés
     * @param filename Nom du fichier
     */
    private void logUnusedParametersWarning(Set<String> unused, String filename) {
        logger.warn("Fichier '{}' : Paramètres définis mais non utilisés dans le SQL : {}", 
                filename, String.join(", ", unused));
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
        if (TemplateConstants.EXECUTION_TYPE_MASSE.equals(executionType) && params.containsKey(TemplateConstants.MASSE_FILE_PARAM)) {
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

    /**
     * Remplace tous les placeholders {{param}} dans le SQL par leurs valeurs.
     * 
     * @param template Template contenant les définitions de paramètres
     * @param sql SQL avec placeholders
     * @param params Map des valeurs des paramètres
     * @return SQL avec placeholders remplacés
     */
    private String replacePlaceholders(TemplateDefinition template, String sql, Map<String, Object> params) {
        String result = sql;
        for (ParameterDefinition paramDef : template.getParameters()) {
            String replacement = buildParameterReplacement(paramDef, params.get(paramDef.getName()));
            result = replacePlaceholder(result, paramDef.getName(), replacement);
        }
        return result;
    }

    /**
     * Remplace un placeholder dans le SQL par sa valeur.
     * 
     * @param sql SQL avec placeholder
     * @param paramName Nom du paramètre
     * @param replacement Valeur de remplacement
     * @return SQL avec placeholder remplacé
     */
    private String replacePlaceholder(String sql, String paramName, String replacement) {
        String placeholder = buildPlaceholder(paramName);
        return sql.replace(placeholder, replacement);
    }

    /**
     * Construit un placeholder au format {{nom_param}}.
     * 
     * @param paramName Nom du paramètre
     * @return Placeholder formaté
     */
    private String buildPlaceholder(String paramName) {
        return "{{" + paramName + "}}";
    }

    /**
     * Construit le remplacement d'un paramètre selon son type et sa valeur.
     * 
     * @param paramDef Définition du paramètre (type, fichier, etc.)
     * @param value Valeur à utiliser pour le remplacement
     * @return Chaîne de remplacement SQL (peut être "NULL", une valeur formatée, etc.)
     */
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

    /**
     * Construit le remplacement pour un paramètre de type fichier (clause IN).
     * 
     * @param value Valeur du paramètre (peut être une List<String> ou String)
     * @return Clause IN formatée ou "NULL" si aucune valeur valide
     */
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

    /**
     * Construit le remplacement pour un paramètre simple (non-fichier).
     * 
     * @param type Type du paramètre (text, number, date)
     * @param value Valeur à formater
     * @return Valeur formatée pour SQL (avec guillemets si nécessaire, format date, etc.)
     */
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
        
        if (isDateFormatDDMMYY(trimmed)) {
            return formatDateAsDDMMYY(trimmed);
        }
        
        if (isDateFormatYYYYMMDD(trimmed)) {
            return convertToDDMMYY(trimmed);
        }
        
        return formatDateAsIs(trimmed);
    }

    /**
     * Vérifie si la date est déjà au format DD/MM/YY.
     * 
     * @param dateValue Date à vérifier
     * @return true si au format DD/MM/YY, false sinon
     */
    private boolean isDateFormatDDMMYY(String dateValue) {
        return dateValue.matches(TemplateConstants.DATE_PATTERN_DDMMYY);
    }

    /**
     * Formate une date déjà au format DD/MM/YY.
     * 
     * @param dateValue Date au format DD/MM/YY
     * @return Date formatée pour SQL avec guillemets
     */
    private String formatDateAsDDMMYY(String dateValue) {
        return "'" + dateValue + "'";
    }

    /**
     * Vérifie si la date est au format YYYY-MM-DD.
     * 
     * @param dateValue Date à vérifier
     * @return true si au format YYYY-MM-DD, false sinon
     */
    private boolean isDateFormatYYYYMMDD(String dateValue) {
        return dateValue.matches(TemplateConstants.DATE_PATTERN_YYYYMMDD);
    }

    /**
     * Convertit une date YYYY-MM-DD en DD/MM/YY en utilisant les API Java standard.
     * 
     * @param dateValue Date au format YYYY-MM-DD
     * @return Date convertie au format DD/MM/YY avec guillemets
     */
    private String convertToDDMMYY(String dateValue) {
        try {
            LocalDate date = LocalDate.parse(dateValue, DateTimeFormatter.ISO_LOCAL_DATE);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yy");
            String formatted = date.format(outputFormatter);
            return "'" + formatted + "'";
        } catch (DateTimeParseException e) {
            // Si le parsing échoue, retourner tel quel avec guillemets
            logger.debug("Impossible de parser la date '{}' : {}", dateValue, e.getMessage());
            return "'" + escapeSqlString(dateValue) + "'";
        }
    }

    /**
     * Formate une date telle quelle (responsabilité du dev SQL).
     * 
     * @param dateValue Date à formater
     * @return Date formatée avec guillemets et échappement SQL
     */
    private String formatDateAsIs(String dateValue) {
        return "'" + escapeSqlString(dateValue) + "'";
    }

    /**
     * Échappe les apostrophes dans les chaînes SQL.
     */
    private String escapeSqlString(String value) {
        return value.replace("'", "''");
    }


    /**
     * Supprime les commentaires de métadonnées (-- @id:, -- @param:, etc.) du contenu SQL.
     * 
     * @param sqlContent Contenu SQL brut avec métadonnées
     * @return SQL pur sans métadonnées
     */
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
    /**
     * Génère du SQL avec lotissement pour gérer les clauses IN > 999 valeurs.
     * 
     * @param template Template contenant les paramètres
     * @param baseSql SQL de base avec placeholders
     * @param params Map des valeurs des paramètres
     * @return SQL avec lots séparés pour la clause IN
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

    /**
     * Trouve le premier paramètre de type fichier dans les paramètres fournis.
     * 
     * @param template Template contenant les définitions de paramètres
     * @param params Map des valeurs des paramètres
     * @return Paramètre de type fichier trouvé, ou null si aucun
     */
    private ParameterDefinition findFileParameterForBatching(TemplateDefinition template, Map<String, Object> params) {
        return template.getParameters().stream()
                .filter(p -> p.isFile() && params.get(p.getName()) instanceof List)
                .findFirst()
                .orElse(null);
    }

    /**
     * Remplace tous les placeholders sauf celui du paramètre fichier (qui sera traité séparément).
     * 
     * @param template Template contenant les définitions de paramètres
     * @param sql SQL avec placeholders
     * @param params Map des valeurs des paramètres
     * @param fileParam Paramètre fichier à exclure du remplacement
     * @return SQL avec placeholders non-fichier remplacés
     */
    private String replaceNonFileParameters(TemplateDefinition template, String sql, 
                                           Map<String, Object> params, ParameterDefinition fileParam) {
        String result = sql;
        for (ParameterDefinition paramDef : template.getParameters()) {
            if (!paramDef.isFile() && !paramDef.getName().equals(fileParam.getName())) {
                String replacement = buildSimpleParameterReplacement(paramDef.getType(), params.get(paramDef.getName()));
                result = replacePlaceholder(result, paramDef.getName(), replacement);
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

    /**
     * Extrait un lot de valeurs depuis la liste complète.
     * 
     * @param values Liste complète des valeurs
     * @param batchIndex Index du lot (0-based)
     * @param batchSize Taille d'un lot
     * @return Liste des valeurs du lot
     */
    private List<String> extractBatch(List<String> values, int batchIndex, int batchSize) {
        int start = batchIndex * batchSize;
        int end = Math.min(start + batchSize, values.size());
        return values.subList(start, end);
    }

    /**
     * Ajoute un lot de SQL au résultat avec commentaire et clause IN formatée.
     * 
     * @param result StringBuilder où ajouter le SQL du lot
     * @param batchIndex Index du lot (0-based)
     * @param totalBatches Nombre total de lots
     * @param batch Valeurs du lot à insérer dans la clause IN
     * @param fileParam Paramètre fichier utilisé pour la clause IN
     * @param sqlTemplate Template SQL avec placeholder pour le paramètre fichier
     */
    private void appendBatch(StringBuilder result, int batchIndex, int totalBatches, 
                             List<String> batch, ParameterDefinition fileParam, String sqlTemplate) {
        if (batchIndex > 0) {
            result.append("\n\n");
        }

        result.append("-- Lot ").append(batchIndex + 1).append("/").append(totalBatches)
               .append(" (").append(batch.size()).append(" valeurs)\n");

        String inClause = formatSingleInClause(batch);
        String sqlForBatch = replacePlaceholder(sqlTemplate, fileParam.getName(), inClause);
        result.append(sqlForBatch);
    }

    /**
     * Formate une liste de valeurs en clause IN SQL (ex: 'val1', 'val2', 'val3').
     * 
     * @param values Liste des valeurs à formater
     * @return Clause IN formatée, ou "NULL" si la liste est vide/null
     */
    private String formatSingleInClause(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "NULL";
        }
        
        return values.stream()
                .map(this::formatInClauseValue)
                .collect(Collectors.joining(", "));
    }

    /**
     * Formate une valeur individuelle pour une clause IN SQL.
     * 
     * @param value Valeur à formater (sera trimée)
     * @return Valeur formatée : "NULL" si vide/null, sinon 'valeur' avec échappement
     */
    private String formatInClauseValue(String value) {
        String trimmed = value.trim();
        
        if (trimmed.isEmpty() || isNullValue(trimmed)) {
            return "NULL";
        }
        
        return formatInClauseValueAsString(trimmed);
    }

    /**
     * Formate une valeur non-nulle pour une clause IN SQL avec guillemets.
     * 
     * @param value Valeur à formater (déjà trimée et non-nulle)
     * @return Valeur formatée avec guillemets et échappement SQL
     */
    private String formatInClauseValueAsString(String value) {
        return "'" + escapeSqlString(value) + "'";
    }


    /**
     * Génère du SQL en mode masse : n lignes dans le fichier CSV = n requêtes SQL dans un seul fichier.
     * Format du fichier : CSV avec une ligne par requête, valeurs séparées par virgule dans l'ordre des paramètres.
     * 
     * @param template Template contenant les définitions de paramètres
     * @param baseSql SQL de base avec placeholders
     * @param params Map contenant les paramètres, dont le fichier CSV en mode masse
     * @return SQL généré avec toutes les requêtes (une par ligne CSV)
     */
    private String generateMasseSql(TemplateDefinition template, String baseSql, Map<String, Object> params) {
        List<String> fileLines = extractCsvLines(params);
        if (fileLines == null || fileLines.isEmpty()) {
            return baseSql;
        }

        List<ParameterDefinition> orderedParams = extractOrderedNonFileParameters(template);
        
        return generateSqlForAllLines(template, baseSql, fileLines, orderedParams, params);
    }

    /**
     * Extrait les lignes du fichier CSV depuis les paramètres.
     * 
     * @param params Map des paramètres
     * @return Liste des lignes CSV, ou null si absent
     */
    @SuppressWarnings("unchecked")
    private List<String> extractCsvLines(Map<String, Object> params) {
        return (List<String>) params.get(TemplateConstants.MASSE_FILE_PARAM);
    }

    /**
     * Extrait la liste ordonnée des paramètres non-fichier du template.
     * 
     * @param template Template contenant les paramètres
     * @return Liste ordonnée des paramètres non-fichier
     */
    private List<ParameterDefinition> extractOrderedNonFileParameters(TemplateDefinition template) {
        return template.getParameters().stream()
                .filter(p -> !p.isFile())
                .collect(Collectors.toList());
    }

    /**
     * Génère le SQL pour toutes les lignes CSV.
     * 
     * @param template Template contenant les définitions de paramètres
     * @param baseSql SQL de base avec placeholders
     * @param fileLines Lignes du fichier CSV
     * @param orderedParams Paramètres ordonnés (non-fichier)
     * @param globalParams Paramètres globaux (ticket, etc.)
     * @return SQL généré avec toutes les requêtes
     */
    private String generateSqlForAllLines(TemplateDefinition template, String baseSql, 
                                          List<String> fileLines, List<ParameterDefinition> orderedParams,
                                          Map<String, Object> globalParams) {
        MassGenerationContext context = new MassGenerationContext(template, baseSql, orderedParams, globalParams);
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < fileLines.size(); i++) {
            if (i > 0) {
                result.append("\n\n");
            }
            
            String sqlForLine = generateSqlForSingleLine(context, fileLines.get(i), i + 1, fileLines.size());
            result.append(sqlForLine);
        }
        
        return result.toString();
    }

    /**
     * Génère le SQL pour une seule ligne CSV.
     * 
     * @param context Contexte de génération (template, SQL, paramètres)
     * @param csvLine Ligne CSV à traiter
     * @param lineNumber Numéro de la ligne (1-based)
     * @param totalLines Nombre total de lignes
     * @return SQL généré pour cette ligne avec header de commentaire
     */
    private String generateSqlForSingleLine(MassGenerationContext context, String csvLine,
                                           int lineNumber, int totalLines) {
        Map<String, Object> lineParams = parseCsvLine(csvLine, context.orderedParams());
        String sqlForLine = replacePlaceholdersForLine(context.template(), context.baseSql(), 
                                                       lineParams, context.globalParams());
        
        return formatQueryHeader(lineNumber, totalLines) + sqlForLine;
    }

    /**
     * Contexte de génération pour le mode masse.
     * Regroupe les paramètres nécessaires à la génération SQL.
     */
    private record MassGenerationContext(
        TemplateDefinition template,
        String baseSql,
        List<ParameterDefinition> orderedParams,
        Map<String, Object> globalParams
    ) {}

    /**
     * Formate le header de commentaire pour une requête (ex: "-- Requête 1/5\n").
     * 
     * @param lineNumber Numéro de la ligne (1-based)
     * @param totalLines Nombre total de lignes
     * @return Header formaté avec commentaire SQL
     */
    private String formatQueryHeader(int lineNumber, int totalLines) {
        return "-- Requête " + lineNumber + "/" + totalLines + "\n";
    }


    /**
     * Parse une ligne CSV et crée un Map avec les valeurs dans l'ordre des paramètres.
     * 
     * @param line Ligne CSV à parser
     * @param orderedParams Liste ordonnée des paramètres (non-fichier)
     * @return Map associant chaque paramètre à sa valeur (null si vide)
     */
    private Map<String, Object> parseCsvLine(String line, List<ParameterDefinition> orderedParams) {
        if (line == null || orderedParams == null || orderedParams.isEmpty()) {
            return new HashMap<>();
        }
        
        String[] values = line.split(",");
        int maxIndex = Math.min(values.length, orderedParams.size());
        
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < maxIndex; i++) {
            String paramName = orderedParams.get(i).getName();
            Object value = extractCsvValue(values[i]);
            result.put(paramName, value);
        }
        return result;
    }

    /**
     * Extrait et nettoie une valeur CSV (trim et conversion null si vide).
     * 
     * @param rawValue Valeur brute du CSV
     * @return Valeur trimée, ou null si vide
     */
    private Object extractCsvValue(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String trimmed = rawValue.trim();
        // Retourner null pour les valeurs vides (sera géré comme NULL dans buildSimpleParameterReplacement)
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Remplace les placeholders pour une ligne spécifique (mode masse).
     */
    /**
     * Remplace les placeholders pour une ligne spécifique (mode masse).
     * 
     * Les valeurs sont d'abord cherchées dans lineParams (valeurs de la ligne CSV),
     * puis dans globalParams (paramètres globaux comme "ticket").
     * 
     * @param template Template contenant les définitions de paramètres
     * @param sql SQL avec placeholders
     * @param lineParams Paramètres spécifiques à la ligne CSV
     * @param globalParams Paramètres globaux (ticket, etc.)
     * @return SQL avec placeholders remplacés pour cette ligne
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
                result = replacePlaceholder(result, paramDef.getName(), replacement);
            }
        }
        
        return result;
    }

}

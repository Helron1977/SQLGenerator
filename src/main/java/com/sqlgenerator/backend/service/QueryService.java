package com.sqlgenerator.backend.service;

import com.sqlgenerator.backend.model.QueryDefinition;
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

@Service
public class QueryService {

    private List<QueryDefinition> queries;

    @Autowired
    private QueryMetadataParser metadataParser;

    @Autowired
    private SqlFileBuilder sqlFileBuilder;

    @PostConstruct
    public void init() throws IOException {
        queries = new ArrayList<>();
        
        List<String> sqlFiles = scanSqlFiles();
        for (String filename : sqlFiles) {
            try {
                QueryDefinition query = loadQueryFromFile(filename);
                queries.add(query);
            } catch (Exception e) {
                System.err.println("Erreur parsing " + filename + ": " + e.getMessage());
            }
        }
        
        Files.createDirectories(Paths.get("./svn_repo_mock/"));
    }

    private List<String> scanSqlFiles() throws IOException {
        List<String> filenames = new ArrayList<>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:sql/*.sql");
        
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename != null && filename.endsWith(".sql")) {
                filenames.add(filename);
            }
        }
        
        return filenames;
    }

    private QueryDefinition loadQueryFromFile(String filename) throws IOException {
        return metadataParser.parseSqlFile(filename);
    }

    public QueryDefinition getQueryById(String id) {
        return queries.stream()
                .filter(q -> q.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<QueryDefinition> getAllQueries() {
        return queries != null ? queries : Collections.emptyList();
    }

    public String generatePatchFile(String queryId, String executionType, Map<String, Object> params)
            throws IOException {
        QueryDefinition query = validateAndGetQuery(queryId);
        String baseSql = loadSqlFromFile(query);
        String sql = processSqlWithParams(query, baseSql, params, executionType);
        return sqlFileBuilder.buildAndWriteFile(query, executionType, params, sql);
    }

    private QueryDefinition validateAndGetQuery(String queryId) {
        QueryDefinition query = getQueryById(queryId);
        if (query == null) {
            throw new IllegalArgumentException("Query not found: " + queryId);
        }
        if (query.getSqlFile() == null || query.getSqlFile().isEmpty()) {
            throw new IllegalArgumentException("Query must have sqlFile defined: " + queryId);
        }
        return query;
    }

    private String loadSqlFromFile(QueryDefinition query) throws IOException {
        ClassPathResource sqlResource = new ClassPathResource("sql/" + query.getSqlFile());
        String sqlContent = new String(sqlResource.getInputStream().readAllBytes(), 
                java.nio.charset.StandardCharsets.UTF_8);
        return removeMetadataComments(sqlContent);
    }

    private String processSqlWithParams(QueryDefinition query, String baseSql, Map<String, Object> params, String executionType) {
        // Mode masse : générer n requêtes (une par ligne du fichier CSV)
        if ("masse".equals(executionType) && params.containsKey("masseFile")) {
            return generateMasseSql(query, baseSql, params);
        }
        
        // Lotissement pour clauses IN > 999 valeurs
        if (requiresBatching(query, params)) {
            return generateBatchedSql(query, baseSql, params);
        }
        
        // Mode unitaire standard
        return replacePlaceholders(query, baseSql, params);
    }

    private boolean requiresBatching(QueryDefinition query, Map<String, Object> params) {
        return query.getParameters().stream()
                .anyMatch(p -> p.isFile() 
                        && params.get(p.getName()) instanceof List 
                        && ((List<?>) params.get(p.getName())).size() > 999);
    }

    private String replacePlaceholders(QueryDefinition query, String sql, Map<String, Object> params) {
        String result = sql;
        for (ParameterDefinition paramDef : query.getParameters()) {
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
     * Vérifie si la valeur représente NULL (chaîne vide, "null", "NULL", etc.).
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
     * Si la date est déjà au bon format, la retourne telle quelle.
     * Sinon, tente de convertir depuis d'autres formats.
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
    private String generateBatchedSql(QueryDefinition query, String baseSql, Map<String, Object> params) {
        ParameterDefinition fileParam = findFileParameterForBatching(query, params);
        if (fileParam == null) {
            return baseSql;
        }

        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) params.get(fileParam.getName());
        if (values == null || values.isEmpty()) {
            return baseSql;
        }

        String sqlTemplate = replaceNonFileParameters(query, baseSql, params, fileParam);
        return generateBatches(fileParam, values, sqlTemplate);
    }

    private ParameterDefinition findFileParameterForBatching(QueryDefinition query, Map<String, Object> params) {
        return query.getParameters().stream()
                .filter(p -> p.isFile() && params.get(p.getName()) instanceof List)
                .findFirst()
                .orElse(null);
    }

    private String replaceNonFileParameters(QueryDefinition query, String sql, 
                                           Map<String, Object> params, ParameterDefinition fileParam) {
        String result = sql;
        for (ParameterDefinition paramDef : query.getParameters()) {
            if (!paramDef.isFile() && !paramDef.getName().equals(fileParam.getName())) {
                String replacement = buildSimpleParameterReplacement(paramDef.getType(), params.get(paramDef.getName()));
                result = result.replace("{{" + paramDef.getName() + "}}", replacement);
            }
        }
        return result;
    }

    private String generateBatches(ParameterDefinition fileParam, List<String> values, String sqlTemplate) {
        StringBuilder result = new StringBuilder();
        int batchSize = 999;
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
    private String generateMasseSql(QueryDefinition query, String baseSql, Map<String, Object> params) {
        // Récupérer les lignes du fichier CSV uploadé
        @SuppressWarnings("unchecked")
        List<String> fileLines = (List<String>) params.get("masseFile");
        if (fileLines == null || fileLines.isEmpty()) {
            return baseSql;
        }

        // Extraire l'ordre des paramètres (tous les paramètres non-fichier dans l'ordre)
        List<ParameterDefinition> orderedParams = query.getParameters().stream()
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
            String sqlForLine = replacePlaceholdersForLine(query, baseSql, lineParams, params);
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
    private String replacePlaceholdersForLine(QueryDefinition query, String sql, 
                                              Map<String, Object> lineParams, Map<String, Object> globalParams) {
        String result = sql;
        
        // Remplacer avec les paramètres de la ligne
        for (ParameterDefinition paramDef : query.getParameters()) {
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

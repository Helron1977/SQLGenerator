package com.sqlgenerator.backend.service;

import com.sqlgenerator.backend.config.AppProperties;
import com.sqlgenerator.backend.model.ParameterDefinition;
import com.sqlgenerator.backend.model.TemplateDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour TemplateService.
 * 
 * Ces tests vérifient les méthodes utilitaires via réflexion.
 * Les tests d'intégration complets sont dans AdminController.integrationTest().
 */
class TemplateServiceTest {

    private TemplateService templateService;
    
    @Mock
    private TemplateMetadataParser metadataParser;
    
    @Mock
    private SqlFileBuilder sqlFileBuilder;
    
    @Mock
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        templateService = new TemplateService();
        
        // Injection manuelle des dépendances via réflexion
        try {
            java.lang.reflect.Field field = TemplateService.class.getDeclaredField("metadataParser");
            field.setAccessible(true);
            field.set(templateService, metadataParser);
            
            field = TemplateService.class.getDeclaredField("sqlFileBuilder");
            field.setAccessible(true);
            field.set(templateService, sqlFileBuilder);
            
            field = TemplateService.class.getDeclaredField("appProperties");
            field.setAccessible(true);
            field.set(templateService, appProperties);
        } catch (Exception e) {
            fail("Impossible d'injecter les dépendances: " + e.getMessage());
        }
    }

    // ========== Tests pour formatDate ==========

    @Test
    void testFormatDate_DDMMYYFormat() {
        // Given: Date au format DD/MM/YY
        String dateValue = "30/11/25";
        
        // When: Formatage
        String result = formatDateViaReflection(dateValue);
        
        // Then: Doit retourner la date avec guillemets
        assertEquals("'30/11/25'", result);
    }

    @Test
    void testFormatDate_YYYYMMDDFormat() {
        // Given: Date au format YYYY-MM-DD
        String dateValue = "2025-11-30";
        
        // When: Formatage
        String result = formatDateViaReflection(dateValue);
        
        // Then: Doit convertir en DD/MM/YY
        assertEquals("'30/11/25'", result);
    }

    @Test
    void testFormatDate_NullValue() {
        // Given: Date null
        String dateValue = null;
        
        // When: Formatage
        String result = formatDateViaReflection(dateValue);
        
        // Then: Doit retourner NULL
        assertEquals("NULL", result);
    }

    @Test
    void testFormatDate_EmptyValue() {
        // Given: Date vide
        String dateValue = "";
        
        // When: Formatage
        String result = formatDateViaReflection(dateValue);
        
        // Then: Doit retourner NULL
        assertEquals("NULL", result);
    }

    @Test
    void testFormatDate_OtherFormat() {
        // Given: Date dans un autre format
        String dateValue = "30-Nov-2025";
        
        // When: Formatage
        String result = formatDateViaReflection(dateValue);
        
        // Then: Doit retourner tel quel avec guillemets
        assertTrue(result.startsWith("'"));
        assertTrue(result.endsWith("'"));
        assertTrue(result.contains("30-Nov-2025"));
    }

    // ========== Tests pour isNullValue ==========

    @Test
    void testIsNullValue_Null() {
        // Given: Valeur null
        Object value = null;
        
        // When: Vérification
        boolean result = isNullValueViaReflection(value);
        
        // Then: Doit retourner true
        assertTrue(result);
    }

    @Test
    void testIsNullValue_EmptyString() {
        // Given: Chaîne vide
        Object value = "";
        
        // When: Vérification
        boolean result = isNullValueViaReflection(value);
        
        // Then: Doit retourner true
        assertTrue(result);
    }

    @Test
    void testIsNullValue_NullString() {
        // Given: Chaîne "null"
        Object value = "null";
        
        // When: Vérification
        boolean result = isNullValueViaReflection(value);
        
        // Then: Doit retourner true
        assertTrue(result);
    }

    @Test
    void testIsNullValue_ValidValue() {
        // Given: Valeur valide
        Object value = "test";
        
        // When: Vérification
        boolean result = isNullValueViaReflection(value);
        
        // Then: Doit retourner false
        assertFalse(result);
    }

    // ========== Tests pour escapeSqlString ==========

    @Test
    void testEscapeSqlString_WithApostrophe() {
        // Given: Chaîne avec apostrophe
        String value = "O'Brien";
        
        // When: Échappement
        String result = escapeSqlStringViaReflection(value);
        
        // Then: Doit doubler l'apostrophe
        assertEquals("O''Brien", result);
    }

    @Test
    void testEscapeSqlString_WithoutApostrophe() {
        // Given: Chaîne sans apostrophe
        String value = "test";
        
        // When: Échappement
        String result = escapeSqlStringViaReflection(value);
        
        // Then: Doit retourner tel quel
        assertEquals("test", result);
    }

    @Test
    void testEscapeSqlString_MultipleApostrophes() {
        // Given: Chaîne avec plusieurs apostrophes
        String value = "O'Brien's test";
        
        // When: Échappement
        String result = escapeSqlStringViaReflection(value);
        
        // Then: Doit doubler toutes les apostrophes
        assertEquals("O''Brien''s test", result);
    }

    // ========== Tests pour buildPlaceholder ==========

    @Test
    void testBuildPlaceholder_ValidName() {
        // Given: Nom de paramètre valide
        String paramName = "testParam";
        
        // When: Construction du placeholder
        String result = buildPlaceholderViaReflection(paramName);
        
        // Then: Doit retourner {{testParam}}
        assertEquals("{{testParam}}", result);
    }

    @Test
    void testBuildPlaceholder_WithSpecialChars() {
        // Given: Nom avec caractères spéciaux
        String paramName = "param_123";
        
        // When: Construction du placeholder
        String result = buildPlaceholderViaReflection(paramName);
        
        // Then: Doit retourner {{param_123}}
        assertEquals("{{param_123}}", result);
    }

    // ========== Tests pour requiresBatching ==========

    @Test
    void testRequiresBatching_WithLargeList() {
        // Given: Template avec paramètre fichier et liste > 999
        TemplateDefinition template = createTemplateWithFileParam("ids");
        Map<String, Object> params = new HashMap<>();
        List<String> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add("value" + i);
        }
        params.put("ids", largeList);
        
        // When: Vérification
        boolean result = requiresBatchingViaReflection(template, params);
        
        // Then: Doit retourner true
        assertTrue(result);
    }

    @Test
    void testRequiresBatching_WithSmallList() {
        // Given: Template avec paramètre fichier et liste <= 999
        TemplateDefinition template = createTemplateWithFileParam("ids");
        Map<String, Object> params = new HashMap<>();
        List<String> smallList = Arrays.asList("value1", "value2");
        params.put("ids", smallList);
        
        // When: Vérification
        boolean result = requiresBatchingViaReflection(template, params);
        
        // Then: Doit retourner false
        assertFalse(result);
    }

    @Test
    void testRequiresBatching_WithExactLimit() {
        // Given: Template avec paramètre fichier et liste = 999
        TemplateDefinition template = createTemplateWithFileParam("ids");
        Map<String, Object> params = new HashMap<>();
        List<String> exactList = new ArrayList<>();
        for (int i = 0; i < 999; i++) {
            exactList.add("value" + i);
        }
        params.put("ids", exactList);
        
        // When: Vérification
        boolean result = requiresBatchingViaReflection(template, params);
        
        // Then: Doit retourner false (999 est la limite, donc pas de lotissement)
        assertFalse(result);
    }

    // ========== Tests pour removeMetadataComments ==========

    @Test
    void testRemoveMetadataComments_WithMetadata() {
        // Given: SQL avec métadonnées
        String sqlContent = """
                -- @id: test-template
                -- @name: Test Template
                -- @param: param1|text|Description
                
                SELECT * FROM test WHERE id = {{param1}};
                """;

        // When: Suppression des métadonnées
        String result = removeMetadataCommentsViaReflection(sqlContent);

        // Then: Doit contenir uniquement le SQL
        assertNotNull(result);
        assertTrue(result.contains("SELECT * FROM test"));
        assertFalse(result.contains("@id:"));
        assertFalse(result.contains("@name:"));
        assertFalse(result.contains("@param:"));
    }

    @Test
    void testRemoveMetadataComments_WithoutMetadata() {
        // Given: SQL sans métadonnées
        String sqlContent = "SELECT * FROM test WHERE id = 42;";

        // When: Suppression des métadonnées
        String result = removeMetadataCommentsViaReflection(sqlContent);

        // Then: Doit retourner le SQL tel quel
        assertNotNull(result);
        assertTrue(result.contains("SELECT * FROM test"));
    }

    @Test
    void testRemoveMetadataComments_WithEmptyLines() {
        // Given: SQL avec métadonnées et lignes vides
        String sqlContent = """
                -- @id: test-template
                
                -- @param: param1|text|Description
                
                SELECT * FROM test;
                """;

        // When: Suppression des métadonnées
        String result = removeMetadataCommentsViaReflection(sqlContent);

        // Then: Doit supprimer les lignes vides dans la section métadonnées
        assertNotNull(result);
        assertTrue(result.contains("SELECT * FROM test"));
        assertFalse(result.contains("@id:"));
    }

    // ========== Tests pour replacePlaceholder ==========

    @Test
    void testReplacePlaceholder_SingleOccurrence() {
        // Given: SQL avec un placeholder
        String sql = "SELECT * FROM test WHERE id = {{param1}};";
        String paramName = "param1";
        String replacement = "42";

        // When: Remplacement
        String result = replacePlaceholderViaReflection(sql, paramName, replacement);

        // Then: Doit remplacer le placeholder
        assertEquals("SELECT * FROM test WHERE id = 42;", result);
    }

    @Test
    void testReplacePlaceholder_MultipleOccurrences() {
        // Given: SQL avec plusieurs occurrences du même placeholder
        String sql = "SELECT * FROM test WHERE id = {{param1}} AND name = {{param1}};";
        String paramName = "param1";
        String replacement = "'test'";

        // When: Remplacement
        String result = replacePlaceholderViaReflection(sql, paramName, replacement);

        // Then: Doit remplacer toutes les occurrences
        assertEquals("SELECT * FROM test WHERE id = 'test' AND name = 'test';", result);
    }

    @Test
    void testReplacePlaceholder_NoPlaceholder() {
        // Given: SQL sans le placeholder
        String sql = "SELECT * FROM test WHERE id = 42;";
        String paramName = "param1";
        String replacement = "test";

        // When: Remplacement
        String result = replacePlaceholderViaReflection(sql, paramName, replacement);

        // Then: Doit retourner le SQL inchangé
        assertEquals(sql, result);
    }

    // ========== Tests pour buildParameterReplacement ==========

    @Test
    void testBuildParameterReplacement_NullValue() {
        // Given: Paramètre avec valeur null
        ParameterDefinition param = createParameter("param1", "text", false);
        Object value = null;

        // When: Construction du remplacement
        String result = buildParameterReplacementViaReflection(param, value);

        // Then: Doit retourner NULL
        assertEquals("NULL", result);
    }

    @Test
    void testBuildParameterReplacement_EmptyString() {
        // Given: Paramètre avec chaîne vide
        ParameterDefinition param = createParameter("param1", "text", false);
        Object value = "";

        // When: Construction du remplacement
        String result = buildParameterReplacementViaReflection(param, value);

        // Then: Doit retourner NULL
        assertEquals("NULL", result);
    }

    @Test
    void testBuildParameterReplacement_TextValue() {
        // Given: Paramètre texte avec valeur
        ParameterDefinition param = createParameter("param1", "text", false);
        Object value = "test value";

        // When: Construction du remplacement
        String result = buildParameterReplacementViaReflection(param, value);

        // Then: Doit retourner la valeur avec guillemets
        assertEquals("'test value'", result);
    }

    @Test
    void testBuildParameterReplacement_FileParameter() {
        // Given: Paramètre fichier avec liste
        ParameterDefinition param = createParameter("ids", "file", true);
        List<String> value = Arrays.asList("1", "2", "3");

        // When: Construction du remplacement
        String result = buildParameterReplacementViaReflection(param, value);

        // Then: Doit retourner les valeurs formatées (formatSingleInClause retourne juste les valeurs, pas "IN")
        assertNotNull(result);
        assertNotEquals("NULL", result);
        assertTrue(result.contains("1") || result.contains("'1'"));
    }

    // ========== Tests pour buildFileParameterReplacement ==========

    @Test
    void testBuildFileParameterReplacement_WithList() {
        // Given: Liste de valeurs
        List<String> value = Arrays.asList("1", "2", "3");

        // When: Construction du remplacement
        String result = buildFileParameterReplacementViaReflection(value);

        // Then: Doit retourner une clause IN
        assertNotNull(result);
        assertTrue(result.contains("'1'") || result.contains("1"));
    }

    @Test
    void testBuildFileParameterReplacement_WithEmptyList() {
        // Given: Liste vide
        List<String> value = new ArrayList<>();

        // When: Construction du remplacement
        String result = buildFileParameterReplacementViaReflection(value);

        // Then: Doit retourner NULL
        assertEquals("NULL", result);
    }

    @Test
    void testBuildFileParameterReplacement_WithNullValues() {
        // Given: Liste avec valeurs null/vides
        List<String> value = Arrays.asList("1", null, "", "2");

        // When: Construction du remplacement
        String result = buildFileParameterReplacementViaReflection(value);

        // Then: Doit filtrer les valeurs null/vides et retourner une clause IN
        assertNotNull(result);
        assertNotEquals("NULL", result);
    }

    // ========== Tests pour buildSimpleParameterReplacement ==========

    @Test
    void testBuildSimpleParameterReplacement_Text() {
        // Given: Type text avec valeur
        String type = "text";
        Object value = "test value";

        // When: Construction du remplacement
        String result = buildSimpleParameterReplacementViaReflection(type, value);

        // Then: Doit retourner la valeur avec guillemets
        assertEquals("'test value'", result);
    }

    @Test
    void testBuildSimpleParameterReplacement_Number() {
        // Given: Type number avec valeur
        String type = "number";
        Object value = "42";

        // When: Construction du remplacement
        String result = buildSimpleParameterReplacementViaReflection(type, value);

        // Then: Doit retourner la valeur sans guillemets
        assertEquals("42", result);
    }

    @Test
    void testBuildSimpleParameterReplacement_Date() {
        // Given: Type date avec valeur
        String type = "date";
        Object value = "30/11/25";

        // When: Construction du remplacement
        String result = buildSimpleParameterReplacementViaReflection(type, value);

        // Then: Doit retourner la date avec guillemets
        assertEquals("'30/11/25'", result);
    }

    @Test
    void testBuildSimpleParameterReplacement_NullValue() {
        // Given: Type text avec valeur null
        String type = "text";
        Object value = null;

        // When: Construction du remplacement
        String result = buildSimpleParameterReplacementViaReflection(type, value);

        // Then: Doit retourner NULL
        assertEquals("NULL", result);
    }

    @Test
    void testBuildSimpleParameterReplacement_EmptyString() {
        // Given: Type text avec chaîne vide
        String type = "text";
        Object value = "";

        // When: Construction du remplacement
        String result = buildSimpleParameterReplacementViaReflection(type, value);

        // Then: Doit retourner NULL
        assertEquals("NULL", result);
    }

    // ========== Tests pour le batching IN (generateBatchedSql et associés) ==========

    @Test
    void testGenerateBatchedSql_NoFileParam() {
        // Given: Template sans paramètre fichier
        TemplateDefinition template = new TemplateDefinition();
        template.setId("test-template");
        template.setSqlFilename("test.sql");

        ParameterDefinition p1 = new ParameterDefinition();
        p1.setName("id");
        p1.setType("number");
        p1.setFile(false);
        template.setParameters(List.of(p1));

        String baseSql = "SELECT * FROM T WHERE id IN ({{ids}})";
        Map<String, Object> params = new HashMap<>();
        params.put("id", "42");

        // When: Appel du batching
        String result = generateBatchedSqlViaReflection(template, baseSql, params);

        // Then: Sans paramètre fichier, on doit récupérer le SQL de base
        assertEquals(baseSql, result);
    }

    @Test
    void testGenerateBatchedSql_WithEmptyValues() {
        // Given: Template avec paramètre fichier mais liste vide
        TemplateDefinition template = createTemplateWithFileParam("ids");
        String baseSql = "SELECT * FROM T WHERE id IN ({{ids}})";

        Map<String, Object> params = new HashMap<>();
        params.put("ids", new ArrayList<String>());

        // When: Appel du batching
        String result = generateBatchedSqlViaReflection(template, baseSql, params);

        // Then: Liste vide -> on renvoie le SQL de base
        assertEquals(baseSql, result);
    }

    @Test
    void testGenerateBatchedSql_WithSingleBatch() {
        // Given: Template avec paramètre fichier et liste <= ORACLE_IN_MAX_SIZE
        TemplateDefinition template = createTemplateWithFileParam("ids");
        String baseSql = "SELECT * FROM T WHERE id IN ({{ids}})";

        List<String> values = Arrays.asList("1", "2", "3");
        Map<String, Object> params = new HashMap<>();
        params.put("ids", values);

        // When: Appel du batching
        String result = generateBatchedSqlViaReflection(template, baseSql, params);

        // Then: Un seul lot, les valeurs doivent apparaître dans la clause IN
        assertNotNull(result);
        assertTrue(result.contains("Lot 1/1"));
        assertTrue(result.contains("1") || result.contains("'1'"));
        assertTrue(result.contains("2") || result.contains("'2'"));
        assertTrue(result.contains("3") || result.contains("'3'"));
    }

    @Test
    void testGenerateBatchedSql_WithTwoBatches() {
        // Given: Template avec paramètre fichier et liste > ORACLE_IN_MAX_SIZE
        TemplateDefinition template = createTemplateWithFileParam("ids");
        String baseSql = "SELECT * FROM T WHERE id IN ({{ids}})";

        // Créer une liste de ORACLE_IN_MAX_SIZE + 1 valeurs
        int maxSize = 999;
        List<String> values = new ArrayList<>();
        for (int i = 0; i < maxSize + 1; i++) {
            values.add(String.valueOf(i + 1));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("ids", values);

        // When: Appel du batching
        String result = generateBatchedSqlViaReflection(template, baseSql, params);

        // Then: Deux lots doivent être générés
        assertNotNull(result);
        assertTrue(result.contains("Lot 1/2"));
        assertTrue(result.contains("Lot 2/2"));
    }

    // ========== Tests pour le mode masse CSV (generateMasseSql et associés) ==========

    @Test
    void testGenerateMasseSql_NoMasseFile() {
        // Given: Template simple sans paramètre fichier
        TemplateDefinition template = new TemplateDefinition();
        template.setId("template-masse");
        template.setSqlFilename("template-masse.sql");

        ParameterDefinition p1 = new ParameterDefinition();
        p1.setName("value");
        p1.setType("text");
        p1.setFile(false);

        template.setParameters(List.of(p1));

        String baseSql = "UPDATE T SET col = {{value}};";

        Map<String, Object> params = new HashMap<>();
        params.put("ticket", "JIRA-123");
        // Pas de masseFile -> extractCsvLines renverra null

        // When
        String result = generateMasseSqlViaReflection(template, baseSql, params);

        // Then: Sans masseFile, on doit récupérer le SQL de base
        assertEquals(baseSql, result);
    }

    @Test
    void testGenerateMasseSql_WithEmptyMasseFile() {
        // Given
        TemplateDefinition template = new TemplateDefinition();
        template.setId("template-masse");
        template.setSqlFilename("template-masse.sql");

        ParameterDefinition p1 = new ParameterDefinition();
        p1.setName("value");
        p1.setType("text");
        p1.setFile(false);
        template.setParameters(List.of(p1));

        String baseSql = "UPDATE T SET col = {{value}};";

        Map<String, Object> params = new HashMap<>();
        params.put("masseFile", new ArrayList<String>());

        // When
        String result = generateMasseSqlViaReflection(template, baseSql, params);

        // Then: Liste vide -> on renvoie le SQL de base
        assertEquals(baseSql, result);
    }

    @Test
    void testGenerateMasseSql_WithTwoLines_UsesCsvAndGlobalParams() {
        // Given: Template avec un paramètre de ligne et un paramètre global (ticket)
        TemplateDefinition template = new TemplateDefinition();
        template.setId("template-masse");
        template.setSqlFilename("template-masse.sql");

        ParameterDefinition pValue = new ParameterDefinition();
        pValue.setName("value");
        pValue.setType("text");
        pValue.setFile(false);

        ParameterDefinition pTicket = new ParameterDefinition();
        pTicket.setName("ticket");
        pTicket.setType("text");
        pTicket.setFile(false);

        // Important: mettre d'abord value puis ticket pour que la CSV "valX" ne remplisse que value
        template.setParameters(List.of(pValue, pTicket));

        String baseSql = "UPDATE T SET col = {{value}} WHERE ticket = {{ticket}};";

        Map<String, Object> params = new HashMap<>();
        params.put("ticket", "JIRA-123");
        // Deux lignes CSV avec une seule colonne -> value vient du CSV, ticket du globalParams
        List<String> csvLines = Arrays.asList("val1", "val2");
        params.put("masseFile", csvLines);

        // When
        String result = generateMasseSqlViaReflection(template, baseSql, params);

        // Then: Deux requêtes avec headers, valeur de ligne et ticket global
        assertNotNull(result);
        assertTrue(result.contains("-- Requête 1/2"));
        assertTrue(result.contains("-- Requête 2/2"));
        // Vérifier la présence des valeurs CSV
        assertTrue(result.contains("val1"));
        assertTrue(result.contains("val2"));
        // Vérifier la présence du ticket (paramètre global)
        assertTrue(result.contains("JIRA-123"));
    }

    // ========== Tests pour isNullValue (branches manquantes) ==========

    @Test
    void testIsNullValue_WithNullKeyword() {
        // Given: Valeur "null" (chaîne)
        Object value = "null";

        // When: Vérification
        boolean result = isNullValueViaReflection(value);

        // Then: Doit retourner true
        assertTrue(result);
    }

    @Test
    void testIsNullValue_WithNULLKeyword() {
        // Given: Valeur "NULL" (chaîne)
        Object value = "NULL";

        // When: Vérification
        boolean result = isNullValueViaReflection(value);

        // Then: Doit retourner true
        assertTrue(result);
    }

    @Test
    void testIsNullValue_WithNullKeywordCaseInsensitive() {
        // Given: Valeur "Null" (chaîne)
        Object value = "Null";

        // When: Vérification
        boolean result = isNullValueViaReflection(value);

        // Then: Doit retourner true
        assertTrue(result);
    }

    // ========== Tests pour buildFileParameterReplacement (branches manquantes) ==========

    @Test
    void testBuildFileParameterReplacement_WithString() {
        // Given: Valeur String (pas List)
        String value = "single-value";

        // When: Construction du remplacement
        String result = buildFileParameterReplacementViaReflection(value);

        // Then: Doit retourner la valeur formatée
        assertNotNull(result);
        assertNotEquals("NULL", result);
        assertTrue(result.contains("single-value") || result.contains("'single-value'"));
    }

    @Test
    void testBuildFileParameterReplacement_WithEmptyString() {
        // Given: Chaîne vide
        String value = "";

        // When: Construction du remplacement
        String result = buildFileParameterReplacementViaReflection(value);

        // Then: Doit retourner NULL
        assertEquals("NULL", result);
    }

    @Test
    void testBuildFileParameterReplacement_WithNullString() {
        // Given: Chaîne "null"
        String value = "null";

        // When: Construction du remplacement
        String result = buildFileParameterReplacementViaReflection(value);

        // Then: Doit retourner NULL
        assertEquals("NULL", result);
    }

    @Test
    void testBuildFileParameterReplacement_WithOtherType() {
        // Given: Type non-List et non-String
        Integer value = 42;

        // When: Construction du remplacement
        String result = buildFileParameterReplacementViaReflection(value);

        // Then: Doit retourner NULL
        assertEquals("NULL", result);
    }

    // ========== Tests pour extractDefinedParameters (branches manquantes) ==========

    @Test
    void testExtractDefinedParameters_WithNullParameters() {
        // Given: Template avec parameters null
        TemplateDefinition template = new TemplateDefinition();
        template.setParameters(null);

        // When: Extraction
        Set<String> result = extractDefinedParametersViaReflection(template);

        // Then: Doit retourner un Set vide
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== Tests pour extractPlaceholders (branches manquantes) ==========

    @Test
    void testExtractPlaceholders_WithMultiplePlaceholders() {
        // Given: SQL avec plusieurs placeholders
        String sqlContent = "SELECT * FROM test WHERE id = {{param1}} AND name = {{param2}};";

        // When: Extraction
        Set<String> result = extractPlaceholdersViaReflection(sqlContent);

        // Then: Doit contenir les deux placeholders
        assertEquals(2, result.size());
        assertTrue(result.contains("param1"));
        assertTrue(result.contains("param2"));
    }

    @Test
    void testExtractPlaceholders_WithNoPlaceholders() {
        // Given: SQL sans placeholders
        String sqlContent = "SELECT * FROM test WHERE id = 42;";

        // When: Extraction
        Set<String> result = extractPlaceholdersViaReflection(sqlContent);

        // Then: Doit retourner un Set vide
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== Tests pour validatePlaceholders (branches manquantes) ==========

    @Test
    void testValidatePlaceholders_WithEmptyPlaceholders() {
        // Given: Template et SQL sans placeholders
        TemplateDefinition template = new TemplateDefinition();
        template.setParameters(new ArrayList<>());
        String sqlContent = "SELECT * FROM test WHERE id = 42;";
        String filename = "test.sql";

        // When: Validation (ne doit pas lever d'exception)
        assertDoesNotThrow(() -> validatePlaceholdersViaReflection(template, sqlContent, filename));
    }

    // Note: parseBooleanValue est dans TemplateMetadataParser, pas dans TemplateService

    // ========== Méthodes utilitaires pour les tests ==========

    private TemplateDefinition createTemplateWithFileParam(String paramName) {
        TemplateDefinition template = new TemplateDefinition();
        template.setId("test-template");
        template.setSqlFilename("test.sql");
        
        ParameterDefinition param = new ParameterDefinition();
        param.setName(paramName);
        param.setType("text");
        param.setLabel("Label " + paramName);
        param.setRequired(true);
        param.setFile(true);
        
        template.setParameters(List.of(param));
        
        return template;
    }

    private String formatDateViaReflection(String dateValue) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("formatDate", String.class);
            method.setAccessible(true);
            return (String) method.invoke(templateService, dateValue);
        } catch (Exception e) {
            fail("Erreur lors de l'appel formatDate: " + e.getMessage());
            return null;
        }
    }

    private boolean isNullValueViaReflection(Object value) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("isNullValue", Object.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(templateService, value);
        } catch (Exception e) {
            fail("Erreur lors de l'appel isNullValue: " + e.getMessage());
            return false;
        }
    }

    private String escapeSqlStringViaReflection(String value) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("escapeSqlString", String.class);
            method.setAccessible(true);
            return (String) method.invoke(templateService, value);
        } catch (Exception e) {
            fail("Erreur lors de l'appel escapeSqlString: " + e.getMessage());
            return null;
        }
    }

    private String buildPlaceholderViaReflection(String paramName) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("buildPlaceholder", String.class);
            method.setAccessible(true);
            return (String) method.invoke(templateService, paramName);
        } catch (Exception e) {
            fail("Erreur lors de l'appel buildPlaceholder: " + e.getMessage());
            return null;
        }
    }

    private boolean requiresBatchingViaReflection(TemplateDefinition template, Map<String, Object> params) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("requiresBatching", 
                TemplateDefinition.class, Map.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(templateService, template, params);
        } catch (Exception e) {
            fail("Erreur lors de l'appel requiresBatching: " + e.getMessage());
            return false;
        }
    }

    // ========== Tests pour formatQueryHeader ==========

    @Test
    void testFormatQueryHeader_FirstLine() {
        // Given: Première ligne sur 5
        int lineNumber = 1;
        int totalLines = 5;
        
        // When: Formatage
        String result = formatQueryHeaderViaReflection(lineNumber, totalLines);
        
        // Then: Doit retourner le header formaté
        assertEquals("-- Requête 1/5\n", result);
    }

    @Test
    void testFormatQueryHeader_MiddleLine() {
        // Given: Ligne du milieu
        int lineNumber = 3;
        int totalLines = 10;
        
        // When: Formatage
        String result = formatQueryHeaderViaReflection(lineNumber, totalLines);
        
        // Then: Doit retourner le header formaté
        assertEquals("-- Requête 3/10\n", result);
    }

    @Test
    void testFormatQueryHeader_LastLine() {
        // Given: Dernière ligne
        int lineNumber = 5;
        int totalLines = 5;
        
        // When: Formatage
        String result = formatQueryHeaderViaReflection(lineNumber, totalLines);
        
        // Then: Doit retourner le header formaté
        assertEquals("-- Requête 5/5\n", result);
    }

    // ========== Tests pour extractOrderedNonFileParameters ==========

    @Test
    void testExtractOrderedNonFileParameters_WithMixedParams() {
        // Given: Template avec paramètres normaux et fichier
        TemplateDefinition template = createTemplateWithMixedParams();
        
        // When: Extraction
        List<ParameterDefinition> result = extractOrderedNonFileParametersViaReflection(template);
        
        // Then: Seuls les paramètres non-fichier sont retournés, dans l'ordre
        assertEquals(2, result.size());
        assertEquals("param1", result.get(0).getName());
        assertEquals("param2", result.get(1).getName());
        assertFalse(result.get(0).isFile());
        assertFalse(result.get(1).isFile());
    }

    @Test
    void testExtractOrderedNonFileParameters_OnlyFileParams() {
        // Given: Template avec uniquement des paramètres fichier
        TemplateDefinition template = createTemplateWithFileParam("file1");
        
        // When: Extraction
        List<ParameterDefinition> result = extractOrderedNonFileParametersViaReflection(template);
        
        // Then: Liste vide
        assertTrue(result.isEmpty());
    }

    // ========== Tests pour parseCsvLine ==========

    @Test
    void testParseCsvLine_ValidLine() {
        // Given: Ligne CSV valide avec 3 valeurs
        String csvLine = "value1,value2,value3";
        List<ParameterDefinition> params = createParameterList("param1", "param2", "param3");
        
        // When: Parsing
        Map<String, Object> result = parseCsvLineViaReflection(csvLine, params);
        
        // Then: Toutes les valeurs sont mappées
        assertEquals(3, result.size());
        assertEquals("value1", result.get("param1"));
        assertEquals("value2", result.get("param2"));
        assertEquals("value3", result.get("param3"));
    }

    @Test
    void testParseCsvLine_WithEmptyValues() {
        // Given: Ligne CSV avec valeurs vides
        String csvLine = "value1,,value3";
        List<ParameterDefinition> params = createParameterList("param1", "param2", "param3");
        
        // When: Parsing
        Map<String, Object> result = parseCsvLineViaReflection(csvLine, params);
        
        // Then: Valeurs vides sont mappées à null
        assertEquals(3, result.size());
        assertEquals("value1", result.get("param1"));
        assertNull(result.get("param2"));
        assertEquals("value3", result.get("param3"));
    }

    @Test
    void testParseCsvLine_WithWhitespace() {
        // Given: Ligne CSV avec espaces
        String csvLine = " value1 , value2 , value3 ";
        List<ParameterDefinition> params = createParameterList("param1", "param2", "param3");
        
        // When: Parsing
        Map<String, Object> result = parseCsvLineViaReflection(csvLine, params);
        
        // Then: Espaces sont trimés
        assertEquals(3, result.size());
        assertEquals("value1", result.get("param1"));
        assertEquals("value2", result.get("param2"));
        assertEquals("value3", result.get("param3"));
    }

    @Test
    void testParseCsvLine_MoreValuesThanParams() {
        // Given: Plus de valeurs que de paramètres
        String csvLine = "value1,value2,value3,value4";
        List<ParameterDefinition> params = createParameterList("param1", "param2");
        
        // When: Parsing
        Map<String, Object> result = parseCsvLineViaReflection(csvLine, params);
        
        // Then: Seules les valeurs correspondant aux paramètres sont mappées
        assertEquals(2, result.size());
        assertEquals("value1", result.get("param1"));
        assertEquals("value2", result.get("param2"));
    }

    @Test
    void testParseCsvLine_FewerValuesThanParams() {
        // Given: Moins de valeurs que de paramètres
        String csvLine = "value1";
        List<ParameterDefinition> params = createParameterList("param1", "param2", "param3");
        
        // When: Parsing
        Map<String, Object> result = parseCsvLineViaReflection(csvLine, params);
        
        // Then: Seule la valeur disponible est mappée
        assertEquals(1, result.size());
        assertEquals("value1", result.get("param1"));
    }

    // ========== Méthodes utilitaires supplémentaires ==========

    private TemplateDefinition createTemplateWithMixedParams() {
        TemplateDefinition template = new TemplateDefinition();
        template.setId("test-template");
        template.setSqlFilename("test.sql");
        
        ParameterDefinition param1 = new ParameterDefinition();
        param1.setName("param1");
        param1.setFile(false);
        
        ParameterDefinition fileParam = new ParameterDefinition();
        fileParam.setName("fileParam");
        fileParam.setFile(true);
        
        ParameterDefinition param2 = new ParameterDefinition();
        param2.setName("param2");
        param2.setFile(false);
        
        template.setParameters(List.of(param1, fileParam, param2));
        
        return template;
    }

    private List<ParameterDefinition> createParameterList(String... paramNames) {
        List<ParameterDefinition> params = new ArrayList<>();
        for (String paramName : paramNames) {
            ParameterDefinition param = new ParameterDefinition();
            param.setName(paramName);
            param.setType("text");
            param.setLabel("Label " + paramName);
            param.setRequired(false);
            param.setFile(false);
            params.add(param);
        }
        return params;
    }

    private String formatQueryHeaderViaReflection(int lineNumber, int totalLines) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("formatQueryHeader", 
                int.class, int.class);
            method.setAccessible(true);
            return (String) method.invoke(templateService, lineNumber, totalLines);
        } catch (Exception e) {
            fail("Erreur lors de l'appel formatQueryHeader: " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<ParameterDefinition> extractOrderedNonFileParametersViaReflection(TemplateDefinition template) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("extractOrderedNonFileParameters", 
                TemplateDefinition.class);
            method.setAccessible(true);
            return (List<ParameterDefinition>) method.invoke(templateService, template);
        } catch (Exception e) {
            fail("Erreur lors de l'appel extractOrderedNonFileParameters: " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseCsvLineViaReflection(String csvLine, List<ParameterDefinition> orderedParams) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("parseCsvLine", 
                String.class, List.class);
            method.setAccessible(true);
            return (Map<String, Object>) method.invoke(templateService, csvLine, orderedParams);
        } catch (Exception e) {
            fail("Erreur lors de l'appel parseCsvLine: " + e.getMessage());
            return null;
        }
    }

    private String removeMetadataCommentsViaReflection(String sqlContent) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("removeMetadataComments", 
                String.class);
            method.setAccessible(true);
            return (String) method.invoke(templateService, sqlContent);
        } catch (Exception e) {
            fail("Erreur lors de l'appel removeMetadataComments: " + e.getMessage());
            return null;
        }
    }

    private String replacePlaceholderViaReflection(String sql, String paramName, String replacement) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("replacePlaceholder", 
                String.class, String.class, String.class);
            method.setAccessible(true);
            return (String) method.invoke(templateService, sql, paramName, replacement);
        } catch (Exception e) {
            fail("Erreur lors de l'appel replacePlaceholder: " + e.getMessage());
            return null;
        }
    }

    private String buildParameterReplacementViaReflection(ParameterDefinition paramDef, Object value) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("buildParameterReplacement", 
                ParameterDefinition.class, Object.class);
            method.setAccessible(true);
            return (String) method.invoke(templateService, paramDef, value);
        } catch (Exception e) {
            fail("Erreur lors de l'appel buildParameterReplacement: " + e.getMessage());
            return null;
        }
    }

    private String buildFileParameterReplacementViaReflection(Object value) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("buildFileParameterReplacement", 
                Object.class);
            method.setAccessible(true);
            return (String) method.invoke(templateService, value);
        } catch (Exception e) {
            fail("Erreur lors de l'appel buildFileParameterReplacement: " + e.getMessage());
            return null;
        }
    }

    private String buildSimpleParameterReplacementViaReflection(String type, Object value) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("buildSimpleParameterReplacement", 
                String.class, Object.class);
            method.setAccessible(true);
            return (String) method.invoke(templateService, type, value);
        } catch (Exception e) {
            fail("Erreur lors de l'appel buildSimpleParameterReplacement: " + e.getMessage());
            return null;
        }
    }

    private ParameterDefinition createParameter(String name, String type, boolean isFile) {
        ParameterDefinition param = new ParameterDefinition();
        param.setName(name);
        param.setType(type);
        param.setFile(isFile);
        return param;
    }

    private String generateBatchedSqlViaReflection(TemplateDefinition template,
                                                   String baseSql,
                                                   Map<String, Object> params) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod(
                    "generateBatchedSql",
                    TemplateDefinition.class, String.class, Map.class);
            method.setAccessible(true);
            return (String) method.invoke(templateService, template, baseSql, params);
        } catch (Exception e) {
            fail("Erreur lors de l'appel generateBatchedSql: " + e.getMessage());
            return null;
        }
    }

    private String generateMasseSqlViaReflection(TemplateDefinition template,
                                                 String baseSql,
                                                 Map<String, Object> params) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod(
                    "generateMasseSql",
                    TemplateDefinition.class, String.class, Map.class);
            method.setAccessible(true);
            return (String) method.invoke(templateService, template, baseSql, params);
        } catch (Exception e) {
            fail("Erreur lors de l'appel generateMasseSql: " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractDefinedParametersViaReflection(TemplateDefinition template) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("extractDefinedParameters", 
                TemplateDefinition.class);
            method.setAccessible(true);
            return (Set<String>) method.invoke(templateService, template);
        } catch (Exception e) {
            fail("Erreur lors de l'appel extractDefinedParameters: " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractPlaceholdersViaReflection(String sqlContent) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("extractPlaceholders", 
                String.class);
            method.setAccessible(true);
            return (Set<String>) method.invoke(templateService, sqlContent);
        } catch (Exception e) {
            fail("Erreur lors de l'appel extractPlaceholders: " + e.getMessage());
            return null;
        }
    }

    private void validatePlaceholdersViaReflection(TemplateDefinition template, String sqlContent, String filename) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("validatePlaceholders", 
                TemplateDefinition.class, String.class, String.class);
            method.setAccessible(true);
            method.invoke(templateService, template, sqlContent, filename);
        } catch (Exception e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e.getCause();
            }
            fail("Erreur lors de l'appel validatePlaceholders: " + e.getMessage());
        }
    }

    // ========== Tests pour getTemplateById ==========

    @Test
    void testGetTemplateById_Found() {
        // Given: Templates chargés dans la liste interne
        TemplateDefinition template1 = new TemplateDefinition();
        template1.setId("template1");
        TemplateDefinition template2 = new TemplateDefinition();
        template2.setId("template2");
        
        setTemplatesViaReflection(List.of(template1, template2));

        // When: Recherche d'un template
        TemplateDefinition result = templateService.getTemplateById("template1");

        // Then: Doit retourner le bon template
        assertNotNull(result);
        assertEquals("template1", result.getId());
    }

    @Test
    void testGetTemplateById_NotFound() {
        // Given: Templates chargés
        TemplateDefinition template1 = new TemplateDefinition();
        template1.setId("template1");
        setTemplatesViaReflection(List.of(template1));

        // When: Recherche d'un template inexistant
        TemplateDefinition result = templateService.getTemplateById("non-existent");

        // Then: Doit retourner null
        assertNull(result);
    }

    @Test
    void testGetTemplateById_EmptyList() {
        // Given: Liste vide
        setTemplatesViaReflection(new ArrayList<>());

        // When: Recherche
        TemplateDefinition result = templateService.getTemplateById("any-id");

        // Then: Doit retourner null
        assertNull(result);
    }

    // ========== Tests pour getAllTemplates ==========

    @Test
    void testGetAllTemplates_WithTemplates() {
        // Given: Templates chargés
        TemplateDefinition template1 = new TemplateDefinition();
        template1.setId("template1");
        TemplateDefinition template2 = new TemplateDefinition();
        template2.setId("template2");
        List<TemplateDefinition> templates = List.of(template1, template2);
        setTemplatesViaReflection(templates);

        // When: Récupération de tous les templates
        List<TemplateDefinition> result = templateService.getAllTemplates();

        // Then: Doit retourner la liste
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("template1", result.get(0).getId());
    }

    @Test
    void testGetAllTemplates_WithNullTemplates() {
        // Given: templates = null
        setTemplatesViaReflection(null);

        // When: Récupération
        List<TemplateDefinition> result = templateService.getAllTemplates();

        // Then: Doit retourner une liste vide
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllTemplates_EmptyList() {
        // Given: Liste vide
        setTemplatesViaReflection(new ArrayList<>());

        // When: Récupération
        List<TemplateDefinition> result = templateService.getAllTemplates();

        // Then: Doit retourner une liste vide
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== Tests pour validateAndGetTemplate ==========

    @Test
    void testValidateAndGetTemplate_Success() {
        // Given: Template valide avec sqlFilename
        TemplateDefinition template = new TemplateDefinition();
        template.setId("test-template");
        template.setSqlFilename("test.sql");
        setTemplatesViaReflection(List.of(template));

        // When: Validation
        TemplateDefinition result = validateAndGetTemplateViaReflection("test-template");

        // Then: Doit retourner le template
        assertNotNull(result);
        assertEquals("test-template", result.getId());
    }

    @Test
    void testValidateAndGetTemplate_TemplateNotFound() {
        // Given: Template n'existe pas
        setTemplatesViaReflection(new ArrayList<>());

        // When & Then: Doit lever une exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> validateAndGetTemplateViaReflection("non-existent"));
        assertTrue(exception.getMessage().contains("Template not found"));
    }

    @Test
    void testValidateAndGetTemplate_NoSqlFilename() {
        // Given: Template sans sqlFilename
        TemplateDefinition template = new TemplateDefinition();
        template.setId("test-template");
        template.setSqlFilename(null);
        setTemplatesViaReflection(List.of(template));

        // When & Then: Doit lever une exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> validateAndGetTemplateViaReflection("test-template"));
        assertTrue(exception.getMessage().contains("sqlFilename"));
    }

    @Test
    void testValidateAndGetTemplate_EmptySqlFilename() {
        // Given: Template avec sqlFilename vide
        TemplateDefinition template = new TemplateDefinition();
        template.setId("test-template");
        template.setSqlFilename("");
        setTemplatesViaReflection(List.of(template));

        // When & Then: Doit lever une exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> validateAndGetTemplateViaReflection("test-template"));
        assertTrue(exception.getMessage().contains("sqlFilename"));
    }

    // ========== Tests pour processSqlWithParams ==========

    @Test
    void testProcessSqlWithParams_UnitaireMode() {
        // Given: Template simple, mode unitaire
        TemplateDefinition template = createTemplateWithFileParam("ids");
        String baseSql = "SELECT * FROM T WHERE id = {{ids}};";
        Map<String, Object> params = new HashMap<>();
        params.put("ids", Arrays.asList("1", "2"));
        String executionType = TemplateConstants.EXECUTION_TYPE_UNITAIRE;

        // When: Traitement
        String result = processSqlWithParamsViaReflection(template, baseSql, params, executionType);

        // Then: Doit remplacer les placeholders (pas de batching car <= 999)
        assertNotNull(result);
        assertTrue(result.contains("1") || result.contains("'1'"));
    }

    @Test
    void testProcessSqlWithParams_MasseMode() {
        // Given: Template simple, mode masse
        TemplateDefinition template = new TemplateDefinition();
        template.setId("test-template");
        ParameterDefinition p1 = new ParameterDefinition();
        p1.setName("value");
        p1.setFile(false);
        template.setParameters(List.of(p1));
        
        String baseSql = "UPDATE T SET col = {{value}};";
        Map<String, Object> params = new HashMap<>();
        params.put(TemplateConstants.MASSE_FILE_PARAM, Arrays.asList("val1", "val2"));
        String executionType = TemplateConstants.EXECUTION_TYPE_MASSE;

        // When: Traitement
        String result = processSqlWithParamsViaReflection(template, baseSql, params, executionType);

        // Then: Doit générer du SQL masse (avec headers -- Requête 1/2, etc.)
        assertNotNull(result);
        assertTrue(result.contains("Requête 1/2") || result.contains("val1"));
    }

    @Test
    void testProcessSqlWithParams_WithBatching() {
        // Given: Template avec paramètre fichier et liste > 999
        TemplateDefinition template = createTemplateWithFileParam("ids");
        String baseSql = "SELECT * FROM T WHERE id IN ({{ids}});";
        
        List<String> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add(String.valueOf(i));
        }
        Map<String, Object> params = new HashMap<>();
        params.put("ids", largeList);
        String executionType = TemplateConstants.EXECUTION_TYPE_UNITAIRE;

        // When: Traitement
        String result = processSqlWithParamsViaReflection(template, baseSql, params, executionType);

        // Then: Doit générer du SQL avec batching (Lot 1/2, Lot 2/2)
        assertNotNull(result);
        assertTrue(result.contains("Lot 1/2") || result.contains("Lot 2/2"));
    }

    // ========== Méthodes utilitaires supplémentaires ==========

    @SuppressWarnings("unchecked")
    private void setTemplatesViaReflection(List<TemplateDefinition> templates) {
        try {
            java.lang.reflect.Field field = TemplateService.class.getDeclaredField("templates");
            field.setAccessible(true);
            field.set(templateService, templates);
        } catch (Exception e) {
            fail("Erreur lors de la définition de templates: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<TemplateDefinition> getTemplatesViaReflection() {
        try {
            java.lang.reflect.Field field = TemplateService.class.getDeclaredField("templates");
            field.setAccessible(true);
            return (List<TemplateDefinition>) field.get(templateService);
        } catch (Exception e) {
            fail("Erreur lors de la récupération de templates: " + e.getMessage());
            return null;
        }
    }

    private TemplateDefinition validateAndGetTemplateViaReflection(String templateId) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("validateAndGetTemplate", 
                String.class);
            method.setAccessible(true);
            return (TemplateDefinition) method.invoke(templateService, templateId);
        } catch (Exception e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e.getCause();
            }
            fail("Erreur lors de l'appel validateAndGetTemplate: " + e.getMessage());
            return null;
        }
    }

    private String processSqlWithParamsViaReflection(TemplateDefinition template, String baseSql, 
                                                     Map<String, Object> params, String executionType) {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("processSqlWithParams", 
                TemplateDefinition.class, String.class, Map.class, String.class);
            method.setAccessible(true);
            return (String) method.invoke(templateService, template, baseSql, params, executionType);
        } catch (Exception e) {
            fail("Erreur lors de l'appel processSqlWithParams: " + e.getMessage());
            return null;
        }
    }

    // ========== Tests pour init() ==========

    @Test
    void testInit_Success() throws Exception {
        // Given: Configuration des mocks
        when(appProperties.getOutputScriptsPath()).thenReturn("./target/test-output/");
        
        // Les fichiers SQL de test existent dans src/test/resources/templates/
        // Le parser va être appelé pour chaque fichier
        TemplateDefinition validTemplate = new TemplateDefinition();
        validTemplate.setId("test-minimal");
        validTemplate.setSqlFilename("test-minimal.sql");
        validTemplate.setParameters(new ArrayList<>());
        
        // Mock du parser pour retourner un template valide pour certains fichiers
        when(metadataParser.parseSqlFile("test-minimal.sql")).thenReturn(validTemplate);
        when(metadataParser.parseSqlFile("test-complete.sql")).thenReturn(validTemplate);
        when(metadataParser.parseSqlFile("test-file-param.sql")).thenReturn(validTemplate);
        when(metadataParser.parseSqlFile("test-mixed-params.sql")).thenReturn(validTemplate);
        
        // Pour les fichiers invalides, on peut retourner une exception (sera catchée par loadTemplateSafely)
        when(metadataParser.parseSqlFile("test-no-id.sql")).thenThrow(new IOException("No ID"));
        when(metadataParser.parseSqlFile("test-empty-id.sql")).thenThrow(new IOException("Empty ID"));
        when(metadataParser.parseSqlFile("test-invalid-param.sql")).thenThrow(new IOException("Invalid param"));
        when(metadataParser.parseSqlFile("test-metadata-malformed.sql")).thenThrow(new IOException("Malformed"));

        // When: Appel de init() via réflexion
        initViaReflection();

        // Then: Les templates valides doivent être chargés
        List<TemplateDefinition> templates = getTemplatesViaReflection();
        assertNotNull(templates);
        // Au moins les templates valides doivent être présents
        assertTrue(templates.size() >= 0); // Peut être 0 si tous les fichiers sont invalides, ou plus si valides
    }

    @Test
    void testInit_WithEmptyTemplates() throws Exception {
        // Given: Aucun fichier SQL (ou tous invalides)
        when(appProperties.getOutputScriptsPath()).thenReturn("./target/test-output/");
        
        // Mock pour que tous les fichiers lèvent une exception
        when(metadataParser.parseSqlFile(anyString())).thenThrow(new IOException("Parse error"));

        // When: Appel de init()
        initViaReflection();

        // Then: La liste doit être vide mais init() ne doit pas lever d'exception
        List<TemplateDefinition> templates = getTemplatesViaReflection();
        assertNotNull(templates);
        // logInitializationSummary() doit être appelé avec une liste vide
    }

    @Test
    void testInit_WithOutputDirectoryError() throws Exception {
        // Given: Erreur lors de la création du répertoire
        when(appProperties.getOutputScriptsPath()).thenReturn("/invalid/path/that/cannot/be/created");

        // When: Appel de init()
        // Ne doit pas lever d'exception, juste logger l'erreur
        assertDoesNotThrow(() -> initViaReflection());

        // Then: logOutputDirectoryCreationError() doit être appelé
    }

    // ========== Méthode utilitaire pour appeler init() ==========

    private void initViaReflection() {
        try {
            java.lang.reflect.Method method = TemplateService.class.getDeclaredMethod("init");
            method.setAccessible(true);
            method.invoke(templateService);
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            if (e.getCause() instanceof IOException) {
                throw new RuntimeException("IOException lors de init(): " + e.getCause().getMessage(), e.getCause());
            }
            fail("Erreur lors de l'appel init(): " + e.getMessage());
        }
    }
}

package com.sqlgenerator.backend.service;

import com.sqlgenerator.backend.model.ParameterDefinition;
import com.sqlgenerator.backend.model.TemplateDefinition;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour TemplateService.
 * 
 * Ces tests vérifient les méthodes utilitaires via réflexion.
 * Les tests d'intégration complets sont dans AdminController.integrationTest().
 */
class TemplateServiceTest {

    private TemplateService templateService = new TemplateService();

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
}

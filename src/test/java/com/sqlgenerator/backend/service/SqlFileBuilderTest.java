package com.sqlgenerator.backend.service;

import com.sqlgenerator.backend.config.AppProperties;
import com.sqlgenerator.backend.model.TemplateDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour SqlFileBuilder.
 * 
 * Ces tests vérifient que la construction et l'écriture des fichiers SQL
 * fonctionnent correctement.
 */
class SqlFileBuilderTest {

    private SqlFileBuilder sqlFileBuilder;
    private AppProperties appProperties;
    private TemplateDefinition testTemplate;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        appProperties = mock(AppProperties.class);
        sqlFileBuilder = new SqlFileBuilder();
        
        // Injection manuelle via réflexion
        try {
            java.lang.reflect.Field field = SqlFileBuilder.class.getDeclaredField("appProperties");
            field.setAccessible(true);
            field.set(sqlFileBuilder, appProperties);
        } catch (Exception e) {
            fail("Impossible d'injecter appProperties: " + e.getMessage());
        }

        // Template de test
        testTemplate = new TemplateDefinition();
        testTemplate.setId("test-template");
        testTemplate.setName("Test Template");
    }

    // ========== Tests pour buildHeader ==========

    @Test
    void testBuildHeader_WithAllParams() {
        // Given
        Map<String, Object> params = new HashMap<>();
        params.put("ticket", "JIRA-123");
        String executionType = "unitaire";

        // When
        String header = sqlFileBuilder.buildHeader(testTemplate, executionType, params);

        // Then
        assertNotNull(header);
        assertTrue(header.contains("Fichier de Script Généré"));
        assertTrue(header.contains("Test Template"));
        assertTrue(header.contains("test-template"));
        assertTrue(header.contains("JIRA-123"));
        assertTrue(header.contains("unitaire"));
    }

    @Test
    void testBuildHeader_WithNullTicket() {
        // Given
        Map<String, Object> params = new HashMap<>();
        params.put("ticket", null);
        String executionType = "masse";

        // When
        String header = sqlFileBuilder.buildHeader(testTemplate, executionType, params);

        // Then
        assertNotNull(header);
        assertTrue(header.contains("masse"));
        assertTrue(header.contains("null")); // null sera converti en string
    }

    // ========== Tests pour buildCompleteFile ==========

    @Test
    void testBuildCompleteFile_WithHeaderAndSql() {
        // Given
        Map<String, Object> params = new HashMap<>();
        params.put("ticket", "JIRA-123");
        String executionType = "unitaire";
        String sql = "SELECT * FROM test WHERE id = 42;";

        // When
        String fileContent = sqlFileBuilder.buildCompleteFile(testTemplate, executionType, params, sql);

        // Then
        assertNotNull(fileContent);
        assertTrue(fileContent.contains("Fichier de Script Généré"));
        assertTrue(fileContent.contains("SELECT * FROM test WHERE id = 42;"));
        assertTrue(fileContent.contains("\n")); // Ligne vide entre header et SQL
    }

    @Test
    void testBuildCompleteFile_WithEmptySql() {
        // Given
        Map<String, Object> params = new HashMap<>();
        params.put("ticket", "JIRA-123");
        String executionType = "unitaire";
        String sql = "";

        // When
        String fileContent = sqlFileBuilder.buildCompleteFile(testTemplate, executionType, params, sql);

        // Then
        assertNotNull(fileContent);
        assertTrue(fileContent.contains("Fichier de Script Généré"));
    }

    // ========== Tests pour generateFileName ==========

    @Test
    void testGenerateFileName_Unitaire() {
        // Given
        String templateId = "test-template";
        String executionType = "unitaire";

        // When
        String fileName = sqlFileBuilder.generateFileName(templateId, executionType);

        // Then
        assertNotNull(fileName);
        assertTrue(fileName.startsWith("test-template_unitaire_"));
        assertTrue(fileName.endsWith(".sql"));
        assertTrue(fileName.matches("test-template_unitaire_\\d{14}\\.sql"));
    }

    @Test
    void testGenerateFileName_Masse() {
        // Given
        String templateId = "test-template";
        String executionType = "masse";

        // When
        String fileName = sqlFileBuilder.generateFileName(templateId, executionType);

        // Then
        assertNotNull(fileName);
        assertTrue(fileName.startsWith("test-template_masse_"));
        assertTrue(fileName.endsWith(".sql"));
    }

    @Test
    void testGenerateFileName_UniqueTimestamps() throws InterruptedException {
        // Given
        String templateId = "test-template";
        String executionType = "unitaire";

        // When
        String fileName1 = sqlFileBuilder.generateFileName(templateId, executionType);
        Thread.sleep(1000); // Attendre 1 seconde pour avoir un timestamp différent
        String fileName2 = sqlFileBuilder.generateFileName(templateId, executionType);

        // Then
        assertNotEquals(fileName1, fileName2, "Les noms de fichiers doivent être uniques");
    }

    // ========== Tests pour writeFile ==========

    @Test
    void testWriteFile_Success() throws IOException {
        // Given
        when(appProperties.getOutputScriptsPath()).thenReturn(tempDir.toString());
        String fileName = "test.sql";
        String content = "SELECT * FROM test;";

        // When
        sqlFileBuilder.writeFile(fileName, content);

        // Then
        Path filePath = tempDir.resolve(fileName);
        assertTrue(Files.exists(filePath));
        String fileContent = Files.readString(filePath);
        assertEquals(content, fileContent);
    }

    @Test
    void testWriteFile_WithSpecialCharacters() throws IOException {
        // Given
        when(appProperties.getOutputScriptsPath()).thenReturn(tempDir.toString());
        String fileName = "test.sql";
        String content = "SELECT * FROM test WHERE name = 'Jean''s name';";

        // When
        sqlFileBuilder.writeFile(fileName, content);

        // Then
        Path filePath = tempDir.resolve(fileName);
        assertTrue(Files.exists(filePath));
        String fileContent = Files.readString(filePath);
        assertEquals(content, fileContent);
    }

    @Test
    void testWriteFile_WithMultilineContent() throws IOException {
        // Given
        when(appProperties.getOutputScriptsPath()).thenReturn(tempDir.toString());
        String fileName = "test.sql";
        String content = "SELECT * FROM test;\nUPDATE test SET name = 'test';\n";

        // When
        sqlFileBuilder.writeFile(fileName, content);

        // Then
        Path filePath = tempDir.resolve(fileName);
        assertTrue(Files.exists(filePath));
        String fileContent = Files.readString(filePath);
        assertEquals(content, fileContent);
    }

    // ========== Tests pour buildAndWriteFile ==========

    @Test
    void testBuildAndWriteFile_Success() throws IOException {
        // Given
        when(appProperties.getOutputScriptsPath()).thenReturn(tempDir.toString());
        Map<String, Object> params = new HashMap<>();
        params.put("ticket", "JIRA-123");
        String executionType = "unitaire";
        String sql = "SELECT * FROM test;";

        // When
        String fileName = sqlFileBuilder.buildAndWriteFile(testTemplate, executionType, params, sql);

        // Then
        assertNotNull(fileName);
        assertTrue(fileName.startsWith("test-template_unitaire_"));
        assertTrue(fileName.endsWith(".sql"));
        
        Path filePath = tempDir.resolve(fileName);
        assertTrue(Files.exists(filePath));
        String fileContent = Files.readString(filePath);
        assertTrue(fileContent.contains("Fichier de Script Généré"));
        assertTrue(fileContent.contains("SELECT * FROM test;"));
    }

    @Test
    void testBuildAndWriteFile_WithMasseType() throws IOException {
        // Given
        when(appProperties.getOutputScriptsPath()).thenReturn(tempDir.toString());
        Map<String, Object> params = new HashMap<>();
        params.put("ticket", "JIRA-456");
        String executionType = "masse";
        String sql = "UPDATE test SET name = 'test';";

        // When
        String fileName = sqlFileBuilder.buildAndWriteFile(testTemplate, executionType, params, sql);

        // Then
        assertNotNull(fileName);
        assertTrue(fileName.startsWith("test-template_masse_"));
        
        Path filePath = tempDir.resolve(fileName);
        assertTrue(Files.exists(filePath));
        String fileContent = Files.readString(filePath);
        assertTrue(fileContent.contains("masse"));
        assertTrue(fileContent.contains("JIRA-456"));
    }
}


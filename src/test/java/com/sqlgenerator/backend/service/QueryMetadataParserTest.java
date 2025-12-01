package com.sqlgenerator.backend.service;

import com.sqlgenerator.backend.model.ParameterDefinition;
import com.sqlgenerator.backend.model.QueryDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour QueryMetadataParser.
 * 
 * Ces tests vérifient que le parsing des métadonnées SQL fonctionne correctement
 * et que les cas limites sont bien gérés.
 * 
 * Pourquoi tester le parser ?
 * - C'est une partie critique : une erreur de parsing casse toute l'application
 * - Validation des formats de métadonnées
 * - Détection précoce des erreurs de configuration
 */
class QueryMetadataParserTest {

    private QueryMetadataParser parser;

    @BeforeEach
    void setUp() {
        parser = new QueryMetadataParser();
    }

    @Test
    void testParseSqlFile_WithAllMetadata() throws IOException {
        // Given: Fichier SQL complet avec toutes les métadonnées
        // When
        QueryDefinition query = parser.parseSqlFile("test-complete.sql");
        
        // Then
        assertNotNull(query);
        assertEquals("test-complete", query.getId());
        assertEquals("Test Complet", query.getName());
        assertEquals("Description complète de test avec tous les champs", query.getDescription());
        assertEquals(3, query.getTags().size());
        assertTrue(query.getTags().contains("tag1"));
        assertTrue(query.getTags().contains("tag2"));
        assertTrue(query.getTags().contains("tag3"));
        assertEquals(2, query.getParameters().size());
        assertEquals("test-complete.sql", query.getSqlFile());
    }

    @Test
    void testParseSqlFile_WithFileParameter() throws IOException {
        // Given: Fichier avec paramètre fichier pour IN
        // When
        QueryDefinition query = parser.parseSqlFile("test-file-param.sql");
        
        // Then
        assertNotNull(query);
        assertEquals(1, query.getParameters().size());
        ParameterDefinition param = query.getParameters().get(0);
        assertEquals("ids", param.getName());
        assertEquals("text", param.getType());
        assertEquals("Liste des IDs", param.getLabel());
        assertTrue(param.isRequired());
        assertTrue(param.isFile()); // Paramètre fichier
    }

    @Test
    void testParseSqlFile_MissingId_ThrowsException() {
        // Given: Fichier sans @id (obligatoire)
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> parser.parseSqlFile("test-no-id.sql")
        );
        
        assertTrue(exception.getMessage().contains("L'ID est obligatoire"));
        assertTrue(exception.getMessage().contains("test-no-id.sql"));
        assertTrue(exception.getMessage().contains("-- @id:"));
    }

    @Test
    void testParseSqlFile_EmptyId_ThrowsException() {
        // Given: Fichier avec @id vide
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> parser.parseSqlFile("test-empty-id.sql")
        );
        
        assertTrue(exception.getMessage().contains("L'ID est obligatoire"));
        assertTrue(exception.getMessage().contains("-- @id:"));
    }

    @Test
    void testParseSqlFile_WithOptionalMetadata() throws IOException {
        // Given: Fichier avec seulement l'ID obligatoire
        // When
        QueryDefinition query = parser.parseSqlFile("test-minimal.sql");
        
        // Then
        assertNotNull(query);
        assertEquals("minimal-query", query.getId());
        assertNull(query.getName());
        assertNull(query.getDescription());
        assertNull(query.getTags());
        assertNotNull(query.getParameters());
        assertTrue(query.getParameters().isEmpty());
    }

    @Test
    void testParseSqlFile_MixedParameterTypes() throws IOException {
        // Given: Mélange de paramètres normaux et fichier
        // When
        QueryDefinition query = parser.parseSqlFile("test-mixed-params.sql");
        
        // Then
        assertEquals(3, query.getParameters().size());
        
        // Vérifier l'ordre et les types
        assertFalse(query.getParameters().get(0).isFile()); // id
        assertTrue(query.getParameters().get(1).isFile());  // file_ids
        assertFalse(query.getParameters().get(2).isFile()); // name
        
        assertEquals("id", query.getParameters().get(0).getName());
        assertEquals("file_ids", query.getParameters().get(1).getName());
        assertEquals("name", query.getParameters().get(2).getName());
    }

    @Test
    void testParseSqlFile_InvalidParameterFormat_Ignored() throws IOException {
        // Given: Paramètre avec format invalide (moins de 3 parties)
        // When
        QueryDefinition query = parser.parseSqlFile("test-invalid-param.sql");
        
        // Then: Seuls les paramètres valides sont parsés
        assertEquals(2, query.getParameters().size());
        assertEquals("valid", query.getParameters().get(0).getName());
        assertEquals("valid2", query.getParameters().get(1).getName());
    }

    @Test
    void testParseSqlFile_RealWorldExample() throws IOException {
        // Given: Exemple réel (update-person-name.sql)
        // When
        QueryDefinition query = parser.parseSqlFile("update-person-name.sql");
        
        // Then
        assertEquals("update-person-name", query.getId());
        assertEquals("Mise à jour du nom d'une personne", query.getName());
        assertEquals("Met à jour le nom d'une personne dans la table PERSON.", query.getDescription());
        assertEquals(2, query.getTags().size());
        assertTrue(query.getTags().contains("person"));
        assertTrue(query.getTags().contains("update"));
        assertEquals(2, query.getParameters().size());
        
        ParameterDefinition personId = query.getParameters().get(0);
        assertEquals("person_id", personId.getName());
        assertEquals("text", personId.getType());
        assertEquals("ID Personne", personId.getLabel());
        assertTrue(personId.isRequired());
        assertFalse(personId.isFile());
        
        ParameterDefinition name = query.getParameters().get(1);
        assertEquals("name", name.getName());
        assertEquals("Nom", name.getLabel());
        assertTrue(name.isRequired());
    }

    @Test
    void testParseSqlFile_WithActivateContrats() throws IOException {
        // Given: Exemple réel avec paramètre fichier (activate-contrats.sql)
        // When
        QueryDefinition query = parser.parseSqlFile("activate-contrats.sql");
        
        // Then
        assertEquals("activate-contrats", query.getId());
        assertEquals("Activation de contrats en masse", query.getName());
        assertEquals(1, query.getParameters().size());
        
        ParameterDefinition param = query.getParameters().get(0);
        assertEquals("contrat_iide", param.getName());
        assertTrue(param.isFile());
        assertTrue(param.isRequired());
        assertEquals("Fichier contenant les IDs contrats (1 par ligne)", param.getLabel());
    }
}


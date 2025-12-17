package com.sqlgenerator.backend.controller;

import com.sqlgenerator.backend.model.FormSchema;
import com.sqlgenerator.backend.service.FormSchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour TemplateController.
 * 
 * Ces tests vérifient que les endpoints de listing et récupération
 * de templates fonctionnent correctement.
 */
@WebMvcTest(TemplateController.class)
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FormSchemaService formSchemaService;

    private FormSchema testSchema;
    private List<FormSchema> allSchemas;

    @BeforeEach
    void setUp() {
        // Schéma de test
        testSchema = new FormSchema();
        testSchema.setTemplateId("test-template");
        testSchema.setName("Test Template");
        testSchema.setDescription("Description du template de test");

        // Liste de tous les schémas
        allSchemas = new ArrayList<>();
        allSchemas.add(testSchema);
    }

    // ========== Tests pour GET /api/templates ==========

    @Test
    void testGetAllTemplates_Success() throws Exception {
        // Given: Service retourne la liste des templates
        when(formSchemaService.getAllFormSchemas()).thenReturn(allSchemas);

        // When & Then
        mockMvc.perform(get("/api/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].templateId").value("test-template"))
                .andExpect(jsonPath("$[0].name").value("Test Template"));
    }

    @Test
    void testGetAllTemplates_EmptyList() throws Exception {
        // Given: Aucun template disponible
        when(formSchemaService.getAllFormSchemas()).thenReturn(new ArrayList<>());

        // When & Then
        mockMvc.perform(get("/api/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ========== Tests pour GET /api/templates/{id} ==========

    @Test
    void testGetTemplate_Success() throws Exception {
        // Given: Template existe
        when(formSchemaService.getFormSchema("test-template")).thenReturn(testSchema);

        // When & Then
        mockMvc.perform(get("/api/templates/test-template"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateId").value("test-template"))
                .andExpect(jsonPath("$.name").value("Test Template"))
                .andExpect(jsonPath("$.description").value("Description du template de test"));
    }

    @Test
    void testGetTemplate_NotFound() throws Exception {
        // Given: Template n'existe pas
        when(formSchemaService.getFormSchema("non-existent")).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/templates/non-existent"))
                .andExpect(status().isNotFound());
    }

    // ========== Tests pour GET /api/templates/{id}/request-body ==========

    @Test
    void testGetRequestBody_UnitaireMode_Success() throws Exception {
        // Given: Template existe et mode unitaire
        Map<String, Object> bodyStructure = new HashMap<>();
        bodyStructure.put("ticket", "JIRA-123");
        bodyStructure.put("person_id", "42");
        bodyStructure.put("name", "Jean Dupont");

        when(formSchemaService.getUnitBodyStructure("test-template")).thenReturn(bodyStructure);

        // When & Then
        mockMvc.perform(get("/api/templates/test-template/request-body")
                        .param("mode", "unitaire"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket").value("JIRA-123"))
                .andExpect(jsonPath("$.person_id").value("42"))
                .andExpect(jsonPath("$.name").value("Jean Dupont"));
    }

    @Test
    void testGetRequestBody_DefaultMode_Unitaire() throws Exception {
        // Given: Template existe, mode par défaut (unitaire)
        Map<String, Object> bodyStructure = new HashMap<>();
        bodyStructure.put("ticket", "JIRA-123");

        when(formSchemaService.getUnitBodyStructure("test-template")).thenReturn(bodyStructure);

        // When & Then
        mockMvc.perform(get("/api/templates/test-template/request-body"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket").value("JIRA-123"));
    }

    @Test
    void testGetRequestBody_MasseMode_Success() throws Exception {
        // Given: Template existe et mode masse disponible
        Map<String, Object> bodyStructure = new HashMap<>();
        bodyStructure.put("ticket", "JIRA-123");

        when(formSchemaService.getMassBodyStructure("test-template")).thenReturn(bodyStructure);

        // When & Then
        mockMvc.perform(get("/api/templates/test-template/request-body")
                        .param("mode", "masse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket").value("JIRA-123"));
    }

    @Test
    void testGetRequestBody_MasseMode_NotAvailable() throws Exception {
        // Given: Template existe mais mode masse non disponible (template avec IN)
        when(formSchemaService.getMassBodyStructure("template-with-in")).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/templates/template-with-in/request-body")
                        .param("mode", "masse"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetRequestBody_TemplateNotFound() throws Exception {
        // Given: Template n'existe pas
        when(formSchemaService.getUnitBodyStructure("non-existent")).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/templates/non-existent/request-body"))
                .andExpect(status().isNotFound());
    }
}


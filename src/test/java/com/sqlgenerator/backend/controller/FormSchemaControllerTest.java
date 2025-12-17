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
 * Tests unitaires pour FormSchemaController.
 * 
 * Ces tests vérifient que les endpoints de listing et récupération
 * de schémas de formulaires fonctionnent correctement.
 */
@WebMvcTest(FormSchemaController.class)
class FormSchemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FormSchemaService formSchemaService;

    private FormSchema testSchema;
    private Map<String, Object> testBodyStructure;

    @BeforeEach
    void setUp() {
        testSchema = new FormSchema();
        testSchema.setTemplateId("test-template");
        testSchema.setName("Test Template");
        testSchema.setDescription("A test template");
        testSchema.setTags(List.of("test", "example"));

        testBodyStructure = new HashMap<>();
        testBodyStructure.put("ticket", "JIRA-123");
        testBodyStructure.put("param1", "value1");
    }

    // ========== Tests pour GET /api/forms ==========

    @Test
    void testGetAllForms_Success() throws Exception {
        // Given: FormSchemaService retourne une liste de schémas
        FormSchema schema1 = new FormSchema();
        schema1.setTemplateId("template1");
        FormSchema schema2 = new FormSchema();
        schema2.setTemplateId("template2");
        when(formSchemaService.getAllFormSchemas()).thenReturn(List.of(schema1, schema2));

        // When & Then
        mockMvc.perform(get("/api/forms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].templateId").value("template1"));
    }

    @Test
    void testGetAllForms_EmptyList() throws Exception {
        // Given: Aucun schéma disponible
        when(formSchemaService.getAllFormSchemas()).thenReturn(new ArrayList<>());

        // When & Then
        mockMvc.perform(get("/api/forms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ========== Tests pour GET /api/forms/{id} ==========

    @Test
    void testGetForm_Success() throws Exception {
        // Given: FormSchemaService retourne un schéma
        when(formSchemaService.getFormSchema("test-template")).thenReturn(testSchema);

        // When & Then
        mockMvc.perform(get("/api/forms/test-template"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateId").value("test-template"))
                .andExpect(jsonPath("$.name").value("Test Template"));
    }

    @Test
    void testGetForm_NotFound() throws Exception {
        // Given: FormSchemaService retourne null
        when(formSchemaService.getFormSchema("non-existent")).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/forms/non-existent"))
                .andExpect(status().isNotFound());
    }

    // ========== Tests pour GET /api/forms/{id}/request-body ==========

    @Test
    void testGetRequestBody_UnitaireMode() throws Exception {
        // Given: FormSchemaService retourne une structure de body
        when(formSchemaService.getUnitBodyStructure("test-template")).thenReturn(testBodyStructure);

        // When & Then
        mockMvc.perform(get("/api/forms/test-template/request-body")
                        .param("mode", "unitaire"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket").value("JIRA-123"))
                .andExpect(jsonPath("$.param1").value("value1"));
    }

    @Test
    void testGetRequestBody_DefaultMode() throws Exception {
        // Given: Mode par défaut (unitaire)
        when(formSchemaService.getUnitBodyStructure("test-template")).thenReturn(testBodyStructure);

        // When & Then
        mockMvc.perform(get("/api/forms/test-template/request-body"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket").exists());
    }

    @Test
    void testGetRequestBody_MasseMode() throws Exception {
        // Given: Mode masse disponible
        Map<String, Object> massBody = new HashMap<>();
        massBody.put("ticket", "JIRA-123");
        massBody.put("masseFile", List.of("line1", "line2"));
        when(formSchemaService.getMassBodyStructure("test-template")).thenReturn(massBody);

        // When & Then
        mockMvc.perform(get("/api/forms/test-template/request-body")
                        .param("mode", "masse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket").value("JIRA-123"))
                .andExpect(jsonPath("$.masseFile").isArray());
    }

    @Test
    void testGetRequestBody_MasseModeNotAvailable() throws Exception {
        // Given: Mode masse non disponible (template avec IN)
        when(formSchemaService.getMassBodyStructure("template-with-in")).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/forms/template-with-in/request-body")
                        .param("mode", "masse"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetRequestBody_NotFound() throws Exception {
        // Given: Template inexistant
        when(formSchemaService.getUnitBodyStructure("non-existent")).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/forms/non-existent/request-body"))
                .andExpect(status().isNotFound());
    }
}


package com.sqlgenerator.backend.service;
import com.sqlgenerator.backend.model.FormField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests unitaires pour FormSchemaService.
 * 
 * Ces tests vérifient que le service construit correctement les schémas de formulaires
 * à partir des templates SQL.
 */
class FormSchemaServiceTest {

    private FormSchemaService formSchemaService;
    
    @Mock
    private TemplateService templateService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        formSchemaService = new FormSchemaService();
        
        // Injection manuelle via réflexion
        try {
            java.lang.reflect.Field field = FormSchemaService.class.getDeclaredField("templateService");
            field.setAccessible(true);
            field.set(formSchemaService, templateService);
        } catch (Exception e) {
            fail("Impossible d'injecter templateService: " + e.getMessage());
        }
    }

    // ========== Tests pour createTicketField ==========

    @Test
    void testCreateTicketField_AllProperties() {
        // When: Création du champ ticket via réflexion
        FormField ticket = createTicketFieldViaReflection();
        
        // Then: Toutes les propriétés sont correctement configurées
        assertNotNull(ticket);
        assertEquals(TemplateConstants.TICKET_PARAM, ticket.getName());
        assertEquals(TemplateConstants.FIELD_TYPE_TEXT, ticket.getType());
        assertEquals("Numéro du ticket", ticket.getLabel());
        assertTrue(ticket.isRequired());
        assertNotNull(ticket.getHelpText());
        assertTrue(ticket.getHelpText().contains("ticket"));
    }

    // ========== Tests pour mapParameterTypeToFieldType ==========

    @Test
    void testMapParameterTypeToFieldType_Number() {
        // When: Mapping du type "number"
        String result = mapParameterTypeToFieldTypeViaReflection("number");
        
        // Then: Doit retourner FIELD_TYPE_NUMBER
        assertEquals(TemplateConstants.FIELD_TYPE_NUMBER, result);
    }

    @Test
    void testMapParameterTypeToFieldType_Integer() {
        // When: Mapping du type "integer"
        String result = mapParameterTypeToFieldTypeViaReflection("integer");
        
        // Then: Doit retourner FIELD_TYPE_NUMBER
        assertEquals(TemplateConstants.FIELD_TYPE_NUMBER, result);
    }

    @Test
    void testMapParameterTypeToFieldType_Date() {
        // When: Mapping du type "date"
        String result = mapParameterTypeToFieldTypeViaReflection("date");
        
        // Then: Doit retourner FIELD_TYPE_DATE
        assertEquals(TemplateConstants.FIELD_TYPE_DATE, result);
    }

    @Test
    void testMapParameterTypeToFieldType_Text() {
        // When: Mapping du type "text"
        String result = mapParameterTypeToFieldTypeViaReflection("text");
        
        // Then: Doit retourner FIELD_TYPE_TEXT
        assertEquals(TemplateConstants.FIELD_TYPE_TEXT, result);
    }

    @Test
    void testMapParameterTypeToFieldType_Unknown() {
        // When: Mapping d'un type inconnu
        String result = mapParameterTypeToFieldTypeViaReflection("unknown");
        
        // Then: Doit retourner FIELD_TYPE_TEXT par défaut
        assertEquals(TemplateConstants.FIELD_TYPE_TEXT, result);
    }

    @Test
    void testMapParameterTypeToFieldType_Null() {
        // When: Mapping d'un type null (via switch, null va dans default)
        // Note: Le switch Java ne gère pas null directement, il va dans le default
        String result = mapParameterTypeToFieldTypeViaReflection(null);
        
        // Then: Doit retourner FIELD_TYPE_TEXT par défaut (via default du switch)
        assertEquals(TemplateConstants.FIELD_TYPE_TEXT, result);
    }

    // ========== Tests pour getSelectExampleValue ==========

    @Test
    void testGetSelectExampleValue_WithOptions() {
        // Given: Champ select avec options
        FormField field = new FormField();
        field.setType(TemplateConstants.FIELD_TYPE_SELECT);
        field.setOptions(List.of("option1", "option2", "option3"));
        
        // When: Récupération de la valeur d'exemple
        Object result = getSelectExampleValueViaReflection(field);
        
        // Then: Doit retourner la première option
        assertEquals("option1", result);
    }

    @Test
    void testGetSelectExampleValue_EmptyOptions() {
        // Given: Champ select sans options
        FormField field = new FormField();
        field.setType(TemplateConstants.FIELD_TYPE_SELECT);
        field.setOptions(Collections.emptyList());
        
        // When: Récupération de la valeur d'exemple
        Object result = getSelectExampleValueViaReflection(field);
        
        // Then: Doit retourner une chaîne vide
        assertEquals("", result);
    }

    @Test
    void testGetSelectExampleValue_NullOptions() {
        // Given: Champ select avec options null
        FormField field = new FormField();
        field.setType(TemplateConstants.FIELD_TYPE_SELECT);
        field.setOptions(null);
        
        // When: Récupération de la valeur d'exemple
        Object result = getSelectExampleValueViaReflection(field);
        
        // Then: Doit retourner une chaîne vide
        assertEquals("", result);
    }

    // ========== Tests pour getTextExampleValue ==========

    @Test
    void testGetTextExampleValue_TicketField() {
        // Given: Champ text avec nom "ticket"
        FormField field = new FormField();
        field.setType(TemplateConstants.FIELD_TYPE_TEXT);
        field.setName(TemplateConstants.TICKET_PARAM);
        
        // When: Récupération de la valeur d'exemple
        Object result = getTextExampleValueViaReflection(field);
        
        // Then: Doit retourner un UUID d'exemple
        assertNotNull(result);
        assertTrue(result instanceof String);
        String uuid = (String) result;
        assertTrue(uuid.contains("-")); // Format UUID
    }

    @Test
    void testGetTextExampleValue_OtherField() {
        // Given: Champ text avec un autre nom
        FormField field = new FormField();
        field.setType(TemplateConstants.FIELD_TYPE_TEXT);
        field.setName("otherField");
        
        // When: Récupération de la valeur d'exemple
        Object result = getTextExampleValueViaReflection(field);
        
        // Then: Doit retourner "example-value"
        assertEquals("example-value", result);
    }

    // ========== Tests pour getExampleValueForJson ==========

    @Test
    void testGetExampleValueForJson_Number() {
        // Given: Champ number
        FormField field = new FormField();
        field.setType(TemplateConstants.FIELD_TYPE_NUMBER);
        
        // When: Récupération de la valeur d'exemple
        Object result = getExampleValueForJsonViaReflection(field);
        
        // Then: Doit retourner 123
        assertEquals(123, result);
    }

    @Test
    void testGetExampleValueForJson_Date() {
        // Given: Champ date
        FormField field = new FormField();
        field.setType(TemplateConstants.FIELD_TYPE_DATE);
        
        // When: Récupération de la valeur d'exemple
        Object result = getExampleValueForJsonViaReflection(field);
        
        // Then: Doit retourner "30/11/25"
        assertEquals("30/11/25", result);
    }

    // ========== Méthodes utilitaires pour les tests via réflexion ==========

    private FormField createTicketFieldViaReflection() {
        try {
            java.lang.reflect.Method method = FormSchemaService.class.getDeclaredMethod("createTicketField");
            method.setAccessible(true);
            return (FormField) method.invoke(formSchemaService);
        } catch (Exception e) {
            fail("Erreur lors de l'appel createTicketField: " + e.getMessage());
            return null;
        }
    }

    private String mapParameterTypeToFieldTypeViaReflection(String paramType) {
        try {
            java.lang.reflect.Method method = FormSchemaService.class.getDeclaredMethod("mapParameterTypeToFieldType", String.class);
            method.setAccessible(true);
            return (String) method.invoke(formSchemaService, paramType);
        } catch (Exception e) {
            fail("Erreur lors de l'appel mapParameterTypeToFieldType: " + e.getMessage());
            return null;
        }
    }

    private Object getSelectExampleValueViaReflection(FormField field) {
        try {
            java.lang.reflect.Method method = FormSchemaService.class.getDeclaredMethod("getSelectExampleValue", FormField.class);
            method.setAccessible(true);
            return method.invoke(formSchemaService, field);
        } catch (Exception e) {
            fail("Erreur lors de l'appel getSelectExampleValue: " + e.getMessage());
            return null;
        }
    }

    private Object getTextExampleValueViaReflection(FormField field) {
        try {
            java.lang.reflect.Method method = FormSchemaService.class.getDeclaredMethod("getTextExampleValue", FormField.class);
            method.setAccessible(true);
            return method.invoke(formSchemaService, field);
        } catch (Exception e) {
            fail("Erreur lors de l'appel getTextExampleValue: " + e.getMessage());
            return null;
        }
    }

    private Object getExampleValueForJsonViaReflection(FormField field) {
        try {
            java.lang.reflect.Method method = FormSchemaService.class.getDeclaredMethod("getExampleValueForJson", FormField.class);
            method.setAccessible(true);
            return method.invoke(formSchemaService, field);
        } catch (Exception e) {
            fail("Erreur lors de l'appel getExampleValueForJson: " + e.getMessage());
            return null;
        }
    }
}


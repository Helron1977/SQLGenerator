package com.sqlgenerator.backend.service;
import com.sqlgenerator.backend.model.FormField;
import com.sqlgenerator.backend.model.FormSchema;
import com.sqlgenerator.backend.model.ParameterDefinition;
import com.sqlgenerator.backend.model.TemplateDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


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

    // ========== Tests pour buildJsonBodyFromFields ==========

    @Test
    void testBuildJsonBodyFromFields_WithFields() {
        // Given: Liste de champs
        List<FormField> fields = new ArrayList<>();
        FormField field1 = new FormField();
        field1.setName("param1");
        field1.setType(TemplateConstants.FIELD_TYPE_TEXT);
        fields.add(field1);
        
        FormField field2 = new FormField();
        field2.setName("param2");
        field2.setType(TemplateConstants.FIELD_TYPE_NUMBER);
        fields.add(field2);

        // When: Construction du JSON body
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) buildJsonBodyFromFieldsViaReflection(fields);

        // Then: Doit contenir les champs (sauf fichiers)
        assertNotNull(result);
        assertTrue(result.containsKey("param1"));
        assertTrue(result.containsKey("param2"));
    }

    @Test
    void testBuildJsonBodyFromFields_WithFileField() {
        // Given: Liste avec un champ fichier
        List<FormField> fields = new ArrayList<>();
        FormField textField = new FormField();
        textField.setName("param1");
        textField.setType(TemplateConstants.FIELD_TYPE_TEXT);
        fields.add(textField);
        
        FormField fileField = new FormField();
        fileField.setName("fileParam");
        fileField.setType(TemplateConstants.FIELD_TYPE_FILE);
        fields.add(fileField);

        // When: Construction du JSON body
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) buildJsonBodyFromFieldsViaReflection(fields);

        // Then: Ne doit pas contenir le champ fichier
        assertNotNull(result);
        assertTrue(result.containsKey("param1"));
        assertFalse(result.containsKey("fileParam"));
    }

    @Test
    void testBuildJsonBodyFromFields_EmptyList() {
        // Given: Liste vide
        List<FormField> fields = new ArrayList<>();

        // When: Construction du JSON body
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) buildJsonBodyFromFieldsViaReflection(fields);

        // Then: Doit retourner une map vide
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testBuildJsonBodyFromFields_NullList() {
        // Given: Liste null
        List<FormField> fields = null;

        // When: Construction du JSON body
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) buildJsonBodyFromFieldsViaReflection(fields);

        // Then: Doit retourner une map vide
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== Tests pour getParameterOrderDescription ==========

    @Test
    void testGetParameterOrderDescription_WithParameters() {
        // Given: Template avec paramètres
        TemplateDefinition template = new TemplateDefinition();
        List<ParameterDefinition> params = new ArrayList<>();
        ParameterDefinition param1 = new ParameterDefinition();
        param1.setName("param1");
        param1.setFile(false);
        params.add(param1);
        ParameterDefinition param2 = new ParameterDefinition();
        param2.setName("param2");
        param2.setFile(false);
        params.add(param2);
        template.setParameters(params);

        // When: Récupération de la description
        String result = getParameterOrderDescriptionViaReflection(template);

        // Then: Doit retourner les noms séparés par des virgules
        assertNotNull(result);
        assertTrue(result.contains("param1"));
        assertTrue(result.contains("param2"));
        assertTrue(result.contains(","));
    }

    @Test
    void testGetParameterOrderDescription_WithFileParameter() {
        // Given: Template avec paramètre fichier
        TemplateDefinition template = new TemplateDefinition();
        List<ParameterDefinition> params = new ArrayList<>();
        ParameterDefinition textParam = new ParameterDefinition();
        textParam.setName("param1");
        textParam.setFile(false);
        params.add(textParam);
        ParameterDefinition fileParam = new ParameterDefinition();
        fileParam.setName("fileParam");
        fileParam.setFile(true);
        params.add(fileParam);
        template.setParameters(params);

        // When: Récupération de la description
        String result = getParameterOrderDescriptionViaReflection(template);

        // Then: Ne doit pas contenir le paramètre fichier
        assertNotNull(result);
        assertTrue(result.contains("param1"));
        assertFalse(result.contains("fileParam"));
    }

    @Test
    void testGetParameterOrderDescription_EmptyParameters() {
        // Given: Template sans paramètres
        TemplateDefinition template = new TemplateDefinition();
        template.setParameters(new ArrayList<>());

        // When: Récupération de la description
        String result = getParameterOrderDescriptionViaReflection(template);

        // Then: Doit retourner "Aucun paramètre"
        assertEquals("Aucun paramètre", result);
    }

    @Test
    void testGetParameterOrderDescription_NullParameters() {
        // Given: Template avec paramètres null
        TemplateDefinition template = new TemplateDefinition();
        template.setParameters(null);

        // When: Récupération de la description
        String result = getParameterOrderDescriptionViaReflection(template);

        // Then: Doit retourner "Aucun paramètre"
        assertEquals("Aucun paramètre", result);
    }

    // ========== Méthodes utilitaires pour les tests ==========

    private Object buildJsonBodyFromFieldsViaReflection(List<FormField> fields) {
        try {
            java.lang.reflect.Method method = FormSchemaService.class.getDeclaredMethod("buildJsonBodyFromFields", 
                List.class);
            method.setAccessible(true);
            return method.invoke(formSchemaService, fields);
        } catch (Exception e) {
            fail("Erreur lors de l'appel buildJsonBodyFromFields: " + e.getMessage());
            return null;
        }
    }

    private String getParameterOrderDescriptionViaReflection(TemplateDefinition template) {
        try {
            java.lang.reflect.Method method = FormSchemaService.class.getDeclaredMethod("getParameterOrderDescription", 
                TemplateDefinition.class);
            method.setAccessible(true);
            return (String) method.invoke(formSchemaService, template);
        } catch (Exception e) {
            fail("Erreur lors de l'appel getParameterOrderDescription: " + e.getMessage());
            return null;
        }
    }

    // ========== Tests pour getAllFormSchemas (branches manquantes) ==========

    @Test
    void testGetAllFormSchemas_WithNullTemplates() {
        // Given: TemplateService retourne null
        when(templateService.getAllTemplates()).thenReturn(null);

        // When: Récupération de tous les schémas
        List<FormSchema> result = formSchemaService.getAllFormSchemas();

        // Then: Doit retourner une liste vide
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllFormSchemas_WithEmptyTemplates() {
        // Given: TemplateService retourne une liste vide
        when(templateService.getAllTemplates()).thenReturn(new ArrayList<>());

        // When: Récupération de tous les schémas
        List<FormSchema> result = formSchemaService.getAllFormSchemas();

        // Then: Doit retourner une liste vide
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== Tests pour getFormSchema (branches manquantes) ==========

    @Test
    void testGetFormSchema_WithNullTemplate() {
        // Given: Template n'existe pas
        when(templateService.getTemplateById("non-existent")).thenReturn(null);

        // When: Récupération du schéma
        FormSchema result = formSchemaService.getFormSchema("non-existent");

        // Then: Doit retourner null
        assertNull(result);
    }

    // ========== Tests pour getUnitBodyStructure (branches manquantes) ==========

    @Test
    void testGetUnitBodyStructure_WithNullFormSchema() {
        // Given: Template n'existe pas
        when(templateService.getTemplateById("non-existent")).thenReturn(null);

        // When: Récupération de la structure
        Map<String, Object> result = formSchemaService.getUnitBodyStructure("non-existent");

        // Then: Doit retourner null
        assertNull(result);
    }

    // ========== Tests pour getMassBodyStructure (branches manquantes) ==========

    @Test
    void testGetMassBodyStructure_WithNullFormSchema() {
        // Given: Template n'existe pas
        when(templateService.getTemplateById("non-existent")).thenReturn(null);

        // When: Récupération de la structure
        Map<String, Object> result = formSchemaService.getMassBodyStructure("non-existent");

        // Then: Doit retourner null
        assertNull(result);
    }

    @Test
    void testGetMassBodyStructure_WithNullMassFields() {
        // Given: Template avec IN (pas de mode masse)
        TemplateDefinition template = new TemplateDefinition();
        template.setId("test-template");
        ParameterDefinition fileParam = new ParameterDefinition();
        fileParam.setName("ids");
        fileParam.setFile(true);
        template.setParameters(List.of(fileParam));
        
        when(templateService.getTemplateById("test-template")).thenReturn(template);

        // When: Récupération de la structure masse
        Map<String, Object> result = formSchemaService.getMassBodyStructure("test-template");

        // Then: Doit retourner null (mode masse non disponible)
        assertNull(result);
    }

    // ========== Tests pour buildJsonBodyFromFields (branches manquantes) ==========

    @Test
    void testBuildJsonBodyFromFields_WithNullField() {
        // Given: Liste avec un champ null
        List<FormField> fields = new ArrayList<>();
        fields.add(null);
        FormField validField = new FormField();
        validField.setName("param1");
        validField.setType(TemplateConstants.FIELD_TYPE_TEXT);
        fields.add(validField);

        // When: Construction du JSON body
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) buildJsonBodyFromFieldsViaReflection(fields);

        // Then: Ne doit pas contenir le champ null
        assertNotNull(result);
        assertTrue(result.containsKey("param1"));
        assertEquals(1, result.size());
    }

    @Test
    void testBuildJsonBodyFromFields_WithFieldNullName() {
        // Given: Liste avec un champ ayant un nom null
        List<FormField> fields = new ArrayList<>();
        FormField fieldWithNullName = new FormField();
        fieldWithNullName.setName(null);
        fieldWithNullName.setType(TemplateConstants.FIELD_TYPE_TEXT);
        fields.add(fieldWithNullName);
        FormField validField = new FormField();
        validField.setName("param1");
        validField.setType(TemplateConstants.FIELD_TYPE_TEXT);
        fields.add(validField);

        // When: Construction du JSON body
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) buildJsonBodyFromFieldsViaReflection(fields);

        // Then: Ne doit pas contenir le champ avec nom null
        assertNotNull(result);
        assertTrue(result.containsKey("param1"));
        assertEquals(1, result.size());
    }

    // ========== Tests pour getExampleValueForJson (branches manquantes) ==========

    @Test
    void testGetExampleValueForJson_WithSelectField() {
        // Given: Champ de type select avec options
        FormField selectField = new FormField();
        selectField.setName("status");
        selectField.setType(TemplateConstants.FIELD_TYPE_SELECT);
        selectField.setOptions(List.of("active", "inactive", "pending"));

        // When: Récupération de la valeur d'exemple
        Object result = getExampleValueForJsonViaReflection(selectField);

        // Then: Doit retourner une des options
        assertNotNull(result);
        assertTrue(result instanceof String);
        assertTrue(selectField.getOptions().contains(result));
    }

    // ========== Tests pour buildFormSchema (branches manquantes) ==========

    @Test
    void testBuildFormSchema_WithNullName() {
        // Given: Template avec name null
        TemplateDefinition template = new TemplateDefinition();
        template.setId("test-template");
        template.setName(null);
        template.setParameters(new ArrayList<>());
        
        when(templateService.getTemplateById("test-template")).thenReturn(template);

        // When: Construction du schéma
        FormSchema result = formSchemaService.getFormSchema("test-template");

        // Then: Doit utiliser l'ID comme nom
        assertNotNull(result);
        assertEquals("test-template", result.getName());
    }

    @Test
    void testBuildFormSchema_WithNullTags() {
        // Given: Template avec tags null
        TemplateDefinition template = new TemplateDefinition();
        template.setId("test-template");
        template.setTags(null);
        template.setParameters(new ArrayList<>());
        
        when(templateService.getTemplateById("test-template")).thenReturn(template);

        // When: Construction du schéma
        FormSchema result = formSchemaService.getFormSchema("test-template");

        // Then: Doit avoir une liste vide de tags
        assertNotNull(result);
        assertNotNull(result.getTags());
        assertTrue(result.getTags().isEmpty());
    }

    // ========== Tests pour hasInParameter (branches manquantes) ==========

    @Test
    void testHasInParameter_WithNullParameters() {
        // Given: Template avec parameters null
        TemplateDefinition template = new TemplateDefinition();
        template.setParameters(null);

        // When: Vérification
        boolean result = hasInParameterViaReflection(template);

        // Then: Doit retourner false
        assertFalse(result);
    }

    // ========== Méthodes utilitaires supplémentaires ==========

    private boolean hasInParameterViaReflection(TemplateDefinition template) {
        try {
            java.lang.reflect.Method method = FormSchemaService.class.getDeclaredMethod("hasInParameter", 
                TemplateDefinition.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(formSchemaService, template);
        } catch (Exception e) {
            fail("Erreur lors de l'appel hasInParameter: " + e.getMessage());
            return false;
        }
    }
}


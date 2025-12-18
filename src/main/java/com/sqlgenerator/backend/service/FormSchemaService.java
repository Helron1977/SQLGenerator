package com.sqlgenerator.backend.service;

import com.sqlgenerator.backend.model.FormField;
import com.sqlgenerator.backend.model.FormSchema;
import com.sqlgenerator.backend.model.ParameterDefinition;
import com.sqlgenerator.backend.model.TemplateDefinition;
import com.sqlgenerator.backend.model.RequestBodySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsable de la construction des schémas de formulaires
 * à partir des TemplateDefinition (templates SQL).
 *
 * Ce service encapsule les règles métier déjà présentes pour Swagger
 * (ticket, executionType, IN, mode masse) mais de manière indépendante
 * d'OpenAPI, pour alimenter un front custom.
 */
@Service
public class FormSchemaService {

    @Autowired
    private TemplateService templateService;

    /**
     * Retourne les schémas de formulaires pour tous les templates disponibles.
     * 
     * Les templates marqués comme "hidden" (templates de test) sont exclus de cette liste.
     */
    public List<FormSchema> getAllFormSchemas() {
        List<TemplateDefinition> templates = templateService.getAllTemplates();
        if (templates == null || templates.isEmpty()) {
            return Collections.emptyList();
        }

        return templates.stream()
                .filter(template -> !template.isHidden()) // Exclure les templates cachés
                .map(this::buildFormSchema)
                .collect(Collectors.toList());
    }

    /**
     * Retourne le schéma de formulaire pour un template donné.
     *
     * @param id identifiant du template
     * @return FormSchema ou null si non trouvé
     */
    public FormSchema getFormSchema(String id) {
        TemplateDefinition template = templateService.getTemplateById(id);
        return template != null ? buildFormSchema(template) : null;
    }

    /**
     * Retourne la structure JSON du body attendu pour un template donné.
     * Contient les clés avec des valeurs d'exemple/vides pour construire le formulaire.
     *
     * @param id identifiant du template
     * @return RequestBodySchema ou null si non trouvé
     */
    /**
     * Retourne la structure JSON pour le mode unitaire (prêt à copier-coller).
     */
    public Map<String, Object> getUnitBodyStructure(String id) {
        FormSchema formSchema = getFormSchema(id);
        if (formSchema == null) {
            return null;
        }
        return buildJsonBodyFromFields(formSchema.getUnitFields());
    }

    /**
     * Retourne la structure JSON pour le mode masse (prêt à copier-coller).
     * Retourne null si le mode masse n'est pas disponible pour ce template.
     */
    public Map<String, Object> getMassBodyStructure(String id) {
        FormSchema formSchema = getFormSchema(id);
        if (formSchema == null) {
            return null;
        }
        
        // Vérifier que le mode masse est disponible
        if (formSchema.getMassFields() == null || formSchema.getMassFields().isEmpty()) {
            return null; // Mode masse non disponible (template avec IN)
        }
        
        return buildJsonBodyFromFields(formSchema.getMassFields());
    }

    /**
     * @deprecated Utiliser getUnitBodyStructure() ou getMassBodyStructure() à la place.
     */
    @Deprecated
    public RequestBodySchema getRequestBodySchema(String id) {
        FormSchema formSchema = getFormSchema(id);
        if (formSchema == null) {
            return null;
        }

        RequestBodySchema bodySchema = new RequestBodySchema();
        bodySchema.setTemplateId(formSchema.getTemplateId());
        bodySchema.setName(formSchema.getName());
        bodySchema.setDescription(formSchema.getDescription());
        bodySchema.setMassModeAvailable(formSchema.getMassFields() != null && !formSchema.getMassFields().isEmpty());

        // Structure pour le mode unitaire (JSON prêt à copier-coller)
        bodySchema.setUnitBodyStructure(buildJsonBodyFromFields(formSchema.getUnitFields()));

        // Structure pour le mode masse (JSON prêt à copier-coller, si disponible)
        if (bodySchema.isMassModeAvailable()) {
            bodySchema.setMassBodyStructure(buildJsonBodyFromFields(formSchema.getMassFields()));
        } else {
            bodySchema.setMassBodyStructure(null);
        }

        return bodySchema;
    }

    /**
     * Construit un JSON (Map) prêt à être copié-collé dans le body de POST /api/patch/{id}.
     * Les valeurs sont des exemples réalistes.
     * 
     * Note : Les fichiers (type "file") sont exclus du JSON car ils doivent être uploadés via multipart.
     */
    private Map<String, Object> buildJsonBodyFromFields(List<FormField> fields) {
        Map<String, Object> jsonBody = new HashMap<>();

        if (fields == null || fields.isEmpty()) {
            return jsonBody;
        }

        for (FormField field : fields) {
            if (field == null || field.getName() == null) {
                continue;
            }

            // Exclure les fichiers du JSON (ils doivent être uploadés via multipart, pas dans le JSON)
            if (TemplateConstants.FIELD_TYPE_FILE.equals(field.getType())) {
                continue;
            }

            Object value = getExampleValueForJson(field);
            jsonBody.put(field.getName(), value);
        }

        return jsonBody;
    }

    /**
     * Retourne une valeur d'exemple réaliste pour un champ dans un JSON.
     */
    private Object getExampleValueForJson(FormField field) {
        String fieldType = field.getType();
        
        if (TemplateConstants.FIELD_TYPE_SELECT.equals(fieldType)) {
            return getSelectExampleValue(field);
        }
        
        if (TemplateConstants.FIELD_TYPE_NUMBER.equals(fieldType)) {
            return 123;
        }
        
        if (TemplateConstants.FIELD_TYPE_DATE.equals(fieldType)) {
            return "30/11/25";
        }
        
        // Type text ou par défaut
        return getTextExampleValue(field);
    }

    /**
     * Retourne une valeur d'exemple pour un champ de type select.
     * 
     * @param field Champ de type select
     * @return Première option disponible, ou chaîne vide
     */
    private Object getSelectExampleValue(FormField field) {
        if (field.getOptions() != null && !field.getOptions().isEmpty()) {
            return field.getOptions().get(0);
        }
        return "";
    }

    /**
     * Retourne une valeur d'exemple pour un champ de type text.
     * 
     * @param field Champ de type text
     * @return Valeur d'exemple selon le nom du champ
     */
    private Object getTextExampleValue(FormField field) {
        if (TemplateConstants.TICKET_PARAM.equals(field.getName())) {
            return "dc905fff-27a6-452f-aa0d-360c6c37b94a";
        }
        return "example-value";
    }

    /**
     * Construit le schéma de formulaire pour un template.
     */
    private FormSchema buildFormSchema(TemplateDefinition template) {
        FormSchema schema = new FormSchema();
        schema.setTemplateId(template.getId());
        schema.setName(template.getName() != null ? template.getName() : template.getId());
        schema.setDescription(template.getDescription());
        schema.setTags(template.getTags() != null ? template.getTags() : Collections.emptyList());
        
        boolean hasInParameter = hasInParameter(template);
        schema.setHasInParameter(hasInParameter);

        // Modes supportés
        List<String> modes = new ArrayList<>();
        modes.add(TemplateConstants.EXECUTION_TYPE_UNITAIRE);
        if (!hasInParameter) {
            modes.add(TemplateConstants.EXECUTION_TYPE_MASSE);
        }
        schema.setModes(modes);

        // Champs pour le mode unitaire
        schema.setUnitFields(buildUnitFields(template, hasInParameter));

        // Champs pour le mode masse (uniquement si pas de IN)
        if (!hasInParameter) {
            schema.setMassFields(buildMassFields(template));
        } else {
            schema.setMassFields(null);
        }

        return schema;
    }

    private boolean hasInParameter(TemplateDefinition query) {
        return query.getParameters() != null &&
                query.getParameters().stream().anyMatch(ParameterDefinition::isFile);
    }

    /**
     * Construit les champs pour le mode unitaire.
     * Contient :
     * - ticket (obligatoire)
     * - executionType (select : unitaire ou unitaire/masse selon hasInParameter)
     * - tous les paramètres du template (y compris les fichiers pour IN)
     */
    private List<FormField> buildUnitFields(TemplateDefinition template, boolean hasInParameter) {
        List<FormField> fields = new ArrayList<>();

        // Champ ticket
        fields.add(createTicketField());

        // Champ executionType
        FormField executionType = new FormField();
        executionType.setName(TemplateConstants.EXECUTION_TYPE_PARAM);
        executionType.setType(TemplateConstants.FIELD_TYPE_SELECT);
        executionType.setLabel("Type d'exécution");
        executionType.setRequired(true);
        // Champ purement technique : piloté par le front (switch de mode), non affiché en tant que champ
        executionType.setTechnical(true);

        List<String> options = new ArrayList<>();
        options.add(TemplateConstants.EXECUTION_TYPE_UNITAIRE);
        if (!hasInParameter) {
            // Pour les requêtes sans IN, on propose aussi le mode masse
            options.add(TemplateConstants.EXECUTION_TYPE_MASSE);
        }
        executionType.setOptions(options);
        executionType.setHelpText("Choisissez 'unitaire' pour une exécution simple, 'masse' pour un fichier CSV.");
        fields.add(executionType);

        // Paramètres du template
        if (template.getParameters() != null) {
            for (ParameterDefinition param : template.getParameters()) {
                if (param == null || param.getName() == null) {
                    continue;
                }
                fields.add(buildFieldFromParameter(param));
            }
        }

        return fields;
    }

    /**
     * Construit les champs pour le mode masse.
     * Contient :
     * - ticket
     * - masseFile (CSV obligatoire)
     */
    private List<FormField> buildMassFields(TemplateDefinition template) {
        List<FormField> fields = new ArrayList<>();

        // ticket (même champ que pour le mode unitaire)
        fields.add(createTicketField());

        // masseFile
        FormField masseFile = new FormField();
        masseFile.setName(TemplateConstants.MASSE_FILE_PARAM);
        masseFile.setType(TemplateConstants.FIELD_TYPE_FILE);
        masseFile.setLabel("Fichier CSV (mode masse)");
        masseFile.setRequired(true);

        StringBuilder help = new StringBuilder();
        help.append("Fichier CSV avec une ligne par requête, valeurs séparées par virgule dans l'ordre des paramètres : ");
        help.append(getParameterOrderDescription(template));

        masseFile.setHelpText(help.toString());
        fields.add(masseFile);

        return fields;
    }

    /**
     * Crée le champ "ticket" standard utilisé dans tous les formulaires.
     * 
     * @return FormField configuré pour le ticket
     */
    private FormField createTicketField() {
        FormField ticket = new FormField();
        ticket.setName(TemplateConstants.TICKET_PARAM);
        ticket.setType(TemplateConstants.FIELD_TYPE_TEXT);
        ticket.setLabel("Numéro du ticket");
        ticket.setRequired(true);
        ticket.setHelpText("Identifiant fonctionnel du ticket (ex: JIRA, UUID, ...)");
        return ticket;
    }

    /**
     * Construit un FormField à partir d'un ParameterDefinition.
     */
    private FormField buildFieldFromParameter(ParameterDefinition param) {
        FormField field = new FormField();
        field.setName(param.getName());
        field.setRequired(param.isRequired());
        field.setLabel(param.getLabel() != null ? param.getLabel() : param.getName());

        if (param.isFile()) {
            field.setType(TemplateConstants.FIELD_TYPE_FILE);
            field.setHelpText("Fichier texte, 1 valeur par ligne (utilisé pour les clauses IN).");
        } else {
            String type = param.getType() != null ? param.getType().toLowerCase() : TemplateConstants.FIELD_TYPE_TEXT;
            field.setType(mapParameterTypeToFieldType(type));
            if (TemplateConstants.FIELD_TYPE_DATE.equals(field.getType())) {
                field.setHelpText("Format attendu : " + TemplateConstants.DATE_FORMAT + " (ex: 30/11/25)");
            }
        }

        return field;
    }

    /**
     * Mappe un type de paramètre SQL vers un type de champ formulaire.
     * 
     * @param paramType Type du paramètre (number, integer, date, text, etc.)
     * @return Type de champ formulaire correspondant
     */
    private String mapParameterTypeToFieldType(String paramType) {
        if (paramType == null) {
            return TemplateConstants.FIELD_TYPE_TEXT;
        }
        
        switch (paramType) {
            case "number":
            case "integer":
                return TemplateConstants.FIELD_TYPE_NUMBER;
            case "date":
                return TemplateConstants.FIELD_TYPE_DATE;
            default:
                return TemplateConstants.FIELD_TYPE_TEXT;
        }
    }

    /**
     * Génère une description de l'ordre des paramètres pour le mode masse.
     * Reprend la logique déjà utilisée pour Swagger.
     */
    private String getParameterOrderDescription(TemplateDefinition query) {
        if (query.getParameters() == null || query.getParameters().isEmpty()) {
            return "Aucun paramètre";
        }

        List<String> paramNames = query.getParameters().stream()
                .filter(p -> !p.isFile())
                .map(ParameterDefinition::getName)
                .collect(Collectors.toList());

        return String.join(", ", paramNames);
    }
}



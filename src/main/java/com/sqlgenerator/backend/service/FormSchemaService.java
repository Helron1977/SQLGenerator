package com.sqlgenerator.backend.service;

import com.sqlgenerator.backend.model.FormField;
import com.sqlgenerator.backend.model.FormSchema;
import com.sqlgenerator.backend.model.ParameterDefinition;
import com.sqlgenerator.backend.model.QueryDefinition;
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
 * à partir des QueryDefinition.
 *
 * Ce service encapsule les règles métier déjà présentes pour Swagger
 * (ticket, executionType, IN, mode masse) mais de manière indépendante
 * d'OpenAPI, pour alimenter un front custom.
 */
@Service
public class FormSchemaService {

    @Autowired
    private QueryService queryService;

    /**
     * Retourne les schémas de formulaires pour toutes les queries disponibles.
     */
    public List<FormSchema> getAllFormSchemas() {
        List<QueryDefinition> queries = queryService.getAllQueries();
        if (queries == null || queries.isEmpty()) {
            return Collections.emptyList();
        }

        return queries.stream()
                .map(this::buildFormSchema)
                .collect(Collectors.toList());
    }

    /**
     * Retourne le schéma de formulaire pour une query donnée.
     *
     * @param id identifiant de la query
     * @return FormSchema ou null si non trouvée
     */
    public FormSchema getFormSchema(String id) {
        QueryDefinition query = queryService.getQueryById(id);
        return query != null ? buildFormSchema(query) : null;
    }

    /**
     * Retourne la structure JSON du body attendu pour une query donnée.
     * Contient les clés avec des valeurs d'exemple/vides pour construire le formulaire.
     *
     * @param id identifiant de la query
     * @return RequestBodySchema ou null si non trouvée
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
     * Retourne null si le mode masse n'est pas disponible pour cette query.
     */
    public Map<String, Object> getMassBodyStructure(String id) {
        FormSchema formSchema = getFormSchema(id);
        if (formSchema == null) {
            return null;
        }
        
        // Vérifier que le mode masse est disponible
        if (formSchema.getMassFields() == null || formSchema.getMassFields().isEmpty()) {
            return null; // Mode masse non disponible (query avec IN)
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
        bodySchema.setQueryId(formSchema.getQueryId());
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
            if ("file".equals(field.getType())) {
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
        switch (field.getType()) {
            case "select":
                if (field.getOptions() != null && !field.getOptions().isEmpty()) {
                    return field.getOptions().get(0);
                }
                return "";
            case "number":
            case "integer":
                return 123;
            case "date":
                return "30/11/25";
            case "text":
            default:
                // Valeur d'exemple selon le nom du champ
                if ("ticket".equals(field.getName())) {
                    return "dc905fff-27a6-452f-aa0d-360c6c37b94a";
                }
                return "example-value";
        }
    }

    /**
     * Construit un exemple de body formaté en application/x-www-form-urlencoded.
     * Format : "ticket=xxx&executionType=unitaire&param1=value1&param2=value2"
     */
    private String buildFormUrlEncodedExample(List<FormField> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }

        List<String> params = new ArrayList<>();
        for (FormField field : fields) {
            if (field == null || field.getName() == null) {
                continue;
            }

            // Les fichiers ne sont pas dans form-urlencoded, ils sont en multipart
            if ("file".equals(field.getType())) {
                continue;
            }

            String value = getExampleValueForFormUrlEncoded(field);
            params.add(field.getName() + "=" + value);
        }

        return String.join("&", params);
    }

    /**
     * Retourne une valeur d'exemple formatée pour form-urlencoded.
     */
    private String getExampleValueForFormUrlEncoded(FormField field) {
        switch (field.getType()) {
            case "select":
                if (field.getOptions() != null && !field.getOptions().isEmpty()) {
                    return field.getOptions().get(0);
                }
                return "";
            case "number":
            case "integer":
                return "123";
            case "date":
                return "30/11/25";
            case "text":
            default:
                // Valeur d'exemple selon le nom du champ
                if ("ticket".equals(field.getName())) {
                    return "dc905fff-27a6-452f-aa0d-360c6c37b94a";
                }
                return "example-value";
        }
    }

    /**
     * Construit une structure JSON (Map) à partir d'une liste de FormField.
     * Les valeurs sont vides ou des exemples selon le type.
     */
    private Map<String, Object> buildBodyStructureFromFields(List<FormField> fields) {
        Map<String, Object> structure = new HashMap<>();

        if (fields == null || fields.isEmpty()) {
            return structure;
        }

        for (FormField field : fields) {
            if (field == null || field.getName() == null) {
                continue;
            }

            Object exampleValue = getExampleValueForField(field);
            structure.put(field.getName(), exampleValue);
        }

        return structure;
    }

    /**
     * Retourne une valeur d'exemple pour un champ selon son type.
     */
    private Object getExampleValueForField(FormField field) {
        switch (field.getType()) {
            case "file":
                return null; // Les fichiers ne sont pas dans le JSON, ils sont uploadés séparément
            case "select":
                // Retourner la première option ou null
                if (field.getOptions() != null && !field.getOptions().isEmpty()) {
                    return field.getOptions().get(0);
                }
                return null;
            case "number":
            case "integer":
                return 0;
            case "date":
                return "30/11/25"; // Format exemple
            case "text":
            default:
                return ""; // Chaîne vide pour les champs texte
        }
    }

    /**
     * Construit le schéma de formulaire pour une query.
     */
    private FormSchema buildFormSchema(QueryDefinition query) {
        FormSchema schema = new FormSchema();
        schema.setQueryId(query.getId());
        schema.setName(query.getName() != null ? query.getName() : query.getId());
        schema.setDescription(query.getDescription());
        schema.setTags(query.getTags() != null ? query.getTags() : Collections.emptyList());

        boolean hasInParameter = hasInParameter(query);
        schema.setHasInParameter(hasInParameter);

        // Modes supportés
        List<String> modes = new ArrayList<>();
        modes.add(QueryConstants.EXECUTION_TYPE_UNITAIRE);
        if (!hasInParameter) {
            modes.add(QueryConstants.EXECUTION_TYPE_MASSE);
        }
        schema.setModes(modes);

        // Champs pour le mode unitaire
        schema.setUnitFields(buildUnitFields(query, hasInParameter));

        // Champs pour le mode masse (uniquement si pas de IN)
        if (!hasInParameter) {
            schema.setMassFields(buildMassFields(query));
        } else {
            schema.setMassFields(null);
        }

        return schema;
    }

    private boolean hasInParameter(QueryDefinition query) {
        return query.getParameters() != null &&
                query.getParameters().stream().anyMatch(ParameterDefinition::isFile);
    }

    /**
     * Construit les champs pour le mode unitaire.
     * Contient :
     * - ticket (obligatoire)
     * - executionType (select : unitaire ou unitaire/masse selon hasInParameter)
     * - tous les paramètres de la query (y compris les fichiers pour IN)
     */
    private List<FormField> buildUnitFields(QueryDefinition query, boolean hasInParameter) {
        List<FormField> fields = new ArrayList<>();

        // Champ ticket
        FormField ticket = new FormField();
        ticket.setName("ticket");
        ticket.setType("text");
        ticket.setLabel("Numéro du ticket");
        ticket.setRequired(true);
        ticket.setHelpText("Identifiant fonctionnel du ticket (ex: JIRA, UUID, ...)");
        fields.add(ticket);

        // Champ executionType
        FormField executionType = new FormField();
        executionType.setName("executionType");
        executionType.setType("select");
        executionType.setLabel("Type d'exécution");
        executionType.setRequired(true);

        List<String> options = new ArrayList<>();
        options.add(QueryConstants.EXECUTION_TYPE_UNITAIRE);
        if (!hasInParameter) {
            // Pour les requêtes sans IN, on propose aussi le mode masse
            options.add(QueryConstants.EXECUTION_TYPE_MASSE);
        }
        executionType.setOptions(options);
        executionType.setHelpText("Choisissez 'unitaire' pour une exécution simple, 'masse' pour un fichier CSV.");
        fields.add(executionType);

        // Paramètres de la query
        if (query.getParameters() != null) {
            for (ParameterDefinition param : query.getParameters()) {
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
    private List<FormField> buildMassFields(QueryDefinition query) {
        List<FormField> fields = new ArrayList<>();

        // ticket (même champ que pour le mode unitaire)
        FormField ticket = new FormField();
        ticket.setName("ticket");
        ticket.setType("text");
        ticket.setLabel("Numéro du ticket");
        ticket.setRequired(true);
        ticket.setHelpText("Identifiant fonctionnel du ticket (ex: JIRA, UUID, ...)");
        fields.add(ticket);

        // masseFile
        FormField masseFile = new FormField();
        masseFile.setName("masseFile");
        masseFile.setType("file");
        masseFile.setLabel("Fichier CSV (mode masse)");
        masseFile.setRequired(true);

        StringBuilder help = new StringBuilder();
        help.append("Fichier CSV avec une ligne par requête, valeurs séparées par virgule dans l'ordre des paramètres : ");
        help.append(getParameterOrderDescription(query));

        masseFile.setHelpText(help.toString());
        fields.add(masseFile);

        return fields;
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
            field.setType("file");
            field.setHelpText("Fichier texte, 1 valeur par ligne (utilisé pour les clauses IN).");
        } else {
            String type = param.getType() != null ? param.getType().toLowerCase() : "text";
            switch (type) {
                case "number":
                case "integer":
                    field.setType("number");
                    break;
                case "date":
                    field.setType("date");
                    field.setHelpText("Format attendu : " + QueryConstants.DATE_FORMAT + " (ex: 30/11/25)");
                    break;
                default:
                    field.setType("text");
            }
        }

        return field;
    }

    /**
     * Génère une description de l'ordre des paramètres pour le mode masse.
     * Reprend la logique déjà utilisée pour Swagger.
     */
    private String getParameterOrderDescription(QueryDefinition query) {
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


